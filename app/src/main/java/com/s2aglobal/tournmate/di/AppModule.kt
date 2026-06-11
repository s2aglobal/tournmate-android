package com.s2aglobal.tournmate.di

import com.s2aglobal.tournmate.data.repository.CalorieRecordRepository
import com.s2aglobal.tournmate.data.repository.FirestoreCalorieRecordRepository
import com.s2aglobal.tournmate.data.repository.FirestorePlayerRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
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
    abstract fun bindCalorieRecordRepository(impl: FirestoreCalorieRecordRepository): CalorieRecordRepository

    @Binds
    @Singleton
    abstract fun bindAuthService(impl: FirebaseAuthService): AuthService
}
