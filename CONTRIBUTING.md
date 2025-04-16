# 🤝 Contributing to PromptPilot

Thank you for your interest in contributing to **PromptPilot** — a plugin for Android Studio that puts developers in control of AI-assisted code generation.

Whether you're fixing bugs, adding features, improving documentation, or suggesting ideas, your contribution is welcome!

---

## 🛠 Getting Started

1. **Fork this repository**
2. **Clone your fork locally**
   ```bash
   git clone https://github.com/YOUR-USERNAME/PromptPilot.git
   ```
3. **Create a new branch**
   ```bash
   git checkout -b my-feature-branch
   ```
4. **Make your changes and commit**
   ```bash
   git commit -am "feat: add amazing feature"
   ```
5. **Push your branch to GitHub**
   ```bash
   git push origin my-feature-branch
   ```
6. **Open a Pull Request (PR)**

---

## 📋 What You Can Contribute

- ✨ New AI backend integrations (e.g., Claude, Mistral)
- 💡 Features like prompt presets or context menus
- 🐛 Bug fixes and performance improvements
- 🧪 Unit tests and test coverage
- 📝 Better docs, examples, or UI polish

---

## 🧪 Running the Plugin Locally

Use the IntelliJ plugin SDK to test changes:
```bash
./gradlew runIde
```

If you are targeting Android Studio:
- Adjust `platformVersion` and `platformBundledPlugins` in `gradle.properties`
- Install the built plugin `.zip` manually via Preferences > Plugins > Install from Disk

---

## 📐 Code Style

Please follow:
- Kotlin idioms and conventions
- Descriptive commit messages (`feat:`, `fix:`, `refactor:`, etc.)
- Keep logic modular and backend-agnostic where possible

---

## 🛡 License

By contributing to PromptPilot, you agree that your contributions will be licensed under the [MIT License](LICENSE).

---

Thank you for helping improve PromptPilot!

_— Erick Garcia_ ✈️