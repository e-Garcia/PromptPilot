# ✈️ PromptPilot
**Take the controls. Steer your AI.**

<!-- Plugin description -->
PromptPilot is an IntelliJ-based plugin purpose-built for Android Studio. It empowers Android developers to steer AI code generation using context-aware, team-aligned instructions — across multiple AI backends like Gemini and OpenAI.
<!-- Plugin description end -->

Whether you're refining unit tests, generating patch files, or enforcing architecture standards, PromptPilot lets you take the pilot seat and guide your AI assistant exactly where you need it.

---

## 🚀 Features

- 🔌 **Multi-AI Support** – Works with Gemini, OpenAI (GPT), Tabnine, and more.
- 🧠 **Context-Aware Prompting** – Tailor instructions based on selected code, file name, or project structure.
- 🗂️ **AI-Aware Output Destinations** – Send repo context to Copilot, Cursor, or custom paths with automatic directory creation.
- 🧪 **Unit Test Generator** – Generate unit tests aligned with your team’s best practices.
- 🔍 **Patch-Focused Prompts** – Suggest minimal diffs instead of rewriting whole files.
- 🧱 **Pluggable Architecture** – Easily add new AI backends or customize logic.
- 💬 **UI for Prompt Tuning** – Pop-up dialogs to modify instructions on the fly.
- 🔒 **Runs in Android Studio** – Native IntelliJ plugin that works seamlessly in Android development environments.

---

## 🧰 Installation

**1. Install via JetBrains Marketplace: [PromptPilot on Marketplace](https://plugins.jetbrains.com/plugin/27974-promptpilot)**

To install manually:
1. Clone this repository
2. Build the plugin via Gradle
3. Install it in Android Studio via **Preferences > Plugins > Install plugin from disk**

---

## 🤖 Supported Backends

| Backend             | Status           | Notes                               |
|---------------------|------------------|-------------------------------------|
| **Gemini (Google)** | 🛠️ In development | Targeting second major version      |
| **Copilot**         | ✅ Completed      | v1.1.0                              |
| **Cursor**          | 🛠️ In development | Targeting third major version       |
| **OpenAI GPT-4**    | 🛠️ In development | Backend abstraction already planned |
| **Tabnine**         | 🔜 Not started   | Under consideration                 |

---

## 🎯 AI Output Targets

Use the **AI Output Target** dropdown in the PromptPilot tool window to control where the generated repository context file lives:

| Target              | Destination Path                       | Notes                                              |
|---------------------|----------------------------------------|----------------------------------------------------|
| PromptPilot Default | `.promptpilot/repo-context.md`         | Safe default for any AI assistant                  |
| GitHub Copilot      | `.github/copilot-instructions.md`      | Loaded auto-magically by Copilot                   |
| Cursor              | `.cursor/rules`                        | Cursor parses this file for workspace guidelines   |
| Custom              | Any directory + filename you specify   | Paths resolve relative to the project root         |

PromptPilot auto-creates missing directories/files before writing. Switch targets, click **Save Output Target**, and generate again—the destination updates instantly.

---

## 📂 Example Use Cases

- Refactor code to MVVM/MVI/MVP pattern
- Generate unit tests with correct mocking style (e.g. Mockito, Hilt)
- Explain or summarize large methods
- Enforce naming conventions or architecture boundaries
- Generate changelogs based on diffs

---

## 📜 License
PromptPilot is licensed under the [MIT License](LICENSE).

---

## ⚠️ Legal Disclaimer
> This project was developed independently by Erick Josue Gabriel Garcia Garcia, Staff Android Engineer at Walmart Labs, during personal time using personal equipment. 
> It is not affiliated with, sponsored by, or representative of Walmart Inc. or any of its proprietary tools or research. 
> This open-source plugin does not use or reference any confidential, internal, or proprietary information.

---

## ✨ Contributing
We welcome contributions, issues, and feedback! See [CONTRIBUTING.md](CONTRIBUTING.md) for how to get started.

---

## 📈 Roadmap
- [ ] Support for Visual Studio Code through Kotlin Multiplatform
- [ ] Add built in sample prompt templates
- [ ] Context-aware project-wide prompt tuning
- [X] JetBrains Marketplace release
- [ ] Prompt profile presets per team
- [ ] Inline code suggestions

---

## 🢁‍ Maintainer
Developed and maintained by [Erick Garcia](https://github.com/e-Garcia).

_Last updated: November 22, 2025_
