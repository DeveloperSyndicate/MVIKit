package com.developersyndicate.mvikit.sample

import com.developersyndicate.mvikit.annotations.IntentHandler
import com.developersyndicate.mvikit.annotations.MviAction
import com.developersyndicate.mvikit.annotations.MviViewModel

@MviViewModel
class AuthViewModel {
    @IntentHandler
    fun updatePhone(phone: String) {
        updateState {
            this.phone  = phone
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