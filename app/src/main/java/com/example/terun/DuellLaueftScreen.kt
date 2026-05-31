

package com.example.terun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ActiveGreen = Color(0xFF56C596)
val EnemyRed = Color(0xFFFF5A5A)

@Composable
fun DuelLaeuftScreen(
    onGiveUpClicked: () -> Unit
) {
    Scaffold(
        bottomBar = {
            TeRunBottomNavigationRunning(
                selectedItem = "Karte"
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            TeRunRunningTopBar()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MapDark)
            ) {
                TeRunRunningMapBackground()

                MapSpotRunning(0.25f, 0.22f, ActiveGreen)
                MapSpotRunning(0.58f, 0.40f, ActiveGreen)
                MapSpotRunning(0.72f, 0.62f, SpotBlue)

                PlayerMarkerRunning(0.52f, 0.47f, SpotOrange)
                PlayerMarkerRunning(0.60f, 0.43f, EnemyRed)

                FinishFlagRunning(0.80f, 0.80f)

                DuelInfoPanel()
            }

            Button(
                onClick = onGiveUpClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnemyRed
                )
            ) {
                Text(
                    text = "Aufgeben",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TeRunRunningTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(TopBarDark)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Duell läuft",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .background(
                    ActiveGreen.copy(alpha = 0.95f),
                    RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "2 / 3 Spots",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TeRunRunningMapBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grid = Color.White.copy(alpha = 0.08f)

        drawLine(grid, Offset(size.width * 0.30f, 0f), Offset(size.width * 0.30f, size.height), 3f)
        drawLine(grid, Offset(size.width * 0.64f, 0f), Offset(size.width * 0.64f, size.height), 3f)
        drawLine(grid, Offset(0f, size.height * 0.36f), Offset(size.width, size.height * 0.36f), 3f)
        drawLine(grid, Offset(0f, size.height * 0.70f), Offset(size.width, size.height * 0.70f), 3f)

        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, size.height * 0.32f),
            end = Offset(size.width, size.height * 0.32f),
            strokeWidth = 14f
        )

        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(size.width * 0.18f, size.height * 0.82f),
            end = Offset(size.width * 0.78f, size.height * 0.22f),
            strokeWidth = 10f
        )
    }
}

@Composable
fun MapSpotRunning(
    x: Float,
    y: Float,
    color: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * x, size.height * y)

        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = 24f,
            center = center
        )

        drawCircle(
            color = color,
            radius = 10f,
            center = center
        )

        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y + 22f),
            strokeWidth = 4f
        )
    }
}

@Composable
fun PlayerMarkerRunning(
    x: Float,
    y: Float,
    color: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * x, size.height * y)

        drawCircle(
            color = Color.White,
            radius = 18f,
            center = center
        )

        drawCircle(
            color = color,
            radius = 11f,
            center = center
        )

        drawCircle(
            color = Color.White,
            radius = 5f,
            center = center
        )
    }
}

@Composable
fun FinishFlagRunning(
    x: Float,
    y: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val base = Offset(size.width * x, size.height * y)

        drawLine(
            color = Color.White,
            start = base,
            end = Offset(base.x, base.y + 35f),
            strokeWidth = 4f
        )

        drawLine(
            color = EnemyRed,
            start = base,
            end = Offset(base.x + 22f, base.y + 9f),
            strokeWidth = 8f
        )
    }
}

