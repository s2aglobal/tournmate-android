package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.ui.theme.BrandPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleScoreEntryScreen(
    match: Match,
    isCreator: Boolean,
    onSubmit: (scoreA: Int, scoreB: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var scoreA by remember { mutableStateOf("") }
    var scoreB by remember { mutableStateOf("") }
    var showCelebration by remember { mutableStateOf(false) }

    if (showCelebration) {
        val a = scoreA.toIntOrNull() ?: 0
        val b = scoreB.toIntOrNull() ?: 0
        val winnerName = if (a > b) match.teamA.player.name else match.teamB.player.name
        WinnerCelebrationScreen(
            winnerName = winnerName,
            scoreLine = "$a - $b",
            isCreator = isCreator,
            onDone = {
                onSubmit(a, b)
                onDismiss()
            },
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("ENTER SCORE", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(match.teamA.player.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    match.teamA.partner?.let { Text("& ${it.name}", fontSize = 11.sp, color = Color.Gray) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = scoreA, onValueChange = { scoreA = it },
                        modifier = Modifier.width(80.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                    )
                }

                Text("VS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(match.teamB.player.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    match.teamB.partner?.let { Text("& ${it.name}", fontSize = 11.sp, color = Color.Gray) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = scoreB, onValueChange = { scoreB = it },
                        modifier = Modifier.width(80.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            val canSubmit = (scoreA.toIntOrNull() ?: -1) >= 0 && (scoreB.toIntOrNull() ?: -1) >= 0 && scoreA != scoreB
            Button(
                onClick = { showCelebration = true },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            ) {
                Text(
                    if (isCreator) "FINALIZE SCORE" else "SUBMIT SCORE",
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                )
            }
        }
    }
}
