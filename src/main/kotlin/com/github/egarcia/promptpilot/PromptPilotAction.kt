package com.github.egarcia.promptpilot

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

class PromptPilotAction : AnAction("Enhance with AI") {

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val project = event.project

        if (editor == null || project == null) {
            Messages.showMessageDialog(project, "No editor context available.", "PromptPilot", Messages.getErrorIcon())
            return
        }

        val selectedText = editor.selectionModel.selectedText ?: "No code selected."

        // This is where you'd pass `selectedText` to your AI backend of choice
        val aiGeneratedSuggestion = "// [AI suggestion would go here for]:\n$selectedText"

        Messages.showMultilineInputDialog(
            project,
            "AI-Suggested Code (mock):",
            "PromptPilot Result",
            aiGeneratedSuggestion,
            null,
            null
        )
    }

    override fun update(event: AnActionEvent) {
        // Enable only when an editor is open and text is selected
        val editor = event.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() ?: false
        event.presentation.isEnabled = hasSelection
    }
}
