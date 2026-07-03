from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
import pandas as pd
import numpy as np
import joblib
import os
import json

app = FastAPI(title="VEGGO Price Forecasting AI Service")

# Cấu hình đường dẫn lưu model
MODEL_DIR = os.path.dirname(os.path.abspath(__file__))

# Từ điển lưu trữ các mô hình XGBoost của từng nhóm sản phẩm
xgb_models: Dict[str, Any] = {}
xgb_features: Dict[str, List[str]] = {}

# Danh sách các nhóm nông sản chính thức có model
CROP_GROUPS = ['leafy_vegetable', 'root_vegetable', 'tree_fruit']

# Nạp trước các mô hình XGBoost tốt nhất khi khởi động server
for group in CROP_GROUPS:
    model_path = os.path.join(MODEL_DIR, f"model_group_{group}_best.pkl")
    features_path = os.path.join(MODEL_DIR, f"features_group_{group}_best.json")
    
    if os.path.exists(model_path) and os.path.exists(features_path):
        try:
            xgb_models[group] = joblib.load(model_path)
            with open(features_path, 'r') as f:
                xgb_features[group] = json.load(f)
            print(f"✓ Nạp mô hình XGBoost cho nhóm {group} thành công.")
        except Exception as e:
            print(f"X Lỗi nạp mô hình nhóm {group}: {e}")
    else:
        print(f"Lưu ý: Thiếu mô hình hoặc cột của nhóm {group} tại thư mục.")

# --- ĐỊNH NGHĨA STRUCT DỮ LIỆU ĐẦU VÀO API ---
class DailyRecord(BaseModel):
    temp: float
    humidity: float
    precip: float
    solarradiation: float
    is_main_season: int
    is_tet_season: int
    price_ratio_lag_7: float
    month: int
    day_of_year: int

class ForecastRequest(BaseModel):
    crop_group: str          # e.g., 'tree_fruit' hoặc 'leafy_vegetable'
    commodity_name: str      # e.g., 'Mangoes, guavas and mangosteens'
    current_price: float     # Giá của ngày hôm nay (VNĐ/kg)
    history_7_days: List[DailyRecord]

@app.post("/predict")
def predict_price_ratio(request: ForecastRequest):
    group = request.crop_group
    
    # 1. Kiểm tra mô hình nhóm
    if group not in xgb_models:
        raise HTTPException(status_code=400, detail=f"Nhóm nông sản '{group}' chưa được hỗ trợ mô hình dự báo hoặc file model .pkl đang bị thiếu trên server.")
        
    try:
        # Lấy bản ghi ngày hôm nay (ngày cuối cùng trong list)
        today_data = request.history_7_days[-1]
        
        # Tạo dữ liệu dạng DataFrame cho XGBoost
        input_dict = {
            'price_ratio_lag_7': [today_data.price_ratio_lag_7],
            'temp': [today_data.temp],
            'humidity': [today_data.humidity],
            'precip': [today_data.precip],
            'solarradiation': [today_data.solarradiation],
            'is_main_season': [today_data.is_main_season],
            'is_tet_season': [today_data.is_tet_season],
            'month': [today_data.month],
            'day_of_year': [today_data.day_of_year]
        }
        
        df_input = pd.DataFrame(input_dict)
        
        # Thiết lập cột One-Hot encoding sản phẩm
        expected_cols = xgb_features[group]
        for col in expected_cols:
            if col not in df_input.columns:
                if col.startswith("Item_") and col == f"Item_{request.commodity_name}":
                    df_input[col] = 1
                else:
                    df_input[col] = 0
                    
        # Sắp xếp đúng thứ tự cột
        df_input = df_input[expected_cols]
        
        # 2. Dự báo bằng mô hình XGBoost (Đã chứng minh độ tin cậy R² = 0.58 vượt trội)
        xgb_model = xgb_models[group]
        final_ratio = float(xgb_model.predict(df_input)[0])
        
        # --- BƯỚC D: QUÉT TÌM NGÀY BIẾN ĐỘNG SỚM NHẤT (Từ 2 đến 7 ngày) ---
        target_day = 7
        target_ratio = final_ratio
        target_change_percent = (final_ratio - 1.0) * 100
        target_trend = "stable"
        
        # Tỉ lệ biến động lũy tiến mỗi ngày: r = R^(1/7)
        # Giới hạn tỉ lệ tránh số âm/lỗi toán học
        final_ratio_clipped = max(0.1, final_ratio)
        daily_rate = final_ratio_clipped ** (1/7)
        
        # Quét từ ngày 2 đến ngày 7 để tìm ngày đầu tiên biến động vượt ngưỡng 10%
        for d in range(2, 8):
            ratio_d = daily_rate ** d
            change_d = (ratio_d - 1.0) * 100
            
            if change_d >= 10.0 or change_d <= -10.0:
                target_day = d
                target_ratio = ratio_d
                target_change_percent = change_d
                target_trend = "up" if change_d >= 10.0 else "down"
                break
                
        # Tính giá tiền thực tế
        predicted_price = round(request.current_price * target_ratio, -2)
        
        return {
            "predicted_ratio": round(target_ratio, 4),
            "predicted_price_per_kg": predicted_price,
            "change_percent": round(target_change_percent, 2),
            "trend": target_trend,
            "days_until_impact": target_day
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống khi dự báo: {str(e)}")

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "loaded_xgb_groups": list(xgb_models.keys())
    }
