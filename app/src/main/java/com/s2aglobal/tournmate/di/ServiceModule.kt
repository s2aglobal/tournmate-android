package com.s2aglobal.tournmate.di

import android.content.Context
import com.s2aglobal.tournmate.service.court.CourtSearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideCourtSearchService(@ApplicationContext context: Context): CourtSearchService =
        CourtSearchService(context)
}
