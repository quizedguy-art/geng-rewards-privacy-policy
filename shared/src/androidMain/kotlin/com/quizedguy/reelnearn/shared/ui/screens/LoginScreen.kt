package com.quizedguy.reelnearn.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quizedguy.reelnearn.shared.R
import com.quizedguy.reelnearn.shared.ui.theme.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    
    var referralCode by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    
    val isLoading by authViewModel.isLoading.collectAsState(initial = false)
    val error by authViewModel.error.collectAsState(initial = null)
    val isEmailVerified by authViewModel.isEmailVerified.collectAsState(initial = false)
    val pendingEmail by authViewModel.pendingVerificationEmail.collectAsState(initial = null)
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)

    val showVerificationPending = (pendingEmail != null || (currentUser != null && !isEmailVerified))

    var showEditEmailDialog by remember { mutableStateOf(false) }
    var newEmailInput by remember { mutableStateOf("") }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }
    var isResetSuccess by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, showVerificationPending) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && showVerificationPending) {
                authViewModel.checkEmailVerificationSilently(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Neon field colors
    val neonFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor     = NeonPink,
        unfocusedBorderColor   = ReelDivider,
        focusedLabelColor      = NeonPink,
        unfocusedLabelColor    = TextGray,
        cursorColor            = NeonPink,
        focusedTextColor       = TextWhite,
        unfocusedTextColor     = TextWhite,
        focusedContainerColor  = ReelSurfaceHigh,
        unfocusedContainerColor = ReelSurface,
    )

    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = { Text("Correct Email Address", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your correct email address below. We will update your profile and send a new verification link.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newEmailInput,
                        onValueChange = { newEmailInput = it },
                        label = { Text("New Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.updateEmailAddress(context, newEmailInput) { success, _ ->
                            if (success) {
                                showEditEmailDialog = false
                            }
                        }
                    },
                    enabled = newEmailInput.isNotBlank() && !isLoading
                ) {
                    Text("Update & Resend")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog = false 
                resetStatusMessage = null
            },
            title = { Text("Reset Password 🔑", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your email address below. We'll send you a link to reset your password.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { 
                            resetEmailInput = it
                            resetStatusMessage = null
                        },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    resetStatusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = if (isResetSuccess) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.sendPasswordResetEmail(resetEmailInput) { success, message ->
                            isResetSuccess = success
                            resetStatusMessage = message
                        }
                    },
                    enabled = resetEmailInput.isNotBlank() && !isLoading
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showForgotPasswordDialog = false
                    resetStatusMessage = null
                }) {
                    Text("Close")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBlack),
        contentAlignment = Alignment.Center
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPink.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (showVerificationPending) {
                val targetEmail = pendingEmail ?: currentUser?.email ?: ""
                Text(
                    text = "Verify Your Email 🔑",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = "We sent a 6-digit verification code to:\n$targetEmail\n\nEnter the code below to activate your account:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { 
                            if (it.length <= 6) otpInput = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("6-Digit Verification Code") },
                        placeholder = { Text("e.g. 482910") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    error?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isSuccessMsg = it.contains("re-sent", ignoreCase = true) || (it.contains("sent", ignoreCase = true) && !it.contains("failed", ignoreCase = true))
                        Text(
                            text = it,
                            color = if (isSuccessMsg) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            authViewModel.verifyEmailWithOtp(otpInput, context, onLoginSuccess)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = otpInput.length == 6 && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Verify Code 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            authViewModel.resendVerificationEmail()
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Resend 6-Digit Code 🔄", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            authViewModel.checkEmailVerification(context, onLoginSuccess)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ReelDivider),
                        enabled = !isLoading
                    ) {
                        Text("Check Verification Status ✉️", fontSize = 13.sp, color = TextGray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            newEmailInput = targetEmail
                            showEditEmailDialog = true
                        }) {
                            Text("Fix Email Typo ✏️", fontSize = 13.sp, color = NeonCyan)
                        }

                        TextButton(onClick = { authViewModel.signOut() }) {
                            Text("Sign Out", fontSize = 13.sp, color = TextGray)
                        }
                    }
            } else {
                Text(
                    text = if (isLoginMode) "Welcome Back 👋" else "Join Reel n Earn",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite
                )
                Text(
                    text = if (isLoginMode) "Sign in to start earning" else "Sign up & start earning today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = neonFieldColors
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it },
                        label = { Text("Referral Code (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = { Text("Enter code to get 250 bonus", color = TextDimmed) },
                        colors = neonFieldColors
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = neonFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = neonFieldColors
                )

                if (isLoginMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                resetEmailInput = email
                                resetStatusMessage = null
                                showForgotPasswordDialog = true
                            }
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 13.sp,
                                color = NeonCyan
                            )
                        }
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Gradient CTA button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (email.isNotEmpty() && password.isNotEmpty() && (!isLoginMode && name.isNotEmpty() || isLoginMode) && !isLoading)
                                Brush.horizontalGradient(listOf(NeonPink, NeonPurple))
                            else
                                Brush.horizontalGradient(listOf(TextDimmed, TextDimmed))
                        )
                ) {
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                authViewModel.login(context, email, password, onLoginSuccess)
                            } else {
                                authViewModel.signUp(context, email, password, name, referralCode, onLoginSuccess)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        enabled = email.isNotEmpty() && password.isNotEmpty() && (!isLoginMode && name.isNotEmpty() || isLoginMode) && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isLoginMode) "SIGN IN" else "CREATE ACCOUNT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = { isLoginMode = !isLoginMode }) {
                    Text(
                        text = if (isLoginMode) "New here? Sign Up →" else "Already have an account? Sign In",
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


