package com.github.egarcia.promptpilot.backends

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

data class GeminiPromptContext(
    val intent: String,
    val target: String,
    val outputFormat: String,
    val instructions: List<String>,
    val style: String
)

object GeminiPromptContextWriter {
    private const val DEFAULT_FILE_NAME = "prompt-context.json"
    private const val CONTEXT_DIRECTORY = ".promptpilot"

    fun writeContextFile(
        project: Project,
        projectRoot: File,
        context: GeminiPromptContext,
        fileName: String = DEFAULT_FILE_NAME
    ) {
        require(projectRoot.exists() && projectRoot.isDirectory) { "Invalid project root directory: ${projectRoot.absolutePath}" }
        require(fileName.isNotBlank()) { "File name cannot be blank" }

        val outputDir = Paths.get(projectRoot.path, CONTEXT_DIRECTORY).toFile()
        Files.createDirectories(outputDir.toPath())
        val promptFile = File(outputDir, fileName)

        val gson = GsonBuilder().setPrettyPrinting().create()
        val outputJson = gson.toJson(context)

        runCatching {
            promptFile.writeText(outputJson)
        }.onSuccess {
            println("✅ Context file generated at: ${promptFile.absolutePath}")
            // Refresh the file in the VFS
            refreshFileInVFS(project, promptFile)
        }.onFailure {
            println("❌ Error writing context file: ${it.message}")
        }
    }

    /**
     * Refreshes the file in the Virtual File System (VFS) to ensure the editor tab updates.
     *
     * @param project The current project.
     * @param file The file to refresh.
     */
    private fun refreshFileInVFS(project: Project, file: File) {
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
        virtualFile?.refresh(false, false)
    }
}