package com.bexner.soccerstats

import android.app.Application
import com.bexner.soccerstats.data.SoccerDatabase
import com.bexner.soccerstats.data.SoccerRepository

/**
 * Manual dependency container. Keeps the project free of a DI framework while
 * still giving every ViewModel a single shared repository.
 */
class SoccerStatsApplication : Application() {

    val repository: SoccerRepository by lazy {
        val db = SoccerDatabase.getInstance(this)
        SoccerRepository(db.teamDao(), db.playerDao())
    }
}
