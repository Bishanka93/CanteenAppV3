package com.example.canteenappv2.ui

object Database {
    val canteens = listOf(
        Canteen(1, "Canteen A"),
        Canteen(2, "Canteen B")
    )

    val foodItems = listOf(
        FoodItem(1, "Chowmein", 50.0, 1),
        FoodItem(2, "Fried Rice", 40.0, 1),
        FoodItem(3, "Veg Sandwich", 20.0, 1),
        FoodItem(4, "Veg Thali", 70.0, 2),
        FoodItem(5, "Masala Dosa", 90.0, 2),
        FoodItem(6, "Chicken Thali", 100.0, 2)
    )
}
