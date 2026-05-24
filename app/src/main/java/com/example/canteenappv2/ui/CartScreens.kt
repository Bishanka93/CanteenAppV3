package com.example.canteenappv2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.canteenappv2.database.MySQLDatabase
import kotlinx.coroutines.launch

@Composable
fun CartScreen(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    onConfirmOrder: suspend (String, List<CartItem>) -> Int,
    onDone: () -> Unit
) {
    var cartStep by remember { mutableIntStateOf(1) }
    var orderToken by remember { mutableIntStateOf(0) }
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // LaunchedEffect already runs in a coroutine — no scope.launch needed
    LaunchedEffect(Unit) {
        canteens = MySQLDatabase.getAllCanteens()
    }

    val groupedItems = remember(cartItems) {
        cartItems.groupBy { it.foodItem.canteenId }
    }

    // canteens is now in the key so this recomputes once the DB call resolves
    val canteensInCart = remember(groupedItems, canteens) {
        groupedItems.keys.mapNotNull { id -> canteens.find { it.id == id } }
    }

    var selectedCanteenId by remember(canteensInCart) {
        mutableIntStateOf(canteensInCart.firstOrNull()?.id ?: -1)
    }

    val selectedCanteenItems = groupedItems[selectedCanteenId] ?: emptyList()
    val selectedCanteenName = canteensInCart.find { it.id == selectedCanteenId }?.name ?: ""

    when (cartStep) {
        1 -> CartItemsLayout(
            items = cartItems,
            groupedItems = groupedItems,
            canteensInCart = canteensInCart,
            selectedCanteenId = selectedCanteenId,
            onCanteenSelect = { selectedCanteenId = it },
            onContinue = { cartStep = 2 },
            modifier = modifier
        )
        2 -> ConfirmationLayout(
            onConfirm = {
                scope.launch {
                    val token = onConfirmOrder(selectedCanteenName, selectedCanteenItems)
                    orderToken = token
                    cartStep = 3
                }
            },
            onBack = { cartStep = 1 },
            modifier = modifier
        )
        3 -> DoneLayout(
            token = orderToken,
            onFinish = onDone,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartItemsLayout(
    items: List<CartItem>,
    groupedItems: Map<Int, List<CartItem>>,
    canteensInCart: List<Canteen>,
    selectedCanteenId: Int,
    onCanteenSelect: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedItems = groupedItems[selectedCanteenId] ?: emptyList()
    val totalPrice = selectedItems.sumOf { it.foodItem.price * it.quantity }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Cart", fontWeight = FontWeight.Bold) }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is empty",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add some delicious food to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                if (canteensInCart.size > 1) {
                    Text(
                        text = "Select Canteen to Order From:",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ScrollableTabRow(
                        selectedTabIndex = canteensInCart.indexOfFirst { it.id == selectedCanteenId }.coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        canteensInCart.forEach { canteen ->
                            Tab(
                                selected = selectedCanteenId == canteen.id,
                                onClick = { onCanteenSelect(canteen.id) },
                                text = { Text(canteen.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(selectedItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        item.foodItem.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Qty: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    "Rs ${String.format("%.2f", item.foodItem.price * item.quantity)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Subtotal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Rs ${String.format("%.2f", totalPrice)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val canteenName = canteensInCart.find { it.id == selectedCanteenId }?.name ?: ""
                        Text("Canteen: $canteenName", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedItems.isNotEmpty(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        "Continue with this Order",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (canteensInCart.size > 1) {
                    Text(
                        text = "Note: You have items from other canteens. You can order them separately.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationLayout(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Confirm your order details?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Confirm Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DoneLayout(token: Int, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Done!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Your order has been placed successfully.")
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Token Number", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = token.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Return to Home", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}