package com.s2aglobal.tournmate.ui.component

import androidx.lifecycle.ViewModel
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.sport.PreferredSportUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Backs [SportPickerSheet] outside Profile; same update path as Profile's "My Sport". */
@HiltViewModel
class SportSwitcherViewModel @Inject constructor(
    private val updater: PreferredSportUpdater,
) : ViewModel() {
    fun select(sport: SportType) {
        updater.setPreferredSport(sport)
    }
}
