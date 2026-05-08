package com.amir.buysmart.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amir.buysmart.presentation.screens.additem.AddItemScreen
import com.amir.buysmart.presentation.screens.auth.AuthScreen
import com.amir.buysmart.presentation.screens.home.HomeScreen
import com.amir.buysmart.presentation.screens.shopping.ShoppingScreen

private object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val ADD_ITEM = "add_item/{listId}"
    const val SHOPPING = "shopping/{listId}"

    fun addItem(listId: String) = "add_item/$listId"
    fun shopping(listId: String) = "shopping/$listId"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(
                onSignedIn = { navController.navigate(Routes.HOME) { popUpTo(Routes.AUTH) { inclusive = true } } }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onAddItem = { listId -> navController.navigate(Routes.addItem(listId)) },
                onGoShopping = { listId -> navController.navigate(Routes.shopping(listId)) }
            )
        }

        composable(
            Routes.ADD_ITEM,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) { backStack ->
            AddItemScreen(
                listId = backStack.arguments?.getString("listId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.SHOPPING,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) { backStack ->
            ShoppingScreen(
                listId = backStack.arguments?.getString("listId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
