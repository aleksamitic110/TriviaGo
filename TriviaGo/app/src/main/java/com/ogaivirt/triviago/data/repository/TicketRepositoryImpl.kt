package com.ogaivirt.triviago.data.repository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ogaivirt.triviago.domain.model.Ticket
import com.ogaivirt.triviago.domain.model.TicketAssignee
import com.ogaivirt.triviago.domain.model.TicketStatus
import com.ogaivirt.triviago.domain.repository.TicketRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TicketRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : TicketRepository {

    override suspend fun getOpenAdminTickets(): Result<List<Ticket>> {
        return try {
            val snapshot = db.collection("tickets")
                .whereEqualTo("assignedTo", TicketAssignee.ADMIN)
                .whereEqualTo("status", TicketStatus.OTVOREN)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val tickets = snapshot.toObjects(Ticket::class.java)
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeTicket(ticketId: String): Result<Unit> {
        return try {
            db.collection("tickets").document(ticketId)
                .update("status", TicketStatus.ZATVOREN)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}