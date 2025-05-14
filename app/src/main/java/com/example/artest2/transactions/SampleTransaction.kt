package com.example.artest2.transactions

import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateNode

class SampleTransaction(transactionID: Long, transactionName: String) :
    BaseTransaction(transactionID, transactionName) {

    companion object {
        const val TRANSACTION_TYPE = "SampleTransaction"
    }

    private val stateNodes = listOf(
        StateNode(
            key = "ExampleDataFetchingState",
            label = "Fetch Initial Data",
            connectedTo = listOf("ExampleProcessingState")
        ),
        StateNode(
            key = "ExampleProcessingState",
            label = "Process Data",
            connectedTo = listOf("ExampleCommitState")
        ),
        StateNode(
            key = "ExampleCommitState",
            label = "Final Commit",
            connectedTo = emptyList() // Terminal node
        )
    )


    override fun getStateEntries(): List<StateNode> {
        return stateNodes
    }

    override fun isValidStateTransition(currentStateKey: String, nextStateKey: String): Boolean {
        val currentNode = stateNodes.find { it.key == currentStateKey }
        return currentNode?.connectedTo?.contains(nextStateKey) ?: false
    }

    override fun executeRemoteDialogCmd(cmd: String) {
        super.executeRemoteDialogCmd(cmd)
        // In a real app, this might interact with a ViewModel to show a Toast or update UI
        println("SampleTransaction: Received command: $cmd")
    }
}
