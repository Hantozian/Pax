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
import com.example.paxik.data.NodeType
import com.example.paxik.ui.theme.PaxikTheme
import com.example.paxik.ui_config.notes.GraphScreen
import com.example.paxik.ui_config.notes.NoteEditorScreen
import com.example.paxik.ui_config.notes.NotesRoute
import com.example.paxik.ui_config.notes.NodeRef
import com.example.paxik.ui_config.notes.NotesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaxikTheme {
                val viewModel: NotesViewModel = viewModel()
                val navController = rememberNavController()

                val openNode: (NodeRef) -> Unit = { ref ->
                    when (ref.type) {
                        NodeType.NOTE -> navController.navigate("note/${ref.id}")
                        else -> {
                            viewModel.reveal(ref)
                            navController.popBackStack("notes", false)
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "notes") {
                    composable("notes") {
                        NotesRoute(
                            viewModel = viewModel,
                            onOpenNote = { id -> navController.navigate("note/$id") },
                            onOpenNode = openNode,
                            onOpenGraph = { navController.navigate("graph") }
                        )
                    }
                    composable("graph") {
                        val activeSpaceId by viewModel.activeSpaceId.collectAsState()
                        GraphScreen(
                            activeSpaceId = activeSpaceId,
                            onOpenNode = openNode,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "note/{noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                    ) {
                        NoteEditorScreen(
                            onClose = { navController.popBackStack() },
                            onOpenNode = openNode
                        )
                    }
                }
            }
        }
    }
}