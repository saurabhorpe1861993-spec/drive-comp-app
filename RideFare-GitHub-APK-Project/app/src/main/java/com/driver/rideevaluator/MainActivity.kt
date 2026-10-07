package com.driver.rideevaluator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driver.rideevaluator.data.RideEvaluatorPrefs
import com.driver.rideevaluator.service.RideMonitorService

data class RideAlertData(
    val id: String,
    val totalFare: Double,
    val pickupKm: Double,
    val dropKm: Double
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF10B981),
                    background = Color(0xFF020617),
                    surface = Color(0xFF0F172A),
                    onPrimary = Color.Black
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FullScreenDriverAlertApp()
                }
            }
        }
    }
}

@Composable
fun FullScreenDriverAlertApp() {
    val context = LocalContext.current
    val prefs = remember { RideEvaluatorPrefs(context) }
    var isAccepted by remember { mutableStateOf(false) }

    var currentRide by remember {
        mutableStateOf(
            RideAlertData("offer-1", 145.0, 1.2, 5.6)
        )
    }

    val totalDistance = currentRide.pickupKm + currentRide.dropKm
    val perKmRate = if (totalDistance > 0) currentRide.totalFare / totalDistance else 0.0

    val rateMatches = perKmRate >= prefs.minPerKmRate
    val pickupMatches = currentRide.pickupKm <= prefs.maxPickupDistanceKm
    val fareMatches = currentRide.totalFare >= prefs.minTotalFare
    val isGoodRide = rateMatches && pickupMatches && fareMatches

    if (isAccepted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = "RIDE ACCEPTED!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "Total Fare: ₹${currentRide.totalFare.toInt()} · Pick up in ${currentRide.pickupKm} km",
                    fontSize = 16.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isAccepted = false
                        currentRide = RideAlertData("offer-2", 390.0, 1.8, 16.5)
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Complete Trip & Return", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isGoodRide) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFF450A0A).copy(alpha = 0.35f))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // GREEN / RED STATUS BADGE
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = if (isGoodRide) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, if (isGoodRide) Color(0xFF10B981) else Color(0xFFEF4444)),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGoodRide) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isGoodRide) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGoodRide) "PROFITABLE RIDE" else "SUB-PAR RIDE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = if (isGoodRide) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            // ONLY 3 METRICS: FARE, RATE, PICKUP & DROP
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL RIDE FARE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 2.sp)
                    Text("₹${currentRide.totalFare.toInt()}", fontSize = 58.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isGoodRide) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Text("EARNING RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
                        Text(
                            text = String.format("₹%.1f / KM", perKmRate),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isGoodRide) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp)
                        ) {
                            Text("PICKUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text("${currentRide.pickupKm} KM", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp)
                        ) {
                            Text("DROP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text("${currentRide.dropKm} KM", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }

            // GIANT ACCEPT BUTTON (80dp height)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        isAccepted = true
                        RideMonitorService.acceptCurrentRideOnScreen(context)
                        Toast.makeText(context, "Ride Accepted!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGoodRide) Color(0xFF10B981) else Color(0xFF059669)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("ACCEPT RIDE", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.Black, letterSpacing = 1.sp)
                }

                OutlinedButton(
                    onClick = {
                        currentRide = RideAlertData("offer-3", 130.0, 4.8, 7.2)
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text("Skip / Pass Offer", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}
