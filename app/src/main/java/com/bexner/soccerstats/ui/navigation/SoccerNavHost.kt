package com.bexner.soccerstats.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bexner.soccerstats.ui.formations.FormationEditScreen
import com.bexner.soccerstats.ui.formations.FormationListScreen
import com.bexner.soccerstats.ui.roster.PlayerEditScreen
import com.bexner.soccerstats.ui.roster.RosterScreen
import com.bexner.soccerstats.ui.teams.TeamEditScreen
import com.bexner.soccerstats.ui.teams.TeamListScreen

object Routes {
    const val TEAM_LIST = "teams"

    const val TEAM_ID_ARG = "teamId"
    const val PLAYER_ID_ARG = "playerId"

    /** teamId of 0 means "new team". */
    const val TEAM_EDIT = "teams/edit/{$TEAM_ID_ARG}"
    fun teamEdit(teamId: Long = 0L) = "teams/edit/$teamId"

    const val ROSTER = "teams/{$TEAM_ID_ARG}/roster"
    fun roster(teamId: Long) = "teams/$teamId/roster"

    /** playerId of 0 means "new player". */
    const val PLAYER_EDIT = "teams/{$TEAM_ID_ARG}/roster/edit/{$PLAYER_ID_ARG}"
    fun playerEdit(teamId: Long, playerId: Long = 0L) = "teams/$teamId/roster/edit/$playerId"

    const val FORMATION_ID_ARG = "formationId"
    const val FORMAT_ARG = "format"

    const val FORMATION_LIST = "formations"

    /** formationId of 0 means "new formation"; format seeds the starting shape. */
    const val FORMATION_EDIT = "formations/edit/{$FORMATION_ID_ARG}?$FORMAT_ARG={$FORMAT_ARG}"
    fun formationEdit(formationId: Long = 0L, format: String = "") =
        "formations/edit/$formationId?$FORMAT_ARG=$format"
}

@Composable
fun SoccerNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.TEAM_LIST) {

        composable(Routes.TEAM_LIST) {
            TeamListScreen(
                onAddTeam = { navController.navigate(Routes.teamEdit()) },
                onOpenTeam = { teamId -> navController.navigate(Routes.roster(teamId)) },
                onOpenFormations = { navController.navigate(Routes.FORMATION_LIST) }
            )
        }

        composable(
            route = Routes.TEAM_EDIT,
            arguments = listOf(navArgument(Routes.TEAM_ID_ARG) { type = NavType.LongType })
        ) {
            TeamEditScreen(onDone = { navController.popBackStack() })
        }

        composable(
            route = Routes.ROSTER,
            arguments = listOf(navArgument(Routes.TEAM_ID_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getLong(Routes.TEAM_ID_ARG) ?: 0L
            RosterScreen(
                onBack = { navController.popBackStack() },
                onEditTeam = { navController.navigate(Routes.teamEdit(teamId)) },
                onAddPlayer = { navController.navigate(Routes.playerEdit(teamId)) },
                onEditPlayer = { playerId -> navController.navigate(Routes.playerEdit(teamId, playerId)) },
                onTeamDeleted = {
                    navController.popBackStack(Routes.TEAM_LIST, inclusive = false)
                }
            )
        }

        composable(Routes.FORMATION_LIST) {
            FormationListScreen(
                onBack = { navController.popBackStack() },
                onCreate = { format -> navController.navigate(Routes.formationEdit(format = format.name)) },
                onOpen = { formationId -> navController.navigate(Routes.formationEdit(formationId)) }
            )
        }

        composable(
            route = Routes.FORMATION_EDIT,
            arguments = listOf(
                navArgument(Routes.FORMATION_ID_ARG) { type = NavType.LongType },
                navArgument(Routes.FORMAT_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            FormationEditScreen(onDone = { navController.popBackStack() })
        }

        composable(
            route = Routes.PLAYER_EDIT,
            arguments = listOf(
                navArgument(Routes.TEAM_ID_ARG) { type = NavType.LongType },
                navArgument(Routes.PLAYER_ID_ARG) { type = NavType.LongType }
            )
        ) {
            PlayerEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
