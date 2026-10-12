package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.data.migration.BackupSchemaType
import com.clawstack.shellguard.totp.data.migration.MultiFormatMigrationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MultiFormatMigrationEngineTest {

    @Test
    fun `test bitwarden authenticator standalone json parsing`() {
        val json = """
            {
              "items": [
                {
                  "key": "JBSWY3DPEHPK3PXP",
                  "issuer": "GitHub",
                  "name": "lucas@example.com",
                  "algorithm": "SHA1",
                  "digits": 6,
                  "period": 30
                }
              ]
            }
        """.trimIndent()

        val result = MultiFormatMigrationEngine.parseString(json)
        assertEquals(BackupSchemaType.BITWARDEN_AUTHENTICATOR, result.schemaType)
        assertEquals(1, result.items.size)

        val item = result.items[0]
        assertEquals("GitHub", item.title)
        assertEquals("lucas@example.com", item.username)
        assertEquals("JBSWY3DPEHPK3PXP", item.secret)
        assertEquals("SHA1", item.algorithm)
    }

    @Test
    fun `test plaintext bitwarden vault extracting zero passwords`() {
        val json = """
            {
              "encrypted": false,
              "folders": [
                {"id": "folder1", "name": "Work"}
              ],
              "items": [
                {
                  "type": 1,
                  "name": "GitHub",
                  "folderId": "folder1",
                  "login": {
                    "username": "lucas@example.com",
                    "password": "supersecretpassword",
                    "totp": "JBSWY3DPEHPK3PXP"
                  }
                },
                {
                  "type": 1,
                  "name": "NoTotpAccount",
                  "login": {
                    "username": "user2",
                    "password": "password2"
                  }
                }
              ]
            }
        """.trimIndent()

        val result = MultiFormatMigrationEngine.parseString(json)
        assertEquals(BackupSchemaType.BITWARDEN_VAULT, result.schemaType)
        assertEquals(1, result.items.size)

        val item = result.items[0]
        assertEquals("GitHub", item.title)
        assertEquals("lucas@example.com", item.username)
        assertEquals("JBSWY3DPEHPK3PXP", item.secret)
        assertEquals("Work", item.category)
    }

    @Test
    fun `test steam guard uri ingestion`() {
        val json = """
            {
              "items": [
                {
                  "key": "steam://JBSWY3DPEHPK3PXP",
                  "issuer": "Steam",
                  "name": "Gamer123"
                }
              ]
            }
        """.trimIndent()

        val result = MultiFormatMigrationEngine.parseString(json)
        assertEquals(BackupSchemaType.BITWARDEN_AUTHENTICATOR, result.schemaType)
        assertEquals(1, result.items.size)

        val item = result.items[0]
        assertEquals("Steam", item.title)
        assertEquals("Gamer123", item.username)
        assertEquals("JBSWY3DPEHPK3PXP", item.secret)
        assertEquals("STEAM", item.algorithm)
        assertEquals(5, item.digits)
    }

    @Test
    fun `test Aegis authentication json parsing`() {
        val json = """
            {
              "db": {
                "entries": [
                  {
                    "type": "totp",
                    "issuer": "Twitter",
                    "name": "john_doe",
                    "info": {
                      "secret": "JBSWY3DPEHPK3PXP",
                      "algo": "SHA1",
                      "digits": 6,
                      "period": 30
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val result = MultiFormatMigrationEngine.parseString(json)
        assertEquals(BackupSchemaType.AEGIS, result.schemaType)
        assertEquals(1, result.items.size)

        val item = result.items[0]
        assertEquals("Twitter", item.title)
        assertEquals("john_doe", item.username)
        assertEquals("JBSWY3DPEHPK3PXP", item.secret)
        assertEquals("SHA1", item.algorithm)
    }
}
