package com.example.artest2.ui // Adjust package name if needed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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
    }
}