package com.example.artest2.states

import android.content.Context
import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateBase
import kotlinx.coroutines.delay

class ExampleProcessingState(parentTransaction: BaseTransaction) :
    StateBase("ExampleProcessingState", parentTransaction) {

    override suspend fun fetchInputData(context: Map<String, Any>): Map<String, Any> {
        println("[${getName()}] Input for processing: $context")
        return context ?: emptyMap()
    }

    override suspend fun executeDialog(inputData: Map<String, Any>,MainActivityContext:Context): Any {
        println("[${getName()}] Processing data: $inputData")
        delay(400)
        val processedValue = (inputData["finalUserName"] as? String)?.length ?: 0
        val result = mapOf("processedValue" to processedValue, "status" to "ProcessedOK")
        println("[${getName()}] Processing result: $result")
        return result
    }

    override suspend fun fetchReturnData(executionResult: Any): Map<String, Any> {
        println("[${getName()}] Preparing return data from processing: $executionResult")
        return executionResult as? Map<String, Any> ?: emptyMap()
    }

    override suspend fun commit(returnData: Map<String, Any>) {
        println("[${getName()}] Committing processing results: $returnData")
        delay(150)
        println("[${getName()}] Processing commit successful.")
    }

    override suspend fun rollback(error: Throwable?) {
        println("[${getName()}] Rolling back processing state...")
        if (error != null) {
            println("[${getName()}] Rollback due to: ${error.message}")
        }
        delay(50)
        println("[${getName()}] Processing rollback completed.")
    }
}
