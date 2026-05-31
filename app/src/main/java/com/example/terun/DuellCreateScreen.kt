package com.example.terun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val MapDark = Color(0xFF1B2A3A)
val TopBarDark = Color(0xFF101820)
val BottomBarDark = Color(0xFF0B1118)

val SpotBlue = Color(0xFF4A8DFF)
val SpotOrange = Color(0xFFFFB547)

@Composable
fun DuelCreateScreen(
    onStartDuelClicked: () -> Unit
) {
    Scaffold(
        bottomBar = {
            TeRunBottomNavigation()
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {

            TeRunTopBar()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MapDark)
            ) {

                TeRunMapBackground()

                MapSpot(
                    x = 0.25f,
                    y = 0.22f,
                    color = SpotBlue
                )

                MapSpot(
                    x = 0.58f,
                    y = 0.40f,
                    color = SpotBlue
                )

                MapSpot(
                    x = 0.72f,
                    y = 0.62f,
                    color = SpotBlue
                )

                MyPositionMarker(
                    x = 0.44f,
                    y = 0.48f
                )

                FinishFlag(
                    x = 0.80f,
                    y = 0.80f
                )
            }

            Button(
                onClick = onStartDuelClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeRunBlue
                )
            ) {
                Text(
                    text = "+ Neues Duell starten",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TeRunTopBar() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(TopBarDark)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "TeRun",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .background(
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Kein Duell aktiv",
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun TeRunMapBackground() {

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val grid = Color.White.copy(alpha = 0.08f)

        drawLine(
            color = grid,
            start = Offset(size.width * 0.30f, 0f),
            end = Offset(size.width * 0.30f, size.height),
            strokeWidth = 3f
        )

        drawLine(
            color = grid,
            start = Offset(size.width * 0.64f, 0f),
            end = Offset(size.width * 0.64f, size.height),
            strokeWidth = 3f
        )

        drawLine(
            color = grid,
            start = Offset(0f, size.height * 0.36f),
            end = Offset(size.width, size.height * 0.36f),
            strokeWidth = 3f
        )

        drawLine(
            color = grid,
            start = Offset(0f, size.height * 0.70f),
            end = Offset(size.width, size.height * 0.70f),
            strokeWidth = 3f
        )
    }
}

@Composable
fun MapSpot(
    x: Float,
    y: Float,
    color: Color
) {

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val center = Offset(
            size.width * x,
            size.height * y
        )

        drawCircle(
            color = color,
            radius = 10f,
            center = center
        )

        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y + 20f),
            strokeWidth = 4f
        )
    }
}

@Composable
fun MyPositionMarker(
    x: Float,
    y: Float
) {

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val center = Offset(
            size.width * x,
            size.height * y
        )

        drawCircle(
            color = Color.White,
            radius = 18f,
            center = center
        )

        drawCircle(
            color = SpotOrange,
            radius = 11f,
            center = center
        )
    }
}

@Composable
fun FinishFlag(
    x: Float,
    y: Float
) {

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val base = Offset(
            size.width * x,
            size.height * y
        )

        drawLine(
            color = Color.White,
            start = base,
            end = Offset(base.x, base.y + 35f),
            strokeWidth = 4f
        )

        drawLine(
            color = Color.Red,
            start = base,
            end = Offset(base.x + 20f, base.y + 8f),
            strokeWidth = 8f
        )
    }
}

@Composable
fun TeRunBottomNavigation() {

    NavigationBar(
        containerColor = BottomBarDark
    ) {

        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Text("⌖") },
            label = { Text("Karte") }
        )

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Text("⚔") },
            label = { Text("Duelle") }
        )

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Text("★") },
            label = { Text("Rangliste") }
        )

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Text("●") },
            label = { Text("Profil") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DuelCreateScreenPreview() {

    DuelCreateScreen(
        onStartDuelClicked = {}
    )
}