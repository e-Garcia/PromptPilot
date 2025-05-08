package com.github.egarcia.promptpilot.toolWindow

import com.github.egarcia.promptpilot.backends.GeminiPromptContext
import com.github.egarcia.promptpilot.backends.GeminiPromptContextWriter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class PromptPilotToolWindowFactory : ToolWindowFactory {

    companion object {
        private const val TOOL_WINDOW_TITLE = "PromptPilot"
        private const val SETTINGS_LABEL_TEXT = "PromptPilot Settings"
        private const val TOGGLE_PATCH_TEXT = "Use Patch Output Format"
        private const val CREATE_REPO_CONTEXT_BUTTON_TEXT = "Create Repo Context File"
        private const val DELETE_REPO_CONTEXT_BUTTON_TEXT = "Delete Repo Context File"
        private const val REPO_CONTEXT_FILE_NAME = "repo-context.md"
        private const val CONTEXT_DIRECTORY = ".promptpilot"
        private const val SAMPLE_CONTEXT_FILE_NAME = "sample-context.md"
        private const val PATCH_FORMAT_INSTRUCTION = "- output: use the patch format"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = createMainPanel(project)
        val scrollPane = JBScrollPane(mainPanel)
        val contentPanel = SimpleToolWindowPanel(true, true)
        contentPanel.setContent(scrollPane)
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(contentPanel, "", false)
        contentManager.addContent(content)
    }

    private fun createMainPanel(project: Project): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val label = JBLabel(SETTINGS_LABEL_TEXT)
        label.alignmentX = java.awt.Component.LEFT_ALIGNMENT

        val togglePatch = JBCheckBox(TOGGLE_PATCH_TEXT, true)
        togglePatch.alignmentX = java.awt.Component.LEFT_ALIGNMENT

        val createRepoContextButton = JButton(CREATE_REPO_CONTEXT_BUTTON_TEXT)
        createRepoContextButton.alignmentX = java.awt.Component.LEFT_ALIGNMENT
        createRepoContextButton.addActionListener {
            createRepoContextFile(project, togglePatch.isSelected)
        }

        val deleteRepoContextButton = JButton(DELETE_REPO_CONTEXT_BUTTON_TEXT)
        deleteRepoContextButton.alignmentX = java.awt.Component.LEFT_ALIGNMENT
        deleteRepoContextButton.addActionListener {
            deleteRepoContextFile(project)
        }

        panel.add(label)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(togglePatch)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(createRepoContextButton)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(deleteRepoContextButton)

        return panel
    }

    private fun createRepoContextFile(project: Project, isPatchFormatEnabled: Boolean) {
        val projectRoot = File(project.basePath ?: ".")
        val contextDir = Paths.get(projectRoot.path, CONTEXT_DIRECTORY).toFile()
        if (!contextDir.exists()) {
            contextDir.mkdirs()
        }

        val repoContextFile = File(contextDir, REPO_CONTEXT_FILE_NAME)

        if (!repoContextFile.exists()) {
            try {
                repoContextFile.createNewFile()

                val sampleContextStream: InputStream? = javaClass.classLoader.getResourceAsStream(SAMPLE_CONTEXT_FILE_NAME)
                val defaultContent = sampleContextStream?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("$SAMPLE_CONTEXT_FILE_NAME not found in resources")

                val finalContent = if (isPatchFormatEnabled) {
                    "$defaultContent\n\n$PATCH_FORMAT_INSTRUCTION"
                } else {
                    defaultContent
                }

                repoContextFile.writeText(finalContent)
                openFileInEditor(project, repoContextFile)

            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "Error creating file: ${e.message}",
                    TOOL_WINDOW_TITLE
                )
            }
        } else {
            updatePatchFormatInstruction(repoContextFile, isPatchFormatEnabled)
            openFileInEditor(project, repoContextFile)
        }
    }

    private fun deleteRepoContextFile(project: Project) {
        val projectRoot = File(project.basePath ?: ".")
        val contextDir = Paths.get(projectRoot.path, CONTEXT_DIRECTORY).toFile()
        val repoContextFile = File(contextDir, REPO_CONTEXT_FILE_NAME)

        if (repoContextFile.exists()) {
            if (repoContextFile.delete()) {
                Messages.showInfoMessage(
                    project,
                    "Repo context file deleted successfully.",
                    TOOL_WINDOW_TITLE
                )
            } else {
                Messages.showErrorDialog(
                    project,
                    "Failed to delete the repo context file.",
                    TOOL_WINDOW_TITLE
                )
            }
        } else {
            Messages.showInfoMessage(
                project,
                "Repo context file does not exist.",
                TOOL_WINDOW_TITLE
            )
        }
    }

    private fun updatePatchFormatInstruction(file: File, isPatchFormatEnabled: Boolean) {
        val content = file.readText()
        val hasInstruction = content.contains(PATCH_FORMAT_INSTRUCTION)

        val updatedContent = when {
            isPatchFormatEnabled && !hasInstruction -> "$content\n\n$PATCH_FORMAT_INSTRUCTION"
            !isPatchFormatEnabled && hasInstruction -> content.replace("\n\n$PATCH_FORMAT_INSTRUCTION", "")
            else -> content
        }

        file.writeText(updatedContent)
    }

    private fun openFileInEditor(project: Project, file: File) {
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.let { virtualFile ->
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        } ?: Messages.showErrorDialog(
            project,
            "Could not find the file.",
            TOOL_WINDOW_TITLE
        )
    }
}
