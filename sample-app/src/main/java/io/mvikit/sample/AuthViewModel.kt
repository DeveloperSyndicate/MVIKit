package io.mvikit.sample

import io.mvikit.annotations.IntentHandler
import io.mvikit.annotations.MviAction
import io.mvikit.annotations.MviViewModel

@MviViewModel
class AuthViewModel {
    @IntentHandler
    fun updateEmail(email: String) {
        updateState {
            this.email  = email
        }
    }
    @IntentHandler
    fun updatePassword(password: String) {
        updateState {
            this.password = password
        }
    }
    @IntentHandler
    fun updatedLoading(isLoading: Boolean) {
        updateState {
            this.isLoading = isLoading
        }
    }
    @MviAction
    fun login() {

    }
}