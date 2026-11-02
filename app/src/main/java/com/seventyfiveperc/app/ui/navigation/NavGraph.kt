package com.seventyfiveperc.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.seventyfiveperc.app.SeventyFivePercApp
import com.seventyfiveperc.app.ui.ViewModelFactory
import com.seventyfiveperc.app.ui.assistant.AssistantScreen
import com.seventyfiveperc.app.ui.assistant.AssistantViewModel
import com.seventyfiveperc.app.ui.dashboard.DashboardScreen
import com.seventyfiveperc.app.ui.dashboard.DashboardViewModel
import com.seventyfiveperc.app.ui.screens.MainScreen
import com.seventyfiveperc.app.ui.profile.ProfileViewModel
import com.seventyfiveperc.app.ui.screens.ProfileSetupScreen
import com.seventyfiveperc.app.ui.screens.SplashScreen
import com.seventyfiveperc.app.ui.timetable.TimetableScreen
import com.seventyfiveperc.app.ui.timetable.TimetableViewModel

import com.seventyfiveperc.app.ui.ocr.OcrScreen
import com.seventyfiveperc.app.ui.ocr.OcrViewModel
import com.seventyfiveperc.app.ui.predictor.PredictorScreen
import com.seventyfiveperc.app.ui.predictor.PredictorViewModel
import com.seventyfiveperc.app.ui.calendar.CalendarScreen
import com.seventyfiveperc.app.ui.calendar.CalendarViewModel

object Routes {
    const val SPLASH = "splash"
    const val MAIN = "main"
    const val PROFILE = "profile"
    const val DASHBOARD = "dashboard"
    const val TIMETABLE = "timetable"
    const val ASSISTANT = "assistant"
    const val OCR_IMPORT = "ocr_import"
    const val PREDICTOR = "predictor"
    const val CALENDAR = "calendar"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as SeventyFivePercApp
    val factory = remember { ViewModelFactory(app.database.attendanceDao()) }

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onSplashFinished = {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) }
            )
        }

        composable(Routes.PROFILE) {
            val vm: ProfileViewModel = viewModel(factory = factory)
            ProfileSetupScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(factory = factory)
            DashboardScreen(
                viewModel = vm,
                onNavigateToAssistant = { navController.navigate(Routes.ASSISTANT) },
                onNavigateToTimetable = { navController.navigate(Routes.TIMETABLE) },
                onNavigateToOcr = { navController.navigate(Routes.OCR_IMPORT) },
                onNavigateToPredictor = { navController.navigate(Routes.PREDICTOR) },
                onNavigateToCalendar = { navController.navigate(Routes.CALENDAR) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToGhostMode = { navController.navigate(Routes.MAIN) }
            )
        }

        composable(Routes.TIMETABLE) {
            val vm: TimetableViewModel = viewModel(factory = factory)
            TimetableScreen(vm, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.ASSISTANT) {
            val vm: AssistantViewModel = viewModel(factory = factory)
            AssistantScreen(vm, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.OCR_IMPORT) {
            val vm: OcrViewModel = viewModel(factory = factory)
            OcrScreen(vm, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.PREDICTOR) {
            val vm: PredictorViewModel = viewModel(factory = factory)
            PredictorScreen(vm, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.CALENDAR) {
            val vm: CalendarViewModel = viewModel(factory = factory)
            CalendarScreen(vm, onBack = { navController.popBackStack() })
        }
    }
}
