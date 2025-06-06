package com.example.eform.navigation


sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login") // Untuk Guru
    object Register : Screen("register") // Untuk Guru
    object Dashboard : Screen("dashboard/{userIdentifier}") // Untuk Guru
    object DashboardStudents : Screen("dashboardstudents/{userIdentifier}") // Untuk Murid
    object CreateForm : Screen("create_form/{userIdentifier}") // Untuk Guru (perlu tahu siapa pembuatnya)
    object HistoryForm : Screen("history_form/{userIdentifier}") // Pertimbangkan apakah perlu {userIdentifier}
    object FavoriteForms : Screen("favorite_forms/{userIdentifier}") // Untuk Guru dan Murid
    object EditForm : Screen("edit_form/{formId}") // Perlu {formId}, mungkin juga {userIdentifier} jika hanya pemilik yg bisa edit
    object FormAnswer: Screen("form_answer/{formId}/{userIdentifier}") // Untuk Murid (perlu tahu siapa yg mengisi)
    object Profile : Screen("profile/{userIdentifier}") // Untuk Guru dan Murid
    object Notification : Screen("notification/{userIdentifier}") // Untuk Guru dan Murid
    object Role : Screen("role")
    object LoginStudents : Screen("loginstudents") // Untuk Murid
    object RegisterStudents : Screen("registerstudents") // Untuk Murid
    object PreviewForm : Screen("preview_form/{formId}")
    object ResponseList : Screen("response_list/{formId}/{formTitle}") // Untuk daftar respons
    object FormResponseDetail : Screen("form_response_detail/{responseId}") // Untuk detail respons

}



//sealed class Screen(val route: String) {
//    object Splash : Screen("splash")
//    object Onboarding : Screen("onboarding")
//    object Login : Screen("login")
//    object Register : Screen("register")
//    object Dashboard : Screen("dashboard/{userIdentifier}") // UBAH DI SINI
//    object DashboardStudents : Screen("dashboardstudents/{userIdentifier}") // UBAH DI SINI
//    object CreateForm : Screen("create_form/{userIdentifier}")
//    object HistoryForm : Screen("history_form")
//    object FavoriteForms : Screen("favorite_forms/{userIdentifier}")
//    object EditForm : Screen("edit_form/{formId}")
//    object FormAnswer: Screen("form_answer/{formId}/{userIdentifier}")
//    object Profile : Screen("profile/{userIdentifier}") // UBAH DI SINI
//    object Notification : Screen("notification/{userIdentifier}")
//    object Role : Screen("role")
//    object LoginStudents : Screen("loginstudents")
//    object RegisterStudents : Screen("registerstudents")
//    object PreviewForm : Screen("preview_form/{formId}")
//}