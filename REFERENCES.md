# 📎 References and Standards Supporting PromptPilot Design

This document tracks authoritative sources and design considerations aligned with PromptPilot's implementation as an IntelliJ/Android Studio plugin.

## 🔧 Android Studio Plugin Compatibility

- Plugin is configured for Android Studio Meerkat:
  https://plugins.jetbrains.com/docs/intellij/android-studio.html#gradle-build-script

- Uses:
  ```kotlin
  androidStudio("2024.3.1.14")
  bundledPlugin("org.jetbrains.android")
  ```

## 🧱 Gradle Plugin Configuration

- Based on JetBrains IntelliJ Platform Plugin DevKit:
  https://plugins.jetbrains.com/docs/intellij/gradle-prerequisites.html

## 🧪 Plugin Verification, Changelog, and Publishing

- Follows structured changelog and verification config:
  https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html

## 📦 Publishing Prepared For JetBrains Marketplace

- Publishing and signing support is in place via:
  - `PUBLISH_TOKEN`
  - `CERTIFICATE_CHAIN`
  - Plugin versioning via SemVer

---

Maintained by: Erick Josue Gabriel Garcia Garcia  
Contact: erick@egarcia.dev
