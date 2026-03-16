package com.ogaivirt.triviago.domain.model

enum class ReportStatus { NOVO, U_OBRADI, REŠENO }

data class Report (
    val reportId: String = "",
    val reportedByUid: String = "",
    val quizId: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: ReportStatus = ReportStatus.NOVO
)