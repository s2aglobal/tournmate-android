package com.s2aglobal.tournmate.ui.screen.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AppAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatePlayerSheet(
    playerName: String,
    onSubmit: (stars: Int, comment: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedStars by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Rate Sportsmanship",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "How was your experience playing with $playerName?",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { selectedStars = star },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star stars",
                            modifier = Modifier.size(36.dp),
                            tint = if (star <= selectedStars) Color(0xFFFFCC00) else Color.LightGray,
                        )
                    }
                }
            }

            if (selectedStars > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (selectedStars) {
                        1 -> "Poor sportsmanship"
                        2 -> "Below average"
                        3 -> "Average"
                        4 -> "Good sportsmanship"
                        5 -> "Excellent sportsmanship"
                        else -> ""
                    },
                    fontSize = 12.sp,
                    color = AppAccent,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.take(200) },
                label = { Text("Comment (optional)") },
                placeholder = { Text("Share your experience...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
            )

            Text(
                "${comment.length}/200",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSubmit(selectedStars, comment.ifBlank { null }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = selectedStars > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppAccent,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFD1D1D6),
                    disabledContentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "Submit Rating",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
