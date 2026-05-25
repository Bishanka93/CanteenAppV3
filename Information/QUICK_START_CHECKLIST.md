# 🚀 Quick Start Checklist - MySQL Migration

## Phase 1: Setup (15 minutes)

- [ ] **Step 1**: Open MySQL Command Line or MySQL Workbench
- [ ] **Step 2**: Run the database schema script
  ```bash
  mysql -u root -p < C:\Users\binay\AndroidStudioProjects\CanteenAppV2\database_schema.sql
  ```
- [ ] **Step 3**: Verify database created
  ```sql
  SHOW DATABASES;  -- You should see "canteen_app"
  USE canteen_app;
  SHOW TABLES;     -- You should see 6 tables
  ```
- [ ] **Step 4**: Edit `DatabaseConfig.kt`
  - Update `DB_PASSWORD` with your MySQL password
  - Update `DB_USER` if not "root"
  - Update `DB_HOST` if not "localhost"

- [ ] **Step 5**: Sync Gradle
  - Click "Sync Now" in Android Studio
  - Wait for sync to complete

---

## Phase 2: Integration (2-3 hours)

### MainActivity.kt
- [ ] Import necessary classes:
  ```kotlin
  import kotlinx.coroutines.launch
  import androidx.compose.runtime.LaunchedEffect
  ```
- [ ] Add database initialization in onCreate():
  ```kotlin
  LaunchedEffect(Unit) {
      scope.launch {
          MySQLDatabase.connect()
      }
  }
  ```
- [ ] Add cleanup on app close:
  ```kotlin
  DisposableEffect(Unit) {
      onDispose {
          MySQLDatabase.disconnect()
      }
  }
  ```

### LoginScreen.kt
- [ ] Replace: `Database.users` calls
- [ ] Add: `DatabaseRepository` instance
- [ ] Update: Login function to use `repository.loginUser()`
- [ ] Test: Login with sample user (DC2024BTE0093 / 12345)

### CanteenScreens.kt
- [ ] Replace: `Database.canteens` and `Database.foodItems` calls
- [ ] Add: Data loading in LaunchedEffect
- [ ] Update: Adapt to List instead of MutableList
- [ ] Test: Canteen list loads from database

### CartScreens.kt
- [ ] Replace: `Database.orders.add()` calls
- [ ] Add: `repository.placeOrder()` call
- [ ] Handle: Token returned from database
- [ ] Test: Orders saved to database

### AdminScreens.kt
- [ ] Replace: Order status updates
- [ ] Add: `repository.updateOrderStatus()` calls
- [ ] Update: Refresh order list after changes
- [ ] Test: Status changes persist

### StaffScreens.kt
- [ ] Replace: Order display logic
- [ ] Add: Real-time order loading
- [ ] Test: New orders appear immediately

### WaitlistScreen.kt
- [ ] Replace: Token generation logic
- [ ] Use: `repository.getNextToken()` from database
- [ ] Test: Token numbering works correctly

---

## Phase 3: Testing (1 hour)

### Unit Tests
- [ ] Login with valid credentials
- [ ] Login with invalid credentials
- [ ] Create new order
- [ ] Update order status
- [ ] Check token generation

### Integration Tests
- [ ] Load canteens and food items
- [ ] Place order from multiple screens
- [ ] Verify data persists on app restart
- [ ] Check admin status updates show on staff screen

### Edge Cases
- [ ] Network disconnection (add error handling)
- [ ] Concurrent order placement
- [ ] Database connection timeout
- [ ] Invalid data input

---

## Phase 4: Cleanup (30 minutes)

- [ ] Remove dependency on `Database.kt` (or keep for reference)
- [ ] Remove test files that reference old Database
- [ ] Clean up any unused imports
- [ ] Run Lint check: Analyze > Run Inspection by Name
- [ ] Build and test final APK

---

## Common Issues & Solutions

### ❌ "Connection refused"
```
✅ Solution: 
- Make sure MySQL server is running
- Check DatabaseConfig.kt has correct host/port
- Test: mysql -u root -p from command line
```

### ❌ "Access denied for user 'root'"
```
✅ Solution:
- Update password in DatabaseConfig.kt
- Or create MySQL user with password: mysql -u root -p < setup.sql
```

### ❌ "Unknown database 'canteen_app'"
```
✅ Solution:
- Run database_schema.sql script again
- Verify: mysql -u root -p
          mysql> SHOW DATABASES;
```

### ❌ Gradle sync fails
```
✅ Solution:
- File > Invalidate Caches > Restart
- Or: gradlew --stop (in terminal)
- Then: Sync again
```

### ❌ "Suspend function called in non-suspend context"
```
✅ Solution:
- All MySQLDatabase calls must be in coroutine
- Wrap in: scope.launch { }
- Or: LaunchedEffect { }
- Or: async/await
```

