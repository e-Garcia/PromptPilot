package com.github.egarcia.promptpilot

import com.github.egarcia.promptpilot.backends.AIPromptContext
import com.github.egarcia.promptpilot.resources.MyBundle
import com.github.egarcia.promptpilot.resources.Strings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File
import java.io.IOException

class PromptPilotAction : AnAction("Enhance with PromptPilot") {

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val project = event.project

        if (editor == null || project == null) {
            Messages.showMessageDialog(
                project, "No editor context available.", MyBundle.message(
                    Strings.TOOL_WINDOW_TITLE
                ), Messages.getErrorIcon()
            )
            return
        }

        val selectedText = editor.selectionModel.selectedText ?: "No code selected."

        val context = loadPromptContext(project)

        if (context == null) {
            Messages.showErrorDialog(
                project,
                "Failed to load prompt context. Check .promptpilot/prompt-context.json",
                "PromptPilot"
            )
            return
        }

        // This is where you'd pass `selectedText` and `context` to your AI backend
        val aiGeneratedSuggestion =
            "// [AI suggestion would go here for]:\n$selectedText\nContext: $context"

        Messages.showMultilineInputDialog(
            project,
            "AI-Suggested Code (mock):",
            "PromptPilot Result",
            aiGeneratedSuggestion,
            null,
            null
        )
    }

    private fun loadPromptContext(project: Project): AIPromptContext? {
        val contextFile = File(
            project.basePath,
            "${FileConstants.REPO_CONTEXT_DIR}${FileConstants.REPO_CONTEXT_FILENAME}"
        )
        return try {
            val contextInstructions = contextFile.readText()
            return AIPromptContext(
                intent = "Enhance code with AI suggestions",
                target = "Selected code in editor",
                outputFormat = "Code",
                instructions = contextInstructions,
                style = "Professional"
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun update(event: AnActionEvent) {
        // Enable only when an editor is open and text is selected
        val editor = event.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() ?: false
        event.presentation.isEnabled = hasSelection
    }
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
