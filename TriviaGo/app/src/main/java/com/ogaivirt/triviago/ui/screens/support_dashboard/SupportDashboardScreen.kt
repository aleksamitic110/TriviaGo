package com.ogaivirt.triviago.ui.screens.support_dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ogaivirt.triviago.domain.model.Report
import com.ogaivirt.triviago.domain.model.VerificationRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SupportDashboardScreen(
    viewModel: SupportDashboardViewModel = hiltViewModel(),
    onNavigateToQuizDetail: (quizId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Prijave", "Verifikacije")

    // State for rejection dialog
    var showRejectDialog by remember { mutableStateOf<VerificationRequest?>(null) }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onInfoMessageShown()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onErrorMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kontrolna tabla podrške") },

                navigationIcon = {

                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Tab Row
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(text = title) }
                    )
                }
            }


            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    page == 0 -> {
                        if (uiState.reports.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Nema otvorenih prijava.", textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.reports, key = { it.reportId }) { report ->
                                    ReportListItem(
                                        report = report,
                                        viewModel = viewModel,
                                        onItemClick = { onNavigateToQuizDetail(report.quizId) }
                                    )
                                }
                            }
                        }
                    }

                    page == 1 -> {
                        if (uiState.verificationRequests.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Nema zahteva za verifikaciju.", textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.verificationRequests, key = { it.id }) { request ->
                                    VerificationRequestItem(
                                        request = request,
                                        onApproveClick = { viewModel.onApproveVerificationRequest(request.id) },
                                        onRejectClick = { showRejectDialog = request }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    if (showRejectDialog != null) {
        val requestToReject = showRejectDialog!!
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Odbij Zahtev") },
            text = {
                Column {
                    Text("Unesite razlog odbijanja za korisnika ${requestToReject.username}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Razlog") },
                        isError = reason.isBlank()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onRejectVerificationRequest(requestToReject.id, requestToReject.userId, reason)
                        showRejectDialog = null
                    },
                    enabled = reason.isNotBlank()
                ) {
                    Text("Odbij")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = null }) {
                    Text("Otkaži")
                }
            }
        )
    }
}



@Composable
private fun ReportListItem(
    report: Report,
    viewModel: SupportDashboardViewModel,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Prijava za kviz: ${report.quizId.takeIf { it.isNotBlank() } ?: "N/A (Bug)"}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Razlog: ${report.reason}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Prijavio: ${report.reportedByUid}", style = MaterialTheme.typography.bodySmall)
            Text("Status: ${report.status.name}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.onEscalateToAdmin(report) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adminu")
                }
                OutlinedButton(
                    onClick = { viewModel.onEscalateToDev(report, context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Developeru")
                }
                Button(
                    onClick = { viewModel.onResolveClick(report.reportId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reši")
                }
            }
        }
    }
}

@Composable
fun VerificationRequestItem(
    request: VerificationRequest,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Korisnik: ${request.username} (${request.userId})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Podneto: ${formatTimestamp(request.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))

            AsyncImage(
                model = request.imageUrl,
                contentDescription = "Slika za verifikaciju",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { /* TODO: Open image fullscreen? */ },
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
                ) {
                    Text("Odbij")
                }
                Button(
                    onClick = onApproveClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Odobri (za Admina)")
                }
            }
        }
    }
}


private fun formatTimestamp(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(date)
}