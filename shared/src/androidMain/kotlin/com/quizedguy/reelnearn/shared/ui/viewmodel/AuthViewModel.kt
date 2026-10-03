package com.quizedguy.reelnearn.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()

    private val _userReferralCode = MutableStateFlow<String?>(null)
    val userReferralCode = _userReferralCode.asStateFlow()

    private val _referredBy = MutableStateFlow<String?>(null)
    val referredBy = _referredBy.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(auth.currentUser?.isEmailVerified ?: false)
    val isEmailVerified = _isEmailVerified.asStateFlow()

    private val _pendingVerificationEmail = MutableStateFlow<String?>(null)
    val pendingVerificationEmail = _pendingVerificationEmail.asStateFlow()

    private val _pendingReferralCode = MutableStateFlow<String?>(null)

    private var profileListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var isApplyingReferral = false
    private var cachedDeviceId: String = ""

    init {
        checkUserRole()
    }

    fun checkUserRole(context: Context? = null) {
        val user = auth.currentUser
        profileListener?.remove()
        
        if (context != null) {
            cachedDeviceId = UsageStatsHelper.getDeviceId(context)
        }
        
        if (user != null) {
            _isEmailVerified.value = user.isEmailVerified
            profileListener = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    
                    if (snapshot != null && snapshot.exists()) {
                        // Resilient check for isAdmin (works even if stored as String "true" or for primary admin email)
                        val adminVal = snapshot.get("isAdmin")
                        val isPrimaryAdmin = user.email == "luckykaseqq@gmail.com" || user.email == "reelnearn@gmail.com"
                        _isAdmin.value = adminVal == true || adminVal.toString().lowercase() == "true" || isPrimaryAdmin
                        val nameStr = snapshot.getString("name") ?: user.displayName ?: ""
                        _userName.value = nameStr
                        
                        val refBy = snapshot.getString("referredBy")
                        _referredBy.value = refBy

                        val refCode = snapshot.getString("referralCode")
                        if (refCode == null) {
                            // Assign a referral code to legacy users
                            val newCode = generateReferralCode()
                            db.collection("users").document(user.uid).update("referralCode", newCode)
                            _userReferralCode.value = newCode
                        } else {
                            _userReferralCode.value = refCode
                        }

                        // Retroactive / Automatic Referral Processing
                        val pendingCode = snapshot.getString("pendingReferralCode")
                        val isVerified = user.isEmailVerified || snapshot.getBoolean("emailVerified") == true
                        _isEmailVerified.value = isVerified

                        if (!pendingCode.isNullOrEmpty() && refBy == null && isVerified && !isApplyingReferral) {
                            val userNameForRef = if (nameStr.isNotBlank()) nameStr else (user.email?.substringBefore("@") ?: "New User")
                            applyReferral(user.uid, userNameForRef, user.email ?: "", pendingCode, cachedDeviceId) { success, msg ->
                                android.util.Log.i("AuthViewModel", "Automatic referral processing result: success=$success, msg=$msg")
                            }
                        }
                    } else if (snapshot != null && !snapshot.exists()) {
                        // Create profile if missing
                        val isPrimaryAdmin = user.email == "luckykaseqq@gmail.com" || user.email == "reelnearn@gmail.com"
                        val newCode = generateReferralCode()
                        val userData = hashMapOf(
                            "email" to (user.email ?: ""),
                            "points" to 0,
                            "isAdmin" to isPrimaryAdmin,
                            "referralCode" to newCode,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(user.uid).set(userData)
                        _isAdmin.value = isPrimaryAdmin
                        _userReferralCode.value = newCode
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }

    fun login(context: Context, email: String, pass: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        val deviceId = UsageStatsHelper.getDeviceId(context)
        cachedDeviceId = deviceId
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        if (!user.isEmailVerified) {
                            _isLoading.value = false
                            _isEmailVerified.value = false
                            _pendingVerificationEmail.value = email
                            _error.value = "Please verify your email before signing in."
                            return@addOnCompleteListener
                        }
                        if (deviceId.isNotEmpty()) {
                            db.collection("users").document(user.uid).update("deviceId", deviceId)
                        }
                        _currentUser.value = user
                        _isEmailVerified.value = true
                        _pendingVerificationEmail.value = null
                        checkUserRole(context)
                        onSuccess()
                    }
                } else {
                    _isLoading.value = false
                    _error.value = task.exception?.message ?: "Login failed"
                }
            }
    }

    private fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return "Please enter your email address."

        // Strict Email Format Validation
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$")
        if (!emailRegex.matches(trimmed)) {
            return "Please enter a valid email address (e.g. name@gmail.com)."
        }

        val domain = trimmed.substringAfter("@").lowercase()

        // Block Disposable & Fake Email Domains
        val fakeDomains = setOf(
            "test.com", "fake.com", "example.com", "tempmail.com", "mailinator.com",
            "guerrillamail.com", "10minutemail.com", "trashmail.com", "yopmail.com",
            "dispostable.com", "temp-mail.org", "fakeinbox.com", "sharklasers.com"
        )
        if (fakeDomains.contains(domain)) {
            return "Disposable or test email addresses are not allowed. Please use a valid email address."
        }

        // Detect & Block Common Domain Typos
        val typoMap = mapOf(
            "gmai.com" to "gmail.com",
            "gamil.com" to "gmail.com",
            "gmial.com" to "gmail.com",
            "gmaill.com" to "gmail.com",
            "gmal.com" to "gmail.com",
            "gmai.co" to "gmail.com",
            "yaho.com" to "yahoo.com",
            "yahoo.co" to "yahoo.com",
            "outlok.com" to "outlook.com",
            "hotmial.com" to "hotmail.com"
        )

        if (typoMap.containsKey(domain)) {
            val suggested = typoMap[domain]
            return "Did you mean '@$suggested'? Please check your email spelling."
        }

        return null
    }

    fun signUp(context: Context, email: String, pass: String, name: String, referralCode: String? = null, onSuccess: () -> Unit) {
        val emailError = validateEmail(email)
        if (emailError != null) {
            _error.value = emailError
            return
        }

        _isLoading.value = true
        _error.value = null
        val deviceId = UsageStatsHelper.getDeviceId(context)
        cachedDeviceId = deviceId

        // Proceed with sign up (device anti-cheat is enforced on referral claims & redemptions)
        proceedSignUp(context, email, pass, name, referralCode, deviceId, onSuccess)
    }

    private fun getActionCodeSettings(): com.google.firebase.auth.ActionCodeSettings {
        return com.google.firebase.auth.ActionCodeSettings.newBuilder()
            .setUrl("https://geng-money.firebaseapp.com/__/auth/action")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.quizedguy.reelnearn",
                true,
                "30"
            )
            .build()
    }

    private fun sendVerificationEmailToUser(user: com.google.firebase.auth.FirebaseUser, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            user.sendEmailVerification(getActionCodeSettings()).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete?.invoke(true, null)
                } else {
                    android.util.Log.w("AuthViewModel", "sendEmailVerification with ActionCodeSettings failed: ${task.exception?.message}, falling back to standard sendEmailVerification")
                    user.sendEmailVerification().addOnCompleteListener { fallbackTask ->
                        if (fallbackTask.isSuccessful) {
                            onComplete?.invoke(true, null)
                        } else {
                            onComplete?.invoke(false, fallbackTask.exception?.message ?: task.exception?.message)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            user.sendEmailVerification().addOnCompleteListener { fallbackTask ->
                if (fallbackTask.isSuccessful) {
                    onComplete?.invoke(true, null)
                } else {
                    onComplete?.invoke(false, fallbackTask.exception?.message ?: e.message)
                }
            }
        }
    }

    private fun generateOtpCode(): String {
        return (100000..999999).random().toString()
    }

    private fun proceedSignUp(context: Context, email: String, pass: String, name: String, referralCode: String?, deviceId: String, onSuccess: () -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val otpCode = generateOtpCode()
                        // Send branded OTP verification email via Firestore Trigger Email Extension
                        sendOtpEmailViaFirestore(email, otpCode)

                        val newCode = generateReferralCode()
                        val userData = hashMapOf<String, Any>(
                            "email" to email,
                            "name" to name,
                            "points" to 0,
                            "referralCode" to newCode,
                            "deviceId" to deviceId,
                            "emailVerified" to false,
                            "verificationOtp" to otpCode,
                            "otpCreatedAt" to System.currentTimeMillis(),
                            "createdAt" to System.currentTimeMillis()
                        )
                        if (!referralCode.isNullOrEmpty()) {
                            userData["pendingReferralCode"] = referralCode.uppercase().trim()
                        }

                        db.collection("users").document(user.uid).set(userData)
                            .addOnSuccessListener {
                                _currentUser.value = user
                                _userName.value = name
                                _userReferralCode.value = newCode
                                _isEmailVerified.value = false
                                _pendingVerificationEmail.value = email
                                _pendingReferralCode.value = referralCode
                                _isLoading.value = false
                            }
                            .addOnFailureListener {
                                _isLoading.value = false
                                _error.value = "Auth successful but profile setup failed"
                            }
                    }
                } else {
                    _isLoading.value = false
                    _error.value = task.exception?.message ?: "Sign up failed"
                }
            }
    }

    fun checkEmailVerification(context: Context, onSuccess: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            _error.value = "User session expired. Please sign in again."
            return
        }
        _isLoading.value = true
        _error.value = null
        val deviceId = UsageStatsHelper.getDeviceId(context)
        cachedDeviceId = deviceId

        user.reload().addOnCompleteListener { task ->
            if (user.isEmailVerified) {
                _isLoading.value = false
                _isEmailVerified.value = true
                _pendingVerificationEmail.value = null
                val uid = user.uid

                db.collection("users").document(uid).update("emailVerified", true)

                // Read pendingReferralCode directly from Firestore if in-memory state was reset on app restart
                db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                    val pendingCode = _pendingReferralCode.value ?: userDoc.getString("pendingReferralCode")
                    val referredBy = userDoc.getString("referredBy")
                    val userNameStr = _userName.value ?: userDoc.getString("name") ?: user.email?.substringBefore("@") ?: "New User"

                    if (!pendingCode.isNullOrEmpty() && referredBy == null && !isApplyingReferral) {
                        applyReferral(uid, userNameStr, user.email ?: "", pendingCode, deviceId) { success, msg ->
                            checkUserRole(context)
                            onSuccess()
                        }
                    } else {
                        checkUserRole(context)
                        onSuccess()
                    }
                }.addOnFailureListener {
                    checkUserRole(context)
                    onSuccess()
                }
            } else {
                // Check Firestore document flag as secondary verification
                db.collection("users").document(user.uid).get().addOnSuccessListener { userDoc ->
                    _isLoading.value = false
                    if (userDoc.getBoolean("emailVerified") == true) {
                        _isEmailVerified.value = true
                        _pendingVerificationEmail.value = null
                        checkUserRole(context)
                        onSuccess()
                    } else {
                        _error.value = "Email not verified yet. Check your inbox/spam, or tap 'Continue to App 🚀' to enter immediately."
                    }
                }.addOnFailureListener {
                    _isLoading.value = false
                    _error.value = "Email not verified yet. Check your inbox/spam, or tap 'Continue to App 🚀' to enter immediately."
                }
            }
        }
    }

    fun checkEmailVerificationSilently(context: Context) {
        val user = auth.currentUser ?: return
        if (user.isEmailVerified) return
        val deviceId = UsageStatsHelper.getDeviceId(context)
        cachedDeviceId = deviceId

        user.reload().addOnCompleteListener { task ->
            if (user.isEmailVerified) {
                _isEmailVerified.value = true
                _pendingVerificationEmail.value = null
                val uid = user.uid
                db.collection("users").document(uid).update("emailVerified", true)
                db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                    val pendingCode = _pendingReferralCode.value ?: userDoc.getString("pendingReferralCode")
                    val referredBy = userDoc.getString("referredBy")
                    val userNameStr = _userName.value ?: userDoc.getString("name") ?: user.email?.substringBefore("@") ?: "New User"

                    if (!pendingCode.isNullOrEmpty() && referredBy == null && !isApplyingReferral) {
                        applyReferral(uid, userNameStr, user.email ?: "", pendingCode, deviceId) { success, msg ->
                            checkUserRole(context)
                        }
                    } else {
                        checkUserRole(context)
                    }
                }.addOnFailureListener {
                    checkUserRole(context)
                }
            }
        }
    }

    fun bypassVerificationAndProceed(context: Context, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        _isLoading.value = true
        _error.value = null
        val uid = user.uid
        val deviceId = UsageStatsHelper.getDeviceId(context)
        cachedDeviceId = deviceId

        _isEmailVerified.value = true
        _pendingVerificationEmail.value = null
        db.collection("users").document(uid).update("emailVerified", true)

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            _isLoading.value = false
            val pendingCode = _pendingReferralCode.value ?: userDoc.getString("pendingReferralCode")
            val referredBy = userDoc.getString("referredBy")
            val userNameStr = _userName.value ?: userDoc.getString("name") ?: user.email?.substringBefore("@") ?: "New User"

            if (!pendingCode.isNullOrEmpty() && referredBy == null && !isApplyingReferral) {
                applyReferral(uid, userNameStr, user.email ?: "", pendingCode, deviceId) { success, msg ->
                    checkUserRole(context)
                    onSuccess()
                }
            } else {
                checkUserRole(context)
                onSuccess()
            }
        }.addOnFailureListener {
            _isLoading.value = false
            checkUserRole(context)
            onSuccess()
        }
    }

    fun verifyEmailWithOtp(otpInput: String, context: Context, onSuccess: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            _error.value = "User session expired. Please sign in again."
            return
        }
        val cleaned = otpInput.trim()
        if (cleaned.length != 6 || !cleaned.all { it.isDigit() }) {
            _error.value = "Please enter the 6-digit verification code sent to your email."
            return
        }
        _isLoading.value = true
        _error.value = null
        val uid = user.uid
        val deviceId = UsageStatsHelper.getDeviceId(context)
        cachedDeviceId = deviceId

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val storedOtp = userDoc.getString("verificationOtp")
            val otpTime = userDoc.getLong("otpCreatedAt") ?: 0L
            val now = System.currentTimeMillis()

            if (storedOtp != null && storedOtp == cleaned) {
                if (now - otpTime > 30 * 60 * 1000) { // 30 mins expiry
                    _isLoading.value = false
                    _error.value = "Verification code has expired. Please tap 'Resend Code'."
                    return@addOnSuccessListener
                }

                // OTP verified! Mark emailVerified true in Firestore and local state
                db.collection("users").document(uid).update(
                    "emailVerified", true,
                    "verificationOtp", com.google.firebase.firestore.FieldValue.delete(),
                    "otpCreatedAt", com.google.firebase.firestore.FieldValue.delete()
                ).addOnSuccessListener {
                    _isLoading.value = false
                    _isEmailVerified.value = true
                    _pendingVerificationEmail.value = null

                    // Process pending referral code if present
                    val pendingCode = _pendingReferralCode.value ?: userDoc.getString("pendingReferralCode")
                    val referredBy = userDoc.getString("referredBy")
                    val userNameStr = _userName.value ?: userDoc.getString("name") ?: user.email?.substringBefore("@") ?: "New User"

                    if (!pendingCode.isNullOrEmpty() && referredBy == null && !isApplyingReferral) {
                        applyReferral(uid, userNameStr, user.email ?: "", pendingCode, deviceId) { success, msg ->
                            checkUserRole(context)
                            onSuccess()
                        }
                    } else {
                        checkUserRole(context)
                        onSuccess()
                    }
                }.addOnFailureListener { e ->
                    _isLoading.value = false
                    _error.value = "Failed to complete verification: ${e.localizedMessage}"
                }
            } else {
                _isLoading.value = false
                _error.value = "Incorrect verification code. Please check your email inbox (or Spam folder)."
            }
        }.addOnFailureListener { e ->
            _isLoading.value = false
            _error.value = "Verification check failed: ${e.localizedMessage}"
        }
    }

    private fun sendOtpEmailViaFirestore(emailStr: String, otpCode: String) {
        if (emailStr.isBlank()) return
        val trimmedEmail = emailStr.trim()
        val subject = "$otpCode is your Reel n Earn verification code"
        val plainText = """
Hello,

Your verification code for Reel n Earn is: $otpCode

This code is valid for 30 minutes. Please enter it in the app to complete your account verification.

If you did not create a Reel n Earn account, please ignore this email.

Best regards,
Reel n Earn Support Team
reelnearn@gmail.com
        """.trimIndent()

        val htmlContent = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Reel n Earn Verification Code</title>
</head>
<body style="margin: 0; padding: 0; background-color: #0d0f17; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #ffffff;">
  <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0d0f17; padding: 30px 15px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" max-width="560px" border="0" cellspacing="0" cellpadding="0" style="max-width: 560px; background-color: #161926; border-radius: 16px; border: 1px solid #282d42; overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.4);">
          <!-- Header -->
          <tr>
            <td align="center" style="padding: 32px 24px 20px 24px; background: linear-gradient(135deg, #ff007a 0%, #7928ca 100%);">
              <h1 style="margin: 0; color: #ffffff; font-size: 26px; font-weight: 800; letter-spacing: 0.5px;">Reel n Earn</h1>
              <p style="margin: 6px 0 0 0; color: rgba(255,255,255,0.9); font-size: 13px; font-weight: 500;">Account Security & Verification</p>
            </td>
          </tr>
          <!-- Body -->
          <tr>
            <td style="padding: 32px 28px;">
              <p style="margin: 0 0 16px 0; font-size: 15px; line-height: 1.6; color: #e2e8f0;">
                Hello,
              </p>
              <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                Thank you for joining <strong>Reel n Earn</strong>! Use the 6-digit verification code below to activate your account:
              </p>
              
              <!-- OTP Box -->
              <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="margin: 28px 0;">
                <tr>
                  <td align="center">
                    <div style="display: inline-block; background-color: #0d0f17; border: 2px solid #ff007a; border-radius: 12px; padding: 16px 36px; text-align: center;">
                      <span style="font-family: 'Courier New', Courier, monospace; font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #ffd700;">$otpCode</span>
                    </div>
                  </td>
                </tr>
              </table>

              <p style="margin: 0 0 12px 0; font-size: 13px; color: #94a3b8; text-align: center;">
                ⏱️ This code will expire in <strong>30 minutes</strong>.
              </p>
              
              <hr style="border: none; border-top: 1px solid #282d42; margin: 28px 0;">

              <p style="margin: 0; font-size: 12px; line-height: 1.5; color: #64748b;">
                <strong>Security Notice:</strong> If you did not request this verification code, please ignore this email. No changes will be made to your account.
              </p>
            </td>
          </tr>
          <!-- Footer -->
          <tr>
            <td align="center" style="padding: 20px; background-color: #0f111a; border-top: 1px solid #1e2235;">
              <p style="margin: 0; font-size: 12px; color: #64748b;">
                © 2026 Reel n Earn. All rights reserved.
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
        """.trimIndent()

        val mailData = hashMapOf(
            "to" to listOf(trimmedEmail),
            "message" to hashMapOf(
                "subject" to subject,
                "text" to plainText,
                "html" to htmlContent
            ),
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("mail").add(mailData)
            .addOnSuccessListener {
                android.util.Log.i("AuthViewModel", "Trigger Email doc queued successfully for $trimmedEmail")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthViewModel", "Failed to queue Trigger Email doc: ${e.message}")
            }
    }

    private fun dispatchCloudFunctionVerificationEmail(emailStr: String, otpCode: String? = null) {
        if (emailStr.isBlank()) return
        if (!otpCode.isNullOrBlank()) {
            sendOtpEmailViaFirestore(emailStr, otpCode)
        }
    }

    private var lastResendTimestamp: Long = 0L

    fun resendVerificationEmail() {
        val user = auth.currentUser ?: return
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastResendTimestamp) / 1000
        if (elapsedSeconds < 60) {
            val remaining = 60 - elapsedSeconds
            _error.value = "Please wait $remaining seconds before requesting another code."
            return
        }

        _isLoading.value = true
        _error.value = null
        lastResendTimestamp = now

        val newOtp = generateOtpCode()
        val uid = user.uid

        db.collection("users").document(uid).update(
            "verificationOtp", newOtp,
            "otpCreatedAt", now
        ).addOnCompleteListener {
            user.email?.let { email ->
                sendOtpEmailViaFirestore(email, newOtp)
            }
            _isLoading.value = false
            _error.value = "A new 6-digit verification code has been re-sent to your inbox!"
        }
    }

    fun updateEmailAddress(context: Context, newEmail: String, onResult: (Boolean, String) -> Unit) {
        val emailErr = validateEmail(newEmail)
        if (emailErr != null) {
            onResult(false, emailErr)
            return
        }
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "User session expired. Please sign in again.")
            return
        }
        _isLoading.value = true
        _error.value = null
        val trimmed = newEmail.trim()
        user.updateEmail(trimmed)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    db.collection("users").document(user.uid).update("email", trimmed)
                    sendVerificationEmailToUser(user)
                    _pendingVerificationEmail.value = trimmed
                    _isLoading.value = false
                    onResult(true, "Email address updated! A verification email was sent to $trimmed")
                } else {
                    _isLoading.value = false
                    val rawMsg = task.exception?.message ?: "Failed to update email address"
                    val msg = if (rawMsg.contains("recent", ignoreCase = true) || rawMsg.contains("reauthenticate", ignoreCase = true)) {
                        "For security reasons, please sign out and sign in again before changing your email."
                    } else {
                        rawMsg
                    }
                    _error.value = msg
                    onResult(false, msg)
                }
            }
    }

    fun sendPasswordResetEmail(emailStr: String, onResult: (Boolean, String) -> Unit) {
        val emailErr = validateEmail(emailStr)
        if (emailErr != null) {
            onResult(false, emailErr)
            return
        }
        _isLoading.value = true
        _error.value = null
        val trimmed = emailStr.trim()
        
        // Dispatch branded password reset email via Firestore Trigger Email Extension (from reelnearn@gmail.com)
        sendPasswordResetEmailViaFirestore(trimmed)

        auth.sendPasswordResetEmail(trimmed, getActionCodeSettings())
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    onResult(true, "Password reset email sent to $trimmed! Please check your inbox.")
                } else {
                    // Even if Firebase Auth link throttles, the custom trigger email was queued
                    onResult(true, "Password reset email sent to $trimmed! Please check your inbox.")
                }
            }
    }

    private fun sendPasswordResetEmailViaFirestore(emailStr: String) {
        if (emailStr.isBlank()) return
        val trimmedEmail = emailStr.trim()
        val subject = "Reset your Reel n Earn password"
        val plainText = """
Hello,

A password reset was requested for your Reel n Earn account ($trimmedEmail).

Please click the secure link below to reset your password:
https://geng-money.firebaseapp.com/__/auth/action?mode=resetPassword&email=$trimmedEmail

If you did not request a password reset, please ignore this email. Your account remains secure.

Best regards,
Reel n Earn Support Team
reelnearn@gmail.com
        """.trimIndent()

        val htmlContent = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Reset your Reel n Earn password</title>
</head>
<body style="margin: 0; padding: 0; background-color: #0d0f17; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #ffffff;">
  <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0d0f17; padding: 30px 15px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" max-width="560px" border="0" cellspacing="0" cellpadding="0" style="max-width: 560px; background-color: #161926; border-radius: 16px; border: 1px solid #282d42; overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.4);">
          <tr>
            <td align="center" style="padding: 32px 24px 20px 24px; background: linear-gradient(135deg, #ff007a 0%, #7928ca 100%);">
              <h1 style="margin: 0; color: #ffffff; font-size: 26px; font-weight: 800; letter-spacing: 0.5px;">Reel n Earn</h1>
              <p style="margin: 6px 0 0 0; color: rgba(255,255,255,0.9); font-size: 13px; font-weight: 500;">Password Reset Request</p>
            </td>
          </tr>
          <tr>
            <td style="padding: 32px 28px;">
              <p style="margin: 0 0 16px 0; font-size: 15px; line-height: 1.6; color: #e2e8f0;">
                Hello,
              </p>
              <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                We received a request to reset the password for your <strong>Reel n Earn</strong> account (<strong>$trimmedEmail</strong>). Tap the button below to choose a new password:
              </p>
              
              <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="margin: 28px 0;">
                <tr>
                  <td align="center">
                    <a href="https://geng-money.firebaseapp.com/__/auth/action?mode=resetPassword&email=$trimmedEmail" style="display: inline-block; background: linear-gradient(135deg, #ff007a 0%, #7928ca 100%); color: #ffffff; text-decoration: none; font-weight: 700; font-size: 16px; padding: 14px 32px; border-radius: 10px; box-shadow: 0 4px 15px rgba(255, 0, 122, 0.4);">
                      Reset Password 🔑
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 0 0 12px 0; font-size: 13px; color: #94a3b8; text-align: center;">
                If the button above does not work, copy and paste this link into your browser:<br>
                <a href="https://geng-money.firebaseapp.com/__/auth/action?mode=resetPassword&email=$trimmedEmail" style="color: #00e5ff; word-break: break-all; font-size: 12px;">https://geng-money.firebaseapp.com/__/auth/action?mode=resetPassword&email=$trimmedEmail</a>
              </p>
              
              <hr style="border: none; border-top: 1px solid #282d42; margin: 28px 0;">

              <p style="margin: 0; font-size: 12px; line-height: 1.5; color: #64748b;">
                <strong>Security Notice:</strong> If you did not request a password reset, please ignore this email. Your password will remain unchanged.
              </p>
            </td>
          </tr>
          <tr>
            <td align="center" style="padding: 20px; background-color: #0f111a; border-top: 1px solid #1e2235;">
              <p style="margin: 0; font-size: 12px; color: #64748b;">
                © 2026 Reel n Earn. All rights reserved. • Support: reelnearn@gmail.com
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
        """.trimIndent()

        val mailData = hashMapOf(
            "to" to listOf(trimmedEmail),
            "message" to hashMapOf(
                "subject" to subject,
                "text" to plainText,
                "html" to htmlContent
            ),
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("mail").add(mailData)
            .addOnSuccessListener {
                android.util.Log.i("AuthViewModel", "Password reset email queued for $trimmedEmail")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthViewModel", "Failed to queue password reset email: ${e.message}")
            }
    }

    fun sendPasswordResetForCurrentUser(onResult: (Boolean, String) -> Unit) {
        val userEmail = currentUser.value?.email ?: auth.currentUser?.email
        if (userEmail.isNullOrBlank()) {
            onResult(false, "No active user email found.")
            return
        }
        sendPasswordResetEmail(userEmail, onResult)
    }

    private fun applyReferral(
        newUserId: String, 
        newUserName: String, 
        newUserEmail: String, 
        referralCode: String, 
        currentDeviceId: String, 
        onResult: (Boolean, String) -> Unit
    ) {
        if (isApplyingReferral) {
            onResult(false, "Referral application already in progress.")
            return
        }
        isApplyingReferral = true

        val cleanedCode = referralCode.uppercase().trim()
        db.collection("users")
            .whereEqualTo("referralCode", cleanedCode)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    isApplyingReferral = false
                    _isLoading.value = false
                    android.util.Log.w("AuthViewModel", "Referral code '$cleanedCode' not found.")
                    val errMsg = "Referral code '$cleanedCode' is invalid. Please check the code and try again."
                    _error.value = errMsg
                    onResult(false, errMsg)
                } else {
                    val referrerDoc = documents.documents[0]
                    val referrerId = referrerDoc.id
                    val referrerDeviceId = referrerDoc.getString("deviceId") ?: ""
                    
                    // Anti-Cheat Check 0: Prevent self-referral by account
                    if (referrerId == newUserId) {
                        isApplyingReferral = false
                        _isLoading.value = false
                        android.util.Log.w("AuthViewModel", "Self-referral by account blocked.")
                        val errMsg = "You cannot use your own referral code."
                        _error.value = errMsg
                        onResult(false, errMsg)
                        return@addOnSuccessListener
                    }

                    // Anti-Cheat Check 1: Prevent self-referral on same physical device
                    if (currentDeviceId.isNotEmpty() && referrerDeviceId.isNotEmpty() && referrerDeviceId == currentDeviceId) {
                        isApplyingReferral = false
                        _isLoading.value = false
                        android.util.Log.w("AuthViewModel", "Self-referral on same device blocked.")
                        val errMsg = "Referral rewards cannot be claimed using a self-referral on the same device."
                        _error.value = errMsg
                        onResult(false, errMsg)
                        return@addOnSuccessListener
                    }
                    
                    // Anti-Cheat Check 2: Prevent multiple referral claims on the same device
                    if (currentDeviceId.isNotEmpty()) {
                        db.collection("users")
                            .whereEqualTo("deviceId", currentDeviceId)
                            .get()
                            .addOnSuccessListener { deviceUsers ->
                                val alreadyClaimedOnDevice = deviceUsers.documents.any { doc ->
                                    doc.id != newUserId && doc.getString("referredBy") != null
                                }
                                if (alreadyClaimedOnDevice) {
                                    isApplyingReferral = false
                                    _isLoading.value = false
                                    android.util.Log.w("AuthViewModel", "Multiple referral claims on same device blocked.")
                                    val errMsg = "A referral bonus has already been claimed on this device."
                                    _error.value = errMsg
                                    onResult(false, errMsg)
                                    return@addOnSuccessListener
                                }
                                
                                executeReferralBatch(newUserId, referrerId, newUserName, newUserEmail, currentDeviceId, onResult)
                            }
                            .addOnFailureListener {
                                executeReferralBatch(newUserId, referrerId, newUserName, newUserEmail, currentDeviceId, onResult)
                            }
                    } else {
                        executeReferralBatch(newUserId, referrerId, newUserName, newUserEmail, currentDeviceId, onResult)
                    }
                }
            }
            .addOnFailureListener { e ->
                isApplyingReferral = false
                _isLoading.value = false
                android.util.Log.e("AuthViewModel", "Failed to query referral code: ${e.message}")
                onResult(false, "Failed to query referral code: ${e.localizedMessage}")
            }
    }

    fun claimReferralCode(context: Context, referralCode: String, onResult: (Boolean, String) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "User session expired. Please sign in again.")
            return
        }
        val cleanedCode = referralCode.uppercase().trim()
        if (cleanedCode.isEmpty()) {
            onResult(false, "Please enter a valid referral code.")
            return
        }
        _isLoading.value = true
        _error.value = null
        val deviceId = UsageStatsHelper.getDeviceId(context)
        cachedDeviceId = deviceId

        db.collection("users").document(user.uid).get().addOnSuccessListener { userDoc ->
            val existingReferredBy = userDoc.getString("referredBy")
            if (existingReferredBy != null) {
                _isLoading.value = false
                onResult(false, "You have already claimed a referral bonus.")
                return@addOnSuccessListener
            }

            val myRefCode = userDoc.getString("referralCode") ?: _userReferralCode.value ?: ""
            if (cleanedCode.equals(myRefCode, ignoreCase = true)) {
                _isLoading.value = false
                onResult(false, "You cannot use your own referral code.")
                return@addOnSuccessListener
            }

            val userNameStr = _userName.value ?: userDoc.getString("name") ?: user.email?.substringBefore("@") ?: "User"

            applyReferral(user.uid, userNameStr, user.email ?: "", cleanedCode, deviceId, onResult)
        }.addOnFailureListener { e ->
            _isLoading.value = false
            onResult(false, "Failed to claim code: ${e.localizedMessage}")
        }
    }

    private fun executeReferralBatch(
        newUserId: String, 
        referrerId: String, 
        newUserName: String, 
        newUserEmail: String, 
        deviceId: String, 
        onResult: (Boolean, String) -> Unit
    ) {
        val batch = db.batch()
        
        // 1. Reward new user (250 pts) and delete pendingReferralCode
        val newUserRef = db.collection("users").document(newUserId)
        batch.update(newUserRef, 
            "points", com.google.firebase.firestore.FieldValue.increment(250), 
            "referredBy", referrerId,
            "pendingReferralCode", com.google.firebase.firestore.FieldValue.delete()
        )
        
        // 2. Reward referrer (250 pts)
        val referrerRef = db.collection("users").document(referrerId)
        batch.update(referrerRef, "points", com.google.firebase.firestore.FieldValue.increment(250))
        
        // 3. Log referral
        val referralRecord = hashMapOf(
            "referrerId" to referrerId,
            "referredId" to newUserId,
            "referredName" to newUserName,
            "referredEmail" to newUserEmail,
            "deviceId" to deviceId,
            "pointsAwarded" to 250,
            "timestamp" to System.currentTimeMillis()
        )
        val referralRef = db.collection("referrals").document()
        batch.set(referralRef, referralRecord)
        
        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isApplyingReferral = false
                _isLoading.value = false
                _referredBy.value = referrerId
                android.util.Log.i("AuthViewModel", "Referral batch successfully executed for new user $newUserId and referrer $referrerId")
                onResult(true, "🎉 Referral code applied! 250 bonus points added to your account!")
            } else {
                android.util.Log.e("AuthViewModel", "Referral batch commit failed: ${task.exception?.message}, executing fallback sequential writes...")
                // Fallback sequential updates if batch is rejected
                val newUserRef = db.collection("users").document(newUserId)
                newUserRef.update(
                    "points", com.google.firebase.firestore.FieldValue.increment(250),
                    "referredBy", referrerId,
                    "pendingReferralCode", com.google.firebase.firestore.FieldValue.delete()
                ).addOnSuccessListener {
                    db.collection("users").document(referrerId).update("points", com.google.firebase.firestore.FieldValue.increment(250))
                    val referralRef = db.collection("referrals").document()
                    referralRef.set(referralRecord)

                    isApplyingReferral = false
                    _isLoading.value = false
                    _referredBy.value = referrerId
                    onResult(true, "🎉 Referral code applied! 250 bonus points added to your account!")
                }.addOnFailureListener { fallbackErr ->
                    isApplyingReferral = false
                    _isLoading.value = false
                    _error.value = "Failed to process referral bonus: ${fallbackErr.localizedMessage}"
                    onResult(false, "Failed to process referral bonus: ${fallbackErr.localizedMessage}")
                }
            }
        }
    }

    private fun generateReferralCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    fun signOut() {
        profileListener?.remove()
        profileListener = null
        auth.signOut()
        _currentUser.value = null
        _isAdmin.value = false
        _userName.value = null
        _referredBy.value = null
        _isEmailVerified.value = false
        _pendingVerificationEmail.value = null
        _pendingReferralCode.value = null
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }
}


