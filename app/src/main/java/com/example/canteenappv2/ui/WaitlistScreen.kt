package com.example.canteenappv2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitlistScreen(modifier: Modifier = Modifier, orders: List<OrderItem>) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredOrders = orders.filter { order ->
        order.token.toString().contains(searchQuery) ||
                order.canteenName.contains(searchQuery, ignoreCase = true) ||
                order.items.any { it.foodItem.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wait List", fontWeight = FontWeight.Bold) }) },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by Token, Canteen, or Food") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = true // Pile from bottom up
            ) {
                items(filteredOrders) { order ->
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    Text(
                        text = cartItem.foodItem.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "x ${cartItem.quantity}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
