package com.bexner.soccerstats.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.bexner.soccerstats.data.dao.PlayerDao
import com.bexner.soccerstats.data.dao.TeamDao
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.data.entity.Team

class Converters {
    @TypeConverter
    fun positionToString(position: Position): String = position.name

    @TypeConverter
    fun stringToPosition(value: String): Position =
        runCatching { Position.valueOf(value) }.getOrDefault(Position.UNASSIGNED)
}

@Database(
    entities = [Team::class, Player::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoccerDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao

    companion object {
        @Volatile
        private var INSTANCE: SoccerDatabase? = null

        fun getInstance(context: Context): SoccerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoccerDatabase::class.java,
                    "soccer_stats.db"
                )
                    // Foreign keys drive cascade deletes of players when a team is removed.
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
