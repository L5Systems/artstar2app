package com.example.artest2


class DialogManager {
    // You can still have other constants or helper functions here
    public enum class DialogType { // Make sure this exact name matches your nav_graph
        VESSEL_SELECTION,
        USER_INPUT_TEXT,
        CONFIRMATION,
        INFO_DISPLAY
        // Add other generic dialog types as needed
    }
   val SOME_CONSTANT = "value"

    // The DialogType enum is now top-level, but DialogManager can still exist

}
