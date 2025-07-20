package com.github.egarcia.promptpilot.toolWindow

import com.github.egarcia.promptpilot.FileConstants
import com.github.egarcia.promptpilot.resources.MyBundle
import com.github.egarcia.promptpilot.SettingsKeys
import com.github.egarcia.promptpilot.resources.Dimensions
import com.github.egarcia.promptpilot.resources.Strings
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
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

class PromptPilotToolWindowFactory : ToolWindowFactory {
    val isDebugLayoutEnabled = SettingsKeys.DEBUG_LAYOUT
    val selectedColumnIndex = 0
    val fileNameColumnIndex = 1

    private lateinit var filesTableModel: DefaultTableModel
    private lateinit var filesTable: JBTable
    private lateinit var projectInstance: Project
    private val windowTitle by lazy { MyBundle.message(Strings.TOOL_WINDOW_TITLE) }
    private val unknownErrorMessage by lazy { MyBundle.message(Strings.ERROR_UNKNOWN) }

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
        val topSettingsPanel = createSettingsPanel(
            project,
            MyBundle.message(Strings.SETTINGS_LABEL),
            MyBundle.message(Strings.TOGGLE_PATCH_TEXT),
            MyBundle.message(Strings.CREATE_REPO_CONTEXT_BUTTON),
            MyBundle.message(Strings.DELETE_REPO_CONTEXT_BUTTON)
        )

        val filesScrollPane = createFilesTablePanel(project)
        val fileActionsPanel = createFileActionsPanel(project)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT

