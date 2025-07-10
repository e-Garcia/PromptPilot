// Updated Kotlin class with new features
package com.github.egarcia.promptpilot.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*
import javax.swing.table.DefaultTableModel

class PromptPilotToolWindowFactory : ToolWindowFactory {

    companion object {
        private const val TOOL_WINDOW_TITLE = "PromptPilot"
        private const val SETTINGS_LABEL_TEXT = "PromptPilot Settings"
        private const val TOGGLE_PATCH_TEXT = "Use Patch Output Format"
        private const val CREATE_REPO_CONTEXT_BUTTON_TEXT = "Create Repo Context"
        private const val DELETE_REPO_CONTEXT_BUTTON_TEXT = "Delete Repo Context"
        private const val REPO_CONTEXT_FILE_NAME = "repo-context.md"
        private const val OUTPUT_CONTEXT_DIRECTORY = ".promptpilot"
        private const val SOURCE_CONTEXT_DIRECTORY = ".promptpilot/source-context"
        private const val SAMPLE_CONTEXT_FILE_NAME = "sample-context.md"
        private const val PATCH_FORMAT_INSTRUCTION = "- output: use the patch format"
        private const val FILES_LABEL_TEXT = "Files in Source Context Directory ($SOURCE_CONTEXT_DIRECTORY):"
        private const val ADD_NEW_SOURCE_FILE_BUTTON_TEXT = "Add New Source File"
        private const val DEFAULT_NEW_FILE_NAME = "new-context-file.md"
        private const val LAYOUT_DEBUG_ENABLED = false // Set to true to enable layout debugging
    }

