package com.example.artest2.core

 enum class TransactionStatus(val displayName: String, val isTerminal: Boolean = false) {

        // --- Initial and Intermediate Statuses ---
        /**
         * Transaction has been initialized but not yet started processing.
         */
        INITIALIZED("Initialized"),

        /**
         * Transaction is currently active and processing its states.
         */
        PROCESSING("Processing"),

        /**
         * Transaction is temporarily paused, potentially waiting for external input
         * or a scheduled resumption. (Less common for fully automated flows but can be useful).
         */
        PAUSED("Paused"),

        /**
         * Transaction has requested a UI interaction (e.g., user input via a dialog/fragment)
         * and is suspended waiting for the UI to provide a result.
         */
        WAITING_FOR_UI("Waiting for UI"),

        /**
         * Transaction is in the process of rolling back changes due to an error or cancellation.
         */
        ROLLING_BACK("Rolling Back"),


        // --- Terminal Success Statuses ---
        /**
         * Transaction completed all its states successfully.
         */
        SUCCESS("Success", isTerminal = true),

        /**
         * Transaction completed, but with some warnings or non-critical issues.
         * Still considered a form of success.
         */
        SUCCESS_WITH_WARNINGS("Success with Warnings", isTerminal = true),


        // --- Terminal Failure/Cancellation Statuses ---
        /**
         * Transaction failed due to an error during processing or commit.
         */
        FAILURE("Failure", isTerminal = true),

        /**
         * Transaction was explicitly cancelled by the user or system.
         */
        CANCELLED("Cancelled", isTerminal = true),

        /**
         * Transaction timed out waiting for a resource, UI input, or state completion.
         */
        TIMED_OUT("Timed Out", isTerminal = true),

        /**
         * Transaction was rolled back to its initial state (or a safe state)
         * after an error.
         */
        ROLLED_BACK("Rolled Back", isTerminal = true),

        /**
         * An unknown or unrecoverable error occurred.
         */
        UNKNOWN_ERROR("Unknown Error", isTerminal = true);


        /**
         * Helper to quickly check if the status represents a successful outcome.
         */
        fun isSuccess(): Boolean {
            return this == SUCCESS || this == SUCCESS_WITH_WARNINGS
        }

        /**
         * Helper to quickly check if the status represents any kind of failure or cancellation.
         */
        fun isFailureOrCancelled(): Boolean {
            return this == FAILURE || this == CANCELLED || this == TIMED_OUT || this == ROLLED_BACK || this == UNKNOWN_ERROR
        }

        /**
         * Returns true if the transaction is in a state where it's actively doing work
         * or waiting for an immediate response (not paused or in a final state).
         */
        fun isActiveProcessing(): Boolean {
            return this == PROCESSING || this == WAITING_FOR_UI || this == ROLLING_BACK
        }
    }