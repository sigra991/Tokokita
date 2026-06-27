package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShopViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: ShopViewModel = viewModel()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute in listOf("dashboard", "products", "kasir", "hutang", "laporan")) {
                            NavigationBar(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.drawBehind {
                                    drawLine(
                                        color = com.example.ui.theme.BorderGray,
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                                    label = { Text("Beranda") },
                                    selected = currentRoute == "dashboard",
                                    onClick = { 
                                        navController.navigate("dashboard") { 
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true 
                                        } 
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Produk") },
                                    label = { Text("Produk") },
                                    selected = currentRoute == "products",
                                    onClick = { 
                                        navController.navigate("products") { 
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true 
                                        } 
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Kasir") },
                                    label = { Text("Kasir") },
                                    selected = currentRoute == "kasir",
                                    onClick = { 
                                        navController.navigate("kasir") { 
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true 
                                        } 
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Hutang") },
                                    label = { Text("Hutang") },
                                    selected = currentRoute == "hutang",
                                    onClick = { 
                                        navController.navigate("hutang") { 
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true 
                                        } 
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Laporan") },
                                    label = { Text("Laporan") },
                                    selected = currentRoute == "laporan",
                                    onClick = { 
                                        navController.navigate("laporan") { 
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true 
                                        } 
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(viewModel = viewModel)
                        }
                        composable("products") {
                            ProductsScreen(
                                viewModel = viewModel,
                                onNavigateToAddProduct = { navController.navigate("add_edit_product/0") },
                                onNavigateToEditProduct = { id -> navController.navigate("add_edit_product/$id") }
                            )
                        }
                        composable(
                            route = "add_edit_product/{productId}",
                            arguments = listOf(navArgument("productId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getInt("productId") ?: 0
                            AddEditProductScreen(
                                viewModel = viewModel,
                                productId = productId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("kasir") {
                            KasirScreen(viewModel = viewModel)
                        }
                        composable("hutang") {
                            HutangScreen(
                                viewModel = viewModel,
                                onNavigateToCustomerDetail = { id -> navController.navigate("customer_detail/$id") }
                            )
                        }
                        composable(
                            route = "customer_detail/{customerId}",
                            arguments = listOf(navArgument("customerId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val customerId = backStackEntry.arguments?.getInt("customerId") ?: 0
                            DetailPelangganScreen(
                                viewModel = viewModel,
                                customerId = customerId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("laporan") {
                            LaporanScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
