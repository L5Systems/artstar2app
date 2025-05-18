package com.example.artest2.manager

import android.app.Application
import android.util.Log
import com.example.artest2.core.BaseTransaction
import com.example.artest2.manager.TransactionCreator // Import the interface
import com.example.artest2.transactions.SampleTransaction

class TransactionFactory(private val application: Application) {

    // Store a map of transaction type strings to their creator functions
    private val prototypeTransactions: MutableMap<String, TransactionCreator> = mutableMapOf()

    init {
        // Register known transaction types and their creators
        registerTransactionType(
            SampleTransaction.TRANSACTION_TYPE,
            SampleTransaction::create // Pass the 'create' function reference from SampleTransaction.Companion
        )

        // Example for another transaction type if you had one:
        // registerTransactionType(
        //     AnotherTransaction.TRANSACTION_TYPE,
        //     AnotherTransaction::create
        // )
    }

    fun registerTransactionType(type: String, creator: TransactionCreator) {
        if (prototypeTransactions.containsKey(type)) {
            Log.w("TransactionFactory", "Replacing existing creator for transaction type: $type")
        }
        prototypeTransactions[type] = creator
        Log.i("TransactionFactory", "Registered transaction type: $type")
    }

    fun createTransaction(type: String, manager: TransactionManager): BaseTransaction? {
        Log.d("TransactionFactory", "Attempting to create transaction of type: '$type'")
        val creator = prototypeTransactions[type]

        return if (creator != null) {
            try {
                // Generate ID and a default name, or make them configurable
                val newId = System.currentTimeMillis()
                val transactionName = "$type Instance" // Generic name, can be customized
                Log.d("TransactionFactory", "Creator found for type: $type. Instantiating...")
                val transaction = creator.create(newId, transactionName, manager, application)
                Log.i("TransactionFactory", "$type instantiated successfully: $transaction")
                transaction
            } catch (e: Exception) {
                Log.e("TransactionFactory", "Exception during $type instantiation via creator", e)
                null
            }
        } else {
            Log.w("TransactionFactory", "No creator found for transaction type: '$type'")
            null
        }
    }
}