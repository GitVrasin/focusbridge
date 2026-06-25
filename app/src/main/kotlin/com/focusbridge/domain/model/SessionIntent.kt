package com.focusbridge.domain.model

enum class SessionIntent(val label: String) {
    BOREDOM("boredom"),
    HABIT("habit"),
    BREAK_REST("break / rest"),
    AVOID_TASK("avoid task"),
    SPECIFIC_GOAL("specific goal")
}
