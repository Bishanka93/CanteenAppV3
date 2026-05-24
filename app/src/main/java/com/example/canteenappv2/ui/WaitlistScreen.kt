package com.example.canteenappv2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.canteenappv2.database.MySQLDatabase
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitlistScreen(modifier: Modifier = Modifier) {
    var orders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }

    // Poll all non-completed orders every 3 seconds so the list stays live
    LaunchedEffect(Unit) {
        while (true) {
            orders = MySQLDatabase.getAllOrders()
                .filter { it.status != OrderStatus.COMPLETED }
            delay(3000)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wait List", fontWeight = FontWeight.Bold) }) },
        modifier = modifier
    ) { padding ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No active orders", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = true
            ) {
                items(orders) { order ->
                    OrderWidget(order)
                }
            }
        }
    }
}

@Composable
fun OrderWidget(order: OrderItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Token: ${order.token}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = order.canteenName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (order.status) {
                        OrderStatus.PENDING -> MaterialTheme.colorScheme.errorContainer
                        OrderStatus.PREPARING -> MaterialTheme.colorScheme.secondaryContainer
                        OrderStatus.READY -> MaterialTheme.colorScheme.primaryContainer
                        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ) {
                    Text(
                        text = order.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (order.status) {
                            OrderStatus.PENDING -> MaterialTheme.colorScheme.onErrorContainer
                            OrderStatus.PREPARING -> MaterialTheme.colorScheme.onSecondaryContainer
                            OrderStatus.READY -> MaterialTheme.colorScheme.onPrimaryContainer
                            OrderStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            order.items.forEach { cartItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cartItem.foodItem.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "x ${cartItem.quantity}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}