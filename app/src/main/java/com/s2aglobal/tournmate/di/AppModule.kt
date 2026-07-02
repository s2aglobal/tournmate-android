package com.s2aglobal.tournmate.di

import com.s2aglobal.tournmate.data.repository.CalorieRecordRepository
import com.s2aglobal.tournmate.data.repository.FirestoreCalorieRecordRepository
import com.s2aglobal.tournmate.data.repository.FirestoreMatchRepository
import com.s2aglobal.tournmate.data.repository.FirestorePlayerRepository
import com.s2aglobal.tournmate.data.repository.FirestorePlaySessionRepository
import com.s2aglobal.tournmate.data.repository.FirestoreRatingRepository
import com.s2aglobal.tournmate.data.repository.FirestoreRegistrationRepository
import com.s2aglobal.tournmate.data.repository.FirestoreTournamentRepository
import com.s2aglobal.tournmate.data.repository.MatchRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.PlaySessionRepository
import com.s2aglobal.tournmate.data.repository.RatingRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TournamentRepository
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.auth.FirebaseAuthService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: FirestorePlayerRepository): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindTournamentRepository(impl: FirestoreTournamentRepository): TournamentRepository

    @Binds
    @Singleton
    abstract fun bindRegistrationRepository(impl: FirestoreRegistrationRepository): RegistrationRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(impl: FirestoreMatchRepository): MatchRepository

    @Binds
    @Singleton
    abstract fun bindCalorieRecordRepository(impl: FirestoreCalorieRecordRepository): CalorieRecordRepository

    @Binds
    @Singleton
    abstract fun bindPlaySessionRepository(impl: FirestorePlaySessionRepository): PlaySessionRepository

    @Binds
    @Singleton
    abstract fun bindRatingRepository(impl: FirestoreRatingRepository): RatingRepository

    @Binds
    @Singleton
    abstract fun bindAuthService(impl: FirebaseAuthService): AuthService
}
