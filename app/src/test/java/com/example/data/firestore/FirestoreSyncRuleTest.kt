package com.example.data.firestore

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AgendaTask
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreSyncRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticated_cannotReadTasks() = runBlocking {
        auth.signOut()
        try {
            firestore.collection("users").document("user_123").collection("tasks").get().await()
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun unauthenticated_cannotWriteTasks() = runBlocking {
        auth.signOut()
        try {
            val task = mapOf(
                "id" to "t1",
                "userId" to "user_123",
                "date" to "2026-10-08",
                "title" to "Morning Workout",
                "status" to "pending"
            )
            firestore.collection("users").document("user_123").collection("tasks").document("t1")
                .set(task).await()
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun crossUser_cannotAccessOtherUserData() = runBlocking {
        val aliceUid = signInTestUser("alice@example.com")
        auth.signOut()
        val bobUid = signInTestUser("bob@example.com")

        // Bob tries to read Alice's tasks
        try {
            firestore.collection("users").document(aliceUid).collection("tasks").get().await()
            fail("Expected PERMISSION_DENIED for cross-user read")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }

        // Bob tries to write to Alice's tasks
        try {
            val task = mapOf(
                "id" to "t1",
                "userId" to aliceUid,
                "date" to "2026-10-08",
                "title" to "Malicious Task",
                "status" to "pending"
            )
            firestore.collection("users").document(aliceUid).collection("tasks").document("t1")
                .set(task).await()
            fail("Expected PERMISSION_DENIED for cross-user write")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun authenticatedOwner_canCreateAndReadTask() = runBlocking {
        val uid = signInTestUser("owner@example.com")

        val taskData = mapOf(
            "id" to "task_1",
            "userId" to uid,
            "date" to "2026-10-08",
            "timeSlot" to "09:00",
            "timeEnd" to "10:00",
            "title" to "Deep Work Session",
            "description" to "Coding lifestyle app",
            "category" to "Work",
            "status" to "pending",
            "priority" to "High",
            "orderIndex" to 0
        )

        val docRef = firestore.collection("users").document(uid).collection("tasks").document("task_1")
        docRef.set(taskData).await()

        val snapshot = docRef.get().await()
        assertTrue(snapshot.exists())
        assertEquals("Deep Work Session", snapshot.getString("title"))
        assertEquals("pending", snapshot.getString("status"))
    }

    @Test
    fun authenticatedOwner_canSaveDiaryAndProgress() = runBlocking {
        val uid = signInTestUser("diary_owner@example.com")

        val diaryData = mapOf(
            "id" to "diary_1",
            "userId" to uid,
            "date" to "2026-10-08",
            "time" to "21:00",
            "title" to "Daily Wins",
            "content" to "Accomplished all agenda priorities today.",
            "mood" to "Inspired",
            "energy" to 5
        )

        val diaryRef = firestore.collection("users").document(uid).collection("diary_entries").document("diary_1")
        diaryRef.set(diaryData).await()

        val diarySnapshot = diaryRef.get().await()
        assertTrue(diarySnapshot.exists())
        assertEquals("Daily Wins", diarySnapshot.getString("title"))

        // Progress document
        val progressData = mapOf(
            "id" to "progress_2026-10-08",
            "userId" to uid,
            "date" to "2026-10-08",
            "completedTasks" to 4,
            "totalTasks" to 5,
            "waterGlasses" to 8,
            "morningRoutineDone" to true,
            "completionRate" to 0.8
        )

        val progressRef = firestore.collection("users").document(uid).collection("progress").document("progress_2026-10-08")
        progressRef.set(progressData).await()

        val progressSnapshot = progressRef.get().await()
        assertTrue(progressSnapshot.exists())
        assertEquals(8L, progressSnapshot.getLong("waterGlasses"))
    }
}
