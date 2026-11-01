package com.example.paxik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paxik.ui.theme.PaxikTheme
import com.example.paxik.ui_config.notes.NotesScreen
import com.example.paxik.ui_config.notes.NotesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaxikTheme {
                val viewModel: NotesViewModel = viewModel()
                val notes by viewModel.notes.collectAsState()
                NotesScreen(
                    notes = notes,
                    onAddNote = viewModel::addNote,
                    onEditNote = { id, title, text -> viewModel.saveNote(id, title, text) },
                    onDeleteNote = viewModel::deleteNote
                )
            }
        }
    }
}