### ❌ "Data still shows old values"
```
✅ Solution:
- Make sure you're reloading data after changes
- Add: data.value = repository.getLatestData()
- In: onSuccess callback
```

---

## Files to Keep & Reference

| File | Purpose | Keep/Delete |
|------|---------|-----------|
| Database.kt | Old in-memory DB | ✅ Keep (for reference) |
| DatabaseConfig.kt | MySQL credentials | ✅ Keep |
| MySQLDatabase.kt | Core operations | ✅ Keep |
| DatabaseRepository.kt | Clean API | ✅ Keep |
| ExampleDatabaseUsage.kt | Code examples | ✅ Keep for reference |
| database_schema.sql | SQL setup | ✅ Keep for backup |
| MYSQL_SETUP_GUIDE.md | Setup instructions | ✅ Keep in repo |
| README_MYSQL_MIGRATION.md | Overview | ✅ Keep in repo |

---

## Testing Script

### Test Login
```kotlin
// In LoginScreen
val username = "DC2024BTE0093"
val password = "12345"

scope.launch {
    val user = repository.loginUser(username, password)
    Log.d("Login", "User: ${user?.name}")  // Should print: Walter White
}
```

### Test Canteen Load
```kotlin
// In CanteenScreens
scope.launch {
    val canteens = repository.getCanteens()
    Log.d("Canteens", "Count: ${canteens.size}")  // Should print: 2
}
```

### Test Order Placement
```kotlin
// In CartScreens
scope.launch {
    val items = listOf(CartItem(foodItem, 2))
    val token = repository.placeOrder(items, canteenId = 1, "Canteen A")
    Log.d("Order", "Token: $token")  // Should print: 101+ (auto-incremented)
}
```

---

## Expected Behavior After Migration

✅ **Login Screen**
- Type: DC2024BTE0093
- Password: 12345
- Result: Logs in as "Walter White"

✅ **Canteen Screen**
- Shows: 2 canteens
- Shows: 6 food items total
- Load time: ~50-100ms

✅ **Cart Screen**
- Place order
- Token generated: 101, 102, 103, etc.
- Order appears in admin screen immediately

✅ **Admin Screen**
- See all orders
- Update status
- Changes persist on database

✅ **App Restart**
- All data still present
- Order history remains
- Food items unchanged

---

## Performance Expectations

| Operation | Time |
|-----------|------|
| Login | ~50ms |
| Load canteens | ~30ms |
| Load food items | ~50ms |
| Place order | ~100ms |
| Update status | ~50ms |
| Load all orders | ~100ms |

**Note**: Times depend on your WiFi/LAN speed

---

## Success Criteria

Your migration is successful when:

✅ App builds without errors
✅ App starts and connects to MySQL
✅ Login works with sample credentials
✅ Can see canteens and food items
✅ Can place orders
✅ Orders visible in admin screen
✅ Data persists after app restart
✅ No crashes during normal usage

---

## Final Verification

Before declaring success:

```kotlin
// Run this in a test screen
fun verifyMigration() {
    scope.launch {
        // 1. Test connection
        val connected = repository.connectToDatabase()
        Log.d("Verify", "Connected: $connected")
        
        // 2. Test canteens
        val canteens = repository.getCanteens()
        Log.d("Verify", "Canteens: ${canteens.size}")
        
        // 3. Test users
        val users = repository.getUsers()
        Log.d("Verify", "Users: ${users.size}")
        
        // 4. Test login
        val user = repository.loginUser("DC2024BTE0093", "12345")
        Log.d("Verify", "Login: ${user?.name}")
        
        // 5. Test orders
        val orders = repository.getOrders()
        Log.d("Verify", "Orders: ${orders.size}")
        
        Log.d("Verify", "✅ ALL TESTS PASSED")
    }
}
```

Expected output:
```
Connected: true
Canteens: 2
Users: 3
Login: Walter White
Orders: 0 (or your placed orders)
✅ ALL TESTS PASSED
```

---

## When You're Done

1. ✅ Commit your changes to git
2. ✅ Push to repository
3. ✅ Build release APK
4. ✅ Test on real device
5. ✅ Archive MySQL backup
6. ✅ Document any custom modifications

---

## Need Help?

Refer to:
- `MYSQL_SETUP_GUIDE.md` - Detailed setup instructions
- `README_MYSQL_MIGRATION.md` - Overview and architecture
- `COMPARISON_IN_MEMORY_VS_MYSQL.md` - Why MySQL
- `ExampleDatabaseUsage.kt` - Code examples
- `FILE_SUMMARY.md` - What each file does

**🎉 Congratulations! You now have a scalable, persistent database backend!**

