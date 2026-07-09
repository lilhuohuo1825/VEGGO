import os
import shutil

original_green_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783360222161.jpg"
output_green_path = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable/ic_veggopay_green.png"

if os.path.exists(original_green_path):
    shutil.copy(original_green_path, output_green_path)
    print("Successfully copied original green-background logo to:", output_green_path)
else:
    print("Original green logo not found at:", original_green_path)
