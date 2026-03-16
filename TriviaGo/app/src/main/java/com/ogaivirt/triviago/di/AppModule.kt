package com.ogaivirt.triviago.di

import com.google.firebase.auth.FirebaseAuth
import com.ogaivirt.triviago.data.repository.AuthRepositoryImpl
import com.ogaivirt.triviago.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ogaivirt.triviago.R
import com.ogaivirt.triviago.data.repository.QuizRepositoryImpl
import com.ogaivirt.triviago.domain.repository.QuizRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ogaivirt.triviago.data.location.DefaultLocationClient
import com.ogaivirt.triviago.data.location.LocationClient
import com.ogaivirt.triviago.data.location.PlacesApiService
import com.ogaivirt.triviago.data.repository.NotificationRepositoryImpl
import com.ogaivirt.triviago.data.repository.ReportRepositoryImpl
import com.ogaivirt.triviago.data.repository.SettingsRepositoryImpl
import com.ogaivirt.triviago.data.repository.StatisticsRepositoryImpl
import com.ogaivirt.triviago.data.repository.TicketRepositoryImpl
import com.ogaivirt.triviago.data.repository.VerificationRepositoryImpl // Import Impl
import com.ogaivirt.triviago.domain.repository.NotificationRepository
import com.ogaivirt.triviago.domain.repository.ReportRepository
import com.ogaivirt.triviago.domain.repository.SettingsRepository
import com.ogaivirt.triviago.domain.repository.StatisticsRepository
import com.ogaivirt.triviago.domain.repository.TicketRepository
import com.ogaivirt.triviago.domain.repository.VerificationRepository // Import Interface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository = impl

    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()


    @Provides
    @Singleton
    fun provideQuizRepository(
        db: FirebaseFirestore,
        auth: FirebaseAuth,
        authRepo: AuthRepository
    ): QuizRepository {
        return QuizRepositoryImpl(db, auth, authRepo)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideLocationClient(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationClient {
        return DefaultLocationClient(context, fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideStatisticsRepository(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ): StatisticsRepository {
        return StatisticsRepositoryImpl(db, auth)
    }

    @Provides
    @Singleton
    fun providePlacesApiService(): PlacesApiService {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideReportRepository(db: FirebaseFirestore, auth: FirebaseAuth): ReportRepository {
        return ReportRepositoryImpl(db, auth)
    }

    @Provides
    @Singleton
    fun provideTicketRepository(db: FirebaseFirestore): TicketRepository {
        return TicketRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(db: FirebaseFirestore, authRepo: AuthRepository): NotificationRepository {
        return NotificationRepositoryImpl(db, authRepo)
    }

    @Provides
    @Singleton
    fun provideVerificationRepository(
        db: FirebaseFirestore,
        storage: FirebaseStorage,
        authRepo: AuthRepository
    ): VerificationRepository {
        return VerificationRepositoryImpl(db, storage, authRepo)
    }

}