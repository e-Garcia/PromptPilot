package com.github.egarcia.promptpilot.file

import com.github.egarcia.promptpilot.FileConstants
import com.github.egarcia.promptpilot.SettingsKeys
import com.github.egarcia.promptpilot.file.ContextOutputTarget.Companion.fromId
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

    private val selectedTarget: ContextOutputTarget
        get() {
            val valueSet = properties.isValueSet(SettingsKeys.OUTPUT_TARGET_KEY)
            val storedTarget = properties.getValue(SettingsKeys.OUTPUT_TARGET_KEY)?.let { fromId(it) }
            if (storedTarget != null) return storedTarget

            if (!valueSet && hasCustomOverrides()) {
                return ContextOutputTarget.CUSTOM
            }
            return ContextOutputTarget.PROMPT_PILOT
        }

    private fun hasCustomOverrides(): Boolean {
        val customDir = properties.getValue(SettingsKeys.CUSTOM_OUTPUT_DIR)
        val customFile = properties.getValue(SettingsKeys.CUSTOM_OUTPUT_FILENAME)
        val dirDiffers = !customDir.isNullOrBlank() && customDir != FileConstants.OUTPUT_DIR
        val fileDiffers = !customFile.isNullOrBlank() && customFile != FileConstants.REPO_CONTEXT_FILENAME
        return dirDiffers || fileDiffers
    }

    private fun resolveOutputLocation(): OutputLocation {
        val target = selectedTarget
        val location = target.defaultLocation()
        val relativeDir = when {
            target.isCustom -> properties.getValue(SettingsKeys.CUSTOM_OUTPUT_DIR)?.takeUnless { it.isBlank() }
                ?: FileConstants.OUTPUT_DIR
            location != null -> location.relativeDir
            else -> FileConstants.OUTPUT_DIR
        }

        val filename = when {
            target.isCustom -> properties.getValue(SettingsKeys.CUSTOM_OUTPUT_FILENAME)?.takeUnless { it.isBlank() }
                ?: FileConstants.REPO_CONTEXT_FILENAME
            location != null -> location.filename
            else -> FileConstants.REPO_CONTEXT_FILENAME
        }

        return OutputLocation(relativeDir, filename)
    }

    fun ensureDirectoriesExist() {
        val location = resolveOutputLocation()
        var lastAttempted = FileConstants.SOURCE_CONTEXT_DIR
        runCatching {
            ensureDirectoryExists(sourceDir)
            lastAttempted = location.relativeDir
            val outputDir = Paths.get(basePath, location.relativeDir).normalize().toFile()
            ensureDirectoryExists(outputDir)
        }.onFailure { e ->
            throw IllegalStateException(
                MyBundle.message(
                    Strings.ERROR_CREATING_OUTPUT_DIRECTORY,
                    lastAttempted,
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
        repoFile.parentFile?.let { ensureDirectoryExists(it) }
        var content = if (selectedFilesContent.isEmpty()) {
            MyBundle.message(
                Strings.NO_FILES_SELECTED_MESSAGE,
                FileConstants.SOURCE_CONTEXT_DIR,
                repoFile.name
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

    fun getOutputFile(): File {
        val location = resolveOutputLocation()
        val directory = Paths.get(basePath, location.relativeDir).normalize().toFile()
        return File(directory, location.filename)
    }

    fun currentOutputTarget(): ContextOutputTarget = selectedTarget

    fun currentOutputLocation(): OutputLocation = resolveOutputLocation()

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(File.separatorChar, '_')
            .replace("..", "_")
            .replace(Regex(FileConstants.INVALID_FILENAME_REGEX), "_")
    }

    private fun ensureDirectoryExists(directory: File) {
        if (!fsOps.exists(directory)) {
            fsOps.createDirectories(directory.toPath())
        }
    }
}
