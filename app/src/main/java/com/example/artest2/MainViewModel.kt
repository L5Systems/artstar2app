package com.example.artest2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artest2.core.StateNode
import com.example.artest2.manager.TransactionManager
import com.example.artest2.transactions.SampleTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Example LiveData for a title
    private val _activityTitle = MutableLiveData<String>().apply {
        value = "AR Test App" // Default title
    }
    val activityTitle: LiveData<String> = _activityTitle

    // Example LiveData for some user status
    private val _userLoggedIn = MutableLiveData<Boolean>().apply {
        value = false // Default to not logged in
    }
    val userLoggedIn: LiveData<Boolean> = _userLoggedIn

    // Add more LiveData properties and functions as needed for your MainActivity

    fun updateActivityTitle(newTitle: String) {
        _activityTitle.value = newTitle
    }

    fun setUserLoggedIn(isLoggedIn: Boolean) {
        _userLoggedIn.value = isLoggedIn
    }

    // Example: A function that might be called from MainActivity
    fun onAppStart() {
        // Perform any on-start logic for the MainActivity
        println("MainViewModel: App Started")
        // You could load initial data here or check user session
        //updateActivityTitle("New Title from ViewModel")
        //setUserLoggedIn(true)

    }


}