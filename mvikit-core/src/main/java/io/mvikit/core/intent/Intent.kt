package io.mvikit.core.intent

/**
 * Marker interface representing a user action or intent within the MVI architecture.
 *
 * All user actions dispatched to the ViewModel must implement this interface.
 * Handlers can be registered declaratively on the ViewModel using the `intent` API.
 */
interface Intent
