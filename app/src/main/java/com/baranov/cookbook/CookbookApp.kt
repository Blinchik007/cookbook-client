package com.baranov.cookbook

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baranov.cookbook.screens.HomeScreen
import com.baranov.cookbook.screens.LoginScreen
import com.baranov.cookbook.screens.ProfileScreen
import com.baranov.cookbook.screens.RecipeViewMode
import com.baranov.cookbook.screens.RecipeViewScreen
import com.baranov.cookbook.screens.recipeEditor.RecipeEditorScreen
import com.baranov.cookbook.screens.RegisterScreen


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CookbookApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home_screen"
    ) {
        composable("home_screen") {
            HomeScreen(rootNavController = navController)
        }
        composable("login_screen") {
            LoginScreen(
                onNavigateToMain = {
                    navController.popBackStack(
                        route = "home_screen",
                        inclusive = false
                    )
                },
                onNavigateToRegistration = {
                    navController.navigate("register_screen")
                }
            )
        }
        composable("register_screen") {
            RegisterScreen(
                onNavigateToMain = {
                    navController.popBackStack(
                        route = "home_screen",
                        inclusive = false
                    )
                }
            )
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

        // Просмотр локального рецепта (свой/скачанный)
        composable(
            route = "recipe_view/local/{localId}",
            arguments = listOf(navArgument("localId") { type = NavType.LongType })
        ) { backStackEntry ->
            val localId = backStackEntry.arguments?.getLong("localId") ?: -1L
            RecipeViewScreen(
                mode = RecipeViewMode.Local(localId),
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("recipe_editor/$id") }
            )
        }

        // Просмотр публичного рецепта с сервера (не скачан)
        composable(
            route = "recipe_view/server/{serverId}",
            arguments = listOf(navArgument("serverId") { type = NavType.IntType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
            RecipeViewScreen(
                mode = RecipeViewMode.Server(serverId),
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("recipe_editor/$id") }
            )
        }

        composable("profile_screen") {
            ProfileScreen(onFinish = { navController.popBackStack() })
        }
    }
}