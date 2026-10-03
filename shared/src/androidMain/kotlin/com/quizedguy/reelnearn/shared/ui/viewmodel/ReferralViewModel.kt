package com.quizedguy.reelnearn.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.PropertyName
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReferralViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _referrals = MutableStateFlow<List<ReferralRecord>>(emptyList())
    val referrals = _referrals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadReferrals()
    }

    fun loadReferrals() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        db.collection("referrals")
            .whereEqualTo("referrerId", userId)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) {
                    Log.e("ReferralViewModel", "Error loading referrals: ${e.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ReferralRecord::class.java)
                    }.sortedByDescending { it.timestamp }
                    _referrals.value = list
                }
            }
    }
}

data class ReferralRecord(
    @get:PropertyName("referredName")
    @set:PropertyName("referredName")
    var referredName: String = "",
    
    @get:PropertyName("referredEmail")
    @set:PropertyName("referredEmail")
    var referredEmail: String = "",
    
    @get:PropertyName("pointsAwarded")
    @set:PropertyName("pointsAwarded")
    var pointsAwarded: Int = 0,
    
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long = 0
)


