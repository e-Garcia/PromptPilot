package com.github.egarcia.promptpilot.resources

import org.jetbrains.annotations.NonNls

/**
 * Contains constants for keys defined in the MyBundle.properties resource bundle.
 * This provides a way to refer to localized strings with compile time safety
 * and autocompletion, similar to Android's R.string.
 */
object Strings {

    @NonNls
    const val TOOL_WINDOW_TITLE = "tool.window.title"

    @NonNls
    const val SETTINGS_LABEL = "settings.label"

    @NonNls
    const val TOGGLE_PATCH_TEXT = "toggle.patch.text"

    @NonNls
    const val CREATE_REPO_CONTEXT_BUTTON = "create.repo.context.button"

    @NonNls
    const val DELETE_REPO_CONTEXT_BUTTON = "delete.repo.context.button"
    @NonNls
    const val REFRESH_LIST_BUTTON = "refresh.list.button"

    @NonNls
    const val FILES_LABEL = "files.label"

    @NonNls
    const val ADD_NEW_SOURCE_FILE_BUTTON = "add.new.source.file.button"
    @NonNls
    const val NEW_SOURCE_FILE_DIALOG_TITLE = "new.source.file.dialog.title"
    @NonNls
    const val NEW_SOURCE_FILE_DIALOG_MESSAGE = "new.source.file.dialog.message"
    @NonNls
    const val SELECTED_COLUMN_LABEL = "selected.column.label"
    @NonNls
    const val FILE_NAME_COLUMN_LABEL = "file.name.column.label"

    @NonNls
    const val WARNING_FILE_ALREADY_EXISTS = "warning.file.already.exists"
    @NonNls
    const val ERROR_CREATING_OUTPUT_DIRECTORY = "error.creating.output.directory"
    @NonNls
    const val ERROR_DIRECTORY_EMPTY = "error.directory.empty"
    @NonNls
    const val ERROR_FILE_NOT_FOUND = "error.file.not.found"
    @NonNls
    const val ERROR_FILE_CREATION_FAILED = "error.file.creation.failed"
    @NonNls
    const val ERROR_FILE_DELETE_FAILED = "error.file.delete.failed"
    @NonNls
    const val FILE_GENERATED_SUCCESS = "success.file.context.generated"
    @NonNls
    const val FILE_DELETED_SUCCESS = "success.file.context.deleted"
    @NonNls
    const val ERROR_FILES_LIST = "error.files.list"
    @NonNls
    const val ERROR_UNKNOWN = "error.unknown"

}