package com.kap1bala.icypower.data.ha

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op implementation used in v1 before the real OkHttp/WebSocket client
 * lands (plan PR #3).
 *
 * Critical: this must NEVER touch the network. We don't have INTERNET
 * permission in the manifest yet, and OkHttp construction alone would
 * reserve a connection pool we won't use.
 */
object NoOpHaClient : HaClient {
    override suspend fun probe(): Boolean = false
    override suspend fun getStates(): List<HaState> = emptyList()
    override suspend fun getState(entityId: String): HaState? = null
    override suspend fun getHistory(entityId: String, since: String): List<HaHistoryPoint> = emptyList()
    override fun subscribeStateChanges(entityIds: List<String>): Flow<HaEvent> = emptyFlow()
}
