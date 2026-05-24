package com.example.canteenappv2.database

import com.example.canteenappv2.ui.Canteen
import com.example.canteenappv2.ui.CartItem
import com.example.canteenappv2.ui.FoodItem
import com.example.canteenappv2.ui.OrderItem
import com.example.canteenappv2.ui.OrderStatus
import com.example.canteenappv2.ui.User

/**
 * Repository Pattern Implementation for Database Operations
 * Provides a clean abstraction layer for data access
 */
class DatabaseRepository {
    
    suspend fun loginUser(rollNo: String, password: String): User? {
        val user = MySQLDatabase.getUserByRollNo(rollNo)
        return if (user?.password == password) user else null
    }
    
    suspend fun getCanteens(): List<Canteen> {
        return MySQLDatabase.getAllCanteens()
    }
    
    suspend fun getFoodItems(canteenId: Int? = null): List<FoodItem> {
        return if (canteenId != null) {
            MySQLDatabase.getFoodItemsByCanteen(canteenId)
        } else {
            MySQLDatabase.getAllFoodItems()
        }
    }
    
    suspend fun getUsers(): List<User> {
        return MySQLDatabase.getAllUsers()
    }
    
    suspend fun registerUser(user: User): Boolean {
        return MySQLDatabase.addUser(user)
    }
    
    suspend fun placeOrder(items: List<CartItem>, canteenId: Int, canteenName: String): Int? {
        val token = MySQLDatabase.getNextToken()
        val success = MySQLDatabase.addOrder(token, items, canteenId, canteenName)
        return if (success) token else null
    }
    
    suspend fun getOrders(): List<OrderItem> {
        return MySQLDatabase.getAllOrders()
    }
    
    suspend fun updateOrderStatus(token: Int, status: OrderStatus): Boolean {
        return MySQLDatabase.updateOrderStatus(token, status)
    }
    
    suspend fun setFoodItemAvailability(foodItemId: Int, available: Boolean): Boolean {
        return MySQLDatabase.updateFoodItemAvailability(foodItemId, available)
    }
    
    suspend fun connectToDatabase(): Boolean {
        return MySQLDatabase.connect()
    }
    
    fun disconnectFromDatabase() {
        MySQLDatabase.disconnect()
    }
}

