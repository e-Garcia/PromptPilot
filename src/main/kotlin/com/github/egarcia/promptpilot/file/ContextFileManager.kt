package com.github.egarcia.promptpilot.file

import com.github.egarcia.promptpilot.FileConstants
import com.github.egarcia.promptpilot.SettingsKeys
import com.github.egarcia.promptpilot.resources.MyBundle
import com.github.egarcia.promptpilot.resources.Strings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Paths

class ContextFileManager(
    private val project: Project,
    private val fsOps: FileSystemOps = FileSystemOpsImpl()
) {
    private val basePath = project.basePath ?: "."
    private val sourceDir = Paths.get(basePath, FileConstants.SOURCE_CONTEXT_DIR).toFile()
    private val properties get() = PropertiesComponent.getInstance(project)
    private val localFS = LocalFileSystem.getInstance()

    private val outputDir: File
        get() = properties.getValue(SettingsKeys.CUSTOM_OUTPUT_DIR)
            ?.let { Paths.get(basePath, it).toFile() }
            ?: Paths.get(basePath, FileConstants.OUTPUT_DIR).toFile()

    private val outputFilename: String
        get() = properties.getValue(SettingsKeys.CUSTOM_OUTPUT_FILENAME)
            ?: FileConstants.REPO_CONTEXT_FILENAME

    fun ensureDirectoriesExist() {
        runCatching {
            if (!fsOps.exists(sourceDir)) fsOps.createDirectories(sourceDir.toPath())
            if (!fsOps.exists(outputDir)) fsOps.createDirectories(outputDir.toPath())
        }.onFailure { e ->
            throw IllegalStateException(
                MyBundle.message(
                    Strings.ERROR_CREATING_OUTPUT_DIRECTORY,
                    FileConstants.OUTPUT_DIR,
                    e.message ?: MyBundle.message(Strings.ERROR_UNKNOWN)
                )
            )
        }
    }

    fun listSourceFiles(): Result<List<File>> = runCatching {
        fsOps.listFiles(sourceDir)?.filter { it.isFile }
            ?: throw IllegalStateException(MyBundle.message(Strings.ERROR_FILES_LIST_SOURCE_FILES_FAILED))
    }

    fun openFile(file: File) {
        localFS.refreshAndFindFileByIoFile(file)?.let {
            it.refresh(false, false)
            FileEditorManager.getInstance(project).openFile(it, true)
        }
    }

    fun createSampleFileFromTemplate(fileName: String, templateResourceName: String): Result<File> =
        runCatching {
            val sanitized = sanitizeFileName(fileName)

            val finalName = if (!sanitized.endsWith(FileConstants.DEFAULT_FILE_EXTENSION))
                "$sanitized${FileConstants.DEFAULT_FILE_EXTENSION}" else sanitized

            val newFile = File(sourceDir, finalName)
            if (fsOps.exists(newFile)) {
                error(MyBundle.message(Strings.WARNING_FILE_ALREADY_EXISTS, finalName))
            }

            val content = javaClass.classLoader.getResourceAsStream(templateResourceName)
                ?.bufferedReader()?.use { it.readText() }
                ?: error(MyBundle.message(Strings.ERROR_FILE_NOT_FOUND, templateResourceName))

            fsOps.writeToFile(newFile, content)
            newFile
        }

    fun createRepoContextFileFromContent(
        selectedFilesContent: Map<String, String>,
        isPatchFormatEnabled: Boolean
    ): Result<File> = runCatching {
        val repoFile = getOutputFile()
        var content = if (selectedFilesContent.isEmpty()) {
            MyBundle.message(
                Strings.NO_FILES_SELECTED_MESSAGE,
                FileConstants.SOURCE_CONTEXT_DIR,
                outputFilename
            )
        } else {
            selectedFilesContent.values.joinToString("\n").trimEnd()
        }

        content = content.replace("\n\n${FileConstants.PATCH_FORMAT_INSTRUCTION}", "")
            .replace(FileConstants.PATCH_FORMAT_INSTRUCTION, "")

        if (isPatchFormatEnabled) {
            if (content.isNotBlank() && !content.endsWith("\n\n")) {
                content += if (content.endsWith("\n")) "\n" else "\n\n"
            }
            content += FileConstants.PATCH_FORMAT_INSTRUCTION
        }

        repoFile.writeText(content)
        repoFile
    }


    fun deleteRepoContextFile(): Result<Unit> = runCatching {
        val file = getOutputFile()
        when {
            !fsOps.exists(file) -> error(MyBundle.message(Strings.ERROR_FILE_NOT_FOUND, file.name))
            !fsOps.delete(file) -> error(MyBundle.message(Strings.ERROR_FILE_DELETE_FAILED, file.name))
            else -> localFS.refreshAndFindFileByIoFile(file.parentFile)?.refresh(false, true)
        }
    }

    fun readSelectedFilesWithContent(selectedFiles: List<String>): Result<Map<String, String>> =
        runCatching {
            val contents = mutableMapOf<String, String>()
            for (fileName in selectedFiles) {
                val file = File(sourceDir, fileName)
                contents[fileName] = if (fsOps.exists(file)) {
                    fsOps.readText(file)
                } else {
                    MyBundle.message(Strings.ERROR_FILE_NOT_FOUND, fileName)
                }
            }
            contents
        }

    fun getOutputFile(): File = File(outputDir, outputFilename)

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(File.separatorChar, '_')
            .replace("..", "_")
            .replace(Regex(FileConstants.INVALID_FILENAME_REGEX), "_")
    }
}