    private lateinit var filesTableModel: DefaultTableModel
    private lateinit var filesTable: JBTable
    private lateinit var projectInstance: Project

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.projectInstance = project
        val mainPanel = createMainPanel(project)
        val scrollPane = JBScrollPane(mainPanel)
        val contentPanel = SimpleToolWindowPanel(true, true)
        contentPanel.setContent(scrollPane)
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(contentPanel, "", false)
        contentManager.addContent(content)
    }

    private fun createMainPanel(project: Project): JPanel {

        // Top panel setup
        val topSettingsPanel = createSettingsPanel(
            project,
            SETTINGS_LABEL_TEXT,
            TOGGLE_PATCH_TEXT,
            CREATE_REPO_CONTEXT_BUTTON_TEXT,
            DELETE_REPO_CONTEXT_BUTTON_TEXT
        )

        // Files Panel setup
        val filesScrollPane = createFilesTablePanel(project)

        // File actions panel setup
        val fileActionsPanel = createFileActionsPanel(project)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT

        panel.add(topSettingsPanel)
        panel.add(Box.createVerticalStrut(5))
        panel.add(filesScrollPane)
        panel.add(Box.createVerticalStrut(5))
        panel.add(fileActionsPanel)

        updateFilesInPanel(project)

        val outerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        outerPanel.add(panel)
        return outerPanel
    }

    private fun addNewSourceFile(project: Project) {
        val sourceDir = getSourceContextDirectory(project)
        if (!sourceDir.exists()) Files.createDirectories(sourceDir.toPath())

        val fileName = Messages.showInputDialog(
            project,
            "Enter the name for the new source file (e.g., my-feature.md):",
            "New Source File Name",
            Messages.getQuestionIcon(),
            DEFAULT_NEW_FILE_NAME,
            null
        ) ?: return

        val sanitized = fileName.replace(File.separatorChar, '_').replace("..", "_")
        val finalName = if (!sanitized.endsWith(".md")) "$sanitized.md" else sanitized
        val newFile = File(sourceDir, finalName)

        if (newFile.exists()) {
            Messages.showWarningDialog(project, "File '$finalName' already exists.", TOOL_WINDOW_TITLE)
            return
        }

        val content = javaClass.classLoader.getResourceAsStream(SAMPLE_CONTEXT_FILE_NAME)?.bufferedReader()?.use { it.readText() }
            ?: run {
                Messages.showErrorDialog(project, "$SAMPLE_CONTEXT_FILE_NAME not found.", TOOL_WINDOW_TITLE)
                return
            }

        FileUtil.writeToFile(newFile, content)
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(newFile)?.let {
            FileEditorManager.getInstance(project).openFile(it, true)
        }
        updateFilesInPanel(project)
    }

    private fun createRepoContextFile(project: Project, isPatchFormatEnabled: Boolean) {
        val outputContextDir = getOutputContextDirectory(project)
        if (!outputContextDir.exists()) {
            try {
                Files.createDirectories(outputContextDir.toPath())
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputContextDir.parentFile)
                    ?.refresh(false, true)
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputContextDir)
                    ?.refresh(false, true)
            } catch (e: Exception) {
                Messages.showErrorDialog(project, "Error creating output directory '$OUTPUT_CONTEXT_DIRECTORY': ${e.message}", TOOL_WINDOW_TITLE)
                return
            }
        }

        val repoContextFile = File(outputContextDir, REPO_CONTEXT_FILE_NAME)
        val selectedFilesWithContent = getSelectedFilesWithContent(project)

        var combinedContent = ""

        if (selectedFilesWithContent.isNotEmpty()) {
            selectedFilesWithContent.forEach { (_, fileContent) ->
                combinedContent += fileContent + "\n"
            }
            if (combinedContent.isNotEmpty()) {
                combinedContent = combinedContent.trimEnd('\n')
            }
        } else {
            combinedContent = "// No files were selected from '$SOURCE_CONTEXT_DIRECTORY'.\n// '$REPO_CONTEXT_FILE_NAME' is intentionally empty or will only contain patch instructions."
        }

        try {
            var finalContent = combinedContent
            finalContent = finalContent.replace("\n\n$PATCH_FORMAT_INSTRUCTION", "").replace(PATCH_FORMAT_INSTRUCTION, "")

            if (isPatchFormatEnabled) {
                if (finalContent.isNotBlank() && !finalContent.endsWith("\n\n")) {
                    if (!finalContent.endsWith("\n")) {
                        finalContent += "\n"
                    }
                    finalContent += "\n"
                }
                finalContent += PATCH_FORMAT_INSTRUCTION
            }

            repoContextFile.writeText(finalContent)
            Messages.showInfoMessage(project, "$REPO_CONTEXT_FILE_NAME has been generated/updated successfully with selected content.", TOOL_WINDOW_TITLE)
            openFileInEditor(project, repoContextFile)

        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Error creating or updating repo context file: ${e.message}", TOOL_WINDOW_TITLE)
            println("Error in createRepoContextFile: ${e.message}")
        }
    }

    private fun deleteRepoContextFile(project: Project) {
        val repoFile = File(getOutputContextDirectory(project), REPO_CONTEXT_FILE_NAME)
        if (repoFile.exists() && repoFile.delete()) {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoFile.parentFile)?.refresh(false, true)
            Messages.showInfoMessage(project, "$REPO_CONTEXT_FILE_NAME deleted.", TOOL_WINDOW_TITLE)
        } else {
            Messages.showErrorDialog(project, "Could not delete $REPO_CONTEXT_FILE_NAME.", TOOL_WINDOW_TITLE)
        }
    }

    private fun openFileInEditor(project: Project, file: File) {
        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (vFile != null) {
            vFile.refresh(false, false)
            FileEditorManager.getInstance(project).openFile(vFile, true)
        }
    }

    private fun getSourceContextDirectory(project: Project) =
        Paths.get(project.basePath ?: ".", SOURCE_CONTEXT_DIRECTORY).toFile()

    private fun getOutputContextDirectory(project: Project) =
        Paths.get(project.basePath ?: ".", OUTPUT_CONTEXT_DIRECTORY).toFile()

    private fun getSelectedFilesWithContent(project: Project): Map<String, String> {
        val dir = getSourceContextDirectory(project)
        val content = mutableMapOf<String, String>()
        for (i in 0 until filesTableModel.rowCount) {
            val selected = filesTableModel.getValueAt(i, 0) as? Boolean ?: false
            val name = filesTableModel.getValueAt(i, 1) as? String ?: continue
            if (selected && !name.startsWith("The directory") && !name.startsWith("Could not list") && !name.startsWith("Created directory")) {
                val file = File(dir, name)
                content[name] = if (file.exists()) file.readText() else "Error: File not found."
            }
        }
        return content
    }

    private fun updateFilesInPanel(project: Project) {
        filesTableModel.rowCount = 0
        val dir = getSourceContextDirectory(project)
        try {
            if (!dir.exists()) Files.createDirectories(dir.toPath())
            val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
            if (files.isEmpty()) {
                filesTableModel.addRow(arrayOf(false, "The directory '$SOURCE_CONTEXT_DIRECTORY' is empty."))
            } else {
                files.sortedBy { it.name }.forEach { filesTableModel.addRow(arrayOf(false, it.name)) }
            }
        } catch (e: Exception) {
            filesTableModel.addRow(arrayOf(false, "Error: ${e.message}"))
        }
    }


    private fun createSettingsPanel(
        project: Project,
        settingsLabelText: String,
        togglePatchText: String,
        createRepoContextButtonText: String,
        deleteRepoContextButtonText: String
    ): JPanel {
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        if (LAYOUT_DEBUG_ENABLED) topPanel.border = BorderFactory.createLineBorder(JBColor.YELLOW) // Consider making border optional or a parameter

        val settingsLabel = JBLabel(settingsLabelText)
        val togglePatch = JBCheckBox(togglePatchText, true) // Default state might need to be a parameter or managed elsewhere
        togglePatch.alignmentX = Component.LEFT_ALIGNMENT // Align checkbox to the left within its panel
        val createRepoContextButton = JButton(createRepoContextButtonText, AllIcons.Actions.AddFile)
        createRepoContextButton.addActionListener {
            createRepoContextFile(project, togglePatch.isSelected)
        }
        val deleteRepoContextButton = JButton(deleteRepoContextButtonText, AllIcons.Actions.GC)
        deleteRepoContextButton.addActionListener {
            deleteRepoContextFile(project)
        }


        listOf(
            settingsLabel, Box.createVerticalStrut(10),
            togglePatch, Box.createVerticalStrut(10),
            createRepoContextButton, Box.createVerticalStrut(10),
            deleteRepoContextButton, Box.createVerticalStrut(10),
        ).forEach {
            topPanel.add(it)
        }

        // Wrap in a background panel to control X_AXIS alignment if needed,
        // or return topPanel directly if the BoxLayout X_AXIS behavior is desired.
        val topPanelBackground = JPanel()
        topPanelBackground.layout = BoxLayout(topPanelBackground, BoxLayout.X_AXIS)
        topPanelBackground.add(topPanel)

        return topPanelBackground // or return topPanel directly
    }

    private fun createFileActionsPanel(project: Project): JPanel {
        val fileActionsPanel = JPanel()
        fileActionsPanel.layout = BoxLayout(fileActionsPanel, BoxLayout.X_AXIS)

        val refreshFilesButton = JButton("Refresh List", AllIcons.Actions.Refresh)
        refreshFilesButton.addActionListener { updateFilesInPanel(project) }

        val addNewSourceFileButton = JButton(ADD_NEW_SOURCE_FILE_BUTTON_TEXT, AllIcons.Actions.AddFile)
        addNewSourceFileButton.addActionListener { addNewSourceFile(project) }

        fileActionsPanel.add(refreshFilesButton)
        fileActionsPanel.add(Box.createRigidArea(Dimension(5, 0)))
        fileActionsPanel.add(addNewSourceFileButton)
        fileActionsPanel.add(Box.createHorizontalGlue()) // Pushes buttons to the left
        if (LAYOUT_DEBUG_ENABLED) fileActionsPanel.border = BorderFactory.createLineBorder(JBColor.YELLOW) // Optional: make border a parameter or remove

        return fileActionsPanel
    }


    private fun createFilesTablePanel(project: Project): JPanel {
        val filesPanel = JPanel()
        filesPanel.layout = BoxLayout(filesPanel, BoxLayout.Y_AXIS)

        // Files Label setup
        val filesLabelPanel = JPanel()
        filesLabelPanel.layout = BoxLayout(filesLabelPanel, BoxLayout.X_AXIS)
        val filesLabel = JBLabel(FILES_LABEL_TEXT)
        filesLabel.alignmentX = Component.LEFT_ALIGNMENT // Align label to the left within its panel
        filesLabelPanel.add(filesLabel)
        filesLabelPanel.add(Box.createHorizontalGlue()) // Pushes label to the left
        if (LAYOUT_DEBUG_ENABLED) filesLabelPanel.border = BorderFactory.createLineBorder(JBColor.YELLOW)

        // Files Table setup
        val columnNames = arrayOf("Selected", "File Name")

        filesTableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column == 0 // Only the "Selected" checkbox is editable
            }

            override fun getColumnClass(columnIndex: Int): Class<*> {
                return if (columnIndex == 0) Boolean::class.javaObjectType else String::class.java
            }
        }
        filesTable = JBTable(filesTableModel)
        filesTable.columnModel.getColumn(0).preferredWidth = 70 // Make checkbox column narrower
        filesTable.columnModel.getColumn(0).maxWidth = 70

        val filesScrollPane = JBScrollPane(filesTable)
        filesScrollPane.preferredSize = Dimension(0, 200) // Adjust height as needed

        if (LAYOUT_DEBUG_ENABLED) filesScrollPane.border = BorderFactory.createLineBorder(JBColor.GREEN)

        filesPanel.add(filesLabelPanel)
        filesPanel.add(Box.createVerticalStrut(5)) // Spacing between label and table
        filesPanel.add(filesScrollPane)

        // Initial population of the table
        updateFilesInPanel(project)

        return filesPanel
    }


}
