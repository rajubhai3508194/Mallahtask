package com.example
import com.example.viewmodel.ViewModelFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AdMobManager
import com.example.data.AppDatabase
import com.example.data.TaskMallahRepository
import com.example.ui.TaskMallahApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TaskMallahViewModel
import com.example.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Firebase manually with safe fallback options to avoid startup crashes when google-services.json is missing
    try {
      if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
        val options = com.google.firebase.FirebaseOptions.Builder()
          .setApplicationId("1:1234567890:android:abcdef")
          .setApiKey("AIzaSyDummyKeyForInitializationOnly")
          .setProjectId("taskmallah-prod")
          .build()
        com.google.firebase.FirebaseApp.initializeApp(this, options)
      }
    } catch (e: Exception) {
      android.util.Log.e("FirebaseInit", "Failed to initialize FirebaseApp: ${e.message}")
    }

    // Initialize AdMob
    AdMobManager.initialize(this)

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = TaskMallahRepository(applicationContext, database)
    val factory = ViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[TaskMallahViewModel::class.java]

    setContent {
      MyApplicationTheme {
        TaskMallahApp(viewModel = viewModel)
      }
    }
  }
}
