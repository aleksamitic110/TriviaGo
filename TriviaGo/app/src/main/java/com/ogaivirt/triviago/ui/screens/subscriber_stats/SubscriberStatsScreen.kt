package com.ogaivirt.triviago.ui.screens.subscriber_stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ogaivirt.triviago.domain.model.QuestionStatistic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberStatsScreen(
    viewModel: SubscriberStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Moja statistika za kviz") }) }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TODO: Prikazati i tekst pitanja, a ne samo ID
            items(uiState.statistics, key = { it.questionId }) { stat ->
                StatListItem(statistic = stat)
            }
        }
    }
}

@Composable
private fun StatListItem(statistic: QuestionStatistic) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text("Pitanje ID: ${statistic.questionId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))


            Text(
                "Faktor lakoće (EF): %.2f".format(statistic.easinessFactor),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Broj tačnih ponavljanja: ${statistic.repetitions}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                "Sledeće ponavljanje za: ${statistic.interval} dana",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}