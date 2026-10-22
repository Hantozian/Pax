package com.example.paxik.ui_config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paxik.data.Note

@Composable
 fun Greet(note: Note, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ){
        Box(
            modifier = Modifier.background(Color.Gray, shape = RoundedCornerShape(18.dp)).size(310.dp, 560.dp),
        ) {
            Card(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = note.title, fontSize = 20.sp)
                    Text(text = note.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }




        }
    }




@Composable
 fun add() {
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.BottomCenter,
    ){
        Box(
            modifier = Modifier.background(Color.Gray, shape = RoundedCornerShape(25.dp)).size(310.dp, 60.dp),
        ) {

        }
    }


}

@Composable
 fun menu() {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp), contentAlignment = Alignment.TopCenter,
    ){
        Box(
            modifier = Modifier.background(Color.Gray, shape = RoundedCornerShape(25.dp)).size(310.dp, 60.dp),
        ) {

        }
    }


}
