# MySQL Database Setup Guide for Canteen App

## Overview
This guide explains how to convert your Android Jetpack Compose app from in-memory local database to MySQL.

## Files Created/Modified

### 1. **database_schema.sql** (Root Directory)
- SQL script to create all necessary tables
- Run this in MySQL to initialize your database
- Includes sample data

### 2. **DatabaseConfig.kt** (app/src/main/java/com/example/canteenappv2/ui/)
- Configuration file for MySQL connection parameters
- Edit DB_USER and DB_PASSWORD with your MySQL credentials
- Modify DB_HOST if using a remote server (default: localhost)

### 3. **MySQLDatabase.kt** (app/src/main/java/com/example/canteenappv2/ui/)
- Main database access layer with coroutine support
- All CRUD operations for Canteens, FoodItems, Users, Orders
- Replaces the in-memory Database.kt

### 4. **Updated build.gradle.kts**
- Added MySQL connector dependency (8.0.33)
- Added Kotlin coroutines support

---

## Step-by-Step Setup Instructions

### Step 1: Install MySQL
Since you already have SQL installed:
- Ensure MySQL server is running
- Default port: 3306

### Step 2: Create Database and Tables
1. Open MySQL Command Line Client or MySQL Workbench
2. Run the SQL script:
```sql
SOURCE C:\Users\binay\AndroidStudioProjects\CanteenAppV2\database_schema.sql
```
Or copy-paste the entire content from `database_schema.sql` file

### Step 3: Update Connection Credentials
Edit `DatabaseConfig.kt`:
```kotlin
const val DB_USER = "root"           // Your MySQL username
const val DB_PASSWORD = "your_password"  // Your MySQL password
const val DB_HOST = "localhost"      // Change if using remote server
```

### Step 4: Update build.gradle.kts
The dependencies are already added. Now sync:
1. Click "Sync Now" in Android Studio
2. Wait for gradle sync to complete

### Step 5: Update MainActivity.kt (Initialize Connection)
Add this to your MainActivity's onCreate():

```kotlin
val scope = rememberCoroutineScope()
LaunchedEffect(Unit) {
    scope.launch {
        val connected = MySQLDatabase.connect()
        if (connected) {
            Log.d("Database", "Connected to MySQL successfully")
        } else {
            Log.e("Database", "Failed to connect to MySQL")
        }
    }
}

// Add this in your onDestroy or when app closes:
DisposableEffect(Unit) {
    onDispose {
        MySQLDatabase.disconnect()
    }
}
```

### Step 6: Migrate Usage from Database.kt to MySQLDatabase.kt
Replace all calls:

**Old Code:**
```kotlin
val canteens = Database.canteens
val users = Database.users
```

**New Code:**
```kotlin
var canteens by remember { mutableStateOf(listOf<Canteen>()) }
var users by remember { mutableStateOf(listOf<User>()) }

LaunchedEffect(Unit) {
    scope.launch {
        canteens = MySQLDatabase.getAllCanteens()
        users = MySQLDatabase.getAllUsers()
    }
}
```

---

## Database Schema Overview

### Tables Created:

1. **canteens** - Canteen information
2. **food_items** - Menu items with prices
3. **users** - App users (students, staff, admin)
4. **orders** - Customer orders
5. **order_items** - Items in each order
6. **available_tokens** - Token management for queue system

---

## Key Functions Available

### Canteen Operations
```kotlin
MySQLDatabase.getAllCanteens()
```

### Food Items
```kotlin
MySQLDatabase.getAllFoodItems()
MySQLDatabase.getFoodItemsByCanteen(canteenId)
MySQLDatabase.updateFoodItemAvailability(foodItemId, isAvailable)
```

### User Management
```kotlin
MySQLDatabase.getUserByRollNo(rollNo)
MySQLDatabase.getAllUsers()
MySQLDatabase.addUser(user)
```

### Order Management
```kotlin
MySQLDatabase.getAllOrders()
MySQLDatabase.addOrder(token, items, canteenId, canteenName)
MySQLDatabase.updateOrderStatus(token, newStatus)
MySQLDatabase.getNextToken()
```

All functions are **suspend functions** (coroutine-based) for non-blocking operations.

---

## Troubleshooting

### Connection Failed Error
1. Check if MySQL server is running
2. Verify credentials in DatabaseConfig.kt
3. Ensure database name is "canteen_app"

### "Unknown database" Error
- Run the SQL schema file again to create the database

### "Access denied for user 'root'"
- Update password in DatabaseConfig.kt
- Or change MySQL user/password to match

### Driver Not Found Error
- Ensure gradle sync completed successfully
- Check MySQL connector dependency in build.gradle.kts

---

## Important Notes

1. **Threading**: All database operations use Dispatchers.IO for non-blocking queries
2. **Connection Management**: Call MySQLDatabase.connect() at app startup
3. **Security**: For production, use environment variables for credentials (don't hardcode passwords)
4. **Data Migration**: If you have existing data, you'll need to migrate it from in-memory Database.kt to MySQL first

---

## Migration Example

If you want to migrate existing in-memory data to MySQL:

```kotlin
// One-time migration function
suspend fun migrateData() {
    val oldDb = Database // Your old in-memory database
    
    oldDb.users.forEach { user ->
        MySQLDatabase.addUser(user)
    }
    
    oldDb.orders.forEach { order ->
        MySQLDatabase.addOrder(
            order.token,
            order.items,
            canteenId = 1,
            order.canteenName
        )
    }
}
```

Run this once when user opts for database upgrade.

---

## Next Steps

1. Execute database_schema.sql in MySQL
2. Update DatabaseConfig.kt with your credentials
3. Sync gradle
4. Update MainActivity.kt to call MySQLDatabase.connect()
5. Gradually migrate UI components to use MySQLDatabase instead of Database
6. Test all CRUD operations

Good luck with your migration!

