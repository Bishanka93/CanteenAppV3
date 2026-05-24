package com.example.canteenappv2.database

import androidx.compose.runtime.mutableStateListOf
import com.example.canteenappv2.ui.Canteen
import com.example.canteenappv2.ui.FoodItem
import com.example.canteenappv2.ui.OrderItem
import com.example.canteenappv2.ui.User

object Database {
    val canteens = mutableStateListOf(
        Canteen(1, "Canteen A"),
        Canteen(2, "Canteen B")
    )

    val foodItems = mutableStateListOf(
        FoodItem(1, "Chowmein", 50.0, 1, "chowmein"),
        FoodItem(2, "Fried Rice", 40.0, 1, "fried_rice"),
        FoodItem(3, "Veg Sandwich", 20.0, 1, "veg_sandwich"),
        FoodItem(4, "Veg Thali", 70.0, 2, "veg_thali"),
        FoodItem(5, "Masala Dosa", 90.0, 2, "masala_dosa"),
        FoodItem(6, "Chicken Thali", 100.0, 2, "chicken_thali")
    )
    
    val users = mutableStateListOf(
        User("Walter White", "DC2024BTE0093", "12345"),
        User("Canteen Manager", "STAFF_A", "admin123", isStaff = true, canteenId = 1),
        User("Main Admin", "ADMIN", "admin123", isAdmin = true)
    )

    val orders = mutableStateListOf<OrderItem>()

    private val availableTokens = mutableStateListOf<Int>()
    private var lastToken = 100

    fun getNextToken(): Int {
        return if (availableTokens.isNotEmpty()) {
            availableTokens.removeAt(0)
        } else {
            ++lastToken
        }
    }

    fun releaseToken(token: Int) {
        if (!availableTokens.contains(token)) {
            availableTokens.add(token)
            availableTokens.sort()
        }
    }
}
