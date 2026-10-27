package com.tripian.trpcore.ui.timeline.compose.nav

import com.tripian.trpcore.base.TRPCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holds back the Timeline map until a current Mapbox access token is applied. Starts
 * [State.Ready] when the applied token is the host-supplied or a fresh cached one, otherwise
 * [State.Resolving] until [resolve] settles.
 *
 * @param tokenCurrent whether a host-supplied or fresh cached token is applied when the gate is created
 * @param ensureToken resolves and applies the token for a user id, keeping an already applied
 * token when the refresh fails; `false` only when no token is available at all
 */
internal class MapboxTokenGate(
    tokenCurrent: Boolean = TRPCore.core.isAppliedMapboxAccessTokenCurrent(),
    private val ensureToken: suspend (String?) -> Boolean = { TRPCore.core.ensureMapboxAccessToken(it) }
) {

    /** Where the gate stands for the Timeline destination. */
    sealed interface State {
        data object Resolving : State
        data object Ready : State
        data object Unavailable : State
    }

    private val mutableState = MutableStateFlow(if (tokenCurrent) State.Ready else State.Resolving)

    val state: StateFlow<State> = mutableState.asStateFlow()

    private val resolveLock = Mutex()

    /**
     * Resolves the token for [uniqueId] and moves to [State.Ready] or [State.Unavailable].
     * Concurrent calls run the resolver once; calls after the gate has settled do nothing.
     */
    suspend fun resolve(uniqueId: String?) {
        resolveLock.withLock {
            if (mutableState.value != State.Resolving) return
            mutableState.value = if (ensureToken(uniqueId)) State.Ready else State.Unavailable
        }
    }
}
