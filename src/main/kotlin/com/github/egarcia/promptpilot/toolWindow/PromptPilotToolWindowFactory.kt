package com.github.egarcia.promptpilot.toolWindow

import com.github.egarcia.promptpilot.backends.GeminiPromptContext
import com.github.egarcia.promptpilot.backends.GeminiPromptContextWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import java.io.File
import javax.swing.JButton
import javax.swing.BoxLayout
import javax.swing.JPanel

class PromptPilotToolWindowFactory : ToolWindowFactory {

    companion object {
        private const val TOOL_WINDOW_TITLE = "PromptPilot"
        private const val SETTINGS_LABEL_TEXT = "PromptPilot Settings"
        private const val TOGGLE_PATCH_TEXT = "Use Patch Output Format"
        private const val GENERATE_BUTTON_TEXT = "Generate Gemini Context"
        private const val CONTEXT_FILE_SUCCESS_MESSAGE = "prompt-context.json generated with output: %s."
        private const val DEFAULT_OUTPUT_FORMAT = "patch"
        private const val OTHER_OUTPUT_FORMAT = "code"
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
            val outputFormat = if (togglePatch.isSelected) DEFAULT_OUTPUT_FORMAT else OTHER_OUTPUT_FORMAT
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

        // Add components to the panel with preferred gaps between components.
        panel.add(label)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(togglePatch)
        panel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        panel.add(generateButton)

        return panel
    }
}