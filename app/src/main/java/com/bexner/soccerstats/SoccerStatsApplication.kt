package com.bexner.soccerstats

import android.app.Application
import com.bexner.soccerstats.data.SoccerDatabase
import com.bexner.soccerstats.data.SoccerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container. Keeps the project free of a DI framework while
 * still giving every ViewModel a single shared repository.
 */
class SoccerStatsApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: SoccerRepository by lazy {
        val db = SoccerDatabase.getInstance(this)
        SoccerRepository(db.teamDao(), db.playerDao(), db.formationDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Fills the formation library on first launch, and after an upgrade that
        // added it to an existing database.
        applicationScope.launch {
            repository.seedPresetFormationsIfEmpty()
        }
    }
}
