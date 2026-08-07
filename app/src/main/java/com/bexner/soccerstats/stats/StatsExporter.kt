package com.bexner.soccerstats.stats

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.bexner.soccerstats.data.entity.EventSide
import com.bexner.soccerstats.data.entity.Position
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the workbook and hands it to Android's share sheet.
 *
 * Files are written to `cacheDir/exports` and served through a FileProvider, so
 * nothing needs storage permissions and the OS cleans up after itself.
 */
object StatsExporter {

    private val fileStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val readable = SimpleDateFormat("EEE d MMM yyyy, h:mm a", Locale.getDefault())

    // ---- Workbook assembly ----

    fun gameSheets(stats: GameStats, teamName: String): List<Sheet> = listOf(
        Sheet(
            "Summary",
            listOf(
                listOf(cell("Metric"), cell("Value")),
                listOf(cell("Team"), cell(teamName)),
                listOf(cell("Opponent"), cell(stats.game.opponent)),
                listOf(cell("Venue"), cell(stats.game.venue.label)),
                listOf(cell("Kickoff"), cell(readable.format(Date(stats.game.kickoffAt)))),
                listOf(cell("Location"), cell(stats.game.location)),
                listOf(cell("Result"), cell(stats.result)),
                listOf(cell("Goals for"), cell(stats.totals.goalsFor)),
                listOf(cell("Goals against"), cell(stats.totals.goalsAgainst)),
                listOf(cell("Shots for"), cell(stats.totals.shotsFor)),
                listOf(cell("Shots on target for"), cell(stats.totals.shotsOnFor)),
                listOf(cell("Shots against"), cell(stats.totals.shotsAgainst)),
                listOf(cell("Shots on target against"), cell(stats.totals.shotsOnAgainst)),
                listOf(cell("Corners for"), cell(stats.totals.cornersFor)),
                listOf(cell("Corners against"), cell(stats.totals.cornersAgainst)),
                listOf(cell("Fouls committed"), cell(stats.totals.foulsFor)),
                listOf(cell("Fouls against"), cell(stats.totals.foulsAgainst)),
                listOf(cell("Saves"), cell(stats.totals.savesFor)),
                listOf(cell("Match minutes"), cell(stats.game.clockElapsedMs / 60000))
            )
        ),
        playersSheet(stats.players),
        minutesSheet(stats.players),
        eventsSheet(stats)
    )

    fun seasonSheets(season: SeasonStats): List<Sheet> = buildList {
        add(
            Sheet(
                "Season",
                listOf(
                    listOf(cell("Metric"), cell("Value")),
                    listOf(cell("Team"), cell(season.teamName)),
                    listOf(cell("Games"), cell(season.games.size)),
                    listOf(cell("Record (W-D-L)"), cell(season.record)),
                    listOf(cell("Goals for"), cell(season.totals.goalsFor)),
                    listOf(cell("Goals against"), cell(season.totals.goalsAgainst)),
                    listOf(cell("Shots for"), cell(season.totals.shotsFor)),
                    listOf(cell("Shots against"), cell(season.totals.shotsAgainst)),
                    listOf(cell("Corners for"), cell(season.totals.cornersFor)),
                    listOf(cell("Corners against"), cell(season.totals.cornersAgainst)),
                    listOf(cell("Saves"), cell(season.totals.savesFor))
                )
            )
        )
        add(playersSheet(season.players))
        add(minutesSheet(season.players))
        add(
            Sheet(
                "Games",
                buildList {
                    add(
                        listOf(
                            cell("Date"), cell("Opponent"), cell("Venue"), cell("Result"),
                            cell("GF"), cell("GA"), cell("Shots"), cell("Shots against"),
                            cell("Corners"), cell("Saves")
                        )
                    )
                    season.games.forEach { g ->
                        add(
                            listOf(
                                cell(readable.format(Date(g.game.kickoffAt))),
                                cell(g.game.opponent),
                                cell(g.game.venue.label),
                                cell(g.result),
                                cell(g.totals.goalsFor),
                                cell(g.totals.goalsAgainst),
                                cell(g.totals.shotsFor),
                                cell(g.totals.shotsAgainst),
                                cell(g.totals.cornersFor),
                                cell(g.totals.savesFor)
                            )
                        )
                    }
                }
            )
        )
        // Every event from every game, so the raw log is always recoverable.
        add(
            Sheet(
                "All events",
                buildList {
                    add(eventHeader(includeGame = true))
                    season.games.forEach { g ->
                        g.events.sortedBy { it.clockMs }.forEach { event ->
                            add(eventRow(g, event, includeGame = true))
                        }
                    }
                }
            )
        )
    }

