package com.aminmart.passwordmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aminmart.passwordmanager.ui.screens.auth.AuthScreen
import com.aminmart.passwordmanager.ui.screens.passwordlist.PasswordListScreen
import com.aminmart.passwordmanager.ui.screens.passworddetail.PasswordDetailScreen
import com.aminmart.passwordmanager.ui.screens.addeditpassword.AddEditPasswordScreen
import com.aminmart.passwordmanager.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.PasswordList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.PasswordList.route) {
            PasswordListScreen(
                onNavigateToDetail = { passwordId ->
                    navController.navigate(Screen.PasswordDetail.createRoute(passwordId))
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.AddPassword.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.PasswordDetail.route,
            arguments = listOf(
                navArgument("passwordId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getLong("passwordId") ?: return@composable
            PasswordDetailScreen(
                passwordId = passwordId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.EditPassword.createRoute(passwordId)) }
            )
        }
        
        composable(Screen.AddPassword.route) {
            AddEditPasswordScreen(
                passwordId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.EditPassword.route,
            arguments = listOf(
                navArgument("passwordId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getLong("passwordId") ?: return@composable
            AddEditPasswordScreen(
                passwordId = passwordId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
