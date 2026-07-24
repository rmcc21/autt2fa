package com.example.security

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AuthUserInfo(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String = "EMPLOYEE",
    val provider: String = "Email/Password"
)

sealed class AuthResultState {
    object Idle : AuthResultState()
    object Loading : AuthResultState()
    data class Success(val user: AuthUserInfo) : AuthResultState()
    data class Error(val message: String) : AuthResultState()
}

class FirebaseAuthManager(private val context: Context) {

    private var firebaseAuth: FirebaseAuth? = null

    private val _authState = MutableStateFlow<AuthResultState>(AuthResultState.Idle)
    val authState: StateFlow<AuthResultState> = _authState.asStateFlow()

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            checkCurrentSession()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Firebase Auth initialization warning: ${e.message}")
        }
    }

    fun isFirebaseConfigured(): Boolean {
        return firebaseAuth != null
    }

    private fun checkCurrentSession() {
        val currentUser = firebaseAuth?.currentUser
        if (currentUser != null) {
            val user = AuthUserInfo(
                uid = currentUser.uid,
                email = currentUser.email ?: "user@organization.com",
                displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Enrolled User",
                role = if (currentUser.email?.contains("admin", ignoreCase = true) == true || currentUser.email?.contains("hr", ignoreCase = true) == true) "HR_ADMIN" else "EMPLOYEE",
                provider = currentUser.providerData.firstOrNull()?.providerId ?: "Firebase"
            )
            _authState.value = AuthResultState.Success(user)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String, selectedRole: String): Result<AuthUserInfo> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized on device."))
        _authState.value = AuthResultState.Loading
        return try {
            val res = auth.signInWithEmailAndPassword(email, pass).await()
            val fbUser = res.user ?: throw Exception("Authentication returned empty user.")
            val user = AuthUserInfo(
                uid = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = fbUser.displayName ?: email.substringBefore("@"),
                role = selectedRole,
                provider = "Email/Password"
            )
            _authState.value = AuthResultState.Success(user)
            Result.success(user)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Invalid email or password"
            _authState.value = AuthResultState.Error(errorMsg)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String, role: String): Result<AuthUserInfo> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized on device."))
        _authState.value = AuthResultState.Loading
        return try {
            val res = auth.createUserWithEmailAndPassword(email, pass).await()
            val fbUser = res.user ?: throw Exception("Failed to register account.")
            val user = AuthUserInfo(
                uid = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = name.ifBlank { email.substringBefore("@") },
                role = role,
                provider = "Email/Password"
            )
            _authState.value = AuthResultState.Success(user)
            Result.success(user)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Registration failed"
            _authState.value = AuthResultState.Error(errorMsg)
            Result.failure(e)
        }
    }

    suspend fun launchGoogleSignIn(webClientId: String, role: String): Result<AuthUserInfo> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized on device."))
        _authState.value = AuthResultState.Loading
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId.ifBlank { "100000000000-dummy.apps.googleusercontent.com" })
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleCredential.idToken

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            val fbUser = authResult.user ?: throw Exception("Google sign in failed.")

            val user = AuthUserInfo(
                uid = fbUser.uid,
                email = fbUser.email ?: "google.user@org.com",
                displayName = fbUser.displayName ?: "Google Auth User",
                role = role,
                provider = "Google Accounts"
            )
            _authState.value = AuthResultState.Success(user)
            Result.success(user)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Google Sign-In cancelled or unavailable"
            _authState.value = AuthResultState.Error(errorMsg)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Sign out error: ${e.message}")
        }
        _authState.value = AuthResultState.Idle
    }
}
