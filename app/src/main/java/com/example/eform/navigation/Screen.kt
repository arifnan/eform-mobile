package com.example.eform.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard/{userIdentifier}") // UBAH DI SINI
    object DashboardStudents : Screen("dashboardstudents/{userIdentifier}") // UBAH DI SINI
    object CreateForm : Screen("create_form/{userIdentifier}")
    object HistoryForm : Screen("history_form")
    object FavoriteForms : Screen("favorite_forms/{userIdentifier}")
    object EditForm : Screen("edit_form/{formId}")
    object FormAnswer: Screen("form_answer/{formId}/{userIdentifier}")
    object Profile : Screen("profile/{userIdentifier}") // UBAH DI SINI
    object Notification : Screen("notification/{userIdentifier}")
    object Role : Screen("role")
    object LoginStudents : Screen("loginstudents")
    object RegisterStudents : Screen("registerstudents")
    object PreviewForm : Screen("preview_form/{formId}")
}