package com.quizedguy.reelnearn.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SponsoredTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Survey", // "Survey", "App Install", "Video", "Special"
    val pointsReward: Int = 100,
    val taskUrl: String = "",
    val sponsorName: String = "Reel n Earn Partner",
    val estimatedMinutes: Int = 5,
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)

data class TaskCompletion(
    val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val taskTitle: String = "",
    val pointsReward: Int = 0,
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    val submittedAt: Long = 0L,
    val reviewedAt: Long? = null
)

class SponsoredTaskViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _tasks = MutableStateFlow<List<SponsoredTask>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _userCompletions = MutableStateFlow<List<TaskCompletion>>(emptyList())
    val userCompletions = _userCompletions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var tasksListener: ListenerRegistration? = null
    private var completionsListener: ListenerRegistration? = null

    init {
        loadActiveTasks()
        loadUserCompletions()
    }

    fun loadActiveTasks() {
        _isLoading.value = true
        tasksListener?.remove()
        tasksListener = db.collection("sponsored_tasks")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(SponsoredTask::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.pointsReward }
                    _tasks.value = list
                }
            }
    }

    fun loadUserCompletions() {
        val userId = auth.currentUser?.uid ?: return
        completionsListener?.remove()
        completionsListener = db.collection("task_completions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TaskCompletion::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.submittedAt }
                    _userCompletions.value = list
                }
            }
    }

    fun submitTaskCompletion(task: SponsoredTask, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false, "User not logged in")
            return
        }

        // Check if user already submitted this task
        val existing = _userCompletions.value.find { it.taskId == task.id }
        if (existing != null) {
            if (existing.status == "Approved") {
                onResult(false, "You have already completed this task.")
                return
            } else if (existing.status == "Pending") {
                onResult(false, "Your submission for this task is pending review.")
                return
            }
        }

        val completion = hashMapOf(
            "taskId" to task.id,
            "userId" to userId,
            "taskTitle" to task.title,
            "pointsReward" to task.pointsReward,
            "status" to "Pending",
            "submittedAt" to System.currentTimeMillis()
        )

        db.collection("task_completions")
            .add(completion)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }

    override fun onCleared() {
        super.onCleared()
        tasksListener?.remove()
        completionsListener?.remove()
    }
}


