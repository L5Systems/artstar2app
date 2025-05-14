package com.example.artest2.core

abstract class BaseTransaction(
    val transactionID: Long,
    val transactionName: String
) {
    val states: MutableList<StateBase> = mutableListOf()
    var currentState: StateBase? = null
    val context: MutableMap<String, Any> = mutableMapOf() // Shared context across states

    // Holds the status specific to this transaction instance when it was saved/loaded.
    // Initialized with default values.
    var transactionStatus: TransStatus = TransStatus(
        selectedTransactionId = transactionID,
        selectedTransactionName = transactionName,
        transactionStatus = "inactive"
    )

    private val transLevelData: MutableList<TransLevelData> = mutableListOf()

    abstract fun getStateEntries(): List<StateNode>
    abstract fun isValidStateTransition(currentStateKey: String, nextStateKey: String): Boolean

    fun getName(): String = transactionName

    fun saveStatus(currentGlobalStatus: TransStatus) {
        // Save the current global status as this transaction's specific status
        this.transactionStatus = currentGlobalStatus.copy()
    }

    fun loadStatus(): TransStatus {
        // Return this transaction's saved status to be applied globally
        return this.transactionStatus.copy()
    }

    fun submitTransLevelData(key: String, submittingState: String, dataType: String, value: Any) {
        transLevelData.add(TransLevelData(key, submittingState, dataType, value))
    }

    fun getTransLevelData(): List<TransLevelData> = transLevelData.toList()

    fun clearTransactionLevelData() {
        transLevelData.clear()
    }

    // This would be implemented to interact with the Android UI, perhaps via ViewModel callbacks
    open fun executeRemoteDialogCmd(cmd: String) {
        println("BaseTransaction: Command received: $cmd. Override in concrete transaction to handle UI.")
        // Example: viewModel.postCommand(cmd)
    }
}
