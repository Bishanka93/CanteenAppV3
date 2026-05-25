package com.example.canteenappv2.database

/**
 * MySQL Database Configuration
 * ─────────────────────────────────────────────────────────────────────────────
 */
object DatabaseConfig {

    // ── Credentials ──────────────────────────────────────────────────────────
    const val DB_PORT     = 3306
    const val DB_NAME     = "canteen_app"
    const val DB_USER     = "canteen_user"   // the remote-capable user created above
    const val DB_PASSWORD = "@Root5659"

    // ── IPs ───────────────────────────────────────────────────────────────────
    /** Special alias that reaches your PC's localhost from the Android Emulator. */
    private const val EMULATOR_HOST   = "10.0.2.2"

    const val HOTSPOT_PC_IP = "10.0.2.2"

    // ── Connection Pool ───────────────────────────────────────────────────────
    //const val MAX_POOL_SIZE          = 5
    //const val MIN_POOL_SIZE          = 2
    const val CONNECTION_TIMEOUT_MS  = 30_000   // 30 s
    const val SOCKET_TIMEOUT_MS      = 10_000   // 10 s — important on mobile networks

    // ── JDBC URL builder ─────────────────────────────────────────────────────
    fun getJdbcUrl(): String {
        val host = resolveHost()
        return "jdbc:mysql://$host:$DB_PORT/$DB_NAME" +
                "?useSSL=false" +
                "&allowPublicKeyRetrieval=true" +
                "&serverTimezone=UTC" +
                "&connectTimeout=$CONNECTION_TIMEOUT_MS" +
                "&socketTimeout=$SOCKET_TIMEOUT_MS" +
                "&autoReconnect=true"          // reconnect automatically if the link drops
    }

    fun resolveHost(): String {
        val isEmulator =
            android.os.Build.FINGERPRINT.startsWith("generic") ||
                    android.os.Build.FINGERPRINT.startsWith("unknown") ||
                    android.os.Build.MODEL.contains("google_sdk", ignoreCase = true) ||
                    android.os.Build.MODEL.contains("Emulator",   ignoreCase = true) ||
                    android.os.Build.MODEL.contains("Android SDK", ignoreCase = true) ||
                    android.os.Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
                    android.os.Build.HARDWARE.contains("goldfish") ||
                    android.os.Build.HARDWARE.contains("ranchu")

        return if (isEmulator) EMULATOR_HOST else HOTSPOT_PC_IP
    }
}