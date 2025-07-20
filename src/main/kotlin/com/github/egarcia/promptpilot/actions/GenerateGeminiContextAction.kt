package com.github.egarcia.promptpilot.actions

import com.github.egarcia.promptpilot.backends.AIPromptContext
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

        val context = AIPromptContext(
            intent = "Generate unit tests",
            target = "Kotlin",
            outputFormat = "patch",
            instructions =
                "Use Hilt for DI mocking"
                        + "/n" + "Follow Given/When/Then test structure"
                        + "/n" + "Prefer assertThat from Truth library"
                        + "/n" + "Avoid Robolectric unless explicitly annotated",
            style = "Concise and Android idiomatic"
        )

        // Get the project root directory
        val projectRoot = File(project.basePath ?: ".")
        // Call the modified writeContextFile() method, passing the project.
        GeminiPromptContextWriter.writeContextFile(project, projectRoot, context)
        //Show the info message.
        Messages.showInfoMessage(
            project,
            "prompt-context.json generated for Gemini.",
            "PromptPilot"
        )
    }
}