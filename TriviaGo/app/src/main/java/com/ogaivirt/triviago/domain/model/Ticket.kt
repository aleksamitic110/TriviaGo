package com.ogaivirt.triviago.domain.model

enum class TicketAssignee {
    ADMIN,
    DEVELOPER
}

enum class TicketStatus {
    OTVOREN,
    ZATVOREN
}

data class Ticket(
    val ticketId: String = "",
    val reportId: String = "",
    val quizId: String = "",
    val createdBySupportUid: String = "",
    val assignedTo: TicketAssignee = TicketAssignee.ADMIN,
    val details: String = "",
    val reportedByUid: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: TicketStatus = TicketStatus.OTVOREN
)