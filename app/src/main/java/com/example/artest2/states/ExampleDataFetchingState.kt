package com.example.artest2.states
import android.app.Dialog
import android.util.Log
import android.content.Context
import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateBase
import com.example.artest2.ui.statedialogs.VesselSelectStateFrag
import kotlinx.coroutines.delay
import android.content.Intent
import com.example.artest2.R


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

    override suspend fun executeDialog(inputData: Map<String, Any>, MainActivityContext:Context):Any {
        Log.i("ART", "[${getName()}] Executing dialog with data: $inputData")
        println("[${getName()}] Executing dialog with data: $inputData")
        delay(300) // Simulate processing or user interaction time

        // Use the passed 'context' parameter directly
        val androidContext: Context = MainActivityContext

        // Optional: Validate input data before creating Intent
        val userId = inputData["userId"] as? String
        val previousStepData = inputData["previousStepData"] as? String

        // Create an Intent to start StateDialogActivity
        //val intent = Intent(androidContext, StateDialogActivity::class.java)

        // Pass data to StateDialogActivity using the Intent
       // if (userId != null) {
       //     intent.putExtra("userId", userId)
       // } else {
       //     Log.w("ART", "[${getName()}] userId is null or not a String in inputData.")
       //     // Decide how to handle this: throw error, provide default, etc.
       // }
      //  if (previousStepData != null) {
      //      intent.putExtra("previousStepData", previousStepData)
      //  } else {
    //        Log.w("ART", "[${getName()}] previousStepData is null or not a String in inputData.")
            // Decide how to handle this
    //    }
     //   intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
       // androidContext.startActivity(intent)
    //    val dialog = Dialog(androidContext)
  //      dialog.setContentView(R.layout.fragment_state_dialog) // Set the layout first
    //    dialog.show()
       // IMPORTANT: The following lines for simulating result need to be replaced
        // with a proper mechanism to get the result back from StateDialogActivity.
        // This is a placeholder and will not work correctly for getting actual user input.
        Log.w("ART", "[${getName()}] WARNING: Simulated dialog result - replace with proper result handling from ActivityResultContracts.")
        delay(2000) // Still simulating, but acknowledge it's not real

        // The actual result would be obtained through a callback mechanism, not here.
        // The data for dialogResult should come from the actual result received from StateDialogActivity.
        val simulatedDialogResult = mapOf("userName" to "SimulatedUser", "consentGiven" to false) // Placeholder data

        Log.i("ART", "[${getName()}] Dialog result (simulated): $simulatedDialogResult")
        println("[${getName()}] Dialog result (simulated): $simulatedDialogResult")
        return simulatedDialogResult // Return the simulated result for now
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
