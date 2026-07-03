# VEGGO Speech-to-Text (Module `speech`)

Tài liệu hướng dẫn module **nhận diện giọng nói** trên Android và cách **test** trên emulator / điện thoại thật.

Module này **độc lập** với backend VEGGO. Speech dùng dịch vụ hệ thống (Google / Samsung). Backend chỉ nhận **text** sau khi đã chuyển đổi xong (ví dụ qua Chatbot).

---

## Tổng quan

```
UI (Activity / Fragment)
    implements SpeechCallback
        ↓
    SpeechManager
        ├─ SpeechRecognizer API (ưu tiên)
        └─ Intent fallback (mở UI nhận diện hệ thống)
        ↓
    Google / Samsung Recognition Service
        ↓
    onResult(text) → UI tự xử lý (điền ô nhập, gửi chat, …)
```

**Đã tích hợp sẵn tại:** `ChatbotActivity` (màn Trợ lý AI).

---

## Cấu trúc package

```
app/src/main/java/com/veggo/app/speech/
├── SpeechManager.java      # Quản lý lifecycle SpeechRecognizer + fallback
├── SpeechCallback.java     # Contract callback cho UI
├── VoiceState.java         # IDLE | READY | LISTENING | PROCESSING | ERROR
├── SpeechConfig.java       # Locale, maxResults, partialResults
├── SpeechAvailability.java # Phát hiện dịch vụ Google / Samsung trên máy
└── SpeechErrorMapper.java  # Map mã lỗi → message tiếng Việt
```

---

## Yêu cầu hệ thống

| Yêu cầu | Ghi chú |
|---------|---------|
| Min SDK | 24 (theo project) |
| Quyền `RECORD_AUDIO` | Xin runtime trước khi ghi âm |
| Internet | SpeechRecognizer cloud cần mạng |
| Dịch vụ nhận diện | Google app **hoặc** Samsung Voice Input |

**Lưu ý:** Kết nối backend (`local.properties`, port 5001) **không liên quan** tới speech. Có thể test micro mà không cần backend; gửi chat thì cần backend.

---

## Cấu hình AndroidManifest

Đã khai báo sẵn:

- `android.permission.RECORD_AUDIO`
- `<queries>` cho `RecognitionService`, `RECOGNIZE_SPEECH`, Google, Samsung (bắt buộc từ Android 11 / targetSdk 30+)

Không cần sửa thêm khi test trên máy thật, trừ khi OEM khác (Xiaomi, Huawei không GMS, …).

---

## Tích hợp nhanh (màn hình mới)

```java
public class MyActivity extends AppCompatActivity implements SpeechCallback {

    private SpeechManager speechManager;

    private final ActivityResultLauncher<Intent> speechIntentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    speechManager.handleActivityResult(result.getResultCode(), result.getData())
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        speechManager = new SpeechManager(this, this);
        speechManager.setIntentLauncher(speechIntentLauncher);
    }

    void onMicClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            speechManager.startListening(this);
        } else {
            // xin quyền RECORD_AUDIO rồi gọi startListening
        }
    }

    @Override public void onReady() { }
    @Override public void onListening() { }
    @Override public void onResult(String text) { /* dùng text */ }
    @Override public void onError(String message) { }
    @Override public void onVoiceStop() { }

    @Override
    protected void onDestroy() {
        speechManager.destroy();
        super.onDestroy();
    }
}
```

Đổi ngôn ngữ:

```java
speechManager.setLocale(Locale.forLanguageTag("en-US"));
// hoặc
speechManager.setConfig(new SpeechConfig.Builder().locale(new Locale("en", "US")).build());
```

Mặc định: `vi-VN`.

---

## Test trên Chatbot (khuyến nghị)

### 1. Chuẩn bị

```bash
# Terminal 1 – backend (chỉ cần khi test gửi tin chat)
cd backend
npm run start
```

Cập nhật `local.properties` nếu test trên **điện thoại thật**:

```properties
# IP máy dev (chạy: ipconfig getifaddr en0 trên macOS)
api.base.url=http://192.168.x.x:5001/api/
```

Sau khi đổi IP → **Rebuild & cài lại app**.

Emulator dùng mặc định:

```properties
api.base.url=http://10.0.2.2:5001/api/
```

### 2. Luồng test micro

1. Mở app → **Trợ lý AI** (Chatbot)
2. Nhấn icon **micro** bên trái ô nhập
3. Cấp quyền **Micro** nếu được hỏi
4. Quan sát banner trạng thái:
   - *Đang khởi động micro…*
   - *Đang ghi âm* (chấm xanh nhấp nháy)
   - Text hiện dần trong ô nhập (partial result)
   - *Đang chuyển thành chữ…*
   - *Đã nhận diện* → tự gửi tin nhắn chat
