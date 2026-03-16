package com.ogaivirt.triviago.ui.screens.admin_dashboard

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
import com.ogaivirt.triviago.domain.model.Ticket
import com.ogaivirt.triviago.domain.model.VerificationRequest
import com.ogaivirt.triviago.domain.model.VerificationStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    onNavigateToQuizDetail: (quizId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Tiketi", "Verifikacije")


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
        topBar = { TopAppBar(title = { Text("Admin Panel") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

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
                        if (uiState.tickets.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Nema otvorenih tiketa.", textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.tickets, key = { it.ticketId }) { ticket ->
                                    TicketListItem(
                                        ticket = ticket,
                                        onItemClick = { onNavigateToQuizDetail(ticket.quizId) },
                                        onDisableCreatorClick = { viewModel.onDisableCreatorClick(ticket.quizId, ticket.ticketId) },
                                        onResolveTicketClick = { viewModel.onResolveTicketClick(ticket.ticketId) }
                                    )
                                }
                            }
                        }
                    }

                    page == 1 -> {
                        if (uiState.approvedVerificationRequests.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Nema zahteva odobrenih od podrške.", textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.approvedVerificationRequests, key = { it.id }) { request ->
                                    AdminVerificationRequestItem(
                                        request = request,
                                        onFinalVerifyClick = { viewModel.onFinalVerifyClick(request.userId, request.id, request.username) },
                                        onFinalRejectClick = { showRejectDialog = request } // Open dialog
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection Dialog for Admin
    if (showRejectDialog != null) {
        val requestToReject = showRejectDialog!!
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Odbij Zahtev (Finalna Odluka)") },
            text = {
                Column {
                    Text("Unesite razlog odbijanja za korisnika ${requestToReject.username}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Razlog") },
                        placeholder = { Text("Zašto je zahtev neprihvatljiv?") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onFinalRejectClick(requestToReject.id, requestToReject.userId, requestToReject.username, reason)
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
private fun TicketListItem(
    ticket: Ticket,
    onItemClick: () -> Unit,
    onDisableCreatorClick: () -> Unit,
    onResolveTicketClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tiket za kviz: ${ticket.quizId}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Problem: ${ticket.details}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Kreirao (podrška): ${ticket.createdBySupportUid}", style = MaterialTheme.typography.bodySmall)
            if (ticket.reportedByUid.isNotBlank()) {
                Text("Prijavio (korisnik): ${ticket.reportedByUid}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Status: ${ticket.status.name}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDisableCreatorClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),

                    ) {
                    Text("Onemogući kreiranje")
                }
                Button(
                    onClick = onResolveTicketClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reši tiket")
                }
            }
        }
    }
}


@Composable
fun AdminVerificationRequestItem(
    request: VerificationRequest,
    onFinalVerifyClick: () -> Unit,
    onFinalRejectClick: () -> Unit
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
                "Status: ${request.status.name}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Green
            )
            Spacer(modifier = Modifier.height(12.dp))

            AsyncImage(
                model = request.imageUrl,
                contentDescription = "Slika za verifikaciju",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onFinalRejectClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
                ) {
                    Text("Odbij")
                }
                Button(
                    onClick = onFinalVerifyClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verifikuj Korisnika")
                }
            }
        }
    }
}