        panel.add(topSettingsPanel)
        panel.add(Box.createVerticalStrut(Dimensions.SPACING_X_SMALL))
        panel.add(filesScrollPane)
        panel.add(Box.createVerticalStrut(Dimensions.SPACING_X_SMALL))
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
            MyBundle.message(Strings.NEW_SOURCE_FILE_DIALOG_MESSAGE),
            MyBundle.message(Strings.NEW_SOURCE_FILE_DIALOG_TITLE),
            Messages.getQuestionIcon(),
            FileConstants.DEFAULT_NEW_FILE,
            null
        ) ?: return

        val sanitized = fileName.replace(File.separatorChar, '_').replace("..", "_")
        val finalName = if (!sanitized.endsWith(FileConstants.DEFAULT_FILE_EXTENSION)) "$sanitized${FileConstants.DEFAULT_FILE_EXTENSION}" else sanitized
        val newFile = File(sourceDir, finalName)

        if (newFile.exists()) {
            Messages.showWarningDialog(
                project,
                MyBundle.message(Strings.WARNING_FILE_ALREADY_EXISTS, finalName),
                windowTitle
            )
            return
        }

        val content =
            javaClass.classLoader.getResourceAsStream(FileConstants.SAMPLE_CONTEXT_FILENAME)
                ?.bufferedReader()?.use { it.readText() }
                ?: run {
                    Messages.showErrorDialog(
                        project,
                        MyBundle.message(
                            Strings.ERROR_FILE_NOT_FOUND,
                            FileConstants.SAMPLE_CONTEXT_FILENAME
                        ),
                        windowTitle
                    )
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
                LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(outputContextDir.parentFile)?.refresh(false, true)
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputContextDir)
                    ?.refresh(false, true)
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    MyBundle.message(
                        Strings.ERROR_CREATING_OUTPUT_DIRECTORY,
                        FileConstants.OUTPUT_DIR,
                        e.message ?: unknownErrorMessage
                    ),
                    windowTitle
                )
                return
            }
        }

        val repoContextFile = File(outputContextDir, FileConstants.REPO_CONTEXT_FILENAME)
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
            combinedContent =
                "// No files were selected from '${FileConstants.SOURCE_CONTEXT_DIR}'.\n// '${FileConstants.REPO_CONTEXT_FILENAME}' is intentionally empty or will only contain patch instructions."
        }

        try {
            var finalContent = combinedContent
            finalContent = finalContent.replace("\n\n${FileConstants.PATCH_FORMAT_INSTRUCTION}", "")
                .replace(FileConstants.PATCH_FORMAT_INSTRUCTION, "")

            if (isPatchFormatEnabled) {
                if (finalContent.isNotBlank() && !finalContent.endsWith("\n\n")) {
                    if (!finalContent.endsWith("\n")) {
                        finalContent += "\n"
                    }
                    finalContent += "\n"
                }
                finalContent += FileConstants.PATCH_FORMAT_INSTRUCTION
            }

            repoContextFile.writeText(finalContent)
            Messages.showInfoMessage(
                project,
                MyBundle.message(
                    Strings.FILE_GENERATED_SUCCESS,
                    FileConstants.REPO_CONTEXT_FILENAME
                ),
                windowTitle
            )
            openFileInEditor(project, repoContextFile)

        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                MyBundle.message(
                    Strings.ERROR_FILE_CREATION_FAILED,
                    e.message ?: unknownErrorMessage
                ),
                windowTitle
            )
        }
    }

    private fun deleteRepoContextFile(project: Project) {
        val repoFile = File(getOutputContextDirectory(project), FileConstants.REPO_CONTEXT_FILENAME)
        if (repoFile.exists() && repoFile.delete()) {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoFile.parentFile)
                ?.refresh(false, true)
            Messages.showInfoMessage(
                project,
                // REPO_CONTEXT_DELETED_SUCCESS
                MyBundle.message(
                    Strings.FILE_DELETED_SUCCESS,
                    FileConstants.REPO_CONTEXT_FILENAME
                ),
                windowTitle
            )
        } else {
            Messages.showErrorDialog(
                project,
                MyBundle.message(
                    Strings.ERROR_FILE_DELETE_FAILED,
                    FileConstants.REPO_CONTEXT_FILENAME
                ),
                windowTitle
            )
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
        Paths.get(project.basePath ?: ".", FileConstants.SOURCE_CONTEXT_DIR).toFile()

    private fun getOutputContextDirectory(project: Project) =
        Paths.get(project.basePath ?: ".", FileConstants.OUTPUT_DIR).toFile()

    private fun getSelectedFilesWithContent(project: Project): Map<String, String> {
        val dir = getSourceContextDirectory(project)
        val content = mutableMapOf<String, String>()
        for (i in 0 until filesTableModel.rowCount) {
            val selected = filesTableModel.getValueAt(i, selectedColumnIndex) as? Boolean ?: false
            val name = filesTableModel.getValueAt(i, fileNameColumnIndex) as? String ?: continue
            if (selected && !name.startsWith("The directory") && !name.startsWith("Could not list") && !name.startsWith(
                    "Created directory"
                )
            ) {
                val file = File(dir, name)
                content[name] = if (file.exists()) file.readText() else MyBundle.message(
                    Strings.ERROR_FILE_NOT_FOUND,
                    name
                )
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
                filesTableModel.addRow(
                    arrayOf<Any>(
                        false,
                        MyBundle.message(
                            Strings.ERROR_DIRECTORY_EMPTY,
                            FileConstants.SOURCE_CONTEXT_DIR
                        )
                    )
                )
            } else {
                files.sortedBy { it.name }
                    .forEach { filesTableModel.addRow(arrayOf(false, it.name)) }
            }
        } catch (e: Exception) {
            filesTableModel.addRow(
                arrayOf<Any>(
                    false,
                    MyBundle.message(
                        Strings.ERROR_FILES_LIST,
                        e.message ?: unknownErrorMessage
                    )
                )
            )
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
        if (isDebugLayoutEnabled) topPanel.border =
            BorderFactory.createLineBorder(JBColor.YELLOW) // Consider making border optional or a parameter

        val settingsLabel = JBLabel(settingsLabelText)
        val togglePatch = JBCheckBox(
            togglePatchText,
            true
        ) // Default state might need to be a parameter or managed elsewhere
        togglePatch.alignmentX =
            Component.LEFT_ALIGNMENT // Align checkbox to the left within its panel
        val createRepoContextButton = JButton(createRepoContextButtonText, AllIcons.Actions.AddFile)
        createRepoContextButton.addActionListener {
            createRepoContextFile(project, togglePatch.isSelected)
        }
        val deleteRepoContextButton = JButton(deleteRepoContextButtonText, AllIcons.Actions.GC)
        deleteRepoContextButton.addActionListener {
            deleteRepoContextFile(project)
        }


        listOf(
            settingsLabel, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
            togglePatch, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
            createRepoContextButton, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
            deleteRepoContextButton, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
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

        val refreshFilesButton = JButton(
            MyBundle.message(Strings.REFRESH_LIST_BUTTON),
            AllIcons.Actions.Refresh
        )
        refreshFilesButton.addActionListener { updateFilesInPanel(project) }

        val addNewSourceFileButton =
            JButton(
                MyBundle.message(Strings.ADD_NEW_SOURCE_FILE_BUTTON),
                AllIcons.Actions.AddFile
            )
        addNewSourceFileButton.addActionListener { addNewSourceFile(project) }

        fileActionsPanel.add(refreshFilesButton)
        fileActionsPanel.add(Box.createRigidArea(Dimension(Dimensions.SPACING_X_SMALL, 0)))
        fileActionsPanel.add(addNewSourceFileButton)
        fileActionsPanel.add(Box.createHorizontalGlue())

        if (isDebugLayoutEnabled) fileActionsPanel.border =
            BorderFactory.createLineBorder(JBColor.YELLOW)

        return fileActionsPanel
    }

    private fun createFilesTablePanel(project: Project): JPanel {
        val filesPanel = JPanel()
        filesPanel.layout = BoxLayout(filesPanel, BoxLayout.Y_AXIS)

        // Files Label setup
        val filesLabelPanel = JPanel()
        filesLabelPanel.layout = BoxLayout(filesLabelPanel, BoxLayout.X_AXIS)
        val filesLabel = JBLabel(
            MyBundle.message(
                Strings.FILES_LABEL,
                FileConstants.SOURCE_CONTEXT_DIR
            )
        )
        filesLabel.alignmentX = Component.LEFT_ALIGNMENT
        filesLabelPanel.add(filesLabel)
        filesLabelPanel.add(Box.createHorizontalGlue())
        if (isDebugLayoutEnabled) {
            filesLabelPanel.border = BorderFactory.createLineBorder(JBColor.YELLOW)
        }

        // Files Table setup
        val columnNames = arrayOf(
            MyBundle.message(Strings.SELECTED_COLUMN_LABEL),
            MyBundle.message(Strings.FILE_NAME_COLUMN_LABEL)
        )

        filesTableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = column == 0
            override fun getColumnClass(columnIndex: Int): Class<*> =
                if (columnIndex == 0) Boolean::class.javaObjectType else String::class.java
        }

        filesTable = JBTable(filesTableModel)
        filesTable.columnModel.getColumn(selectedColumnIndex).preferredWidth = Dimensions.FILE_SELECTED_WIDTH
        filesTable.columnModel.getColumn(selectedColumnIndex).maxWidth = Dimensions.FILE_SELECTED_WIDTH

        val filesScrollPane = JBScrollPane(filesTable)
        if (isDebugLayoutEnabled) {
            filesScrollPane.border = BorderFactory.createLineBorder(JBColor.GREEN)
        }

        filesPanel.add(filesLabelPanel)
        filesPanel.add(Box.createVerticalStrut(Dimensions.SPACING_SMALL))
        filesPanel.add(filesScrollPane)

        updateFilesInPanel(project)

        return filesPanel
    }


}
