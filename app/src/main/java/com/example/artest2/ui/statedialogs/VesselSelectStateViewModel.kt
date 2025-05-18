package com.example.artest2.ui.statedialogs // Or a more specific package like com.example.artest2.features.vesselselection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Example data class for a Vessel, adjust as needed
data class Vessel(
    val id: String,
    val name: String,
    val type: String? = null,
    val capacity: Int? = null
    // Add other relevant vessel properties
)

class VesselSelectStateViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _availableVessels = MutableLiveData<List<Vessel>>(emptyList())
    val availableVessels: LiveData<List<Vessel>> = _availableVessels

    private val _selectedVessel = MutableLiveData<Vessel?>()
    val selectedVessel: LiveData<Vessel?> = _selectedVessel

    // For showing messages like errors or success confirmations
    private val _userMessage = MutableLiveData<String?>()
    val userMessage: LiveData<String?> = _userMessage

    // Event to signal navigation or completion
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    sealed class NavigationEvent {
        data class SelectionConfirmed(val vessel: Vessel) : NavigationEvent()
        object SelectionCancelled : NavigationEvent()
        // Add other navigation events if needed
    }

    init {
        // Load vessels when the ViewModel is created
        fetchAvailableVessels()
    }

    fun fetchAvailableVessels() {
        viewModelScope.launch {
            _isLoading.value = true
            _userMessage.value = null // Clear previous messages
            try {
                // Simulate network call or database query
                delay(1500) // Simulate delay
                val vessels = listOf(
                    Vessel("v1", "Voyager Alpha", "Explorer", 100),
                    Vessel("v2", "Enterprise NX", "Cruiser", 200),
                    Vessel("v3", "Discovery One", "Science Vessel", 150),
                    Vessel("v4", "Reliant Beta", "Frigate", 80),
                    Vessel("v5", "Serenity", "Transport", 50)
                )
                _availableVessels.value = vessels
                if (vessels.isNotEmpty() && _selectedVessel.value == null) {
                    // Optionally pre-select the first vessel
                    // _selectedVessel.value = vessels.first()
                }
            } catch (e: Exception) {
                // Handle error (e.g., show error message to user)
                _userMessage.value = "Error fetching vessels: ${e.message}"
                _availableVessels.value = emptyList() // Clear list on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onVesselSelected(vessel: Vessel) {
        _selectedVessel.value = vessel
    }

    fun confirmSelection() {
        val currentSelection = _selectedVessel.value
        if (currentSelection != null) {
            _navigationEvent.value = NavigationEvent.SelectionConfirmed(currentSelection)
        } else {
            // Optionally show a message if no vessel is selected
            _userMessage.value = "Please select a vessel before confirming."
        }
    }

    fun cancelSelection() {
        _navigationEvent.value = NavigationEvent.SelectionCancelled
    }

    /**
     * Call this after the message has been shown to the user
     * to prevent it from being shown again on configuration change.
     */
    fun userMessageShown() {
        _userMessage.value = null
    }

    /**
     * Call this after a navigation event has been handled.
     */
    fun navigationEventHandled() {
        // This is tricky with LiveData for events.
        // Consider using a SingleLiveEvent pattern or Kotlin Flows/Channels for events
        // to ensure they are only consumed once.
        // For simplicity here, we just clear it, but this might not be robust
        // if the observer re-observes before the event is truly "done".
        // A better approach would be for the observer to set a flag or for the event
        // to carry a "handled" state, or use a different event mechanism.
        // For now, this is a simple clear:
        // _navigationEvent.value = null // Be cautious with this approach for events.
    }
}