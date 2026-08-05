package com.bexner.soccerstats.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bexner.soccerstats.data.dao.FormationDao
import com.bexner.soccerstats.data.dao.GameDao
import com.bexner.soccerstats.data.dao.PlayerDao
import com.bexner.soccerstats.data.dao.TeamDao
import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.Attendance
import com.bexner.soccerstats.data.entity.EventSide
import com.bexner.soccerstats.data.entity.EventType
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.GameAttendance
import com.bexner.soccerstats.data.entity.GameEvent
import com.bexner.soccerstats.data.entity.GameStatus
import com.bexner.soccerstats.data.entity.LineupSlot
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.PlayerStint
import com.bexner.soccerstats.data.entity.Venue
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.data.entity.ShapePhase
import com.bexner.soccerstats.data.entity.Team

class Converters {
    @TypeConverter
    fun positionToString(position: Position): String = position.name

    @TypeConverter
    fun stringToPosition(value: String): Position =
        runCatching { Position.valueOf(value) }.getOrDefault(Position.UNASSIGNED)

    @TypeConverter
    fun matchFormatToString(format: MatchFormat): String = format.name

    @TypeConverter
    fun stringToMatchFormat(value: String): MatchFormat = MatchFormat.fromName(value)

    @TypeConverter
    fun shapePhaseToString(phase: ShapePhase): String = phase.name

    @TypeConverter
    fun stringToShapePhase(value: String): ShapePhase = ShapePhase.fromName(value)

    @TypeConverter
    fun venueToString(venue: Venue): String = venue.name

    @TypeConverter
    fun stringToVenue(value: String): Venue = Venue.fromName(value)

    @TypeConverter
    fun gameStatusToString(status: GameStatus): String = status.name

    @TypeConverter
    fun stringToGameStatus(value: String): GameStatus = GameStatus.fromName(value)

    @TypeConverter
    fun attendanceToString(attendance: Attendance): String = attendance.name

    @TypeConverter
    fun stringToAttendance(value: String): Attendance = Attendance.fromName(value)

    @TypeConverter
    fun eventTypeToString(type: EventType): String = type.name

    @TypeConverter
    fun stringToEventType(value: String): EventType = EventType.fromName(value)

    @TypeConverter
    fun eventSideToString(side: EventSide): String = side.name

    @TypeConverter
    fun stringToEventSide(value: String): EventSide = EventSide.fromName(value)
}

/**
 * v1 -> v2 adds the formation library. Teams and rosters are left untouched —
 * this only creates the two new tables, so existing data survives the upgrade.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `formations` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `format` TEXT NOT NULL,
                `hasKeeper` INTEGER NOT NULL,
                `isPreset` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `formation_slots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `formationId` INTEGER NOT NULL,
                `slotIndex` INTEGER NOT NULL,
                `role` TEXT NOT NULL,
                `x` REAL NOT NULL,
                `y` REAL NOT NULL,
                `label` TEXT NOT NULL,
                FOREIGN KEY(`formationId`) REFERENCES `formations`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_formation_slots_formationId` " +
                "ON `formation_slots` (`formationId`)"
        )
    }
}

/**
 * v2 -> v3 gives every slot a phase, so one formation can hold a defending shape
 * and an attacking shape for the same players. Existing slots become the
 * defending shape, which is what a single-shape formation always represented.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `formation_slots` ADD COLUMN `phase` TEXT NOT NULL DEFAULT 'DEFENDING'"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_formation_slots_formationId_phase` " +
                "ON `formation_slots` (`formationId`, `phase`)"
        )
    }
}

/**
 * v3 -> v4 adds games, attendance, lineups, substitution stints and match events.
 * Purely additive: five new tables, nothing existing is touched.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `games` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `teamId` INTEGER NOT NULL,
                `opponent` TEXT NOT NULL,
                `venue` TEXT NOT NULL,
                `kickoffAt` INTEGER NOT NULL,
                `location` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `formationId` INTEGER,
                `periodCount` INTEGER NOT NULL,
                `periodMinutes` INTEGER NOT NULL,
                `goalsFor` INTEGER NOT NULL,
                `goalsAgainst` INTEGER NOT NULL,
                `clockElapsedMs` INTEGER NOT NULL,
                `clockRunningSince` INTEGER,
                `currentPeriod` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_teamId` ON `games` (`teamId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_games_teamId_kickoffAt` " +
                "ON `games` (`teamId`, `kickoffAt`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `game_attendance` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `gameId` INTEGER NOT NULL,
                `playerId` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                FOREIGN KEY(`gameId`) REFERENCES `games`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`playerId`) REFERENCES `players`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_game_attendance_gameId_playerId` " +
                "ON `game_attendance` (`gameId`, `playerId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_game_attendance_playerId` " +
                "ON `game_attendance` (`playerId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lineup_slots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `gameId` INTEGER NOT NULL,
                `slotIndex` INTEGER NOT NULL,
                `playerId` INTEGER NOT NULL,
                FOREIGN KEY(`gameId`) REFERENCES `games`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`playerId`) REFERENCES `players`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_lineup_slots_gameId_slotIndex` " +
                "ON `lineup_slots` (`gameId`, `slotIndex`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_lineup_slots_playerId` " +
                "ON `lineup_slots` (`playerId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `player_stints` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `gameId` INTEGER NOT NULL,
                `playerId` INTEGER NOT NULL,
                `slotIndex` INTEGER NOT NULL,
                `role` TEXT NOT NULL,
                `onAtMs` INTEGER NOT NULL,
                `offAtMs` INTEGER,
                FOREIGN KEY(`gameId`) REFERENCES `games`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`playerId`) REFERENCES `players`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_stints_gameId` ON `player_stints` (`gameId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_stints_playerId` ON `player_stints` (`playerId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_player_stints_gameId_playerId` " +
                "ON `player_stints` (`gameId`, `playerId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `game_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `gameId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `side` TEXT NOT NULL,
                `playerId` INTEGER,
                `secondaryPlayerId` INTEGER,
                `period` INTEGER NOT NULL,
                `clockMs` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`gameId`) REFERENCES `games`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_events_gameId` ON `game_events` (`gameId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_game_events_gameId_clockMs` " +
                "ON `game_events` (`gameId`, `clockMs`)"
        )
    }
}

@Database(
    entities = [
        Team::class, Player::class, Formation::class, FormationSlot::class,
        Game::class, GameAttendance::class, LineupSlot::class,
        PlayerStint::class, GameEvent::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoccerDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun formationDao(): FormationDao
    abstract fun gameDao(): GameDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
