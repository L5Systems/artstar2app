package com.example.artest2.core

data class TransStatus(
    var vesselName: String = "None specified",
    var vesselType: String = "",
    var lastUpdated: String = "",
    var promptStatus: String = "",
    var vesselId: Long = 0,
    var currentStepNumber: Int = 0,
    var totalSteps: Int = 0,
    var isFirstStep: Boolean = true,
    var isLastStep: Boolean = false,
    var currentStepName: String = "",
    var selectedTransactionName : String = "",
    var selectedTransactionDesc: String = "None specified",
    var selectedTransactionId: Long = -1,
    var transactionStatus: String = "inactive" // e.g., "inactive", "active", "completed", "failed"
)
