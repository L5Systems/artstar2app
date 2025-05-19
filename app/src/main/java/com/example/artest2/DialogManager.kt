package com.example.artest2


class DialogManager {
    // You can still have other constants or helper functions here
    public enum class DialogType { // Make sure this exact name matches your nav_graph
        VESSEL_SELECTION,
        USER_INPUT_TEXT,
        CONFIRMATION,
        INFO_DISPLAY,
        QUANTITY_INPUT_FRAGMENT,
        TEXT_INPUT_DIALOG,
        VESSEL_SELECTION_FRAGMENT,

        CONFIRMATION_DIALOG,
        BUNKER_DETAILS_FRAGMENT,
        // Add other generic dialog types as needed
        GENERIC_MESSAGE
    }
   val SOME_CONSTANT = "value"

    // The DialogType enum is now top-level, but DialogManager can still exist

}
