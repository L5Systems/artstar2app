package com.example.artest2.core

data class TransStatus(
    var vesselName: String = "None specified",
    var vesselType: String = "",
    var lastUpdated: String = "",
    var promptStatus: String = "",
    var vesselId: Long = 0,
    var selectedTransactionName: String = "None specified",
    var selectedTransactionDesc: String = "None specified",
    var selectedTransactionId: Long = -1,
    var transactionStatus: String = "inactive" // e.g., "inactive", "active", "completed", "failed"
)
