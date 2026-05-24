package com.example.canteenappv2.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.canteenappv2.database.MySQLDatabase

@Composable
fun CanteensScreen(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    onAddToCart: (FoodItem, Int) -> Unit,
    selectedCanteen: Canteen?,
    onCanteenSelected: (Canteen?) -> Unit,
    selectedFoodItem: FoodItem?,
    onFoodItemSelected: (FoodItem?) -> Unit
) {
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }
    var foodItems by remember { mutableStateOf<List<FoodItem>>(emptyList()) }

    // LaunchedEffect already runs in a coroutine scope — no need for scope.launch inside
    LaunchedEffect(Unit) {
        canteens = MySQLDatabase.getAllCanteens()
        foodItems = MySQLDatabase.getAllFoodItems()
    }

    when {
        selectedFoodItem != null -> {
            val initialQuantity = cartItems.find { it.foodItem.id == selectedFoodItem.id }?.quantity ?: 0
            FoodDetailsLayout(
                foodItem = selectedFoodItem,
                initialQuantity = initialQuantity,
                onBack = { onFoodItemSelected(null) },
                onAddToCart = { qty ->
                    onAddToCart(selectedFoodItem, qty)
                    onFoodItemSelected(null)
                },
                modifier = modifier
            )
        }
        selectedCanteen != null -> {
            FoodListLayout(
                canteen = selectedCanteen,
                foodItems = foodItems.filter { it.canteenId == selectedCanteen.id },
                onBack = { onCanteenSelected(null) },
                onFoodClick = { onFoodItemSelected(it) },
                modifier = modifier
            )
        }
        else -> {
            CanteenListLayout(
                canteens = canteens,
                onCanteenClick = { onCanteenSelected(it) },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenListLayout(
    canteens: List<Canteen>,
    onCanteenClick: (Canteen) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Canteens", fontWeight = FontWeight.Bold) }) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(canteens) { canteen ->
                CanteenWidget(canteen = canteen, onClick = { onCanteenClick(canteen) })
            }
        }
    }
}

@Composable
fun CanteenWidget(canteen: Canteen, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = canteen.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListLayout(
    canteen: Canteen,
    foodItems: List<FoodItem>,
    onBack: () -> Unit,
    onFoodClick: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Food") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = canteen.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(foodItems) { foodItem ->
                    FoodIconItem(foodItem = foodItem, onClick = { onFoodClick(foodItem) })
                }
            }
        }
    }
}

@Composable
fun FoodIconItem(foodItem: FoodItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageModel = remember(foodItem.imageName) {
        if (foodItem.imageName?.startsWith("content://") == true) {
            Uri.parse(foodItem.imageName)
        } else {
            val resId = context.resources.getIdentifier(foodItem.imageName, "drawable", context.packageName)
            if (resId != 0) resId else null
        }
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(enabled = foodItem.isAvailable) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (foodItem.isAvailable) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = foodItem.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        "No image found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (!foodItem.isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "OUT OF STOCK",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = foodItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (foodItem.isAvailable) Color.Unspecified else Color.Gray
                )
                Text(
                    text = "Rs ${foodItem.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (foodItem.isAvailable) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailsLayout(
    foodItem: FoodItem,
    initialQuantity: Int,
    onBack: () -> Unit,
    onAddToCart: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantity by remember { mutableIntStateOf(if (initialQuantity == 0) 1 else initialQuantity) }
    val context = LocalContext.current
    val imageModel = remember(foodItem.imageName) {
        if (foodItem.imageName?.startsWith("content://") == true) {
            Uri.parse(foodItem.imageName)
        } else {
            val resId = context.resources.getIdentifier(foodItem.imageName, "drawable", context.packageName)
            if (resId != 0) resId else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Details") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .height(250.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = foodItem.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("No image found", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = foodItem.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Rs ${foodItem.price}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (foodItem.isAvailable) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        IconButton(onClick = { if (quantity > 1) quantity-- }) {
                            Text("-", style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(
                            text = quantity.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        IconButton(onClick = { quantity++ }) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onAddToCart(quantity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (initialQuantity > 0) "Update Cart" else "Add to Cart",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Currently Out of Stock",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Go Back")
                }
            }
        }
    }
}