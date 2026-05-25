package com.baranov.cookbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baranov.cookbook.ui.theme.CookbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RecipeRepository.initialize()
        setContent {
            CookbookTheme {
                CookbookApp()
            }
        }
    }
}

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
            route = "recipe_editor/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.IntType; defaultValue = -1 })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getInt("recipeId") ?: -1
            RecipeEditorScreen(
                recipeId = recipeId,
                onFinish = { updatedRecipe ->
                    // TODO: в будущем заменить на ApiClient.createRecipe / updateRecipe
                    if (recipeId == -1) {
                        RecipeRepository.addRecipe(updatedRecipe)
                    } else {
                        RecipeRepository.updateRecipe(updatedRecipe)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}