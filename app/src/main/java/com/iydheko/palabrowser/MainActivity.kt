package com.iydheko.palabrowser

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iydheko.palabrowser.ui.screens.browser.BrowserScreen
import com.iydheko.palabrowser.ui.screens.settings.SettingsScreen
import com.iydheko.palabrowser.ui.theme.PalaBrowserTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            PalaBrowserTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent) {
                        innerPadding ->
                    NavHost(
                            navController = navController,
                            startDestination = "main",
                            enterTransition = {
                                slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left
                                )
                            }, // masuk dari kanan (mirip slide_in_right)
                            exitTransition = {
                                slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left
                                )
                            }, // keluar ke kiri (mirip slide_out_left)
                            popEnterTransition = {
                                slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right
                                )
                            }, // balik dari kiri (mirip slide_in_left)
                            popExitTransition = {
                                slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right
                                )
                            }
                    ) {
                        composable("main") {
                            BrowserScreen(
                                    paddingValues = innerPadding,
                                    navController = navController
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                    paddingValues = innerPadding,
                                    navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}
