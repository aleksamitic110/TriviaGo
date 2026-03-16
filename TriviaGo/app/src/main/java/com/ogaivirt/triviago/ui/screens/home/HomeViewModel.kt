package com.ogaivirt.triviago.ui.screens.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.ogaivirt.triviago.R
import com.ogaivirt.triviago.data.location.LocationClient
import com.ogaivirt.triviago.data.location.PlacesApiService
import com.ogaivirt.triviago.domain.model.Question
import com.ogaivirt.triviago.domain.model.QuestionStatistic
import com.ogaivirt.triviago.domain.model.UserProfile
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.QuizRepository
import com.ogaivirt.triviago.domain.repository.ReportRepository
import com.ogaivirt.triviago.domain.repository.SettingsRepository
import com.ogaivirt.triviago.domain.repository.StatisticsRepository
import com.ogaivirt.triviago.domain.repository.VerificationRepository
import com.ogaivirt.triviago.util.calculateDistanceInMeters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

data class MapMarker(
    val id: String = UUID.randomUUID().toString(),
    val position: LatLng,
    val isLocked: Boolean = true
)

data class MapState(
    val lastKnownLocation: LatLng? = null,
    val isGpsEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val markerPositions: List<LatLng> = emptyList(),
    val mapMarkers: List<MapMarker> = emptyList(),
    val infoMessage: String? = null,
    val profilePictureUrl: String? = null,
    val userProfile: UserProfile? = null,
    val isDrawerVisible: Boolean = false,
    val showBugReportDialog: Boolean = false,
    val isVerificationLoading: Boolean = false
)


