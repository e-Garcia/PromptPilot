package com.github.egarcia.promptpilot

object SettingsKeys {
    const val DEBUG_LAYOUT = false
}

object FileConstants {
    const val OUTPUT_DIR = ".promptpilot"
    const val SOURCE_CONTEXT_DIR = ".promptpilot/source-context"
    const val REPO_CONTEXT_FILENAME = "repo-context.md"
    const val SAMPLE_CONTEXT_FILENAME = "sample-context.md"
    const val DEFAULT_NEW_FILE = "new-context-file.md"
    const val DEFAULT_FILE_EXTENSION = ".md"
    const val PATCH_FORMAT_INSTRUCTION = "- output: use the patch format"
    const val INVALID_FILENAME_REGEX = "[^a-zA-Z0-9._-]"
}