@Composable
fun BoxScope.DuelInfoPanel() {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .fillMaxWidth()
            .background(
                Color(0xFF0B1118).copy(alpha = 0.88f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Aktives Duell",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        DuelStatusRow(
            label = "Team Blau",
            value = "2 Spots",
            color = ActiveGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        DuelStatusRow(
            label = "Team Rot",
            value = "1 Spot",
            color = EnemyRed
        )

        Spacer(modifier = Modifier.height(8.dp))

        DuelStatusRow(
            label = "Nächster Spot",
            value = "180 m",
            color = SpotBlue
        )
    }
}

@Composable
fun DuelStatusRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(9.dp)) {
            drawCircle(color = color)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TeRunBottomNavigationRunning(
    selectedItem: String
) {
    NavigationBar(
        containerColor = BottomBarDark,
        modifier = Modifier.height(70.dp)
    ) {
        FooterItemRunning(
            label = "Karte",
            selected = selectedItem == "Karte",
            icon = { color -> MapFooterIconRunning(color) }
        )

        FooterItemRunning(
            label = "Duelle",
            selected = selectedItem == "Duelle",
            icon = { color -> DuelFooterIconRunning(color) }
        )

        FooterItemRunning(
            label = "Rangliste",
            selected = selectedItem == "Rangliste",
            icon = { color -> TrophyFooterIconRunning(color) }
        )

        FooterItemRunning(
            label = "Profil",
            selected = selectedItem == "Profil",
            icon = { color -> ProfileFooterIconRunning(color) }
        )
    }
}

@Composable
fun RowScope.FooterItemRunning(
    label: String,
    selected: Boolean,
    icon: @Composable (Color) -> Unit
) {
    val itemColor =
        if (selected) TeRunBlue
        else Color.White.copy(alpha = 0.35f)

    NavigationBarItem(
        selected = selected,
        onClick = {},
        icon = {
            icon(itemColor)
        },
        label = {
            Text(
                text = label,
                color = itemColor,
                fontSize = 10.sp
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent
        )
    )
}

@Composable
fun MapFooterIconRunning(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawLine(color, Offset(size.width * 0.18f, size.height * 0.20f), Offset(size.width * 0.18f, size.height * 0.82f), 3f)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.10f), Offset(size.width * 0.50f, size.height * 0.72f), 3f)
        drawLine(color, Offset(size.width * 0.82f, size.height * 0.18f), Offset(size.width * 0.82f, size.height * 0.80f), 3f)

        drawLine(color, Offset(size.width * 0.18f, size.height * 0.20f), Offset(size.width * 0.50f, size.height * 0.10f), 3f)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.10f), Offset(size.width * 0.82f, size.height * 0.18f), 3f)

        drawLine(color, Offset(size.width * 0.18f, size.height * 0.82f), Offset(size.width * 0.50f, size.height * 0.72f), 3f)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.72f), Offset(size.width * 0.82f, size.height * 0.80f), 3f)
    }
}

@Composable
fun DuelFooterIconRunning(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawLine(
            color = color,
            start = Offset(size.width * 0.25f, size.height * 0.75f),
            end = Offset(size.width * 0.75f, size.height * 0.25f),
            strokeWidth = 3f
        )

        drawCircle(
            color = color,
            radius = 4f,
            center = Offset(size.width * 0.25f, size.height * 0.75f)
        )

        drawCircle(
            color = color,
            radius = 4f,
            center = Offset(size.width * 0.75f, size.height * 0.25f)
        )
    }
}

@Composable
fun TrophyFooterIconRunning(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = Stroke(width = 3f)

        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.32f, size.height * 0.18f),
            size = Size(size.width * 0.36f, size.height * 0.34f),
            style = stroke
        )

        drawLine(color, Offset(size.width * 0.50f, size.height * 0.52f), Offset(size.width * 0.50f, size.height * 0.72f), 3f)
        drawLine(color, Offset(size.width * 0.35f, size.height * 0.82f), Offset(size.width * 0.65f, size.height * 0.82f), 3f)

        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.12f, size.height * 0.22f),
            size = Size(size.width * 0.28f, size.height * 0.30f),
            style = stroke
        )

        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.60f, size.height * 0.22f),
            size = Size(size.width * 0.28f, size.height * 0.30f),
            style = stroke
        )
    }
}

@Composable
fun ProfileFooterIconRunning(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawCircle(
            color = color,
            radius = 5f,
            center = Offset(size.width * 0.50f, size.height * 0.30f),
            style = Stroke(width = 3f)
        )

        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.24f, size.height * 0.52f),
            size = Size(size.width * 0.52f, size.height * 0.42f),
            style = Stroke(width = 3f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DuelLaeuftScreenPreview() {
    DuelLaeuftScreen(
        onGiveUpClicked = {}
    )
}