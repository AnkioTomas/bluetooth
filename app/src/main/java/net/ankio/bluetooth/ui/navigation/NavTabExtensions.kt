package net.ankio.bluetooth.ui.navigation

import androidx.navigation.NavHostController

fun NavHostController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
