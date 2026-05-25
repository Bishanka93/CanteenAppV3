package com.example.canteenappv2.ui

data class Canteen(val id: Int, var name: String)

data class FoodItem(
    val id: Int,
    var name: String,
    var price: Double,
    val canteenId: Int,
    var imageName: String? = null,
    var isAvailable: Boolean = true
)

data class CartItem(val foodItem: FoodItem, val quantity: Int)

enum class OrderStatus {
    PENDING, PREPARING, READY, COMPLETED
}

data class OrderItem(
    val token: Int,
    val items: List<CartItem>,
    var status: OrderStatus = OrderStatus.PENDING,
    val canteenName: String,
    val userRollNo: String = ""
)

data class User(
    val name: String,
    val rollNo: String,
    val password: String,
    val isStaff: Boolean = false,
    val isAdmin: Boolean = false,
    val canteenId: Int? = null
)
