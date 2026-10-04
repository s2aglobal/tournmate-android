package com.s2aglobal.tournmate.service.config

import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.BuildConfig
import com.s2aglobal.tournmate.service.AnalyticsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** A dotted numeric version ("1.6", "1.6.2") compared component by component. */
data class AppVersion(val components: List<Int>) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val count = maxOf(components.size, other.components.size)
        for (i in 0 until count) {
            val l = components.getOrElse(i) { 0 }
            val r = other.components.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    override fun toString(): String = components.joinToString(".")

    companion object {
        /** Parses a strictly numeric dotted version; rejects empty components such as "1..6". */
        fun parse(raw: String?): AppVersion? {
            val parts = raw?.trim()?.split(".") ?: return null
            val numbers = parts.mapNotNull { part -> part.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull() }
            if (numbers.isEmpty() || numbers.size != parts.size) return null
            return AppVersion(numbers)
        }

        /** The running app's version, ignoring any non-numeric suffix (e.g. "1.0.0-dev"). */
        val current: AppVersion?
            get() = fromVersionName(BuildConfig.VERSION_NAME)

        fun fromVersionName(versionName: String?): AppVersion? {
            val numeric = versionName?.trim()?.let { Regex("""^\d+(\.\d+)*""").find(it)?.value }
            return parse(numeric)
        }
    }
}

data class AppUpdatePolicy(
    val minimumVersion: AppVersion?,
    val storeUrl: String?,
    val message: String?,
) {
    fun requiresUpdate(from: AppVersion?): Boolean {
        val minimum = minimumVersion ?: return false
        val version = from ?: return false
        return version < minimum
    }
}

data class AppVersionGateState(
    val isUpdateRequired: Boolean = false,
    val policy: AppUpdatePolicy? = null,
)

/**
 * Minimum-version check backed by Firestore `config/app` (shared with iOS).
 * Fails open: a missing, unreadable, or slow (> 5s) config never blocks the app.
 */
@Singleton
class AppVersionGate @Inject constructor(
    private val db: FirebaseFirestore,
    private val analytics: AnalyticsService,
) {
    private val _state = MutableStateFlow(AppVersionGateState())
    val state: StateFlow<AppVersionGateState> = _state.asStateFlow()

    private val currentVersion: AppVersion? = AppVersion.current
    private var simulated = false

    /** Debug-only preview of the blocker. */
    fun simulateUpdateRequired() {
        if (!BuildConfig.DEBUG) return
        simulated = true
        _state.value = AppVersionGateState(
            isUpdateRequired = true,
            policy = AppUpdatePolicy(AppVersion.parse("999"), storeUrl = null, message = null),
        )
    }

    suspend fun check() {
        if (simulated) return
        // Keep the last known answer if this fetch fails.
        val policy = fetchPolicy() ?: return
        val required = policy.requiresUpdate(currentVersion)
        _state.value = AppVersionGateState(isUpdateRequired = required, policy = policy)
        if (required) {
            analytics.log(
                AnalyticsService.EventName.UPDATE_REQUIRED_SHOWN,
                AnalyticsService.Param.CURRENT_VERSION to (currentVersion?.toString() ?: "unknown"),
                AnalyticsService.Param.MINIMUM_VERSION to (policy.minimumVersion?.toString() ?: "none"),
            )
        }
    }

    private suspend fun fetchPolicy(): AppUpdatePolicy? = try {
        withTimeoutOrNull(TIMEOUT_MS) {
            val doc = db.collection("config").document("app").get().await()
            if (!doc.exists()) return@withTimeoutOrNull null
            AppUpdatePolicy(
                minimumVersion = AppVersion.parse(doc.getString("minimumAndroidVersion")),
                storeUrl = doc.getString("androidStoreURL")?.takeIf { it.isNotBlank() },
                message = doc.getString("updateMessage")?.takeIf { it.isNotBlank() },
            )
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val TIMEOUT_MS = 5_000L
    }
}