5. Chờ bot trả lời

**Hủy ghi âm:** nhấn micro lần nữa khi đang ghi.

### 3. Test trên Emulator

| Bước | Việc cần làm |
|------|----------------|
| AVD Microphone | Extended controls → Microphone → **Virtual microphone enabled** |
| Google app | Cài Google / Gboard trên emulator image có Play Store |
| Internet | Emulator phải có mạng |

### 4. Test trên điện thoại thật

| Bước | Việc cần làm |
|------|----------------|
| Wi-Fi | Cùng mạng với máy dev (nếu test gửi chat) |
| Rebuild | Sau mỗi lần đổi `local.properties` |
| Samsung | Bật Voice Input (xem mục dưới) |
| Google | Cài/cập nhật Google từ CH Play nếu dùng Google Voice |

---

## Cấu hình Samsung (Galaxy A8, …)

Samsung thường dùng **Samsung Voice Input**, không phải Google.

1. **Cài đặt** → **Quản lý chung** → **Ngôn ngữ và nhập liệu**
2. **Bàn phím màn hình** → **Samsung Keyboard**
3. **Nhập liệu bằng giọng nói** / **Voice input** → **Bật**
4. Mở lại Chatbot → thử micro

Kiểm tra nhanh ngoài app: mở **Ghi chú** hoặc **Tin nhắn** → thử dictation. Nếu không hoạt động ở đó, lỗi nằm ở cài đặt máy.

---

## VoiceState & Callback

| VoiceState | Ý nghĩa |
|------------|---------|
| `IDLE` | Không ghi âm |
| `READY` | Sẵn sàng nghe |
| `LISTENING` | Đang thu âm |
| `PROCESSING` | Đang nhận diện / chuyển text |
| `ERROR` | Có lỗi |

| Callback | Khi nào gọi |
|----------|-------------|
| `onReady()` | Micro sẵn sàng |
| `onListening()` | Bắt đầu phát hiện giọng nói |
| `onProcessing()` | Hết nói, đang xử lý |
| `onPartialResult(text)` | Text tạm khi đang nói |
| `onResult(text)` | Kết quả cuối |
| `onError(message)` | Lỗi (trừ user hủy) |
| `onVoiceStop()` | Kết thúc phiên (thành công / hủy / lỗi) |

---

## Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| *Nhận diện giọng nói không khả dụng* | Không có dịch vụ Google/Samsung | Bật Samsung Voice Input hoặc cài Google |
| Lỗi ngay trên Samsung | Voice Input chưa bật | Làm theo mục **Cấu hình Samsung** |
| *Không kết nối được máy chủ* (khi gửi chat) | Backend / IP sai | `npm run start`, kiểm tra `api.base.url`, rebuild app |
| Không có partial text | Một số máy không trả partial | Bình thường; vẫn có text khi nói xong |
| *Không có kết nối mạng* | Mất Internet | Bật Wi-Fi / 4G |
| *Thiếu quyền ghi âm* | Chưa cấp RECORD_AUDIO | Cấp quyền trong Cài đặt app |

Kiểm tra dịch vụ speech trên code:

```java
boolean supported = SpeechManager.isSupported(context);
```

---

## Checklist test trước khi báo lỗi

- [ ] App đã **rebuild** sau thay đổi code / `local.properties`
- [ ] Đã cấp quyền **Micro**
- [ ] Điện thoại có **Internet**
- [ ] Samsung: đã **bật Voice Input**
- [ ] Backend đang chạy (nếu test gửi chat): `curl http://<IP>:5001/api/health`
- [ ] Thử micro trong app **Ghi chú** để loại trừ lỗi phần cứng

---

## Mở rộng sau này

Module thiết kế để sau này thay engine nội bộ mà **giữ nguyên API**:

- `SpeechManager`
- `SpeechCallback`
- `VoiceState`

Ứng dụng dự kiến: Chatbot (đã có), Search, form nhập liệu khác.

Engine thay thế có thể tích hợp: Google Cloud Speech-to-Text, Whisper API, v.v.

---

## File liên quan

| File | Mô tả |
|------|--------|
| `app/.../speech/*` | Module speech |
| `app/.../chatbot/ChatbotActivity.java` | Tích hợp + UI voice banner |
| `app/src/main/res/layout/activity_chatbot.xml` | Nút micro + banner trạng thái |
| `app/src/main/res/values/strings.xml` | Chuỗi lỗi / trạng thái voice |
| `app/src/main/AndroidManifest.xml` | Quyền + `<queries>` |
| `docs/CHATBOT.md` | Tài liệu chatbot & backend |
