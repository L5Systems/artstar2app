package com.example.artest2.manager

import android.app.Activity
import com.example.artest2.states.ExampleCommitState
import com.example.artest2.states.ExampleDataFetchingState
import com.example.artest2.states.ExampleProcessingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.artest2.DialogManager
import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateBase
import com.example.artest2.core.StateNode
import com.example.artest2.core.TransStatus
// ... other imports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

class TransactionManager(
    private val application: Application,
    private val externalScope: CoroutineScope // Scope from ViewModel or Application
) {
    companion object {
        const val CONSTANT_STRING = "This is a compile-time constant" // Compile-time constant
        private val _uiActions = MutableSharedFlow<UiAction>()
        val uiActions = _uiActions.asSharedFlow()

        val SOME_PROPERTY = "This is a regular property in a companion object"
        var mutableProperty = 100

        fun utilityFunction() {
            println("Utility function called from companion object. Mutable property: $mutableProperty")
        }
        suspend fun emitUiAction(action: UiAction) {
            _uiActions.emit(action)
        }

    }

    fun instanceMethod() {
        println("Accessing companion object property from instance: ${TransactionManager.SOME_PROPERTY}")
    }



private val transactionFactory: TransactionFactory = TransactionFactory(application) // Initialize factory
    private var transactionPrototypes: MutableMap<String, (Long, String) -> BaseTransaction> =
        mutableMapOf()

    private var openTransactions: MutableList<BaseTransaction> = mutableListOf()
    var fragment: Fragment? = null
    private var activeTransactionId: Long = -1
    private val transactions = mutableMapOf<String, BaseTransaction>()

    // --- UI Action Emitter (for DashboardFragment/Activity to observe) ---
    private var _currentTransStatus = MutableStateFlow(TransStatus()) // Your existing status flow
    val currentTransStatus: StateFlow<TransStatus> = _currentTransStatus.asStateFlow()

    private val transactionMutex = Mutex()
    private var currentTransactionJob: Job? = null

    // Assuming you have a map to store the pending requests:
    private val pendingUiRequests =
        ConcurrentHashMap<String, CompletableDeferred<Map<String, Any>?>>()

    // --- UI Action Emitter (for DashboardFragment/Activity to observe) ---
    // Assuming you have this from previous discussions



    // --- For managing results from UI interactions ---
    // Key: requestId, Value: CompletableDeferred waiting for the UI result

    private var activeTransaction: BaseTransaction? = null
     //************************************************************************************
    // Data class for states to describe the UI input they need.
    // This is passed by the State to the TransactionManager.
    // It does NOT contain internal TM details like requestId or the final onResult.
    //************************************************************************************
    data class InputScreenConfig(
        val dialogType: DialogManager.DialogType, // What kind of input/dialog is it?
        val title: String,                        // Generic title for the screen/dialog
        val message: String? = null,              // Optional message/prompt
        val positiveButtonText: String? = "Confirm",
        val negativeButtonText: String? = "Cancel",
        val neutralButtonText: String? = null,    // If applicable
        val inputHint: String? = null,            // For dialogs/screens that need text input
        val items: List<String>? = null,          // For dialogs/screens that show a list/spinner
        val initialSelection: String? = null,     // For pre-selecting an item in a list/spinner
        val customData: Map<String, Any> = emptyMap() // For any other specific data the UI might need
        // NO requestId, NO onResult callback here. TM handles those.
    )
    //************************************************************************************

    // Sealed class representing actions the UI (DashboardFragment/Activity) should perform.
    // This is emitted by TransactionManager and collected by the UI layer.
    sealed class UiAction {

        data class RequestInputScreen(
            val requestId: String, // Generated by TransactionManager
            val transactionId: String, // ID of the transaction requesting this
            // Fields from InputScreenConfig are copied here by TransactionManager
            val prompt: String,
            val screenIdentifier: String,
            val dialogType: DialogManager.DialogType,
            val title: String,
            val message: String?,
            val positiveButtonText: String?,
            val negativeButtonText: String?,
            val neutralButtonText: String?,
            val inputHint: String?,
            val items: List<String>?,
            val initialSelection: String?,
            val customData: Map<String, Any>,
            // This onResult is the TransactionManager's internal callback mechanism
            // that the UI layer (e.g., DashboardFragment) will invoke with the data.
            val onResult: (resultData: Map<String, Any>?) -> Unit
        ) : UiAction()

        // Example: Action to show a simple non-interactive message
        data class ShowMessage(
            val title: String,
            val message: String,
            val positiveButtonText: String = "OK"
        ) : UiAction()

        data class ShowDialogActivity(
            val requestId: String,
            val stateName: String, // This will be our requestId
            val transactionId: String,
            val dialogType: DialogManager.DialogType,
            val dialogData: Map<String, Any> = emptyMap(),
            // This callback is now for the UI to invoke with the dialog's result.
            // The TransactionManager will use stateName (requestId) to complete the right deferred.
            val callback: (resultData: Map<String, Any>?) -> Unit,
            val onResult: (resultData: Map<String, Any>?) -> Unit,
            val title: String,
            val message: String,
            val positiveButtonText: String,
            val negativeButtonText: String,
            val neutralButtonText: String,
            val inputHint: String,
            val items: Any,
            val initialSelection: String
        ) : UiAction()
        // DialogResult might become less necessary if the callback handles it, or it's used for other scenarios.
        data class DialogResult(val stateName: String, val requestId:String, val data: Bundle) : UiAction()

        data class UpdateTransactionStatus(val status: TransStatus) : UiAction()
        // Add other UI actions as needed (e.g., NavigateTo, ShowLoading, HideLoading)


    }


    suspend fun startTransaction(
        transaction: BaseTransaction,
        initialContext: Map<String, Any> = emptyMap()
    ) {
        transactionMutex.withLock {
            if (currentTransactionJob?.isActive == true) {
                Log.w(
                    "TransactionManager",
                    "Another transaction is already active. Cannot start new one yet."
                )
                // Optionally, you could queue transactions or cancel the existing one.
                return
            }
            transactions[transaction.getTransactionID()] = transaction
            activeTransaction = transaction
            _uiActions.emit(UiAction.UpdateTransactionStatus(transaction.transactionStatus))
            Log.i("TransactionManager", "Starting transaction: ${transaction.transactionID}")
        }

        currentTransactionJob =
            externalScope.launch(Dispatchers.Default) { // Use a background dispatcher
                try {
                    //transaction.executeTransaction(initialContext)
                    transactionMutex.withLock {
                        Log.i(
                            "TransactionManager",
                            "Transaction ${transaction.transactionID} completed successfully."
                        )
                        _uiActions.emit(UiAction.UpdateTransactionStatus(transaction.transactionStatus))
                        if (activeTransaction == transaction) activeTransaction = null
                    }
                } catch (e: Exception) {
                    transactionMutex.withLock {
                        Log.e(
                            "TransactionManager",
                            "Transaction ${transaction.transactionID} failed: ${e.message}",
                            e
                        )
                        //transaction.transactionStatus = TransStatus.transactionStatus // Ensure status is updated
                        _uiActions.emit(UiAction.UpdateTransactionStatus(transaction.transactionStatus))
                        if (activeTransaction == transaction) activeTransaction = null
                    }
                    // Optionally, rethrow or handle specific exceptions
                } finally {
                    transactionMutex.withLock {
                        if (activeTransaction == transaction) {
                            activeTransaction = null
                            Log.d(
                                "TransactionManager",
                                "Cleared active transaction: ${transaction.transactionID}"
                            )
                        }
                    }
                }
            }
    }

    /**
     * Called by a State to request a generic input screen from the UI.
     * This function will suspend until the UI provides a result.
     */
    suspend fun requestInputScreen(
        config: InputScreenConfig,
        currentFragment: Fragment // For NavController context if needed by UI layer
    ): Map<String, Any>? {
        val tx = activeTransaction ?: run {
            Log.e("TransactionManager", "requestInputScreen called with no active transaction.")
            return null // Or throw exception
        }

        val requestId = "uiReq_${tx.transactionID}_${nextRequestId++}"
        val deferredResult = CompletableDeferred<Map<String, Any>?>()
        activeDialogRequests[requestId] = deferredResult

        val actionToEmit = UiAction.RequestInputScreen(
            requestId = requestId,
            transactionId = tx.getTransactionID(),
            dialogType = config.dialogType,
            title = config.title,
            message = config.message,
            positiveButtonText = config.positiveButtonText,
            negativeButtonText = config.negativeButtonText,
            neutralButtonText = config.neutralButtonText,
            inputHint = config.inputHint,
            items = config.items,
            initialSelection = config.initialSelection,
            customData = config.customData,
            prompt="",
            screenIdentifier="TEXT_INPUT_DIALOG",
            onResult = { resultDataMap ->
                // This lambda is what DashboardFragment will call.
                // It, in turn, calls completeUiRequest.
                completeUiRequest(requestId, resultDataMap)
            }
        )

        _uiActions.emit(actionToEmit)
        Log.d(
            "TransactionManager",
            "Emitted RequestInputScreen for type ${actionToEmit.dialogType} with requestId $requestId"
        )

        return deferredResult.await() // Suspend until UI result is provided
    }

    /**
     * Called by the UI layer (e.g., DashboardFragment via the onResult callback)
     * to provide the result of a UI interaction.
     */
    fun completeUiRequest(requestId: String, data: Map<String, Any>?) {
        Log.d("TransactionManager", "Completing UI request for ID: $requestId, Data: $data")
        val deferred = activeDialogRequests.remove(requestId)
        if (deferred != null) {
            if (!deferred.isCompleted) {
                deferred.complete(data)
            } else {
                Log.w("TransactionManager", "Deferred for $requestId was already completed.")
            }
        } else {
            Log.w("TransactionManager", "No active dialog request found for ID: $requestId")
        }
    }


    fun getTransaction(transactionID: String): BaseTransaction? {
        return transactions[transactionID]
    }

    fun getActiveTransaction(): BaseTransaction? {
        return activeTransaction
    }

    // ... other TransactionManager methods ...

    private fun getRandomId(): Long = Random.nextLong(1000, 100000)
    suspend fun addTransactionPrototype(transactionType: String, transactionConstructor: (Long, String) -> BaseTransaction) {

        transactionPrototypes[transactionType] = transactionConstructor
    }

    /**
     * Creates a new transaction using the factory, makes it the active transaction,
     * and updates the global transaction status.
     */
    fun createNewTransaction(transactionType: String): BaseTransaction? {
        Log.d("TransactionManager", "Attempting to create new transaction of type: $transactionType")
        // The factory now returns a BaseTransaction which includes its ID (Long) and name
        val newTransactionInstance = transactionFactory.createTransaction(transactionType, this)

        if (newTransactionInstance != null) {
            // Use the String ID from BaseTransaction's getTransactionID() for the map key
            val transactionStringId = newTransactionInstance.getTransactionID()
            transactions[transactionStringId] = newTransactionInstance // Add to the map

            // Also add to openTransactions list if you use it for other purposes
            openTransactions.add(newTransactionInstance)

            // Set as the active transaction
            activeTransaction = newTransactionInstance
            activeTransactionId = newTransactionInstance.transactionID // Assuming BaseTransaction.transactionID is the Long ID
            // !!! CRITICAL: Populate states for the new transaction !!!
            initializeStatesForTransaction(newTransactionInstance) // Call a new helper method

            // Update the global status
            _currentTransStatus.value = TransStatus(
                selectedTransactionId = newTransactionInstance.transactionID, // This should be the Long ID
                selectedTransactionName = newTransactionInstance.transactionName,
                transactionStatus = "active_pending_start", // Or whatever initial status is appropriate
                currentStepName = newTransactionInstance.getStateEntries().firstOrNull()?.label ?: "Initial Step",
                totalSteps = newTransactionInstance.getStateEntries().size,
                currentStepNumber = if (newTransactionInstance.getStateEntries().isNotEmpty()) 1 else 0,
                isFirstStep = true,
                isLastStep = newTransactionInstance.getStateEntries().size <= 1
            )
            // Optionally, also save this initial status to the transaction instance itself
            newTransactionInstance.transactionStatus = _currentTransStatus.value.copy()

            Log.i(
                "TransactionManager",
                "Transaction '${newTransactionInstance.transactionName}' (ID: $transactionStringId, NumericID: ${newTransactionInstance.transactionID}) created and set active."
            )
        } else {
            Log.e("TransactionManager", "Failed to create transaction of type: $transactionType from factory.")
        }
        return newTransactionInstance
    }

    fun setActiveTransaction(transactionId: Long) {
        val transaction = openTransactions.find { it.transactionID == transactionId }
        if (transaction != null) {
            activeTransactionId = transactionId
            _currentTransStatus.value = transaction.loadStatus() // Load its specific status
            println("TransactionManager: Active transaction set to ID $activeTransactionId (${transaction.getName()})")
        } else {
            println("TransactionManager: Could not find transaction with ID $transactionId to set active.")
            // Optionally reset if no valid transaction is found
            // activeTransactionId = -1
            // _currentTransStatus.value = TransStatus()
        }
    }

    // Simplified state creation. In a real app, this might involve dependency injection.
    fun createStateForTransaction(stateName: String, transaction: BaseTransaction): StateBase? {
        return when (stateName) {
            "ExampleDataFetchingState" -> ExampleDataFetchingState(transaction)
            "ExampleProcessingState" -> ExampleProcessingState(transaction)
            "ExampleCommitState" -> ExampleCommitState(transaction)
            // Add other state types here
            else -> {
                println("Error: Unknown state name provided: $stateName")
                null
            }
        }
    }

    fun setDashboardFragment(fragm: Fragment) {
        fragment = fragm
    }

    fun addStateToActiveTransactionByName(stateName: String): StateBase? {
        val activeTransaction = getActiveTransaction() ?: run {
            println("Error: No active transaction to add state to.")
            return null
        }

        // Check if state already exists (for re-entry logic, simplified here)
        var state = activeTransaction.states.find { it.getName() == stateName }
        if (state != null) {
            println("State $stateName already exists in transaction. Re-entering.")
            // Potentially trim stack or just set as current
            activeTransaction.currentState = state
            return state
        }

        state = createStateForTransaction(stateName, activeTransaction)
        state?.let {
            activeTransaction.states.add(it)
            activeTransaction.currentState = it // Set as current for immediate execution
            println("Added state ${it.getName()} to transaction ${activeTransaction.getName()}")
        }
        return state
    }

    suspend fun executeState(
        state: StateBase,
        initialContext: Map<String, Any>? = null,
        fragment: Fragment
    ): Map<String, Any>? {
        val activeTransaction = getActiveTransaction() ?: run {
            println("Error: No active transaction to execute state.")
            return null
        }
        if (activeTransaction.currentState != state && !activeTransaction.states.contains(state)) {
            println("Error: State ${state.getName()} is not part of the active transaction ${activeTransaction.getName()}.")
            return null
        }

        activeTransaction.currentState = state // Ensure this is the current state being executed
        val executionContext = activeTransaction.context + (initialContext ?: emptyMap())
        var returnData: Map<String, Any>? = null

        println("--- Starting execution for state: ${state.getName()} ---")
        _currentTransStatus.value =
            _currentTransStatus.value.copy(transactionStatus = "Executing ${state.getName()}")

        try {
            val inputData = state.fetchInputData(executionContext)
            val executionResult = state.executeLogic(inputData, fragment)
            returnData = state.fetchReturnData(executionResult)
            state.commit(returnData)

            println("--- State ${state.getName()} completed successfully ---")
            activeTransaction.context.putAll(returnData) // Update shared context
            _currentTransStatus.value =
                _currentTransStatus.value.copy(transactionStatus = "${state.getName()} Committed")

        } catch (e: Exception) {
            println("--- Error during execution of state ${state.getName()}: ${e.message} ---")
            _currentTransStatus.value =
                _currentTransStatus.value.copy(transactionStatus = "Error in ${state.getName()}")
            try {
                state.rollback(e)
            } catch (rollbackError: Exception) {
                println("--- Critical Error during rollback for state ${state.getName()}: ${rollbackError.message} ---")
            }
            // Re-throw or handle error appropriately
            // For this starter, we'll just log and not re-throw to allow UI to update.
            return null // Indicate failure
        } finally {
            // activeTransaction.currentState = null // Or move to next state logic
        }
        return returnData
    }

    fun getCurrentTransactionStateNodes(): List<StateNode> {
        return getActiveTransaction()?.getStateEntries() ?: emptyList()
    }

    fun isValidStateTransition(currentStateKey: String, nextStateKey: String): Boolean {
        return getActiveTransaction()?.isValidStateTransition(currentStateKey, nextStateKey) ?: false
    }

    fun saveActiveTransactionStatus() {
        getActiveTransaction()?.saveStatus(_currentTransStatus.value)
    }

    fun clearTransactionHistoryForActive() {
        getActiveTransaction()?.let {
            it.states.clear()
            it.context.clear()
            it.clearTransactionLevelData()
            // Reset parts of TransStatus related to this transaction's progress
            _currentTransStatus.value = TransStatus(
                selectedTransactionId = it.transactionID,
                selectedTransactionName = it.transactionName,
                transactionStatus = "inactive"
                // Keep vessel_id etc. if they are truly global and not transaction-specific
            )
            println("Cleared history for active transaction ${it.getName()}")
        }
    }

    fun getOpenTransactionsList(): List<BaseTransaction> = openTransactions.toList()

    fun deleteTransaction(transactionId: Long) {
        val removed = openTransactions.removeAll { it.transactionID == transactionId }
        if (removed) {
            println("TransactionManager: Deleted transaction with ID $transactionId")
            if (activeTransactionId == transactionId) {
                activeTransactionId = -1
                _currentTransStatus.value = TransStatus() // Reset global status
                // Optionally, set another transaction as active or create a new default one
            }
        }
    }
    // Define the sealed class for UI actions (can be outside the class too)
    // In TransactionManager.kt (UiAction definition)
    // In TransactionManager.kt (UiAction definition)


// In TransactionManager.kt

    // Map to store active dialog requests and their means of completion
    private val activeDialogRequests = mutableMapOf<String, CompletableDeferred<Map<String, Any>?>>()
    private var nextRequestId = 0 // Simple request ID generator

    // In TransactionManager.kt

    suspend fun requestInputDialogGeneric(
        dialogActionConfig: UiAction.ShowDialogActivity,
        currentFragment: Fragment // Still needed for context, though not directly used by UiAction
    ): Map<String, Any>? {
        val requestId = "dialogReq_${nextRequestId++}"
        val deferredResult = CompletableDeferred<Map<String, Any>?>()
        activeDialogRequests[requestId] = deferredResult

        val finalAction = dialogActionConfig.copy()

        emitUiAction(finalAction)
        Log.d("TransactionManager", "Emitted generic ShowDialogActivity for type ${finalAction.dialogType} with requestId $requestId")
        return deferredResult.await()
    }

    // This method is called by the UI layer (e.g., DashboardFragment via ViewModel)
// when a dialog (like VesselSelectStateFrag) provides its result.
    fun handleDialogResult(requestId: String, data: Map<String, Any>?) {
        Log.d("TransactionManager", "handleDialogResult for requestId: $requestId, Data: $data")
        val deferred = activeDialogRequests.remove(requestId)
        if (deferred != null) {
            deferred.complete(data)
        } else {
            Log.w("TransactionManager", "No active dialog request found for ID: $requestId")
        }
    }
    // You might also need a function to process the result received by the Activity
    fun processDialogActivityResult(resultCode: Int, data: Intent?) {
        // This function will be called from MainActivity
        // It needs to somehow feed the result back into the state machine's logic.
        // This is the trickiest part and depends on how your state machine expects results.
        println("TransactionManager: Received dialog result - resultCode: $resultCode, data: $data")

        // Example: Find the active transaction and current state (assuming it's the one that launched the dialog)
        val activeTransaction = getActiveTransaction()
        val currentState = activeTransaction?.currentState as? ExampleDataFetchingState // Cast to the state that launched the dialog

        currentState?.let {
            // You need a way for the state to "receive" this result.
            // This could involve:
            // 1. The state machine/ViewModel having a mechanism to wait for a specific event.
            // 2. The state itself having a way to resume from a suspended state with a result.
            // This is advanced and often requires rethinking the state execution flow.

            // A simpler approach for now: just log or process the result here in the Manager
            // and then potentially trigger the next state transition based on this result.
            // However, this tightly couples the Manager to the dialog result logic.
            if (resultCode == Activity.RESULT_OK) {
                val userName = data?.getStringExtra("userName") // Get data from the result Intent
                val consentGiven = data?.getBooleanExtra("consentGiven", false)

                println("Dialog Result - UserName: $userName, Consent Given: $consentGiven")

                // How to use this result?
                // Option A (More complex, better separation): The state machine/ViewModel has a suspend function that waits for the result emitted via a different channel (e.g., another SharedFlow).
                // Option B (Simpler for now, less ideal separation): You call a method on the ViewModel/Manager that processes the result and determines the next state transition based on it.
            } else {
                println("Dialog was cancelled or failed.")
                // Handle cancellation/failure
            }
        } ?: run {
            println("No active transaction or current state to process dialog result.")
        }
    }    // ... rest of TransactionManager code ...

    // Function to emit a UI action

    /**
     * Initializes the states for a given transaction based on its definition.
     * This is where states are actually instantiated and added to transaction.states.
     */
    private fun initializeStatesForTransaction(transaction: BaseTransaction) {
        transaction.states.clear() // Clear any previous states if re-initializing
        val stateEntries = transaction.getStateEntries() // This comes from SampleTransaction etc.

        if (stateEntries.isEmpty()) {
            Log.w("TransactionManager", "Transaction '${transaction.getName()}' has no state entries defined.")
            return
        }

        Log.d("TransactionManager", "Initializing ${stateEntries.size} states for transaction '${transaction.getName()}':")
        for (stateNode in stateEntries) {
            val stateInstance = createStateByName(stateNode.handlerStateName, transaction)
            if (stateInstance != null) {
                transaction.states.add(stateInstance) // <<< THE CRITICAL ADDITION
                Log.d("TransactionManager", "  Added state: ${stateInstance.getName()} (handler: ${stateNode.handlerStateName})")
            } else {
                Log.e("TransactionManager", "  Failed to create state instance for handler: ${stateNode.handlerStateName}")
                // Potentially stop transaction initialization or mark as invalid
            }
        }

        // Optionally set the first state as the current state if the transaction should start immediately
        if (transaction.states.isNotEmpty()) {
            transaction.currentState = transaction.states.first()
            Log.i("TransactionManager", "Set initial state for '${transaction.getName()}' to '${transaction.currentState?.getName()}'")
        }
    }

    /**
     * Creates a StateBase instance based on its registered name.
     * (You might have a similar method already, ensure it's used by initializeStatesForTransaction)
     */
    fun createStateByName(stateName: String, transaction: BaseTransaction): StateBase? {
        // This should be your central place for creating state instances.
        // You might have a map of state names to
        return when (stateName) {
            "ExampleDataFetchingState" -> ExampleDataFetchingState(transaction)
            "ExampleProcessingState" -> ExampleProcessingState(transaction)
            "ExampleCommitState" -> ExampleCommitState(transaction)
            "VesselSelectionState" -> com.example.artest2.states.VesselSelectionState(transaction)
            else -> {
                Log.e("TransactionManager", "Unknown state name provided for creation: $stateName")
                null
            }
        }
    }
}
