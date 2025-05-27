package com.example.artest2.states

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction
import com.example.artest2.DialogManager
import com.example.artest2.core.ApiCallData
import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateBase
import com.example.artest2.manager.TransactionManager

// In a state like VesselSelectionState.kt
class VesselSelectionState(parentTransaction: BaseTransaction) :
    StateBase("VesselSelectionState", parentTransaction) {

    override suspend fun executeLogic(
        inputData: Map<String, Any>,
        fragment: Fragment?
    ): StateExecutionResult {
        println("[$stateName] Executing logic with input: $inputData")
        val initialValue = inputData["initialValue"] as? Int ?: 0
        val transactionManager = getTransactionManager()

        if (transactionManager == null) {
            println("[$stateName] Error: TransactionManager not available.")
            return StateExecutionResult.Failure(IllegalStateException("TransactionManager not found"))
        }

        if (fragment == null) {
            println("[$stateName] Error: Fragment host not available for UI operations.")
            return StateExecutionResult.Failure(IllegalStateException("Fragment host not available"))
        }

        try {
            // 1. Request Vessel Selection from the UI via TransactionManager
            println("[$stateName] Requesting vessel selection...")

            // Define the configuration for the input screen.
            // This object describes WHAT the state needs from the UI.
            // The TransactionManager will take this, add a requestId, and manage the callback.
            val inputScreenConfig = com.example.artest2.manager.TransactionManager.InputScreenConfig(
                // No requestId here - TransactionManager will generate it.
                // No onResult here - TransactionManager will manage the result mechanism.
                dialogType = DialogManager.DialogType.VESSEL_SELECTION,
                title = "Select Vessel for Processing",
                message = "Please choose your vessel from the list below.",
                items = listOf("Voyager", "Enterprise", "Discovery", "Reliant"),
                initialSelection = "Voyager",
                positiveButtonText = "Confirm Vessel",
                negativeButtonText = "Skip Vessel",
                customData = mapOf("sourceState" to stateName) // Example of passing extra data
            )

            // This call to TransactionManager will suspend until the UI provides a result.
            // TransactionManager.requestInputScreen will internally:
            //   - Generate a unique requestId.
            //   - Create and store a CompletableDeferred for this requestId.
            //   - Construct the actual UiAction (like RequestInputScreen) including the requestId
            //     and its internal onResult mechanism.
            //   - Emit the UiAction.
            //   - Await the CompletableDeferred.
            val vesselResult: Map<String, Any>? = transactionManager.requestInputScreen( // Renamed method for clarity
                config = inputScreenConfig,
                currentFragment = fragment
            )

            var selectedVessel = "N/A"
            // Assuming your StateInputFragment.ResultKeys are accessible or defined in a shared place
            val resultKeys = com.example.artest2.ui.statedialogs.StateInputFragment.ResultKeys

            if (vesselResult != null && vesselResult[resultKeys.POSITIVE_CLICK] == true) {
                selectedVessel = vesselResult[resultKeys.SELECTED_ITEM] as? String ?: "Unknown Vessel"
                println("[$stateName] Vessel selected: $selectedVessel")
                storeStateOutputData(
                    ApiCallData(
                        "vesselSelection",
                        mapOf("vessel" to selectedVessel),
                        "zztop"
                    )
                )
            } else {
                println("[$stateName] Vessel selection skipped or cancelled.")
                storeStateOutputData(ApiCallData(
                    "vesselSelection",
                    mapOf("vessel" to "SKIPPED"),
                    action=  "zztop"
                ))
            }

            // ... (rest of the logic) ...

            return StateExecutionResult.Success

        } catch (e: kotlinx.coroutines.CancellationException) {
            println("[$stateName] Coroutine cancelled during UI interaction: ${e.message}")
            rollback(e) // Perform rollback actions
            return StateExecutionResult.Failure(e)
        } catch (e: Exception) {
            println("[$stateName] Error during execution: ${e.message}")
            rollback(e) // Perform rollback actions
            return StateExecutionResult.Failure(e)
        }
    }
    override suspend fun fetchInputData(context: Map<String, Any>): Map<String, Any> {
        var inputData = mutableMapOf<String, Any>()
        inputData["initialValue"] = 100
        return inputData
    }

    override suspend fun fetchReturnData(executionResult: Any): Map<String, Any> {
        var returnData = mutableMapOf<String, Any>()
        returnData["returnedValue"] = "Dina Star"
        return returnData
    }

    override suspend fun commit(returnData: Map<String, Any>) {
        Log.d("VesselSelectionState", "Committing with returnData: $returnData")
    }

    override suspend fun rollback(error: Throwable?) {
        Log.d("VesselSelectionState", "Rolling back with error: ${error?.message}")
    }


    // ... fetchInputData, fetchReturnData, commit, rollback ...
}