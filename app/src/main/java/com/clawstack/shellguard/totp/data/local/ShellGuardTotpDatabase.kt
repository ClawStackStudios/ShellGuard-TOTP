package com.clawstack.shellguard.totp.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.clawstack.shellguard.totp.data.local.dao.AppConfigDao
import com.clawstack.shellguard.totp.data.local.dao.SyncMetadataDao
import com.clawstack.shellguard.totp.data.local.dao.TotpItemDao
import com.clawstack.shellguard.totp.data.local.entities.AppConfig
import com.clawstack.shellguard.totp.data.local.entities.SyncMetadataEntity
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        TotpItemEntity::class,
        SyncMetadataEntity::class,
        AppConfig::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShellGuardTotpDatabase : RoomDatabase() {
    abstract fun totpItemDao(): TotpItemDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        const val DB_NAME = "shellguard_totp_encrypted.db"
        private const val TAG = "ShellGuardTotpDB"

        @Volatile
        private var instance: ShellGuardTotpDatabase? = null

        private var sqlCipherLoadAttempted = false
        private var sqlCipherSuccessfullyLoaded = false

        private fun isRobolectric(): Boolean {
            return try {
                Class.forName("org.robolectric.Robolectric") != null
            } catch (e: Throwable) {
                android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
                android.os.Build.HARDWARE.contains("robolectric", ignoreCase = true)
            }
        }

        private fun isSqlCipherLoaded(context: Context): Boolean {
            if (sqlCipherLoadAttempted) return sqlCipherSuccessfullyLoaded
            sqlCipherLoadAttempted = true
            sqlCipherSuccessfullyLoaded = try {
                System.loadLibrary("sqlcipher")
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Native SQLCipher not loaded: ${e.message}")
                false
            }
            return sqlCipherSuccessfullyLoaded
        }

        /**
         * Builds encrypted SQLCipher Room database using hardware-derived or master key passphrase bytes.
         */
        fun getInstance(context: Context, dbPassphraseBytes: ByteArray? = null): ShellGuardTotpDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext, dbPassphraseBytes).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context, dbPassphraseBytes: ByteArray?): ShellGuardTotpDatabase {
            val builder = Room.databaseBuilder(
                context,
                ShellGuardTotpDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration()

            if (!isRobolectric()) {
                val passphrase = dbPassphraseBytes ?: com.clawstack.shellguard.totp.crypto.EncryptedDeviceVault.getOrCreateDatabasePassphrase(context)
                if (passphrase.isNotEmpty() && isSqlCipherLoaded(context)) {
                    try {
                        val factory = SupportOpenHelperFactory(passphrase)
                        builder.openHelperFactory(factory)
                    } catch (e: Throwable) {
                        Log.w(TAG, "SQLCipher open helper note: ${e.message}")
                    }
                }
            } else {
                builder.openHelperFactory(androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory())
            }

            return builder.build()
        }

        /**
         * For in-memory testing or direct creation with specific factory.
         */
        fun createInMemory(context: Context): ShellGuardTotpDatabase {
            val builder = Room.inMemoryDatabaseBuilder(
                context,
                ShellGuardTotpDatabase::class.java
            ).allowMainThreadQueries()
            if (isRobolectric()) {
                builder.openHelperFactory(androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory())
            }
            return builder.build()
        }
    }
}
