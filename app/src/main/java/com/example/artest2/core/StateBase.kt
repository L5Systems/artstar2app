package com.example.artest2.core

import android.content.Intent // Keep for future use, though current actions might not use it directly
import androidx.fragment.app.Fragment // For context if needed by actions
import com.example.artest2.DialogManager // Assuming DialogManager and DialogType are here or accessible
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class StateBase(
    val stateName: String,
    var parentTransaction: BaseTransaction // Assuming BaseTransaction provides access to TransactionManager
) {
    // Holds data specific to this state instance
    private val stateSpecificData: MutableList<ApiCallData> = mutableListOf()

    // --- UI Action Mechanism ---
    // This flow is for the state to emit actions REQUESTING UI changes or interactions.
    // It's intended to be observed by a higher-level component (e.g., TransactionManager or ViewModel)
    // that is responsible for actually handling these UI requests.
    private val _uiRequests = MutableSharedFlow<UiRequest>() // Renamed for clarity: these are requests from the state
    val uiRequests: SharedFlow<UiRequest> = _uiRequests.asSharedFlow()

    // --- Abstract methods for state lifecycle ---
    abstract suspend fun fetchInputData(context: Map<String, Any>): Map<String, Any>

    abstract suspend fun fetchReturnData(executionResult: Any): Map<String, Any>
    abstract suspend fun commit(returnData: Map<String, Any>)
    abstract suspend fun rollback(error: Throwable? = null)
    //abstract suspend fun fetchStateSpecificData(): List<ApiCallData>
    abstract suspend fun executeLogic(inputData: Map<String, Any>,fragment: Fragment?):Any

    fun getName(): String = stateName
    fun getTransactionStatus(): String = parentTransaction.transactionStatus.transactionStatus

    fun storeStateOutputData(data: ApiCallData): Boolean {
        stateSpecificData.add(data)
        return true
    }

    // Helper to get the TransactionManager, assuming BaseTransaction holds it
    protected fun getTransactionManager(): com.example.artest2.manager.TransactionManager? {
        // This is a common pattern, but the exact way to get TransactionManager
        // might differ based on your BaseTransaction implementation.
        // It could be parentTransaction.transactionManager directly
        // or parentTransaction.getManager() etc.
        // For now, let's assume BaseTransaction has a method getTransactionManager()
        return parentTransaction.retrieveTransactionManagerInstance()
    }

    /**
     * A suspend function that a state can call to request a UI interaction
     * and await its result. This simplifies the state's logic.
     *
     * This function EMITS a UiRequest and then SUSPENDS until the
     * TransactionManager (or whichever component handles UiRequests)
     * provides a result via a callback mechanism it sets up.
     *
     * @param request The UiRequest to be processed.
     * @return A Map representing the data received from the UI interaction, or null if cancelled/failed.
     */
    protected suspend fun requestUiInteraction(request: UiRequest.RequestDataFromUi): Map<String, Any>? {
        // The actual implementation of how the result comes back will be handled
        // by the collector of `uiRequests` (e.g., TransactionManager).
        // The TransactionManager will use the request.requestId and a CompletableDeferred
        // or similar mechanism to bridge the request and the result.

        // This method in StateBase is now more about emitting the request.
        // The waiting part is implicitly handled if the calling code in the state
        // calls a suspend function on TransactionManager that does the emit-and-wait.

        // So, this specific helper might be less useful if states call TM directly.
        // However, if states were to directly emit, they'd need a way to await.
        // For now, let's assume states will call a method on TransactionManager
        // like `transactionManager.requestData(requestDetails)` which itself handles
        // emitting the UiRequest and awaiting.

        // If a state wants to directly emit and manage its own suspension (more complex):
        // 1. Create a CompletableDeferred for the result.
        // 2. Pass a callback with the UiRequest that completes this deferred.
        // 3. Emit the UiRequest.
        // 4. Await the deferred.
        // This is what TransactionManager would encapsulate.

        // For simplicity in StateBase, let's make this a simple emit.
        // The state's executeLogic will call a method on TransactionManager.
        _uiRequests.emit(request) // Example: Emitting a request.
        // The actual awaiting logic is better placed in TransactionManager

        // This function's signature implies it should return the result directly.
        // This requires the TransactionManager to provide that result back here.
        // This creates a tight loop. It's cleaner if states call TM's suspend functions.

        // THEREFORE, this helper is probably not the right pattern.
        // States should call a method on TransactionManager which returns the result.
        // StateBase itself doesn't need to manage the suspension for UI results.

        // Let's remove this and have states call TransactionManager directly.
        // If a state needs to emit directly (less common), it can use _uiRequests.emit()
        // but won't get a result back through this flow directly.
        error("requestUiInteraction in StateBase is deprecated; states should call TransactionManager methods.")
    }


    // --- Definition of UiRequest (renamed from UiAction for clarity) ---
    // These are requests that the state makes *to* the UI layer (via TransactionManager/ViewModel).
    sealed class UiRequest {
        // Example: Request to display a dialog and get data back
        data class RequestDataFromUi(
            val requestId: String, // Crucial for matching results back
            val dialogType: DialogManager.DialogType,
            val title: String,
            val message: String? = null,
            val positiveButtonText: String? = "Confirm",
            val negativeButtonText: String? = "Cancel",
            val inputHint: String? = null,
            val items: List<String>? = null,
            val initialSelection: String? = null,
            val customData: Map<String, Any> = emptyMap()
            // The 'onResult' callback is NOT here. The TransactionManager
            // will manage how the result for 'requestId' is delivered
            // back to the suspended coroutine in the state.
        ) : UiRequest()

        // Example: Request to simply navigate, without expecting data back directly to the state
        data class NavigateTo(
            val navigationActionId: Int, // e.g., R.id.action_someFragment_to_anotherFragment
            val args: android.os.Bundle? = null
        ) : UiRequest()

        // Example: Request to show a simple Toast message
        data class ShowToast(val message: String, val durationLong: Boolean = false) : UiRequest()

        // Add other types of UI requests as needed:
        // - ShowSnackbar
        // - UpdateSpecificUiElement (if states have very fine-grained control, less common)
        // - FinishActivity
    }

    /**
     * This is where the primary logic of the state happens.
     * It might involve calculations, calling services, or orchestrating UI interactions
     * by using methods on the TransactionManager that internally emit UiRequests.
     */
    abstract class StateExecutionResult {
        object Success : StateExecutionResult()
        data class Failure(val error: Throwable) : StateExecutionResult()
        data class TransitionTo(val nextStateName: String, val dataForNextState: Map<String, Any>? = null) : StateExecutionResult()
        object WaitingForUi : StateExecutionResult() // Indicates the state has requested UI and is suspended
    }
}