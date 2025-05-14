package com.example.artest2.states

import android.content.Context
import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateBase
import kotlinx.coroutines.delay

class ExampleCommitState(parentTransaction: BaseTransaction) :
    StateBase("ExampleCommitState", parentTransaction) {

    override suspend fun fetchInputData(context: Map<String, Any>): Map<String, Any> {
        println("[${getName()}] Final data for commit: $context")
        return context ?: emptyMap()
    }

    override suspend fun executeDialog(inputData: Map<String, Any>, MainActivityContext:Context): Any {
        println("[${getName()}] Reviewing before final commit: $inputData")
        delay(200)
        // No real dialog, just pass through
        return inputData 
    }

    override suspend fun fetchReturnData(executionResult: Any): Map<String, Any> {
        println("[${getName()}] Finalizing data package: $executionResult")
        val finalPackage = (executionResult as? Map<String, Any> ?: emptyMap()) + mapOf("transactionComplete" to true)
        return finalPackage
    }

    override suspend fun commit(returnData: Map<String, Any>) {
        println("[${getName()}] >>> FINAL TRANSACTION COMMIT: $returnData <<<")
        // Simulate final API call for the whole transaction
        delay(500)
        println("[${getName()}] >>> TRANSACTION COMMIT SUCCESSFUL. <<<")
    }

    override suspend fun rollback(error: Throwable?) {
        println("[${getName()}] Rolling back final commit state (should ideally not happen here if previous states handled their rollbacks)...")
        if (error != null) {
            println("[${getName()}] Rollback due to: ${error.message}")
        }
        delay(50)
        println("[${getName()}] Final commit rollback completed.")
    }
}
