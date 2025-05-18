package com.example.artest2.core

// Represents a node in the transaction flow.
// In a full app, 'componentIdentifier' could be a Fragment class name, a navigation route, or layout ID.
// 'componentProps' would be a Bundle or a map for initial data.
data class StateNode(
    val key: String, // Unique key for the state, e.g., "VesselSelectState"
    val label: String, // User-friendly label
    val connectedTo: List<String> = emptyList(), // Keys of next possible states
    val componentIdentifier: String? = null, // Identifier for the UI component (e.g. Fragment tag)
    val componentProps: Map<String, Any>? = null, // Initial properties for the component
    val handlerStateName: String // Name of the StateBase class that handles this state
)
