package com.focusbridge.domain.model

data class NextAction(
    val id: Long = 0,
    val goalId: Long,
    val label: String,
    val type: NextActionType,
    val target: String,
    val sortOrder: Int = 0
)

enum class NextActionType {
    URL,        // Open a URL in the browser or custom app
    NOTES,      // Open the internal notes screen
    CHECKLIST   // Open the internal checklist screen
}
