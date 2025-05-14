package com.example.artest2.core

import android.content.Context
import android.content.Intent
import com.example.artest2.manager.TransactionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class StateBase(
    private val stateName: String,
    private var parentTransaction: BaseTransaction
) {
    // Holds data specific to this state instance, potentially output from previous steps
    // or data to be passed to API calls during commit.
    private val stateSpecificData: MutableList<ApiCallData> = mutableListOf()

    /**
     * Fetches any necessary input data required for this state's operation.
     * This might involve API calls, database lookups, or accessing previous state data.
     * @param context Optional context object carrying data from previous states or the manager.
     * @return A map representing the fetched data.
     */
    abstract suspend fun fetchInputData(context: Map<String, Any>): Map<String, Any>

    /**
     * Executes the primary logic of this state.   xx
     * In a real app, this might trigger UI display for user interaction.
     * For this starter, it will mostly contain business logic.
     * @param inputData Data fetched by fetchInputData or passed from context.
     * @return A result from the execution, to be used by fetchReturnData.
     */
    abstract suspend fun executeDialog(inputData: Map<String, Any>, MainActivityContext:Context): Any

    /**
     * Fetches or prepares the data that needs to be returned or passed on
     * after the main execution.
     * @param executionResult The result from executeDialog.
     * @return A map representing the data to be committed or passed to the next state.
     */
    abstract suspend fun fetchReturnData(executionResult: Any): Map<String, Any>

    /**
     * Commits the changes or results of this state's operation.
     * This could involve saving data to a database, making API calls, etc.
     * @param returnData The data prepared by fetchReturnData.
     */
    abstract suspend fun commit(returnData: Map<String, Any>)

    /**
     * Rolls back any changes made during this state's operation in case of failure
     * or cancellation.
     * @param error Optional error object that caused the rollback.
     */
    abstract suspend fun rollback(error: Throwable? = null)

    fun getName(): String = stateName

    fun getTransactionStatus(): String = parentTransaction.transactionStatus.transactionStatus

    fun storeStateOutputData(data: ApiCallData): Boolean {
        stateSpecificData.add(data)
        return true
    }

    // Example of how a state might command its parent transaction (e.g., to update UI)
    open fun executeRemoteDialogCmd(cmd: String) {
        parentTransaction.executeRemoteDialogCmd("$cmd for ${getName()}")
    }
    // In StateBase or a separate class
    sealed class UiAction {
        data class ShowDialog(val intent: Intent) : UiAction()
        // ... other UI actions
    }

    // In your state machine or a ViewModel
    private val _uiActions = MutableSharedFlow<UiAction>()
    val uiActions = _uiActions.asSharedFlow()
}
