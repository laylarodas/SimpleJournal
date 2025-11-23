<div align="center">

# SimpleJournal

Kotlin + Firebase practice app for capturing daily journal entries with a modern Android stack.

</div>

## ✅ Goal
SimpleJournal mirrors the core flow of a digital diary:
- Lightweight authentication (anonymous sign-in for now).
- Real-time list of entries backed by Cloud Firestore.
- ViewModel + LiveData to expose UI state safely.
- Coroutines for clean, suspend-based async work.

## 🧰 Tech stack
- **Kotlin** + **ViewBinding** in an Empty Activity template.
- **Firebase Auth** + **Cloud Firestore** through the Firebase BoM.
- **Coroutines** (`kotlinx-coroutines-android` + Play Services).
- **Lifecycle KTX** (`viewmodel-ktx`, `livedata-ktx`).
- **Material Components**, **ConstraintLayout**, **RecyclerView**.

## 🚀 Getting started
1. Clone the repo and open it with Android Studio Iguana (or newer).
2. Run **Gradle Sync**.
3. Create a project in [Firebase Console](https://console.firebase.google.com/):
   - Package name: `com.laylarodas.simplejournal`.
   - Download `google-services.json` and copy it to `app/google-services.json`.
4. Enable **Authentication** (Email + Anonymous) and **Cloud Firestore**.
5. Run `app` to confirm the placeholder UI builds successfully.

## 📁 Suggested structure
```
app/
 └── src/main/java/com/laylarodas/simplejournal
     ├── auth/          # FirebaseAuth wrapper
     ├── data/
     │   ├── model/     # Data classes (JournalEntry)
     │   ├── remote/    # Firestore data sources
     │   └── repository/# Main repository
     ├── ui/main/       # Activity + RecyclerView adapter
     ├── utils/         # Service locator, constants
     └── viewmodel/     # JournalViewModel
```

## 📅 MVP roadmap
- [ ] Email/password sign-in screen.
- [ ] Full CRUD for journal entries.
- [ ] Two-way sync with Firestore.
- [ ] Friendly validation and error feedback.
- [ ] Unit tests for repository and ViewModel.

## 📝 Notes
- `google-services.json` and signing keys stay outside the repo (see `.gitignore`).
- We can migrate to Jetpack Compose later without changing the backend.

Pull requests and improvements are welcome ✨

