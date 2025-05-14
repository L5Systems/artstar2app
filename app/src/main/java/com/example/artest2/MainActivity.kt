package com.example.artest2 // Adjust package name if needed

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer // Import Observer
import androidx.lifecycle.ViewModelProvider // Import ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.artest2.databinding.ActivityMainBinding

// Import your MainViewModel
import com.example.artest2.ui.MainViewModel // Adjust import path if needed

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel // Declare ViewModel variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the MainViewModel
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Example: Call a function in the ViewModel
        mainViewModel.onAppStart()

        // Example: Observe LiveData from the ViewModel
        mainViewModel.activityTitle.observe(this, Observer { title ->
            // Update your Activity's title (e.g., in the ActionBar)
            supportActionBar?.title = title
            Log.d("MainActivity", "Activity title updated to: $title")
        })

        mainViewModel.userLoggedIn.observe(this, Observer { isLoggedIn ->
            // Handle UI changes based on login status
            if (isLoggedIn) {
                Log.d("MainActivity", "User is logged in.")
                // e.g., show user-specific content, hide login button
            } else {
                Log.d("MainActivity", "User is not logged in.")
                // e.g., show login button, restrict access
            }
        })


        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Example: MainActivity interacting with ViewModel
        // You could call this based on some event, e.g., a button click in MainActivity's layout (if any)
        // mainViewModel.updateActivityTitle("New Title from Activity")
        // mainViewModel.setUserLoggedIn(true)
    }
}