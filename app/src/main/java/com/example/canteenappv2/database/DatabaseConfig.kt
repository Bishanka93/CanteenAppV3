package com.example.canteenappv2.database

/**
 * MySQL Database Configuration
 * Update these values according to your MySQL setup
 */
object DatabaseConfig {
    // MySQL Connection Parameters
    const val DB_HOST = "10.0.2.2"          // Your MySQL server host
    const val DB_PORT = 3306                 // MySQL port (default: 3306)
    const val DB_NAME = "canteen_app"        // Database name
    const val DB_USER = "root"               // MySQL username
    const val DB_PASSWORD = "@Root5659"

    // Connection Pool Settings
    const val MAX_POOL_SIZE = 5
    const val MIN_POOL_SIZE = 2
    const val CONNECTION_TIMEOUT_MS = 30000  // 30 seconds

    // Build JDBC URL
    fun getJdbcUrl(): String {
        // 10.0.2.2 is the special alias to reach your PC's localhost from the Android Emulator
        val host = if (android.os.Build.FINGERPRINT.startsWith("generic") ||
            android.os.Build.MODEL.contains("google_sdk")) {
            "10.0.2.2"
        } else {
            DB_HOST // Uses your actual IP (e.g., 192.168.x.x) if using a physical device
        }

        return "jdbc:mysql://$host:$DB_PORT/$DB_NAME?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    }
}

