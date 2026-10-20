package com.tripian.trpcore.ui.timeline.compose.nav

import com.tripian.trpcore.base.TRPCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds back the Timeline map until a Mapbox access token is applied. Starts [State.Ready] when
 * a token is already applied, otherwise [State.Resolving] until [resolve] settles.
 *
 * @param tokenAlreadyApplied whether a token is applied when the gate is created
 * @param ensureToken resolves and applies the token for a user id; `false` when none is available
 */
internal class MapboxTokenGate(
    tokenAlreadyApplied: Boolean = TRPCore.core.hasAppliedMapboxAccessToken(),
    private val ensureToken: suspend (String?) -> Boolean = { TRPCore.core.ensureMapboxAccessToken(it) }
) {

    /** Where the gate stands for the Timeline destination. */
    sealed interface State {
        data object Resolving : State
        data object Ready : State
        data object Unavailable : State
    }

    private val mutableState = MutableStateFlow(if (tokenAlreadyApplied) State.Ready else State.Resolving)

    val state: StateFlow<State> = mutableState.asStateFlow()

    /**
     * Resolves the token for [uniqueId] and moves to [State.Ready] or [State.Unavailable].
     * Does nothing once the gate has settled.
     */
    suspend fun resolve(uniqueId: String?) {
        if (mutableState.value != State.Resolving) return
        mutableState.value = if (ensureToken(uniqueId)) State.Ready else State.Unavailable
    }
}
