package com.github.egarcia.promptpilot.toolWindow

import com.github.egarcia.promptpilot.FileConstants
import com.github.egarcia.promptpilot.SettingsKeys
import com.github.egarcia.promptpilot.file.ContextFileManager
import com.github.egarcia.promptpilot.resources.Dimensions
import com.github.egarcia.promptpilot.resources.MyBundle
import com.github.egarcia.promptpilot.resources.Strings
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.CollapsiblePanel
import com.intellij.ui.HideableDecorator
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.table.DefaultTableModel

class PromptPilotToolWindowFactory : ToolWindowFactory {
    val selectedColumnIndex = 0
    val fileNameColumnIndex = 1

    private lateinit var filesTableModel: DefaultTableModel
    private lateinit var filesTable: JBTable
    private lateinit var fileManager: ContextFileManager
    private val windowTitle by lazy { MyBundle.message(Strings.TOOL_WINDOW_TITLE) }
    private val unknownErrorMessage by lazy { MyBundle.message(Strings.ERROR_UNKNOWN) }


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        fileManager = ContextFileManager(project)
        fileManager.ensureDirectoriesExist()

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

        updateFilesInPanel()

        val outerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        outerPanel.add(panel)
        return outerPanel
    }

    private fun addNewSourceFile(project: Project) {
        val input = Messages.showInputDialog(
            project,
            MyBundle.message(Strings.NEW_SOURCE_FILE_DIALOG_MESSAGE),
            MyBundle.message(Strings.NEW_SOURCE_FILE_DIALOG_TITLE),
            Messages.getQuestionIcon(),
            FileConstants.DEFAULT_NEW_FILE,
            null
        )?.trim()

        if (input.isNullOrEmpty()) {
            Messages.showErrorDialog(
                project,
                MyBundle.message(Strings.ERROR_EMPTY_FILE_NAME),
                windowTitle
            )
            return
        }

        fileManager.createSampleFileFromTemplate(input, FileConstants.SAMPLE_CONTEXT_FILENAME)
            .onSuccess { file ->
                fileManager.openFile(file)
                updateFilesInPanel()
            }
            .onFailure { e ->
                Messages.showErrorDialog(
                    project,
                    e.message ?: unknownErrorMessage,
                    windowTitle
                )
            }
    }


    private fun createRepoContextFile(project: Project, isPatchFormatEnabled: Boolean) {
        val selectedFiles = getSelectedFileNames()

        fileManager.readSelectedFilesWithContent(selectedFiles)
            .mapCatching { fileContents ->
                fileManager.createRepoContextFileFromContent(fileContents, isPatchFormatEnabled)
                    .getOrThrow()
            }
            .onSuccess { file ->
                Messages.showInfoMessage(
                    project,
                    MyBundle.message(Strings.FILE_GENERATED_SUCCESS, file.name),
                    windowTitle
                )
                fileManager.openFile(file)
            }
            .onFailure { e ->
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
        fileManager.deleteRepoContextFile()
            .onSuccess {
                Messages.showInfoMessage(
                    project,
                    MyBundle.message(
                        Strings.FILE_DELETED_SUCCESS,
                        FileConstants.REPO_CONTEXT_FILENAME
                    ),
                    windowTitle
                )
            }
            .onFailure { e ->
                Messages.showErrorDialog(
                    project,
                    e.message ?: unknownErrorMessage,
                    windowTitle
                )
            }
    }

    private fun getSelectedFileNames(): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until filesTableModel.rowCount) {
            val isSelected = filesTableModel.getValueAt(i, selectedColumnIndex) as? Boolean ?: false
            val name = filesTableModel.getValueAt(i, fileNameColumnIndex) as? String ?: continue
            if (isSelected && !name.startsWith("The directory") && !name.startsWith("Could not list")) {
                selected.add(name)
            }
        }
        return selected
    }


    private fun updateFilesInPanel() {
        filesTableModel.rowCount = 0

        fileManager.listSourceFiles()
            .onSuccess { files ->
                if (files.isEmpty()) {
                    filesTableModel.addRow(
                        arrayOf<Any>(
                            false, MyBundle.message(
                                Strings.ERROR_DIRECTORY_EMPTY, FileConstants.SOURCE_CONTEXT_DIR
                            )
                        )
                    )
                } else {
                    files.sortedBy { it.name }.forEach { file ->
                        filesTableModel.addRow(arrayOf(false, file.name))
                    }
                }
            }
            .onFailure { e ->
                filesTableModel.addRow(
                    arrayOf<Any>(
                        false, MyBundle.message(
                            Strings.ERROR_FILES_LIST, e.message ?: unknownErrorMessage
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
        if (isDebugLayoutEnabled(project)) topPanel.border =
            BorderFactory.createLineBorder(JBColor.YELLOW) // Consider making border optional or a parameter

        val settingsLabel = JBLabel(settingsLabelText)

        val properties = PropertiesComponent.getInstance(project)
        val togglePatch = JBCheckBox(
            togglePatchText,
            isPatchFormatEnabled(project)
        )
        togglePatch.alignmentX = Component.LEFT_ALIGNMENT
        togglePatch.addChangeListener {
            properties.setValue(SettingsKeys.PATCH_TOGGLE_KEY, togglePatch.isSelected)
        }

        val customOutputDirLabel = JBLabel(MyBundle.message(Strings.CUSTOM_OUTPUT_DIR_LABEL))
        val customOutputFileLabel = JBLabel(MyBundle.message(Strings.CUSTOM_OUTPUT_FILE_LABEL))

        val customOutputDirField =
            JTextField(properties.getValue(SettingsKeys.CUSTOM_OUTPUT_DIR,  FileConstants.OUTPUT_DIR), 20)
        val customOutputFileField =
            JTextField(properties.getValue(SettingsKeys.CUSTOM_OUTPUT_FILENAME, FileConstants.REPO_CONTEXT_FILENAME), 20)

        val saveButton = JButton(MyBundle.message(Strings.SAVE_OUTPUT_SETTINGS_BUTTON), AllIcons.Actions.Commit)
        saveButton.addActionListener {
            properties.setValue(SettingsKeys.CUSTOM_OUTPUT_DIR, customOutputDirField.text.trim())
            properties.setValue(SettingsKeys.CUSTOM_OUTPUT_FILENAME, customOutputFileField.text.trim())
            Messages.showInfoMessage(
                project,
                MyBundle.message(Strings.SAVE_OUTPUT_SETTINGS_SUCCESS),
                windowTitle
            )
        }
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
            customOutputDirLabel, customOutputDirField, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
            customOutputFileLabel, customOutputFileField, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
            saveButton, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
            createRepoContextButton, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
            deleteRepoContextButton, Box.createVerticalStrut(Dimensions.SPACING_SMALL),
        ).forEach {
            topPanel.add(it)
        }

        val topPanelBackground = JPanel()
        topPanelBackground.layout = BoxLayout(topPanelBackground, BoxLayout.X_AXIS)
        topPanelBackground.add(topPanel)

        return topPanelBackground
    }

    private fun createFileActionsPanel(project: Project): JPanel {
        val fileActionsPanel = JPanel()
        fileActionsPanel.layout = BoxLayout(fileActionsPanel, BoxLayout.X_AXIS)

        val refreshFilesButton = JButton(
            MyBundle.message(Strings.REFRESH_LIST_BUTTON),
            AllIcons.Actions.Refresh
        )
        refreshFilesButton.addActionListener { updateFilesInPanel() }

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

        if (isDebugLayoutEnabled(project)) fileActionsPanel.border =
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
        if (isDebugLayoutEnabled(project)) {
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
        filesTable.columnModel.getColumn(selectedColumnIndex).preferredWidth =
            Dimensions.FILE_SELECTED_WIDTH
        filesTable.columnModel.getColumn(selectedColumnIndex).maxWidth =
            Dimensions.FILE_SELECTED_WIDTH

        val filesScrollPane = JBScrollPane(filesTable)
        if (isDebugLayoutEnabled(project)) {
            filesScrollPane.border = BorderFactory.createLineBorder(JBColor.GREEN)
        }

        filesPanel.add(filesLabelPanel)
        filesPanel.add(Box.createVerticalStrut(Dimensions.SPACING_SMALL))
        filesPanel.add(filesScrollPane)

        updateFilesInPanel()

        return filesPanel
    }

    private fun createAdvancedOutputSettingsPanel(project: Project): JPanel {
        val properties = PropertiesComponent.getInstance(project)

        val customOutputDirField = JTextField(
            properties.getValue(SettingsKeys.CUSTOM_OUTPUT_DIR, FileConstants.OUTPUT_DIR), 20
        )
        val customOutputFileField = JTextField(
            properties.getValue(SettingsKeys.CUSTOM_OUTPUT_FILENAME, FileConstants.REPO_CONTEXT_FILENAME), 20
        )

        val saveButton = JButton(
            MyBundle.message(Strings.SAVE_OUTPUT_SETTINGS_BUTTON),
            AllIcons.Actions.Commit
        )
        saveButton.alignmentX = Component.LEFT_ALIGNMENT
        saveButton.addActionListener {
            properties.setValue(SettingsKeys.CUSTOM_OUTPUT_DIR, customOutputDirField.text.trim())
            properties.setValue(SettingsKeys.CUSTOM_OUTPUT_FILENAME, customOutputFileField.text.trim())
            Messages.showInfoMessage(
                project,
                MyBundle.message(Strings.SAVE_OUTPUT_SETTINGS_SUCCESS),
                MyBundle.message(Strings.TOOL_WINDOW_TITLE)
            )
        }


        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        listOf(
            JBLabel(MyBundle.message(Strings.CUSTOM_OUTPUT_DIR_LABEL)), customOutputDirField,
            JBLabel(MyBundle.message(Strings.CUSTOM_OUTPUT_FILE_LABEL)), customOutputFileField,
            Box.createVerticalStrut(Dimensions.SPACING_X_SMALL),
            saveButton
        ).forEach { contentPanel.add(it) }

        val collapsiblePanel = CollapsiblePanel(
            contentPanel, // JComponent content
            true, // collapseButtonAtLeft
            false, // isCollapsed (default state)
            null, // collapseIcon (use default)
            null, // expandIcon (use default)
            MyBundle.message(Strings.ADVANCED_OUTPUT_SETTINGS_TITLE)
        )
        return collapsiblePanel
    }



    private fun isPatchFormatEnabled(project: Project): Boolean {
        return PropertiesComponent.getInstance(project)
            .getBoolean(SettingsKeys.PATCH_TOGGLE_KEY, false)
    }

    fun isDebugLayoutEnabled(project: Project): Boolean {
        return PropertiesComponent.getInstance(project)
            .getBoolean(SettingsKeys.DEBUG_LAYOUT_KEY, false)
    }

}
