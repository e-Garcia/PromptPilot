# PromptPilot Context Instructions

Describe your project setup, preferred libraries, architecture, or anything else the AI should know when generating code.

Example:
- Architecture: We use MVVM with a clean architecture approach.
- UI: All UI should be built using Jetpack Compose.
- Networking: Retrofit for network requests, with coroutines for asynchronous handling.
- DI: Hilt for dependency injection.
- Error handling: Use sealed classes for handling network and other errors.
- State management: Prefer Kotlin StateFlow over LiveData.
- Style: Follow the Android Kotlin Style Guide.
- Code should handle potential network errors gracefully.
- Asynchronous operations should use Coroutines.