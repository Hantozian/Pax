package com.example.paxik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paxik.ui.theme.PaxikTheme
import com.example.paxik.ui_config.notes.GraphScreen
import com.example.paxik.ui_config.notes.NoteEditorScreen
import com.example.paxik.ui_config.notes.NotesRoute
import com.example.paxik.ui_config.notes.NotesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaxikTheme {
                val viewModel: NotesViewModel = viewModel()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "notes") {
                    composable("notes") {
                        NotesRoute(
                            viewModel = viewModel,
                            onOpenNote = { id -> navController.navigate("note/$id") },
                            onOpenGraph = { navController.navigate("graph") }
                        )
                    }
                    composable("graph") {
                        val spaceNotes by viewModel.spaceNotes.collectAsState()
                        val spaceLinks by viewModel.spaceLinks.collectAsState()
                        val spaceName by viewModel.activeSpaceName.collectAsState()
                        GraphScreen(
                            spaceName = spaceName,
                            notes = spaceNotes,
                            links = spaceLinks,
                            onOpenNote = { id -> navController.navigate("note/$id") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "note/{noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                    ) {
                        NoteEditorScreen(
                            onClose = { navController.popBackStack() },
                            onOpenNote = { id -> navController.navigate("note/$id") }
                        )
                    }
                }
            }
        }
    }
}