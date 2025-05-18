package com.example.artest2.manager


import android.app.Application
import com.example.artest2.core.BaseTransaction
import com.example.artest2.manager.TransactionManager // Assuming TransactionManager is in this package

/**
 * A functional interface responsible for creating instances of BaseTransaction.
 * Implementations of this interface will provide the specific logic for instantiating
 * a particular type of transaction.
 */
fun interface TransactionCreator {
    /**
     * Creates and returns an instance of a class derived from BaseTransaction.
     *
     * @param id The unique ID to be assigned to the new transaction.
     * @param name A descriptive name for the new transaction instance.
     * @param manager The TransactionManager instance that will manage this transaction.
     * @param application The Application context, which might be needed by the transaction
     *                    for accessing resources or other Android services.
     * @return A new instance of a BaseTransaction.
     */
    fun create(id: Long, name: String, manager: TransactionManager, application: Application): BaseTransaction
}