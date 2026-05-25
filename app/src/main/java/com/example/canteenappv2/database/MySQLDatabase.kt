package com.example.canteenappv2.database

import com.example.canteenappv2.ui.Canteen
import com.example.canteenappv2.ui.CartItem
import com.example.canteenappv2.ui.FoodItem
import com.example.canteenappv2.ui.OrderItem
import com.example.canteenappv2.ui.OrderStatus
import com.example.canteenappv2.ui.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import android.util.Log

object MySQLDatabase {

    private var connection: Connection? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(
                DatabaseConfig.getJdbcUrl(),
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD
            )
            Log.d("MySQL", "Connected to ${DatabaseConfig.resolveHost()}:${DatabaseConfig.DB_PORT}")
            connection != null
        } catch (e: Exception) {
            Log.e("MySQL", "Connection failed to ${DatabaseConfig.resolveHost()}: ${e.message}")
            false
        }
    }

    fun disconnect() {
        try {
            connection?.close()
            connection = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns a live connection, reconnecting automatically if the link dropped.
     * Common on mobile/hotspot where the OS kills idle sockets.
     */
    private suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        val conn = connection
        val alive = try {
            conn != null && !conn.isClosed && conn.isValid(3)
        } catch (_: Exception) { false }
        if (!alive) {
            Log.w("MySQL", "Connection lost — reconnecting…")
            try { connection?.close() } catch (_: Exception) {}
            connection = null
            try {
                Class.forName("com.mysql.jdbc.Driver")
                connection = DriverManager.getConnection(
                    DatabaseConfig.getJdbcUrl(),
                    DatabaseConfig.DB_USER,
                    DatabaseConfig.DB_PASSWORD
                )
                Log.d("MySQL", "Reconnected successfully")
            } catch (e: Exception) {
                Log.e("MySQL", "Reconnect failed: ${e.message}")
                return@withContext null
            }
        }
        connection
    }

    // -------------------------------------------------------------------------
    // Canteens
    // -------------------------------------------------------------------------

    suspend fun getAllCanteens(): List<Canteen> = withContext(Dispatchers.IO) {
        val canteens = mutableListOf<Canteen>()
        try {
            val conn = getConnection() ?: return@withContext canteens
            val statement = conn.createStatement()
            val resultSet = statement.executeQuery("SELECT id, name FROM canteens")
            while (resultSet.next()) {
                canteens.add(Canteen(resultSet.getInt("id"), resultSet.getString("name")))
            }
            resultSet.close()
            statement.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        canteens
    }

    suspend fun addCanteen(name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "INSERT INTO canteens (name) VALUES (?)"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, name)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateCanteen(id: Int, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "UPDATE canteens SET name = ? WHERE id = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, name)
            ps.setInt(2, id)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteCanteen(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "DELETE FROM canteens WHERE id = ?"
            val ps = conn.prepareStatement(sql)
            ps.setInt(1, id)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // -------------------------------------------------------------------------
    // Food Items
    // -------------------------------------------------------------------------

    suspend fun getAllFoodItems(): List<FoodItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FoodItem>()
        try {
            val conn = getConnection() ?: return@withContext items
            val statement = conn.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT id, name, price, canteen_id, image_name, is_available FROM food_items"
            )
            while (resultSet.next()) {
                items.add(resultSet.toFoodItem())
            }
            resultSet.close()
            statement.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        items
    }

    suspend fun getFoodItemsByCanteen(canteenId: Int): List<FoodItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FoodItem>()
        try {
            val conn = getConnection() ?: return@withContext items
            val sql = "SELECT id, name, price, canteen_id, image_name, is_available FROM food_items WHERE canteen_id = ?"
            val ps = conn.prepareStatement(sql)
            ps.setInt(1, canteenId)
            val resultSet = ps.executeQuery()
            while (resultSet.next()) {
                items.add(resultSet.toFoodItem())
            }
            resultSet.close()
            ps.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        items
    }

    suspend fun addFoodItem(name: String, price: Double, canteenId: Int, imageName: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "INSERT INTO food_items (name, price, canteen_id, image_name) VALUES (?, ?, ?, ?)"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, name)
            ps.setDouble(2, price)
            ps.setInt(3, canteenId)
            ps.setString(4, imageName)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateFoodItem(id: Int, name: String, price: Double, imageName: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "UPDATE food_items SET name = ?, price = ?, image_name = ? WHERE id = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, name)
            ps.setDouble(2, price)
            ps.setString(3, imageName)
            ps.setInt(4, id)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateFoodItemAvailability(foodItemId: Int, isAvailable: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "UPDATE food_items SET is_available = ? WHERE id = ?"
            val ps = conn.prepareStatement(sql)
            ps.setBoolean(1, isAvailable)
            ps.setInt(2, foodItemId)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

    suspend fun getUserByRollNo(rollNo: String): User? = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext null
            val sql = "SELECT name, roll_no, password, is_staff, is_admin, canteen_id FROM users WHERE roll_no = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, rollNo)
            val resultSet = ps.executeQuery()
            val user = if (resultSet.next()) resultSet.toUser() else null
            resultSet.close()
            ps.close()
            user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        val users = mutableListOf<User>()
        try {
            val conn = getConnection() ?: return@withContext users
            val statement = conn.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT name, roll_no, password, is_staff, is_admin, canteen_id FROM users"
            )
            while (resultSet.next()) {
                users.add(resultSet.toUser())
            }
            resultSet.close()
            statement.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        users
    }

    suspend fun addUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "INSERT INTO users (name, roll_no, password, is_staff, is_admin, canteen_id) VALUES (?, ?, ?, ?, ?, ?)"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.name)
            ps.setString(2, user.rollNo)
            ps.setString(3, user.password)
            ps.setBoolean(4, user.isStaff)
            ps.setBoolean(5, user.isAdmin)
            ps.setObject(6, user.canteenId)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Updates name, password, and canteen_id identified by roll_no (primary key). */
    suspend fun updateUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "UPDATE users SET name = ?, password = ?, canteen_id = ? WHERE roll_no = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.name)
            ps.setString(2, user.password)
            ps.setObject(3, user.canteenId)
            ps.setString(4, user.rollNo)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteUser(rollNo: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "DELETE FROM users WHERE roll_no = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, rollNo)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // -------------------------------------------------------------------------
    // Orders
    // -------------------------------------------------------------------------

    /**
     * Fetches all orders, optionally filtered by canteen.
     * Pass [canteenId] from StaffOrdersScreen so staff only see their canteen's orders.
     * Pass null (default) from admin / waitlist screens to get all orders.
     */
    suspend fun getAllOrders(canteenId: Int? = null): List<OrderItem> = withContext(Dispatchers.IO) {
        val orders = mutableListOf<OrderItem>()
        try {
            val conn = getConnection() ?: return@withContext orders
            val sql = if (canteenId != null) {
                "SELECT id, token, canteen_id, canteen_name, status FROM orders WHERE canteen_id = ?"
            } else {
                "SELECT id, token, canteen_id, canteen_name, status FROM orders"
            }

            val resultSet = if (canteenId != null) {
                val ps = conn.prepareStatement(sql)
                ps.setInt(1, canteenId)
                ps.executeQuery()
            } else {
                conn.createStatement().executeQuery(sql)
            }

            while (resultSet.next()) {
                val orderId = resultSet.getInt("id")
                orders.add(
                    OrderItem(
                        token = resultSet.getInt("token"),
                        items = getOrderItems(orderId),
                        status = OrderStatus.valueOf(resultSet.getString("status")),
                        canteenName = resultSet.getString("canteen_name")
                    )
                )
            }
            resultSet.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        orders
    }

    private suspend fun getOrderItems(orderId: Int): List<CartItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<CartItem>()
        try {
            val conn = getConnection() ?: return@withContext items
            val sql = """
                SELECT oi.food_item_id, oi.quantity, fi.name, fi.price, fi.canteen_id, fi.image_name, fi.is_available
                FROM order_items oi
                JOIN food_items fi ON oi.food_item_id = fi.id
                WHERE oi.order_id = ?
            """
            val ps = conn.prepareStatement(sql)
            ps.setInt(1, orderId)
            val resultSet = ps.executeQuery()
            while (resultSet.next()) {
                items.add(
                    CartItem(
                        foodItem = FoodItem(
                            id = resultSet.getInt("food_item_id"),
                            name = resultSet.getString("name"),
                            price = resultSet.getDouble("price"),
                            canteenId = resultSet.getInt("canteen_id"),
                            imageName = resultSet.getString("image_name"),
                            isAvailable = resultSet.getBoolean("is_available")
                        ),
                        quantity = resultSet.getInt("quantity")
                    )
                )
            }
            resultSet.close()
            ps.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        items
    }

    suspend fun addOrder(token: Int, items: List<CartItem>, canteenId: Int, canteenName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val orderSql = "INSERT INTO orders (token, canteen_id, canteen_name, status) VALUES (?, ?, ?, 'PENDING')"
            val orderStatement = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)
            orderStatement.setInt(1, token)
            orderStatement.setInt(2, canteenId)
            orderStatement.setString(3, canteenName)
            orderStatement.executeUpdate()
            val keys = orderStatement.generatedKeys
            val orderId = if (keys.next()) keys.getInt(1) else return@withContext false
            orderStatement.close()

            items.forEach { item ->
                val itemSql = "INSERT INTO order_items (order_id, food_item_id, quantity, price_at_order) VALUES (?, ?, ?, ?)"
                val itemStatement = conn.prepareStatement(itemSql)
                itemStatement.setInt(1, orderId)
                itemStatement.setInt(2, item.foodItem.id)
                itemStatement.setInt(3, item.quantity)
                itemStatement.setDouble(4, item.foodItem.price)
                itemStatement.executeUpdate()
                itemStatement.close()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateOrderStatus(token: Int, newStatus: OrderStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext false
            val sql = "UPDATE orders SET status = ? WHERE token = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, newStatus.toString())
            ps.setInt(2, token)
            val result = ps.executeUpdate() > 0
            ps.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getNextToken(): Int = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext 101
            val statement = conn.createStatement()
            val resultSet = statement.executeQuery("SELECT MAX(token) as max_token FROM orders")
            val nextToken = if (resultSet.next()) {
                val maxToken = resultSet.getInt("max_token")
                if (maxToken == 0) 101 else maxToken + 1
            } else 101
            resultSet.close()
            statement.close()
            nextToken
        } catch (e: Exception) {
            e.printStackTrace()
            101
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — reduce repetition when mapping ResultSet rows
    // -------------------------------------------------------------------------

    private fun java.sql.ResultSet.toFoodItem() = FoodItem(
        id = getInt("id"),
        name = getString("name"),
        price = getDouble("price"),
        canteenId = getInt("canteen_id"),
        imageName = getString("image_name"),
        isAvailable = getBoolean("is_available")
    )

    private fun java.sql.ResultSet.toUser() = User(
        name = getString("name"),
        rollNo = getString("roll_no"),
        password = getString("password"),
        isStaff = getBoolean("is_staff"),
        isAdmin = getBoolean("is_admin"),
        canteenId = getInt("canteen_id").takeIf { it > 0 }
    )
}