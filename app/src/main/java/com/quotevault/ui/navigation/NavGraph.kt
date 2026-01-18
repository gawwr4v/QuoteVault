package com.quotevault.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quotevault.ui.screens.auth.LoginScreen
import com.quotevault.ui.screens.auth.SignUpScreen
import com.quotevault.ui.screens.auth.AuthViewModel
import com.quotevault.ui.screens.auth.AuthUiState
import com.quotevault.ui.screens.splash.SplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quotevault.ui.screens.favorites.CollectionDetailScreen
import com.quotevault.ui.screens.collections.CollectionsScreen
import com.quotevault.ui.screens.home.HomeScreen
import com.quotevault.ui.screens.settings.SettingsScreen
import com.quotevault.ui.screens.share.ShareScreen
import com.quotevault.ui.screens.account.AccountManagementScreen

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Library : Screen("collections", "Collections", Icons.Filled.List, Icons.Outlined.List)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun QuoteVaultNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Define screens that should show the bottom bar
    val bottomBarRoutes = listOf(Screen.Home.route, Screen.Library.route, Screen.Profile.route)
    val shouldShowBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(Screen.Home, Screen.Library, Screen.Profile)
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                    fontSize = 10.sp
                                ) 
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // We use a Surface/Box to consume the padding and provide background
        // innerPadding from Scaffold includes BottomBar height.
        // We should NOT apply top padding here if the inner screens utilize Scaffold with TopBar.
        Box(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding()) 
        ) {
            val authState by authViewModel.uiState.collectAsState()

            NavHost(
                navController = navController,
                startDestination = "splash"
            ) {
                composable("splash") {
                    SplashScreen(
                        isAuthenticated = when(authState) {
                            is AuthUiState.Success -> true
                            is AuthUiState.Idle -> false
                            is AuthUiState.Loading -> null
                            is AuthUiState.Error -> false
                            is AuthUiState.PasswordResetSent -> false
                        },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo("splash") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }

                // Auth Routes
                composable("login") {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authUiState by authViewModel.uiState.collectAsState()
                    
                    // Navigate on successful login
                    LaunchedEffect(authUiState) {
                        if (authUiState is AuthUiState.Success) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                    
                    LoginScreen(
                        onLoginClick = { email, password ->
                            authViewModel.signIn(email, password)
                        },
                        onSignUpClick = {
                            navController.navigate("signup")
                        },
                        onResetPasswordClick = { email ->
                             authViewModel.resetPassword(email)
                        }
                    )
                }

                composable("signup") {
                    SignUpScreen(
                        onSignUpClick = { _, _ ->
                            navController.navigate(Screen.Home.route) {
                                popUpTo("signup") { inclusive = true }
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        onSignInClick = { navController.popBackStack() }
                    )
                }

                // Main Routes
                composable(Screen.Home.route) {
                    HomeScreen(
                        onProfileClick = { navController.navigate("account_management") },
                        onNavigateToShare = { quoteId -> navController.navigate("share/$quoteId") }
                    )
                }

                composable(Screen.Library.route) {
                    CollectionsScreen(
                        onNavigateToCollection = { collectionId -> navController.navigate("collection/$collectionId") }
                    )
                }

                composable(Screen.Profile.route) {
                    SettingsScreen(
                        onBack = { 
                            // If on profile tab, back might logically go to home or exit
                            navController.navigate(Screen.Home.route) 
                        },
                        onNavigateToAccentColors = {
                            navController.navigate("accent_colors")
                        },
                        onNavigateToTextSize = {
                            navController.navigate("text_size")
                        },
                        onNavigateToAccountManagement = {
                            navController.navigate("account_management")
                        },
                        onLogoutComplete = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )

                }

                composable("accent_colors") {
                     com.quotevault.ui.screens.settings.AccentColorScreen(
                         onBack = { navController.popBackStack() }
                     )
                }
                
                composable("account_management") {
                    AccountManagementScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable("text_size") {
                    com.quotevault.ui.screens.settings.TextSizeScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Other Routes
                composable(
                    route = "collection/{collectionId}",
                    arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                ) { backStackEntry: NavBackStackEntry ->
                    val collectionId = backStackEntry.arguments?.getString("collectionId") ?: return@composable // Fix return
                     // Note: collectionId might be null if not found, but we handle it
                    CollectionDetailScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToShare = { quoteId -> navController.navigate("share/$quoteId") }
                    )
                }

                composable(
                    route = "share/{quoteId}",
                    arguments = listOf(navArgument("quoteId") { type = NavType.StringType })
                ) { backStackEntry: NavBackStackEntry ->
                    val quoteId = backStackEntry.arguments?.getString("quoteId") ?: return@composable
                    ShareScreen(
                        quoteId = quoteId,
                        onBack = { 
                            navController.navigateUp() 
                        }
                    )
                }
            }
        }
    }
}
