package com.quizedguy.reelnearn.shared.ui.viewmodel

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import com.quizedguy.reelnearn.shared.util.RewardedAdManager
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude
import java.util.concurrent.TimeUnit

class PointsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var pointsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var usageListener: ListenerRegistration? = null

    private val _userPoints = MutableStateFlow(0)
    val userPoints = _userPoints.asStateFlow()

    private val _totalPointsEarned = MutableStateFlow(0)
    val totalPointsEarned = _totalPointsEarned.asStateFlow()

    private val _withdrawalHistory = MutableStateFlow(listOf<WithdrawalRequest>())
    val withdrawalHistory = _withdrawalHistory.asStateFlow()

    private val _dailyUsageHistory = MutableStateFlow(listOf<DailyUsageRecord>())
    val dailyUsageHistory = _dailyUsageHistory.asStateFlow()

    private val _collectingRecordIds = MutableStateFlow<Set<String>>(emptySet())
    val collectingRecordIds = _collectingRecordIds.asStateFlow()

    private val _pointCreditEvent = MutableStateFlow<Int?>(null)
    val pointCreditEvent = _pointCreditEvent.asStateFlow()

    private val _lastCheckInDate = MutableStateFlow<String?>(null)
    val lastCheckInDate = _lastCheckInDate.asStateFlow()

    private val _checkInStreak = MutableStateFlow(0)
    val checkInStreak = _checkInStreak.asStateFlow()

    private val _isClaimingCheckIn = MutableStateFlow(false)
    val isClaimingCheckIn = _isClaimingCheckIn.asStateFlow()

    private val _isVip = MutableStateFlow(false)
    val isVip = _isVip.asStateFlow()

    private val _vipExpiresAt = MutableStateFlow(0L)
    val vipExpiresAt = _vipExpiresAt.asStateFlow()

    private val _dailyAdQuestCount = MutableStateFlow(0)
    val dailyAdQuestCount = _dailyAdQuestCount.asStateFlow()

    private val _lastAdQuestDate = MutableStateFlow<String?>(null)
    val lastAdQuestDate = _lastAdQuestDate.asStateFlow()

    private val _lastSpinDate = MutableStateFlow<String?>(null)
    val lastSpinDate = _lastSpinDate.asStateFlow()

    fun clearPointCreditEvent() {
        _pointCreditEvent.value = null
    }

    init {
        loadUserPoints()
        loadWithdrawalHistory()
        loadDailyUsageHistory()
    }

    private fun loadDailyUsageHistory() {
        val userId = auth.currentUser?.uid ?: return
        usageListener?.remove()
        usageListener = db.collection("daily_usage")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DailyUsageRecord::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.date }.take(10)
                    _dailyUsageHistory.value = list
                }
            }
    }

    fun syncUsageHistory(context: android.content.Context) {
        val userId = auth.currentUser?.uid ?: return
        val deviceId = UsageStatsHelper.getDeviceId(context)
        
        // Get user's account creation timestamp to avoid syncing daily usage for pre-registration dates
        val creationTime = auth.currentUser?.metadata?.creationTimestamp
        val registrationTime = if (creationTime == null || creationTime == 0L) {
            System.currentTimeMillis()
        } else {
            creationTime
        }
        val registrationDate = Instant.ofEpochMilli(registrationTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // Sync last 7 days
        val today = LocalDate.now()
        for (i in 0..7) {
            val date = today.minusDays(i.toLong())
            if (date.isBefore(registrationDate)) continue

            val dateStr = date.toString()
            val docId = "${userId}_$dateStr"
            
            val reelWatchMillis = UsageStatsHelper.getReelWatchTimeForDate(context, date)
            val usageMillis = reelWatchMillis
            val pointsPotential = calculatePoints(usageMillis)

            val usageRef = db.collection("daily_usage").document(docId)

            usageRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    usageRef.update(
                        mapOf(
                            "totalMillis" to usageMillis,
                            "pointsPotential" to pointsPotential,
                            "deviceId" to deviceId
                        )
                    )
                } else {
                    val record = hashMapOf(
                        "userId" to userId,
                        "deviceId" to deviceId,
                        "date" to dateStr,
                        "totalMillis" to usageMillis,
                        "pointsPotential" to pointsPotential,
                        "pointsCollected" to 0,
                        "tier1Collected" to false,
                        "tier2Collected" to false,
                        "tier3Collected" to false,
                        "isCollected" to false,
                        "isApproved" to true,
                        "approvedAt" to System.currentTimeMillis(),
                        "collectedAt" to 0L
                    )
                    usageRef.set(record)
                }
            }
        }
    }

    fun calculatePoints(millis: Long): Int {
        val hours = millis / 3600000.0
        var pts = 0
        if (hours >= 2.0) pts += 40
        if (hours >= 3.0) pts += 60
        if (hours >= 4.0) pts += 100
        return pts
    }

    /**
     * Collects milestone reward (Tier 1 = 2h/40pts, Tier 2 = 3h/60pts, Tier 3 = 4h/100pts)
     * Requires watching a Rewarded Video Ad.
     */
    fun collectMilestoneReward(
        context: android.content.Context,
        tier: Int,
        targetDate: String = LocalDate.now().toString(),
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val userId = auth.currentUser?.uid ?: return
        val activity = context as? Activity ?: return
        val docId = "${userId}_$targetDate"

        val (targetHours, points) = when (tier) {
            1 -> Pair(2.0, 40)
            2 -> Pair(3.0, 60)
            3 -> Pair(4.0, 100)
            else -> return
        }

        val usageRef = db.collection("daily_usage").document(docId)
        usageRef.get().addOnSuccessListener { snapshot ->
            val totalMillis = if (targetDate == LocalDate.now().toString()) {
                val liveWatch = UsageStatsHelper.getReelWatchTime(context)
                if (liveWatch > 0L) liveWatch else (snapshot.getLong("totalMillis") ?: 0L)
            } else if (snapshot.exists()) {
                snapshot.getLong("totalMillis") ?: 0L
            } else {
                try {
                    UsageStatsHelper.getReelWatchTimeForDate(context, LocalDate.parse(targetDate))
                } catch (e: Exception) {
                    0L
                }
            }
            val hours = totalMillis / 3600000.0

            if (hours < targetHours) {
                Toast.makeText(context, "Goal not completed! Watch ${targetHours.toInt()}h to unlock $points points.", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false)
                return@addOnSuccessListener
            }

            val tierField = "tier${tier}Collected"
            val alreadyClaimed = snapshot.getBoolean(tierField) ?: false
            if (alreadyClaimed) {
                Toast.makeText(context, "This goal has already been collected!", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false)
                return@addOnSuccessListener
            }

            // Watch Rewarded Video Ad to collect
            RewardedAdManager.showAdWithoutPoints(activity) { adWatched ->
                if (adWatched) {
                    val userRef = db.collection("users").document(userId)
                    val batch = db.batch()
                    batch.update(userRef, "points", com.google.firebase.firestore.FieldValue.increment(points.toLong()))

                    val isTier1 = if (tier == 1) true else (snapshot.getBoolean("tier1Collected") ?: false)
                    val isTier2 = if (tier == 2) true else (snapshot.getBoolean("tier2Collected") ?: false)
                    val isTier3 = if (tier == 3) true else (snapshot.getBoolean("tier3Collected") ?: false)
                    val allCollected = isTier1 && isTier2 && isTier3

                    val updates = mutableMapOf<String, Any>(
                        tierField to true,
                        "pointsCollected" to com.google.firebase.firestore.FieldValue.increment(points.toLong()),
                        "totalMillis" to totalMillis,
                        "isApproved" to true,
                        "isCollected" to allCollected,
                        "lastCollectedAt" to System.currentTimeMillis()
                    )
                    if (snapshot.exists()) {
                        batch.update(usageRef, updates)
                    } else {
                        updates["userId"] = userId
                        updates["date"] = targetDate
                        updates["deviceId"] = UsageStatsHelper.getDeviceId(context)
                        batch.set(usageRef, updates)
                    }

                    batch.commit().addOnSuccessListener {
                        _pointCreditEvent.value = points
                        Toast.makeText(context, "🎉 +$points Points Collected Successfully!", Toast.LENGTH_SHORT).show()
                        onComplete?.invoke(true)
                    }.addOnFailureListener {
                        Toast.makeText(context, "Error collecting points. Please try again.", Toast.LENGTH_SHORT).show()
                        onComplete?.invoke(false)
                    }
                } else {
                    Toast.makeText(context, "Please watch the video ad to collect your reward.", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(false)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
            onComplete?.invoke(false)
        }
    }

    private fun loadWithdrawalHistory() {
        val userId = auth.currentUser?.uid ?: return
        historyListener?.remove()
        historyListener = db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val request = doc.toObject(WithdrawalRequest::class.java)
                            request?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                    _withdrawalHistory.value = list
                    calculateTotalEarned(list, _userPoints.value)
                }
            }
    }

    private fun calculateTotalEarned(history: List<WithdrawalRequest>, currentPoints: Int) {
        val historicalSpend = history.filter { 
            it.status.equals("Approved", ignoreCase = true) || it.status.equals("Pending", ignoreCase = true) 
        }.sumOf { it.pointsDeducted }
        _totalPointsEarned.value = historicalSpend + currentPoints
    }

    private fun loadUserPoints() {
        val userId = auth.currentUser?.uid ?: return
        
        pointsListener?.remove()
        pointsListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val points = snapshot.getLong("points")?.toInt() ?: 0
                    val lastDate = snapshot.getString("lastCheckInDate")
                    val streak = snapshot.getLong("checkInStreak")?.toInt() ?: 0
                    
                    val vipExp = snapshot.getLong("vipExpiresAt") ?: 0L
                    val isVipActive = vipExp > System.currentTimeMillis()
                    val adQuestCount = snapshot.getLong("dailyAdQuestCount")?.toInt() ?: 0
                    val adQuestDate = snapshot.getString("lastAdQuestDate")
                    val lastSpin = snapshot.getString("lastSpinDate")
                    
                    val todayStr = LocalDate.now().toString()
                    val activeQuestCount = if (adQuestDate == todayStr) adQuestCount else 0

                    _lastCheckInDate.value = lastDate
                    _checkInStreak.value = streak
                    _isVip.value = isVipActive
                    _vipExpiresAt.value = vipExp
                    _dailyAdQuestCount.value = activeQuestCount
                    _lastAdQuestDate.value = adQuestDate
                    _lastSpinDate.value = lastSpin
                    
                    // Notify user if points increased
                    if (points > _userPoints.value && _userPoints.value > 0) {
                        val difference = points - _userPoints.value
                        _pointCreditEvent.value = difference
                    }

                    _userPoints.value = points
                    calculateTotalEarned(_withdrawalHistory.value, points)
                }
            }
    }

    fun requestWithdrawal(context: android.content.Context, amount: Int, requiredPoints: Int, rewardName: String, paymentDetails: String? = null) {
        val userId = auth.currentUser?.uid ?: return

        if (_userPoints.value < 1000 || _userPoints.value < requiredPoints) {
            Toast.makeText(context, "Insufficient points for withdrawal.", Toast.LENGTH_SHORT).show()
            return
        }

        val twentyFourHoursMillis = 24 * 60 * 60 * 1000L // 86,400,000 ms
        val currentDeviceTime = System.currentTimeMillis()

        // 1. Local Memory / History Cooldown & Time-Travel Check
        val maxHistoryTimestamp = _withdrawalHistory.value.maxOfOrNull { it.createdAt } ?: 0L
        if (maxHistoryTimestamp > 0L) {
            if (currentDeviceTime < maxHistoryTimestamp) {
                Toast.makeText(context, "Clock anomaly detected! Please set your phone clock to automatic network time.", Toast.LENGTH_LONG).show()
                return
            }
            val elapsedLocal = currentDeviceTime - maxHistoryTimestamp
            if (elapsedLocal < twentyFourHoursMillis) {
                val remainingHours = ((twentyFourHoursMillis - elapsedLocal) / (60 * 60 * 1000L)).coerceAtLeast(1)
                Toast.makeText(context, "Daily limit reached! You can only withdraw once every 24 hours. Try again in $remainingHours hour(s).", Toast.LENGTH_LONG).show()
                return
            }
        }

        // 2. Server-Side Firestore Verification Guard (Double Protection against cleared app data or date hacking)
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val lastWithdrawalAt = userDoc.getLong("lastWithdrawalAt") ?: 0L
            val currentPointsInDb = userDoc.getLong("points")?.toInt() ?: _userPoints.value
            val now = System.currentTimeMillis()

            if (currentPointsInDb < 1000 || currentPointsInDb < requiredPoints) {
                Toast.makeText(context, "Insufficient points for withdrawal.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            if (lastWithdrawalAt > 0L) {
                // Check if phone clock was set backwards before last server withdrawal time
                if (now < lastWithdrawalAt) {
                    Toast.makeText(context, "Clock anomaly detected! Please set your phone clock to automatic network time.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val elapsedServer = now - lastWithdrawalAt
                if (elapsedServer < twentyFourHoursMillis) {
                    val remainingHours = ((twentyFourHoursMillis - elapsedServer) / (60 * 60 * 1000L)).coerceAtLeast(1)
                    Toast.makeText(context, "Daily limit reached! You can only withdraw once every 24 hours. Try again in $remainingHours hour(s).", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
            }

            // Also query withdrawals collection directly to verify no recent withdrawal exists for this user in the last 24h
            db.collection("withdrawals")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { withdrawalDocs ->
                    val recentDocs = withdrawalDocs.documents.filter { doc ->
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        (now - createdAt) < twentyFourHoursMillis || now < createdAt
                    }
                    if (recentDocs.isNotEmpty()) {
                        Toast.makeText(context, "Daily limit reached! You can only make 1 withdrawal every 24 hours.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // Everything is valid & anti-cheat verified! Submit withdrawal:
                    val withdrawal = hashMapOf(
                        "userId" to userId,
                        "amountRs" to amount,
                        "rewardName" to rewardName,
                        "pointsDeducted" to requiredPoints,
                        "paymentDetails" to (paymentDetails ?: ""),
                        "status" to "Pending",
                        "createdAt" to now,
                        "requestedAt" to now
                    )

                    db.collection("withdrawals")
                        .add(withdrawal)
                        .addOnSuccessListener {
                            db.collection("users").document(userId)
                                .update(
                                    "points", currentPointsInDb - requiredPoints,
                                    "lastWithdrawalAt", now
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Withdrawal request submitted successfully!", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to submit request: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    // Fallback submission if collection query fails
                    val withdrawal = hashMapOf(
                        "userId" to userId,
                        "amountRs" to amount,
                        "rewardName" to rewardName,
                        "pointsDeducted" to requiredPoints,
                        "paymentDetails" to (paymentDetails ?: ""),
                        "status" to "Pending",
                        "createdAt" to now,
                        "requestedAt" to now
                    )

                    db.collection("withdrawals")
                        .add(withdrawal)
                        .addOnSuccessListener {
                            db.collection("users").document(userId)
                                .update(
                                    "points", currentPointsInDb - requiredPoints,
                                    "lastWithdrawalAt", now
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Withdrawal request submitted successfully!", Toast.LENGTH_SHORT).show()
                                }
                        }
                }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Network error. Please try again later.", Toast.LENGTH_SHORT).show()
        }
    }

    fun collectPoints(record: DailyUsageRecord, context: android.content.Context, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        if (_collectingRecordIds.value.contains(record.id)) return
        
        // Add to collecting set to lock it
        _collectingRecordIds.value = _collectingRecordIds.value + record.id
        val deviceId = UsageStatsHelper.getDeviceId(context)
        
        // Anti-Cheat Check: Ensure no other user account on the same physical device has already collected points for this date
        val proceedWithCollection = {
            val usageRef = db.collection("daily_usage").document(record.id)
            usageRef.get().addOnSuccessListener { snapshot ->
                val isCollected = snapshot.getBoolean("isCollected") ?: false
                val isApproved  = snapshot.getBoolean("isApproved") ?: false
                val approvedAt  = snapshot.getLong("approvedAt") ?: 0L
                // Server-side expiry guard: deny collection after 7 days
                val isExpired = approvedAt > 0L &&
                    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - approvedAt) >= 7

                if (isCollected || !isApproved || isExpired) {
                    _collectingRecordIds.value = _collectingRecordIds.value - record.id
                    onComplete(false)
                    return@addOnSuccessListener
                }
                
                val activity = context as? Activity
                if (activity == null) {
                    _collectingRecordIds.value = _collectingRecordIds.value - record.id
                    onComplete(false)
                    return@addOnSuccessListener
                }
                
                RewardedAdManager.showAdWithoutPoints(activity) { success ->
                    if (success) {
                        val pointsToCollect = record.pointsPotential
                        val userRef = db.collection("users").document(userId)
                        
                        db.runTransaction { transaction ->
                            val userSnapshot = transaction.get(userRef)
                            val currentPoints = userSnapshot.getLong("points") ?: 0L
                            
                            transaction.update(userRef, "points", currentPoints + pointsToCollect)
                            transaction.update(usageRef, "isCollected", true)
                        }.addOnSuccessListener {
                            _collectingRecordIds.value = _collectingRecordIds.value - record.id
                            onComplete(true)
                        }.addOnFailureListener {
                            _collectingRecordIds.value = _collectingRecordIds.value - record.id
                            onComplete(false)
                        }
                    } else {
                        _collectingRecordIds.value = _collectingRecordIds.value - record.id
                        onComplete(false)
                    }
                }
            }.addOnFailureListener {
                _collectingRecordIds.value = _collectingRecordIds.value - record.id
                onComplete(false)
            }
        }

        if (deviceId.isNotEmpty()) {
            db.collection("daily_usage")
                .whereEqualTo("date", record.date)
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("isCollected", true)
                .get()
                .addOnSuccessListener { query ->
                    val alreadyClaimedByOther = query.documents.any { doc ->
                        doc.getString("userId") != userId
                    }
                    if (alreadyClaimedByOther) {
                        _collectingRecordIds.value = _collectingRecordIds.value - record.id
                        Toast.makeText(context, "Daily usage points for this date have already been claimed on this device.", Toast.LENGTH_LONG).show()
                        onComplete(false)
                    } else {
                        proceedWithCollection()
                    }
                }
                .addOnFailureListener {
                    proceedWithCollection()
                }
        } else {
            proceedWithCollection()
        }
    }

    fun getTodayCheckInDay(): Int {
        val todayStr = LocalDate.now().toString()
        val lastDateStr = _lastCheckInDate.value ?: return 1
        val currentStreak = _checkInStreak.value
        
        if (lastDateStr == todayStr) {
            return if (currentStreak <= 0) 1 else currentStreak
        }
        
        val yesterdayStr = LocalDate.now().minusDays(1).toString()
        return if (lastDateStr == yesterdayStr) {
            if (currentStreak >= 7) 1 else currentStreak + 1
        } else {
            1
        }
    }

    fun hasCheckedInToday(): Boolean {
        return _lastCheckInDate.value == LocalDate.now().toString()
    }

    fun getPointsForDay(day: Int): Int {
        return when (day) {
            1 -> 10
            2 -> 20
            3 -> 30
            4 -> 40
            5 -> 50
            6 -> 60
            7 -> 100
            else -> 10
        }
    }

    fun claimDailyCheckIn(activity: Activity, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        if (hasCheckedInToday() || _isClaimingCheckIn.value) {
            onComplete(false)
            return
        }

        _isClaimingCheckIn.value = true
        val targetDay = getTodayCheckInDay()
        val basePoints = getPointsForDay(targetDay)
        
        RewardedAdManager.showAdWithoutPoints(activity) { adSuccess ->
            if (adSuccess) {
                claimDirectlyTransaction(userId, targetDay, basePoints) { success ->
                    _isClaimingCheckIn.value = false
                    onComplete(success)
                }
            } else {
                _isClaimingCheckIn.value = false
                onComplete(false)
            }
        }
    }

    private fun claimDirectlyTransaction(
        userId: String,
        targetDay: Int,
        basePoints: Int,
        onComplete: (Boolean) -> Unit
    ) {
        val userRef = db.collection("users").document(userId)
        val todayStr = LocalDate.now().toString()
        
        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val currentPoints = userSnapshot.getLong("points") ?: 0L
            
            transaction.update(userRef, mapOf(
                "points" to currentPoints + basePoints,
                "lastCheckInDate" to todayStr,
                "checkInStreak" to targetDay
            ))
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener { e ->
            Log.e("PointsViewModel", "Failed to claim check-in transaction: ${e.message}")
            onComplete(false)
        }
    }

    fun claim2xCheckInWithAd(activity: Activity, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        if (hasCheckedInToday() || _isClaimingCheckIn.value) {
            onComplete(false)
            return
        }

        _isClaimingCheckIn.value = true
        val targetDay = getTodayCheckInDay()
        val basePoints = getPointsForDay(targetDay)
        val doublePoints = basePoints * 2

        RewardedAdManager.showAdWithoutPoints(activity) { adSuccess ->
            if (adSuccess) {
                claimDirectlyTransaction(userId, targetDay, doublePoints) { success ->
                    _isClaimingCheckIn.value = false
                    onComplete(success)
                }
            } else {
                _isClaimingCheckIn.value = false
                onComplete(false)
            }
        }
    }

    fun incrementAdQuestStep(activity: Activity, onComplete: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val todayStr = LocalDate.now().toString()
        val currentCount = if (_lastAdQuestDate.value == todayStr) _dailyAdQuestCount.value else 0

        if (currentCount >= 5) {
            onComplete(false, "You've already completed today's 5-Ad Quest! Check back tomorrow.")
            return
        }

        RewardedAdManager.showAdWithoutPoints(activity) { adSuccess ->
            if (adSuccess) {
                val newCount = currentCount + 1
                val bonusPoints = if (newCount == 5) 50 else 0 // +50 bonus pts on 5th ad

                val updates = mutableMapOf<String, Any>(
                    "dailyAdQuestCount" to newCount,
                    "lastAdQuestDate" to todayStr
                )

                if (bonusPoints > 0) {
                    updates["points"] = com.google.firebase.firestore.FieldValue.increment(bonusPoints.toLong())
                }

                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        val msg = if (newCount == 5) "🎉 Quest Complete! You earned +50 Bonus Points!" else "Ad watched! Progress: $newCount/5"
                        onComplete(true, msg)
                    }
                    .addOnFailureListener {
                        onComplete(false, "Failed to save quest progress.")
                    }
            } else {
                onComplete(false, "Ad was not completed or failed to load.")
            }
        }
    }

    fun activateVipWithPoints(months: Int, costPoints: Int, context: android.content.Context) {
        val userId = auth.currentUser?.uid ?: return
        if (_userPoints.value < costPoints) {
            Toast.makeText(context, "Insufficient points for ReelVIP Pro", Toast.LENGTH_SHORT).show()
            return
        }

        val currentExp = if (_vipExpiresAt.value > System.currentTimeMillis()) _vipExpiresAt.value else System.currentTimeMillis()
        val durationMillis = months * 30L * 24 * 3600 * 1000
        val newExp = currentExp + durationMillis

        val userRef = db.collection("users").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val points = snapshot.getLong("points") ?: 0L
            if (points < costPoints) {
                throw IllegalStateException("Insufficient points")
            }
            transaction.update(userRef, mapOf(
                "points" to points - costPoints,
                "vipExpiresAt" to newExp
            ))
        }.addOnSuccessListener {
            Toast.makeText(context, "👑 Welcome to ReelVIP Pro! Perks activated.", Toast.LENGTH_LONG).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Upgrade failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun activateVipWithCash(plan: String, context: android.content.Context) {
        val userId = auth.currentUser?.uid ?: return
        val months = if (plan == "yearly") 12 else 1
        val currentExp = if (_vipExpiresAt.value > System.currentTimeMillis()) _vipExpiresAt.value else System.currentTimeMillis()
        val durationMillis = months * 30L * 24 * 3600 * 1000
        val newExp = currentExp + durationMillis

        db.collection("users").document(userId)
            .set(mapOf("vipExpiresAt" to newExp), SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "👑 ReelVIP Pro Pass activated successfully!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to activate VIP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun claimLuckySpin(wonPoints: Int, onComplete: ((Boolean) -> Unit)? = null) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete?.invoke(false)
            return
        }
        val todayStr = LocalDate.now().toString()
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentLastSpin = snapshot.getString("lastSpinDate")
            if (currentLastSpin == todayStr) {
                throw IllegalStateException("Already spun today")
            }
            val currentPoints = snapshot.getLong("points") ?: 0L
            transaction.update(userRef, mapOf(
                "points" to currentPoints + wonPoints,
                "lastSpinDate" to todayStr
            ))
        }.addOnSuccessListener {
            _pointCreditEvent.value = wonPoints
            _lastSpinDate.value = todayStr
            onComplete?.invoke(true)
        }.addOnFailureListener {
            onComplete?.invoke(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pointsListener?.remove()
        historyListener?.remove()
    }
}

data class WithdrawalRequest(
    val id: String = "",
    val userId: String = "",
    val amountRs: Int = 0,
    val rewardName: String = "",
    val pointsDeducted: Int = 0,
    val paymentDetails: String? = null,
    val status: String = "Pending",
    val createdAt: Long = 0,
    val giftCardCode: String? = null,
    val processedAt: Long? = null
) {
    @get:Exclude
    val isUpi: Boolean
        get() = rewardName.contains("UPI", ignoreCase = true) ||
                (paymentDetails?.contains("UPI", ignoreCase = true) == true) ||
                rewardName.contains("₹")

    @get:Exclude
    val formattedAmount: String
        get() = if (isUpi) "₹$amountRs" else "$$amountRs"
}

data class DailyUsageRecord(
    @get:Exclude
    val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val date: String = "",
    val totalMillis: Long = 0,
    val pointsPotential: Int = 0,
    val pointsCredited: Int = 0,
    val pointsCollected: Int = 0,
    val tier1Collected: Boolean = false, // 2h - 40 pts
    val tier2Collected: Boolean = false, // 3h - 60 pts
    val tier3Collected: Boolean = false, // 4h - 100 pts
    @get:PropertyName("isCollected")
    @set:PropertyName("isCollected")
    var isCollected: Boolean = false,
    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = true,
    val approvedAt: Long = 0L,
    val collectedAt: Long = 0L,
    val lastCollectedAt: Long = 0L
)


