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
        private const val GENERATE_BUTTON_TEXT = "Generate Gemini Context"
        private const val CONTEXT_FILE_SUCCESS_MESSAGE =
            "prompt-context.json generated with output: %s."
        private const val DEFAULT_OUTPUT_FORMAT = "patch"
        private const val OTHER_OUTPUT_FORMAT = "code"
        private const val CREATE_REPO_CONTEXT_BUTTON_TEXT = "Create Repo Context File"
        private const val REPO_CONTEXT_FILE_NAME = "repo-context.md"
        private const val CONTEXT_DIRECTORY = ".promptpilot"
        private const val SAMPLE_CONTEXT_FILE_NAME = "sample-context.md"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Use a helper function to create the main panel with a proper layout
        val mainPanel = createMainPanel(project)

        // Wrap the main panel in a scroll pane for larger content
        val scrollPane = JBScrollPane(mainPanel)

        // Use SimpleToolWindowPanel for better integration with the tool window
        val contentPanel = SimpleToolWindowPanel(true, true)
        contentPanel.setContent(scrollPane)

        // Create and add the content to the tool window
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(contentPanel, "", false)
        contentManager.addContent(content)
    }

    private fun createMainPanel(project: Project): JPanel {
        val panel = JPanel()
        // using BoxLayout.Y_AXIS, allows for easier management of the components without manual bounds calculations.
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val label = JBLabel(SETTINGS_LABEL_TEXT)
        // Use setAlignmentX to centralize or manage better the label.
        label.alignmentX = java.awt.Component.LEFT_ALIGNMENT

        val togglePatch = JBCheckBox(TOGGLE_PATCH_TEXT, true)
        togglePatch.alignmentX = java.awt.Component.LEFT_ALIGNMENT

        val generateButton = JButton(GENERATE_BUTTON_TEXT)
        generateButton.alignmentX = java.awt.Component.LEFT_ALIGNMENT
        generateButton.addActionListener {
            val outputFormat =
                if (togglePatch.isSelected) DEFAULT_OUTPUT_FORMAT else OTHER_OUTPUT_FORMAT
            val context = GeminiPromptContext(
                intent = "Generate unit tests",
                target = "Kotlin",
                outputFormat = outputFormat,
                instructions = listOf(
                    "Use Hilt for DI mocking",
                    "Follow Given/When/Then test structure",
                    "Prefer assertThat from Truth library",
                    "Avoid Robolectric unless explicitly annotated"
                ),
                style = "Concise and Android idiomatic"
            )
            // Get the project root directory
            val projectRoot = File(project.basePath ?: ".")
            // Call the modified writeContextFile() method, passing the project.
            GeminiPromptContextWriter.writeContextFile(project, projectRoot, context)

            Messages.showInfoMessage(
                project,
                String.format(CONTEXT_FILE_SUCCESS_MESSAGE, outputFormat),
                TOOL_WINDOW_TITLE
            )
        }

        val createRepoContextButton = JButton(CREATE_REPO_CONTEXT_BUTTON_TEXT)
        createRepoContextButton.alignmentX = java.awt.Component.LEFT_ALIGNMENT
        createRepoContextButton.addActionListener {
            createRepoContextFile(project)
        }

        // Add components to the panel with preferred gaps between components.
        panel.add(label)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(togglePatch)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(generateButton)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(createRepoContextButton)

        return panel
    }

    private fun createRepoContextFile(project: Project) {
        val projectRoot = File(project.basePath ?: ".")
        val contextDir = Paths.get(projectRoot.path, CONTEXT_DIRECTORY).toFile()
        if (!contextDir.exists()) {
            contextDir.mkdirs()
        }

        val repoContextFile = File(contextDir, REPO_CONTEXT_FILE_NAME)

        if (!repoContextFile.exists()) {
            try {
                repoContextFile.createNewFile()

                // Load default content from sample-context.md
                val sampleContextStream: InputStream? = javaClass.classLoader.getResourceAsStream(SAMPLE_CONTEXT_FILE_NAME)
                val defaultContent = sampleContextStream?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("$SAMPLE_CONTEXT_FILE_NAME not found in resources")

                // Write the default content to the new file
                repoContextFile.writeText(defaultContent)

                // Refresh the file in the VFS
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoContextFile)
                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(repoContextFile)
                if (virtualFile != null) {
                    // Open the file in the editor
                    FileEditorManager.getInstance(project).openFile(virtualFile, true)
                } else {
                    Messages.showErrorDialog(
                        project,
                        "Could not find the created file.",
                        TOOL_WINDOW_TITLE
                    )
                }

            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "Error creating file: ${e.message}",
                    TOOL_WINDOW_TITLE
                )
            }
        } else {
            Messages.showInfoMessage(
                project,
                "Repository context file already exists.",
                TOOL_WINDOW_TITLE
            )
            // Open the existing file
            val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(repoContextFile)
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            } else {
                Messages.showErrorDialog(
                    project,
                    "Could not find the existing file.",
                    TOOL_WINDOW_TITLE
                )
            }
        }
    }
}

