package com.expensetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.ui.feature.aliases.AliasesScreen
import com.expensetracker.app.ui.feature.excluded.ExcludedTransactionsScreen
import com.expensetracker.app.ui.feature.home.HomeScreen
import com.expensetracker.app.ui.feature.transactionlist.TransactionListScreen
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                    ) {
                        composable("home") {
                            HomeScreen(
                                onSeeAll = { navController.navigate("transactions") },
                                onViewExcluded = { navController.navigate("excluded") },
                                onManageAliases = { navController.navigate("aliases") },
                            )
                        }
                        composable("transactions") {
                            TransactionListScreen(onBack = { navController.popBackStack() })
                        }
                        composable("excluded") {
                            ExcludedTransactionsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("aliases") {
                            AliasesScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
