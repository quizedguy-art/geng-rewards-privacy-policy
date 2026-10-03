package com.quizedguy.reelnearn.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pendingWithdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val pendingWithdrawals = _pendingWithdrawals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _withdrawalHistory = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawalHistory = _withdrawalHistory.asStateFlow()

    private val _usageRecords = MutableStateFlow<List<DailyUsageRecord>>(emptyList())
    val usageRecords = _usageRecords.asStateFlow()

    private val _allTasks = MutableStateFlow<List<SponsoredTask>>(emptyList())
    val allTasks = _allTasks.asStateFlow()

    private val _pendingTaskCompletions = MutableStateFlow<List<TaskCompletion>>(emptyList())
    val pendingTaskCompletions = _pendingTaskCompletions.asStateFlow()

    private val _vipUsersCount = MutableStateFlow(0)
    val vipUsersCount = _vipUsersCount.asStateFlow()

    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val supportTickets = _supportTickets.asStateFlow()

    init {
        loadData()
        loadTasksAndCompletions()
        loadVipStats()
        loadSupportTickets()
    }

    private fun loadSupportTickets() {
        db.collection("support_tickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(SupportTicket::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _supportTickets.value = list
                }
            }
    }

    private fun loadVipStats() {
        val now = System.currentTimeMillis()
        db.collection("users")
            .whereGreaterThan("vipExpiresAt", now)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _vipUsersCount.value = snapshot.size()
                }
            }
    }

    private fun loadData() {
        _isLoading.value = true
        db.collection("withdrawals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(WithdrawalRequest::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _pendingWithdrawals.value = list.filter { it.status == "Pending" }
                    _withdrawalHistory.value = list.filter { it.status != "Pending" }
                }
            }

        db.collection("daily_usage")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(DailyUsageRecord::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _usageRecords.value = list
                }
            }
    }

    private fun loadTasksAndCompletions() {
        db.collection("sponsored_tasks")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(SponsoredTask::class.java)?.copy(id = doc.id)
                    }
                    _allTasks.value = list
                }
            }

        db.collection("task_completions")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TaskCompletion::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.submittedAt }
                    _pendingTaskCompletions.value = list
                }
            }
    }

    fun createSponsoredTask(
        title: String,
        description: String,
        category: String,
        points: Int,
        url: String,
        sponsorName: String,
        minutes: Int
    ) {
        val task = hashMapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "pointsReward" to points,
            "taskUrl" to url,
            "sponsorName" to if (sponsorName.isBlank()) "Reel n Earn Partner" else sponsorName,
            "estimatedMinutes" to minutes,
            "isActive" to true,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("sponsored_tasks").add(task)
    }

    fun toggleTaskActive(taskId: String, currentActive: Boolean) {
        db.collection("sponsored_tasks").document(taskId)
            .update("isActive", !currentActive)
    }

    fun approveTaskCompletion(completion: TaskCompletion) {
        db.collection("task_completions").document(completion.id)
            .update(mapOf(
                "status" to "Approved",
                "reviewedAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                db.collection("users").document(completion.userId)
                    .update("points", FieldValue.increment(completion.pointsReward.toLong()))
            }
    }

    fun rejectTaskCompletion(completionId: String) {
        db.collection("task_completions").document(completionId)
            .update(mapOf(
                "status" to "Rejected",
                "reviewedAt" to System.currentTimeMillis()
            ))
    }

    fun approveRequest(id: String, code: String) {
        db.collection("withdrawals").document(id)
            .update(mapOf(
                "status" to "Approved",
                "giftCardCode" to code,
                "processedAt" to System.currentTimeMillis()
            ))
    }

    fun rejectRequest(id: String) {
        val request = _pendingWithdrawals.value.find { it.id == id }
        if (request != null) {
            db.collection("withdrawals").document(id)
                .update(mapOf(
                    "status" to "Rejected",
                    "processedAt" to System.currentTimeMillis()
                )).addOnSuccessListener {
                    db.collection("users").document(request.userId)
                        .update("points", FieldValue.increment(request.pointsDeducted.toLong()))
                }
        } else {
            val docRef = db.collection("withdrawals").document(id)
            docRef.get().addOnSuccessListener { snapshot ->
                val fetchedRequest = snapshot.toObject(WithdrawalRequest::class.java)
                if (fetchedRequest != null) {
                    docRef.update(mapOf(
                        "status" to "Rejected",
                        "processedAt" to System.currentTimeMillis()
                    )).addOnSuccessListener {
                        db.collection("users").document(fetchedRequest.userId)
                            .update("points", FieldValue.increment(fetchedRequest.pointsDeducted.toLong()))
                    }
                }
            }
        }
    }

    fun creditUsagePoints(record: DailyUsageRecord, points: Int) {
        db.collection("daily_usage").document(record.id)
            .update(mapOf(
                "isApproved" to true,
                "isCollected" to false,
                "pointsPotential" to points,
                "approvedAt" to System.currentTimeMillis()
            ))
    }

    fun verifyUserEmailByAdmin(emailStr: String, onResult: (Boolean, String) -> Unit) {
        val trimmed = emailStr.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "Please enter an email address")
            return
        }
        db.collection("users")
            .whereEqualTo("email", trimmed)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onResult(false, "User '$trimmed' not found in database.")
                } else {
                    val batch = db.batch()
                    snapshot.documents.forEach { doc ->
                        batch.update(doc.reference, "emailVerified", true)
                    }
                    batch.commit().addOnSuccessListener {
                        onResult(true, "Successfully verified email for '$trimmed'!")
                    }.addOnFailureListener { e ->
                        onResult(false, "Failed to update Firestore: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(false, "Query error: ${e.message}")
            }
    }

    fun updateTicketStatus(ticketId: String, newStatus: String) {
        db.collection("support_tickets").document(ticketId)
            .update("status", newStatus)
    }

    fun deleteSupportTicket(ticketId: String) {
        db.collection("support_tickets").document(ticketId).delete()
    }

    fun replyToSupportTicket(ticket: SupportTicket, replyMessage: String, markResolved: Boolean = true) {
        val trimmed = replyMessage.trim()
        if (trimmed.isEmpty()) return

        val updateMap = mutableMapOf<String, Any>(
            "adminReply" to trimmed,
            "repliedAt" to System.currentTimeMillis()
        )
        if (markResolved) {
            updateMap["status"] = "Resolved"
        } else {
            updateMap["status"] = "Replied"
        }

        db.collection("support_tickets").document(ticket.id)
            .update(updateMap)

        // Dispatch branded email notification directly to the user via Trigger Email Firestore extension
        if (ticket.userEmail.isNotBlank()) {
            val emailDoc = hashMapOf(
                "to" to listOf(ticket.userEmail.trim()),
                "message" to hashMapOf(
                    "subject" to "Update on your Reel n Earn Support Ticket [${ticket.category}]",
                    "html" to """
                        <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 28px; border-radius: 16px; max-width: 600px; margin: 0 auto; border: 1px solid #1e293b;">
                            <div style="text-align: center; margin-bottom: 24px;">
                                <h1 style="color: #00F5D4; margin: 0; font-size: 24px; letter-spacing: 1px;">Reel n Earn</h1>
                                <p style="color: #94a3b8; font-size: 13px; margin: 4px 0 0 0;">Customer Support Response</p>
                            </div>
                            
                            <p style="font-size: 16px; margin-bottom: 16px;">Hi <b>${ticket.userName.ifBlank { "User" }}</b>,</p>
                            <p style="color: #cbd5e1; font-size: 14px; line-height: 1.6;">Our support team has reviewed your ticket regarding <b>${ticket.category}</b>.</p>
                            
                            <div style="background-color: #1e293b; padding: 18px; border-left: 4px solid #00F5D4; border-radius: 8px; margin: 20px 0;">
                                <p style="margin: 0; color: #94a3b8; font-size: 12px; font-weight: bold; text-transform: uppercase;">Your Message:</p>
                                <p style="margin: 6px 0 16px 0; color: #e2e8f0; font-style: italic; font-size: 14px;">"${ticket.message}"</p>
                                <hr style="border: none; border-top: 1px solid #334155; margin: 12px 0;" />
                                <p style="margin: 0; color: #00F5D4; font-size: 12px; font-weight: bold; text-transform: uppercase;">Support Team Reply:</p>
                                <p style="margin: 6px 0 0 0; color: #ffffff; font-size: 15px; line-height: 1.6; font-weight: 500;">${trimmed}</p>
                            </div>
                            
                            <div style="background-color: #111827; padding: 12px 16px; border-radius: 8px; margin: 20px 0; font-size: 12px; color: #64748b;">
                                <span>Ticket Status: <b style="color: ${if (markResolved) "#22c55e" else "#38bdf8"};">${if (markResolved) "Resolved" else "In Progress"}</b></span>
                            </div>
                            
                            <p style="color: #94a3b8; font-size: 13px; line-height: 1.5;">If you have any further questions, feel free to reply directly to this email (<a href="mailto:reelnearn@gmail.com" style="color: #00F5D4; text-decoration: none;">reelnearn@gmail.com</a>) or submit a new ticket in the app.</p>
                            <hr style="border: none; border-top: 1px solid #1e293b; margin: 24px 0 16px 0;" />
                            <p style="color: #64748b; font-size: 11px; text-align: center; margin: 0;">&copy; ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} Reel n Earn. All rights reserved.</p>
                        </div>
                    """.trimIndent()
                ),
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("mail").add(emailDoc)
        }
    }
}

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val category: String = "",
    val message: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val currentPoints: Long = 0L,
    val status: String = "Open",
    val createdAt: Long = 0L,
    val adminReply: String? = null,
    val repliedAt: Long? = null
)


