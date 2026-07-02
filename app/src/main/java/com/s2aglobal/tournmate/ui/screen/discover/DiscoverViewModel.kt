package com.s2aglobal.tournmate.ui.screen.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.domain.model.SportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    currentUserStore: CurrentUserStore,
) : ViewModel() {

    val preferredSport: StateFlow<SportType> = currentUserStore.preferredSportFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SportType.BADMINTON,
        )
}
