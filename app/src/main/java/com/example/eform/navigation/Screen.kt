package com.example.eform.navigation


sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login?formCode={formCode}") { // Unified Login Screen
        fun createRoute(formCode: String? = null): String {
            return if (formCode != null) "login?formCode=$formCode" else "login"
        }
    }
    object Dashboard : Screen("dashboard/{userIdentifier}") // For Teacher
    object DashboardStudents : Screen("dashboardstudents/{userIdentifier}") // For Student
    object CreateForm : Screen("create_form/{userIdentifier}") // For Teacher (needs to know who created it)
    object HistoryForm : Screen("history_form/{userIdentifier}") // Needs {userIdentifier}
    object FavoriteForms : Screen("favorite_forms/{userIdentifier}") // For Teacher and Student
    object EditForm : Screen("edit_form/{formId}") // Needs {formId}, maybe also {userIdentifier} if only owner can edit
    object FormAnswer: Screen("form_answer/{formId}/{userIdentifier}") // For Student (needs to know who is filling)
    object Profile : Screen("profile/{userIdentifier}") // For Teacher and Student
    object Notification : Screen("notification/{userIdentifier}") // For Teacher and Student
    // Removed RoleSelectionScreen, LoginStudents, RegisterStudents, and Register
    // All login/registration will be handled by LoginScreen or external.
    object PreviewForm : Screen("preview_form/{formId}")
    object ResponseList : Screen("response_list/{formId}/{formTitle}") // For response list
    object FormResponseDetail : Screen("form_response_detail/{responseId}") // For response detail
    object ResponseDetail : Screen("response_detail/{responseId}")

}