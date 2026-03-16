package com.ogaivirt.triviago.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ogaivirt.triviago.domain.model.Report
import com.ogaivirt.triviago.domain.model.ReportStatus
import com.ogaivirt.triviago.domain.model.Ticket
import com.ogaivirt.triviago.domain.model.TicketAssignee
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.ReportRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth


class ReportRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ReportRepository {

    override suspend fun getAllReports(): Result<List<Report>> {
        return try {
            val snapshot = db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val reports = snapshot.toObjects(Report::class.java)
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOpenSupportReports(): Result<List<Report>> {
        return try {
            val snapshot = db.collection("reports")
                .whereEqualTo("status", ReportStatus.NOVO)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val reports = snapshot.toObjects(Report::class.java)
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReportStatus(reportId: String, newStatus: ReportStatus): Result<Unit> {
        return try {
            db.collection("reports").document(reportId).update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            db.collection("reports").document(reportId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun createTicketFromReport(report: Report, assignedTo: TicketAssignee): Result<Unit> {
        return try {
            val newTicketRef = db.collection("tickets").document()
            val ticket = Ticket(
                ticketId = newTicketRef.id,
                reportId = report.reportId,
                quizId = report.quizId,
                createdBySupportUid = auth.currentUser?.uid ?: "",
                assignedTo = assignedTo,
                details = report.reason,
                reportedByUid = report.reportedByUid
            )

            db.runTransaction { transaction ->
                transaction.set(newTicketRef, ticket)
                transaction.update(db.collection("reports").document(report.reportId), "status", ReportStatus.REŠENO)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun createBugReport(bugDescription: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: "Nepoznat korisnik"
            val newReportRef = db.collection("reports").document()

            val report = Report(
                reportId = newReportRef.id,
                reportedByUid = userId,
                quizId = "",
                reason = "[BUG] $bugDescription",
                status = ReportStatus.NOVO
            )

            newReportRef.set(report).await()
            Log.d("ReportRepo", "Bug report kreiran: ${newReportRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ReportRepo", "Greška pri kreiranju bug report-a", e)
            Result.failure(e)
        }
    }
}