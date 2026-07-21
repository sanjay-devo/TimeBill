package com.timebill.stopwatch.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.timebill.stopwatch.model.Session
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val guestId: String) {
    private val database = FirebaseDatabase.getInstance().reference.child("users").child(guestId)

    suspend fun saveSession(session: Session) {
        val sessionId = database.child("sessions").push().key ?: return
        val sessionWithId = session.copy(id = sessionId)
        database.child("sessions").child(sessionId).setValue(sessionWithId).await()
    }

    fun getSessions(): Flow<List<Session>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = snapshot.children.mapNotNull { it.getValue(Session::class.java) }
                    .sortedByDescending { it.timestamp }
                trySend(sessions)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.child("sessions").addValueEventListener(listener)
        awaitClose { database.child("sessions").removeEventListener(listener) }
    }

    suspend fun deleteSession(sessionId: String) {
        database.child("sessions").child(sessionId).removeValue().await()
    }

    suspend fun deleteAllSessions() {
        database.child("sessions").removeValue().await()
    }

    suspend fun deleteAccount() {
        database.removeValue().await()
    }

    suspend fun saveDefaultHourlyRate(rate: Double) {
        database.child("profile").child("defaultHourlyRate").setValue(rate).await()
    }

    fun getDefaultHourlyRate(): Flow<Double?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rate = snapshot.getValue(Double::class.java)
                trySend(rate)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = database.child("profile").child("defaultHourlyRate")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}