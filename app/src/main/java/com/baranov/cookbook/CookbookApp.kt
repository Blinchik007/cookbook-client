package com.baranov.cookbook

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baranov.cookbook.screens.MainScreen
import com.baranov.cookbook.screens.RecipeEditorScreen


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CookbookApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login_screen"
    ) {
        composable("login_screen") {
            LoginScreen(
                onNavigateToMain = {
                    navController.navigate("main_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                },
                onNavigateToRegistration = {
                    navController.navigate("authorization_screen")
                }
            )
        }
        composable("authorization_screen") {
            AuthorizationScreen(
                onNavigateToMain = {
                    navController.navigate("main_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                }
            )
        }
        composable("main_screen") {
            MainScreen(rootNavController = navController)
        }
        composable(
            route = "recipe_editor/{recipeLocalId}",
            arguments = listOf(navArgument("recipeLocalId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val recipeLocalId = backStackEntry.arguments?.getLong("recipeLocalId") ?: -1L
            RecipeEditorScreen(
                recipeLocalId = recipeLocalId,
                onFinish = { navController.popBackStack() }
            )
        }
    }
}