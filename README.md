# VEGGO Android

VEGGO is an Android Java project prepared for team development with XML layouts, ViewBinding, MVVM, Repository pattern, MongoDB Atlas through a backend API, Firebase services, and Room local cache.

## Clone and Team Setup

Clone repo:

```bash
git clone https://github.com/lilhuohuo1825/VEGGO.git
cd VEGGO
```

Open the project root in Android Studio, then let Gradle sync before editing code.

Install backend dependencies:

```bash
cd backend
npm install
```

Create local backend env from the example:

```bash
cp backend/.env.example backend/.env
```

Update `backend/.env` with your own MongoDB Atlas URI and Firebase service account path. Do not commit `backend/.env`.

Firebase Android setup:

- Download `google-services.json` from Firebase Console.
- Put it at `app/google-services.json`.
- Do not commit `google-services.json` unless the team explicitly decides to share the Firebase config.

Before pushing code:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
cd backend
npm run start
```

Branch workflow:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/<your-feature-name>
```

Commit only source/config template files. Do not commit build outputs, secrets, local IDE files, APK/AAB files, keystores, or dependency folders.

Push your feature branch:

```bash
git push -u origin feature/<your-feature-name>
```

## Main Layers

- `presentation/`: Activity, Fragment, ViewModel, UI state for each feature.
- `domain/`: App models, repository contracts, use cases.
- `data/`: Repository implementations, API/Firebase data sources, DTOs, Room entities and DAOs, mappers.
- `core/`: Shared infrastructure such as network, database, Firebase, preferences, base UI, utilities.
- `adapter/`: RecyclerView adapters.

## AI Chatbot (Trợ lý AI)

Feature documentation: [`docs/CHATBOT.md`](docs/CHATBOT.md)

Covers backend API, intent handling, product/recipe suggestions, Android UI, env config, and how to run locally.

Speech-to-Text module: [`docs/SPEECH.md`](docs/SPEECH.md)

Covers voice input module, Samsung/Google setup, and how to test microphone on emulator and real devices.

## Asset Rules

- UI icons and vector drawables: `app/src/main/res/drawable/`
- Shape/background XML: `app/src/main/res/drawable/`
- Local banners/illustrations/placeholders that should not be density-scaled: `app/src/main/res/drawable-nodpi/`
- Launcher icon only: `app/src/main/res/mipmap-*`
- Real product images: Firebase Storage. Store only image URLs in MongoDB Atlas.

## Team Branches

- `develop`: integration branch.
- `feature/auth-profile`: auth and profile.
- `feature/home-category`: home and category.
- `feature/product`: product list/detail/search.
- `feature/cart-checkout`: cart and checkout.
- `feature/core-data`: API, Firebase, Room, shared setup.

## Coding Rule

Activity/Fragment should not call API, Firebase, or Room directly. Use this flow:

```text
Activity/Fragment -> ViewModel -> UseCase -> Repository -> RemoteDataSource/API/Firebase + DAO/Room
```

## Data Connection Setup

### MongoDB Atlas

Android must not connect directly to MongoDB Atlas. The MongoDB URI belongs in `backend/.env`, and the app calls the backend API.

Backend local API:

```text
http://localhost:5001/api
```

Android emulator API URL:

```text
http://10.0.2.2:5001/api/
```

Android real device on the same Wi-Fi:

```properties
# local.properties
dev.api.mode=physical
dev.api.host=<LAN-IP-máy-chạy-backend>
```

On macOS, get the LAN IP with:

```bash
ifconfig en0 | grep "inet "
```

The app will use:

```text
http://<LAN-IP-máy-chạy-backend>:5001/api/
```

After changing `local.properties`, rebuild and reinstall the app. `127.0.0.1` on a real device points to the phone itself, not the Mac. Only use `dev.api.mode=physical_usb` with:

```bash
adb reverse tcp:5001 tcp:5001
```

Run backend:

```bash
cd backend
npm install
npm run dev
```

Health check:

```text
GET http://localhost:5001/api/health
```

### Firebase

The Android package name is:

```text
com.veggo.app
```

Download `google-services.json` from Firebase Console and put it here:

```text
app/google-services.json
```

For backend Firebase Admin features, download the Firebase service account JSON and put it here:

```text
backend/firebase-service-account.json
```

Do not commit `.env`, Firebase service account files, keystores, generated build files, or `node_modules`.
