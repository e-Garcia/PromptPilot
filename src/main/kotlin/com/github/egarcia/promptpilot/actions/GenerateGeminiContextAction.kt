package com.github.egarcia.promptpilot.actions

import com.github.egarcia.promptpilot.backends.GeminiPromptContext
import com.github.egarcia.promptpilot.backends.GeminiPromptContextWriter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File

class GenerateGeminiContextAction : AnAction("Generate Gemini Context") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: run {
            Messages.showErrorDialog("No active project found.", "PromptPilot")
            return
        }

        val context = GeminiPromptContext(
            intent = "Generate unit tests",
            target = "Kotlin",
            output_format = "patch",
            instructions = listOf(
                "Use Hilt for DI mocking",
                "Follow Given/When/Then test structure",
                "Prefer assertThat from Truth library",
                "Avoid Robolectric unless explicitly annotated"
            ),
            style = "Concise and Android idiomatic"
        )

        GeminiPromptContextWriter.writeContextFile(File(project.basePath ?: "."), context)
        Messages.showInfoMessage(project, "prompt-context.json generated for Gemini.", "PromptPilot")
    }
}

