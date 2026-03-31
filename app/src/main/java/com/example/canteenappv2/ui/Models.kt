package com.example.canteenappv2.ui

data class Canteen(val id: Int, val name: String)

data class FoodItem(val id: Int, val name: String, val price: Double, val canteenId: Int)

data class CartItem(val foodItem: FoodItem, val quantity: Int)

enum class OrderStatus {
    PENDING, PREPARING, READY
}

data class OrderItem(
    val token: Int,
    val items: List<CartItem>,
    val status: OrderStatus = OrderStatus.PENDING,
    val canteenName: String
)