sealed class HomeViewEvent {
    object LaunchImagePicker : HomeViewEvent()
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationClient: LocationClient,
    private val quizRepo: QuizRepository,
    private val authRepo: AuthRepository,
    private val statsRepo: StatisticsRepository,
    private val placesApi: PlacesApiService,
    private val settingsRepo: SettingsRepository,
    private val reportRepo: ReportRepository,
    private val verificationRepo: VerificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val mapState = _mapState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()


    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            authRepo.getUserProfile().onSuccess { userProfile ->
                _mapState.update {
                    it.copy(
                        profilePictureUrl = userProfile?.profilePictureUrl,
                        userProfile = userProfile
                    )
                }
            }
        }
    }


    fun openDrawer() {
        _mapState.update { it.copy(isDrawerVisible = true) }
    }

    fun closeDrawer() {
        _mapState.update { it.copy(isDrawerVisible = false) }
    }

    fun openBugReportDialog() {
        _mapState.update { it.copy(showBugReportDialog = true, isDrawerVisible = false) }
    }

    fun closeBugReportDialog() {
        _mapState.update { it.copy(showBugReportDialog = false) }
    }

    fun onReportBugSubmit(description: String) {
        if (description.isBlank()) {
            _mapState.update { it.copy(infoMessage = "Opis greške ne sme biti prazan.") }
            return
        }
        viewModelScope.launch {
            _mapState.update { it.copy(isLoading = true) }
            reportRepo.createBugReport(description).onSuccess {
                _mapState.update { it.copy(isLoading = false, infoMessage = "Greška uspešno prijavljena. Hvala!", showBugReportDialog = false) }
            }.onFailure {
                _mapState.update { it.copy(isLoading = false, infoMessage = "Došlo je do greške pri slanju prijave.", showBugReportDialog = false) }
            }
        }
    }

    fun onGpsDisabled() {
        _mapState.update { it.copy(isGpsEnabled = false) }
    }

    fun onVerifyAccountClick() {
        _mapState.update { it.copy(isDrawerVisible = false) }
        viewModelScope.launch {
            _eventFlow.emit(HomeViewEvent.LaunchImagePicker)
        }
    }


    fun onVerificationImageSelected(imageUri: Uri?) {
        if (imageUri == null) {
            _mapState.update { it.copy(infoMessage = "Odabir slike otkazan.") }
            return
        }
        viewModelScope.launch {
            _mapState.update { it.copy(isVerificationLoading = true) }
            verificationRepo.requestVerification(imageUri).onSuccess {
                _mapState.update { it.copy(isVerificationLoading = false, infoMessage = "Zahtev za verifikaciju uspešno poslat.") }

                //loadUserProfile()
            }.onFailure { exception ->
                _mapState.update { it.copy(isVerificationLoading = false, infoMessage = "Greška: ${exception.localizedMessage}") }
            }
        }
    }


    private var weightedQuestionBag: MutableList<Question> = mutableListOf()
    private var isDataLoadedForSession = false
    private val UNLOCK_DISTANCE_METERS = 30
    var allQuestions = mutableListOf<Question>()

    private fun loadDataForSession(userLocation: LatLng) {
        if (isDataLoadedForSession) return

        viewModelScope.launch {
            _mapState.update { it.copy(isLoading = true) }

            val userSettings = settingsRepo.getUserSettings().first()
            val radiusInMeters = userSettings.searchRadius
            val desiredMarkerCount = userSettings.markerDensity
            val activeQuizIds = authRepo.getUserProfile().getOrNull()?.activeQuizIds ?: emptyList()

            if (activeQuizIds.isEmpty()) {
                _mapState.update { it.copy(isLoading = false, markerPositions = emptyList(), mapMarkers = emptyList()) }
                isDataLoadedForSession = true
                return@launch
            }

            val allStats = mutableListOf<QuestionStatistic>()
            val questionsFromDb = mutableListOf<Question>()
            activeQuizIds.forEach { quizId ->
                quizRepo.getQuizById(quizId).onSuccess { quiz ->
                    quiz?.let { questionsFromDb.addAll(it.questions) }
                }
                statsRepo.getStatisticsForQuiz(quizId).onSuccess { stats ->
                    allStats.addAll(stats)
                }
            }

            allQuestions = questionsFromDb

            if (allQuestions.isEmpty()) {
                _mapState.update { it.copy(isLoading = false, markerPositions = emptyList(), mapMarkers = emptyList()) }
                isDataLoadedForSession = true
                return@launch
            }

            val statsMap = allStats.associateBy { it.questionId }
            weightedQuestionBag.clear()
            allQuestions.forEach { question ->
                val stats = statsMap[question.id]
                val easinessFactor = stats?.easinessFactor ?: 2.5f
                val weight = (10 / easinessFactor).roundToInt().coerceIn(1, 20)
                repeat(weight) { weightedQuestionBag.add(question) }
            }

            try {
                val apiKey = context.getString(R.string.google_backend_api_key)
                val placesResponse = placesApi.nearbyPlaces(
                    location = "${userLocation.latitude},${userLocation.longitude}",
                    radius = radiusInMeters,
                    apiKey = apiKey
                )

                val placePositions = placesResponse.results.map {
                    LatLng(it.geometry.location.lat, it.geometry.location.lng)
                }

                val finalMarkerPositions = if (placePositions.size <= desiredMarkerCount) {
                    placePositions
                } else {
                    placePositions.shuffled().take(desiredMarkerCount)
                }

                val newMarkers = finalMarkerPositions.map { position ->
                    MapMarker(
                        position = position,
                        isLocked = calculateDistanceInMeters(userLocation, position) > UNLOCK_DISTANCE_METERS
                    )
                }

                _mapState.update { it.copy(isLoading = false, mapMarkers = newMarkers) }
                isDataLoadedForSession = true
            } catch (e: Exception) {
                e.printStackTrace()
                _mapState.update { it.copy(isLoading = false, markerPositions = emptyList(), mapMarkers = emptyList(), infoMessage = "Greška pri dobavljanju mesta.") }
                isDataLoadedForSession = true
            }
        }
    }

    fun onMarkerClick(): Question? {
        if (weightedQuestionBag.isEmpty()) {
            _mapState.value.lastKnownLocation?.let {
                isDataLoadedForSession = false
                loadDataForSession(it)
            }
            return null
        }
        return weightedQuestionBag.removeAt(Random.nextInt(weightedQuestionBag.size))
    }

    fun startLocationUpdates() {
        viewModelScope.launch {
            try {

                val userProfile = authRepo.getUserProfile().getOrNull()
                val activeQuizIds = userProfile?.activeQuizIds ?: emptyList()


                if (activeQuizIds.isEmpty()) {
                    Log.i("HomeViewModel", "Nema aktivnih kvizova – preskačem učitavanje mape.")
                    _mapState.update { it.copy(isLoading = false, mapMarkers = emptyList()) }
                    isDataLoadedForSession = true
                    return@launch
                }


                val initialLocation = locationClient.getCurrentLocation()
                if (initialLocation != null) {
                    val latLng = LatLng(initialLocation.latitude, initialLocation.longitude)
                    _mapState.update { it.copy(lastKnownLocation = latLng, isGpsEnabled = true) }
                    if (!isDataLoadedForSession) {
                        loadDataForSession(latLng)
                    }
                } else {
                    Log.w("HomeViewModel", "Initial location is null, waiting for updates...")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting current location", e)
                _mapState.update { it.copy(isLoading = false, infoMessage = "Greška pri dobavljanju lokacije.") }
            }
        }

        locationClient.getLocationUpdates(15000L)
            .catch { e ->
                if (e is LocationClient.LocationException) {
                    Log.w("HomeViewModel", "LocationException: GPS likely disabled")
                } else {
                    _mapState.update { it.copy(infoMessage = "Greška praćenja lokacije.") }
                }
            }
            .onEach { location ->
                val newLatLng = LatLng(location.latitude, location.longitude)
                if (!isDataLoadedForSession) {
                    loadDataForSession(newLatLng)
                    _mapState.update { it.copy(lastKnownLocation = newLatLng) }
                } else {
                    val currentState = _mapState.value
                    val oldLatLng = currentState.lastKnownLocation
                    val updatedMarkers = currentState.mapMarkers.map { marker ->
                        val distance = calculateDistanceInMeters(newLatLng, marker.position)
                        if (marker.isLocked == (distance > UNLOCK_DISTANCE_METERS)) {
                            marker
                        } else {
                            marker.copy(isLocked = distance > UNLOCK_DISTANCE_METERS)
                        }
                    }
                    val userHasMoved = oldLatLng == null || calculateDistanceInMeters(oldLatLng, newLatLng) > 5
                    if (updatedMarkers != currentState.mapMarkers || userHasMoved) {
                        _mapState.update {
                            it.copy(
                                lastKnownLocation = newLatLng,
                                isGpsEnabled = true,
                                mapMarkers = updatedMarkers
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onInfoMessageShown() {
        _mapState.update { it.copy(infoMessage = null) }
    }

    fun resetAndReloadData() {
        isDataLoadedForSession = false
        _mapState.update { it.copy(mapMarkers = emptyList(), isLoading = true) }
        _mapState.value.lastKnownLocation?.let {
            viewModelScope.launch { loadDataForSession(it) }
        } ?: startLocationUpdates()
    }

    fun forceGpsEnabled() {
        _mapState.update { it.copy(isGpsEnabled = true, isLoading = true) }
    }

    fun updateQuestionBag(updatedStat: QuestionStatistic) {
        weightedQuestionBag.removeAll { it.id == updatedStat.questionId }
        val question = allQuestions.find { it.id == updatedStat.questionId } ?: return
        val newWeight = (10 / updatedStat.easinessFactor).roundToInt().coerceIn(1, 20)
        repeat(newWeight) { weightedQuestionBag.add(question) }
        println("Vreća ažurirana za pitanje ${updatedStat.questionId}, nova težina: $newWeight")
    }
}