    private fun playersSheet(players: List<PlayerStats>): Sheet = Sheet(
        "Players",
        buildList {
            add(
                listOf(
                    cell("#"), cell("Player"), cell("GP"), cell("Minutes"),
                    cell("Goals"), cell("Assists"), cell("Shots"), cell("On target"),
                    cell("Accuracy"), cell("Saves"), cell("Tackles"), cell("50/50s"),
                    cell("Fouls"), cell("Offsides"), cell("Yellow"), cell("Red"),
                    cell("GK minutes"), cell("Goals conceded"), cell("Save rate")
                )
            )
            players.sortedByDescending { it.minutesMs }.forEach { p ->
                add(
                    listOf(
                        cell(p.player.jerseyNumber),
                        cell(p.player.fullName),
                        cell(p.gamesPlayed),
                        cell(p.minutes),
                        cell(p.goals),
                        cell(p.assists),
                        cell(p.shots),
                        cell(p.shotsOn),
                        cell(p.shotAccuracy),
                        cell(p.saves),
                        cell(p.tackles),
                        cell(p.fiftyFifties),
                        cell(p.fouls),
                        cell(p.offsides),
                        cell(p.yellowCards),
                        cell(p.redCards),
                        cell(p.keeperMinutes),
                        cell(if (p.playedInGoal) p.goalsConceded else null),
                        cell(p.savePercentage)
                    )
                )
            }
        }
    )

    private fun minutesSheet(players: List<PlayerStats>): Sheet {
        val roles = listOf(
            Position.GOALKEEPER, Position.DEFENDER, Position.MIDFIELDER,
            Position.FORWARD, Position.UNASSIGNED
        )
        return Sheet(
            "Minutes by position",
            buildList {
                add(
                    listOf(cell("#"), cell("Player")) +
                        roles.map { cell(it.label) } +
                        listOf(cell("Total"))
                )
                players.sortedByDescending { it.minutesMs }.forEach { p ->
                    add(
                        listOf(cell(p.player.jerseyNumber), cell(p.player.fullName)) +
                            roles.map { cell((p.minutesByPosition[it] ?: 0L) / 60000) } +
                            listOf(cell(p.minutes))
                    )
                }
            }
        )
    }

    private fun eventHeader(includeGame: Boolean): List<Cell> = buildList {
        if (includeGame) {
            add(cell("Date"))
            add(cell("Opponent"))
        }
        addAll(
            listOf(
                cell("Half"), cell("Clock"), cell("Event"), cell("Side"), cell("Player"),
                cell("Second player"), cell("Pitch X"), cell("Pitch Y"), cell("Third"),
                cell("Goal X"), cell("Goal Y"), cell("Goal zone"), cell("Net"), cell("Note")
            )
        )
    }

    private fun eventRow(
        stats: GameStats,
        event: com.bexner.soccerstats.data.entity.GameEvent,
        includeGame: Boolean
    ): List<Cell> {
        val byId = stats.players.associate { it.player.id to it.player.fullName }
        return buildList {
            if (includeGame) {
                add(cell(readable.format(Date(stats.game.kickoffAt))))
                add(cell(stats.game.opponent))
            }
            addAll(
                listOf(
                    cell(event.period),
                    cell(event.clockLabel),
                    cell(event.type.label),
                    cell(if (event.side == EventSide.US) "Us" else "Them"),
                    cell(event.playerId?.let { byId[it] }),
                    cell(event.secondaryPlayerId?.let { byId[it] }),
                    cell(event.pitchX?.toDouble()),
                    cell(event.pitchY?.toDouble()),
                    cell(event.pitchThird),
                    cell(event.goalX?.toDouble()),
                    cell(event.goalY?.toDouble()),
                    cell(event.goalZone),
                    cell(event.goalTarget?.label),
                    cell(event.note)
                )
            )
        }
    }

    private fun eventsSheet(stats: GameStats): Sheet = Sheet(
        "Events",
        buildList {
            add(eventHeader(includeGame = false))
            stats.events.sortedBy { it.clockMs }.forEach { event ->
                add(eventRow(stats, event, includeGame = false))
            }
        }
    )

    // ---- Writing and sharing ----

    private fun sanitise(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "export" }

    fun writeGameWorkbook(context: Context, stats: GameStats, teamName: String): File {
        val name = "${sanitise(teamName)}-vs-${sanitise(stats.game.opponent)}-" +
            "${fileStamp.format(Date(stats.game.kickoffAt))}.xlsx"
        return writeWorkbook(context, name, gameSheets(stats, teamName))
    }

    fun writeSeasonWorkbook(context: Context, season: SeasonStats): File {
        val name = "${sanitise(season.teamName)}-season-" +
            "${fileStamp.format(Date(System.currentTimeMillis()))}.xlsx"
        return writeWorkbook(context, name, seasonSheets(season))
    }

    private fun writeWorkbook(context: Context, fileName: String, sheets: List<Sheet>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        XlsxWriter.write(file, sheets)
        return file
    }

    /** Opens the system share sheet for a written workbook. */
    fun share(context: Context, file: File, subject: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share $subject")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
