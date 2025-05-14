package com.example.artest2.core

data class ApiCallData(
    val path: String,
    val data: Any?, // Data to post
    val action: String // e.g., "get" or "post"

)
