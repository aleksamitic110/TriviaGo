package com.ogaivirt.triviago.domain.repository

import com.ogaivirt.triviago.domain.model.Ticket

interface TicketRepository {
    suspend fun getOpenAdminTickets(): Result<List<Ticket>>
    suspend fun closeTicket(ticketId: String): Result<Unit>
}