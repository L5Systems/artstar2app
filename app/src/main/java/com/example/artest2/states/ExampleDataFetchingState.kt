package com.example.artest2.states
import android.app.Dialog
import android.util.Log
import android.content.Context
import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateBase
import com.example.artest2.ui.statedialogs.VesselSelectStateViewModel
import kotlinx.coroutines.delay
import android.content.Intent
import com.example.artest2.DialogManager
import androidx.fragment.app.Fragment
import com.example.artest2.R
import com.example.artest2.core.ApiCallData


class ExampleDataFetchingState(parentTransaction: BaseTransaction) :
    StateBase("ExampleDataFetchingState", parentTransaction) {

    override suspend fun fetchInputData(context: Map<String, Any>): Map<String, Any> {
        Log.i("ART", "Fetching input data...")
        println("Now [${getName()}] Fetching input data...")
        //delay(500) // Simulate network call
        val fetchedData = mapOf(
            "userId" to (context["initialUserId"] ?: "initialUserId cannot be null"),
            "previousStepData" to (context["ExampleDataFetchingState"] ?: "NoDataFromPreviousState")
        )
        println("[${getName()}] Fetched: $fetchedData")
        return fetchedData
    }

    override suspend fun fetchReturnData(executionResult: Any): Map<String, Any> {
        println("[${getName()}] Preparing return data from: $executionResult")
        val dataToReturn = executionResult as Map<*, *>
        val processedData: Map<String, Any> = mapOf(
            "finalUserName" to (dataToReturn["userName"] ?: "defaultUserName"),
            "userAgreed" to (dataToReturn["consentGiven"] ?: false),
            "timestamp" to System.currentTimeMillis()
        )
        println("[${getName()}] Data to return: $processedData")
        return processedData
    }

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
    override suspend fun commit(returnData: Map<String, Any>) {
        println("[${getName()}] Committing data: $returnData")
        // Simulate API call or DB save
        delay(200)
        // Example: storeStateOutputData(ApiCallData("/api/user", returnData, "post"))
        println("[${getName()}] Commit successful.")
    }

    override suspend fun rollback(error: Throwable?) {
        println("[${getName()}] Rolling back...")
        if (error != null) {
            println("[${getName()}] Rollback triggered due to error: ${error.message}")
        }
        delay(100)
        println("[${getName()}] Rollback completed.")
    }
    private fun showDialog(context: Context, dataMap: Map<String, Any>) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(dataMap["title"]?.toString() ?: "Default Title")
            .setMessage(dataMap["message"]?.toString() ?: "Default Message")
            .setPositiveButton("OK") { _, _ -> println("Confirmed") }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
