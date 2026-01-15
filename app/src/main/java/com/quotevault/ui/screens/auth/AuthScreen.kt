package com.quotevault.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel


@Composable

fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onAuthSuccess()
        }
    }
    
    AuthScreenContent(
        uiState = uiState,
        onSignIn = viewModel::signIn,
        onSignUp = viewModel::signUp,
        onResetPassword = viewModel::resetPassword
    )
}

@Composable
fun AuthScreenContent(
    uiState: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onResetPassword: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Reset Password Dialog
    if (showResetDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email to receive a password reset link.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetPassword(resetEmail)
                        showResetDialog = false
                    }
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding() // Push up for keyboard
            .verticalScroll(rememberScrollState()) // Allow scrolling
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed from Center to Top for stability
    ) {
        Spacer(modifier = Modifier.height(56.dp)) // Top spacing
        
        Text(
            text = "QuoteVault",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (uiState is AuthUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (uiState is AuthUiState.PasswordResetSent) {
            Text(
                text = "Password reset email sent! Check your inbox.",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!isSignUp) {
            TextButton(onClick = { showResetDialog = true }) {
                Text("Forgot Password?")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isSignUp) {
                    onSignUp(email, password, fullName)
                } else {
                    onSignIn(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is AuthUiState.Loading
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (isSignUp) "Sign Up" else "Sign In")
            }
        }

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    MaterialTheme {
        AuthScreenContent(
            uiState = AuthUiState.Idle,
            onSignIn = { _, _ -> },
            onSignUp = { _, _, _ -> }
        )
    }
}
