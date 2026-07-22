package com.timebill.stopwatch.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.timebill.stopwatch.model.Client
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.model.UserProfile
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

    fun getSession(sessionId: String): Flow<Session?> = callbackFlow {
        val ref = database.child("sessions").child(sessionId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Session::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateSessionStatus(sessionId: String, status: String, receiptNumber: String) {
        val updates = mapOf(
            "status" to status,
            "receiptNumber" to receiptNumber
        )
        database.child("sessions").child(sessionId).updateChildren(updates).await()
    }

    suspend fun updateSessionDetails(sessionId: String, updates: Map<String, Any?>) {
        database.child("sessions").child(sessionId).updateChildren(updates).await()
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

    suspend fun saveProfile(profile: UserProfile) {
        FirebaseDatabase.getInstance().reference
            .child("profiles")
            .child(guestId)
            .setValue(profile)
            .await()
    }

    fun getProfile(): Flow<UserProfile?> = callbackFlow {
        val ref = FirebaseDatabase.getInstance().reference
            .child("profiles")
            .child(guestId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                trySend(profile)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Clients
    suspend fun saveClient(client: Client) {
        val clientId = client.clientId ?: database.child("clients").push().key ?: return
        val ref = database.child("clients").child(clientId)
        
        if (client.clientId == null) {
            // New client
            val clientWithId = client.copy(clientId = clientId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            ref.setValue(clientWithId).await()
        } else {
            // Update client - only update changed fields to preserve createdAt
            val updates = mutableMapOf<String, Any?>()
            updates["clientName"] = client.clientName
            updates["mobile"] = client.mobile
            updates["email"] = client.email
            updates["address"] = client.address
            updates["updatedAt"] = System.currentTimeMillis()
            ref.updateChildren(updates).await()
        }
    }

    fun getClients(): Flow<List<Client>> = callbackFlow {
        val ref = database.child("clients")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val clients = snapshot.children.mapNotNull { it.getValue(Client::class.java) }
                    .sortedByDescending { it.createdAt }
                trySend(clients)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun deleteClient(clientId: String) {
        database.child("clients").child(clientId).removeValue().await()
    }
}