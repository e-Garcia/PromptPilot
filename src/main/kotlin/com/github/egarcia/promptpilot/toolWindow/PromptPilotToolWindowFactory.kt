package com.github.egarcia.promptpilot.toolWindow

import com.github.egarcia.promptpilot.backends.GeminiPromptContext
import com.github.egarcia.promptpilot.backends.GeminiPromptContextWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import javax.swing.JButton

class PromptPilotToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = null

        val label = JBLabel("PromptPilot Settings")
        label.setBounds(10, 10, 300, 20)

        val togglePatch = JBCheckBox("Use Patch Output Format", true)
        togglePatch.setBounds(10, 40, 250, 20)

        val generateButton = JButton("Generate Gemini Context")
        generateButton.setBounds(10, 70, 250, 30)

        generateButton.addActionListener {
            val outputFormat = if (togglePatch.isSelected) "patch" else "code"
            val context = GeminiPromptContext(
                intent = "Generate unit tests",
                target = "Kotlin",
                output_format = outputFormat,
                instructions = listOf(
                    "Use Hilt for DI mocking",
                    "Follow Given/When/Then test structure",
                    "Prefer assertThat from Truth library",
                    "Avoid Robolectric unless explicitly annotated"
                ),
                style = "Concise and Android idiomatic"
            )

            val projectRoot = java.io.File(project.basePath ?: ".")
            GeminiPromptContextWriter.writeContextFile(projectRoot, context)

            com.intellij.openapi.ui.Messages.showInfoMessage(
                project,
                "prompt-context.json generated with output: $outputFormat.",
                "PromptPilot"
            )
        }

        panel.add(label)
        panel.add(togglePatch)
        panel.add(generateButton)

        val contentPanel = SimpleToolWindowPanel(true, true)
        contentPanel.setContent(JBScrollPane(panel))

        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(contentPanel, "", false)
        contentManager.addContent(content)
    }
}
