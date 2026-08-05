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
import com.bexner.soccerstats.data.dao.PlayerDao
import com.bexner.soccerstats.data.dao.TeamDao
import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.MatchFormat
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

@Database(
    entities = [Team::class, Player::class, Formation::class, FormationSlot::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoccerDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun formationDao(): FormationDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
