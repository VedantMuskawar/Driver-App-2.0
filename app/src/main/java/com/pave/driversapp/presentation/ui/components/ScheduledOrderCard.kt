package com.pave.driversapp.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pave.driversapp.domain.model.OrderStatus
import com.pave.driversapp.domain.model.ScheduledOrder

@Composable
fun ScheduledOrderCard(
    scheduledOrder: ScheduledOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scheduledOrder.orderId,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                StatusChip(status = scheduledOrder.status)
            }
            
            // Client and Address
            InfoRow(
                icon = null,
                iconColor = Color(0xFF4CAF50),
                label = "Client",
                value = scheduledOrder.clientName
            )
            
            InfoRow(
                icon = null,
                iconColor = Color(0xFFFF9800),
                label = "Address",
                value = scheduledOrder.address
            )
            
            // Product and Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoRow(
                    icon = null,
                    iconColor = Color(0xFF2196F3),
                    label = "Product",
                    value = scheduledOrder.productName,
                    modifier = Modifier.weight(1f)
                )
                
                InfoRow(
                    icon = null,
                    iconColor = Color(0xFF9C27B0),
                    label = "Qty",
                    value = "${scheduledOrder.productQuant}",
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Vehicle and Driver
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoRow(
                    icon = null,
                    iconColor = Color(0xFF4CAF50),
                    label = "Vehicle",
                    value = scheduledOrder.vehicleNumber,
                    modifier = Modifier.weight(1f)
                )
                
                InfoRow(
                    icon = null,
                    iconColor = Color(0xFF2196F3),
                    label = "Driver",
                    value = scheduledOrder.driverName,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Time and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoRow(
                    icon = null,
                    iconColor = Color(0xFF9C27B0),
                    label = "Time",
                    value = scheduledOrder.dispatchTimeRange,
                    modifier = Modifier.weight(1f)
                )
                
                InfoRow(
                    icon = null,
                    iconColor = Color(0xFF4CAF50),
                    label = "Amount",
                    value = "₹${String.format("%.2f", scheduledOrder.totalAmount)}",
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Payment Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment: ${scheduledOrder.paySchedule}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                if (scheduledOrder.paymentStatus) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "✓",
                            color = Color(0xFF4CAF50),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paid",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⏳",
                            color = Color(0xFFFF9800),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pending",
                            color = Color(0xFFFF9800),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (status) {
        OrderStatus.PENDING -> Triple(Color(0xFFFF9800), Color.White, "Pending")
        OrderStatus.READY_FOR_DISPATCH -> Triple(Color(0xFF2196F3), Color.White, "Ready")
        OrderStatus.DISPATCHED -> Triple(Color(0xFF9C27B0), Color.White, "Dispatched")
        OrderStatus.DELIVERED -> Triple(Color(0xFF4CAF50), Color.White, "Delivered")
        OrderStatus.CANCELLED -> Triple(Color(0xFFF44336), Color.White, "Cancelled")
    }
    
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector?,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Column {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
