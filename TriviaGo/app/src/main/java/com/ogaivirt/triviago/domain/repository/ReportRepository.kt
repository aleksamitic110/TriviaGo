package com.ogaivirt.triviago.domain.repository

import com.ogaivirt.triviago.domain.model.Report
import com.ogaivirt.triviago.domain.model.ReportStatus
import com.ogaivirt.triviago.domain.model.TicketAssignee

interface ReportRepository {
    suspend fun getAllReports(): Result<List<Report>>
    suspend fun getOpenSupportReports(): Result<List<Report>>
    suspend fun updateReportStatus(reportId: String, newStatus: ReportStatus): Result<Unit>
    suspend fun deleteReport(reportId: String): Result<Unit>
    suspend fun createTicketFromReport(report: Report, assignedTo: TicketAssignee): Result<Unit>
    suspend fun createBugReport(bugDescription: String): Result<Unit>
}