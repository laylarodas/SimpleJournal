<div align="center">

# SimpleJournal

Kotlin + Firebase sample app for capturing daily journal entries with modern Android architecture.

</div>

## ✅ Objetivo
SimpleJournal es un proyecto de práctica que replica el flujo básico de un diario digital:
- Autenticación simple (inicio anónimo por ahora).
- Lista de entradas en tiempo real usando Cloud Firestore.
- ViewModel + LiveData para un estado reactivo.
- Coroutines para llamadas asíncronas limpias.

## 🧰 Stack
- **Kotlin** + **ViewBinding** en una `Empty Activity`.
- **Firebase Auth** + **Cloud Firestore** (via Firebase BoM).
- **Coroutines** (`kotlinx-coroutines-android` + Play Services).
- **Lifecycle KTX** (`viewmodel-ktx`, `livedata-ktx`).
- **Material Components**, **ConstraintLayout**, **RecyclerView**.

## 🚀 Primeros pasos
1. Clona el repo y abre en Android Studio Iguana o superior.
2. Ejecuta **Gradle Sync**.
3. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/):
   - Package name: `com.laylarodas.simplejournal`.
   - Descarga `google-services.json` y guárdalo en `app/google-services.json`.
4. Habilita **Authentication (Email/Anonymous)** y **Cloud Firestore**.
5. Ejecuta `Run > app` para verificar que compila y muestra la UI de prueba.

## 📁 Estructura sugerida
```
app/
 └── src/main/java/com/laylarodas/simplejournal
     ├── auth/          # FirebaseAuth wrapper
     ├── data/
     │   ├── model/     # Data classes (JournalEntry)
     │   ├── remote/    # Firestore data sources
     │   └── repository/# Repositorio principal
     ├── ui/main/       # Activity + RecyclerView adapter
     ├── utils/         # Service locator, constantes
     └── viewmodel/     # JournalViewModel
```

## 📅 Roadmap MVP
- [ ] Sign-in con email/password.
- [ ] CRUD completo de entradas.
- [ ] Sincronización bidireccional con Firestore.
- [ ] Validaciones y mensajes de error amigables.
- [ ] Tests unitarios (repositorio y ViewModel).

## 📝 Notas
- `google-services.json` y archivos de claves quedan fuera del repo (ver `.gitignore`).
- Se puede migrar a Jetpack Compose más adelante manteniendo el mismo backend.

Pull requests y mejoras son bienvenidos ✨

