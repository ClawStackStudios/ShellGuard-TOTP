package com.clawstack.shellguard.totp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.remote.ApiClient
import com.clawstack.shellguard.totp.data.remote.NetworkSecurityHelper
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigInteger
import java.security.Principal
import java.security.PublicKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SelfSignedTlsGatewayTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("shellguard_auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        ApiClient.reset()
    }

    @After
    fun teardown() {
        ApiClient.reset()
    }

    @Test
    fun tofuTrustManagerPinsOnFirstUseAndRejectsMismatch() {
        var pinned: String? = null
        val tm = NetworkSecurityHelper.createTofuTrustManager(
            getPinnedFingerprint = { pinned },
            onPinCertificate = { pinned = it }
        )

        val certA = fakeCert("cert-alpha".toByteArray())
        val certB = fakeCert("cert-beta".toByteArray())

        // First use: pins certA
        tm.checkServerTrusted(arrayOf(certA), "RSA")
        val expectedFp = NetworkSecurityHelper.computeSha256Fingerprint(certA)
        assertEquals(expectedFp, pinned)

        // Second use with same cert: succeeds
        tm.checkServerTrusted(arrayOf(certA), "RSA")

        // Third use with different cert: fails closed with CertificateException
        var rejected = false
        try {
            tm.checkServerTrusted(arrayOf(certB), "RSA")
        } catch (e: CertificateException) {
            rejected = true
        }
        assertTrue("Expected CertificateException on mismatched certificate", rejected)
    }

    @Test
    fun tofuTrustManagerRejectsEmptyChain() {
        val tm = NetworkSecurityHelper.createTofuTrustManager()
        var rejected = false
        try {
            tm.checkServerTrusted(emptyArray(), "RSA")
        } catch (e: CertificateException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun apiClientRecreatesClientWhenAllowSelfSignedToggles() {
        assertFalse(ApiClient.allowSelfSignedCerts)

        val strictClient1 = ApiClient.getClient("https://192.168.1.50:6464", allowSelfSigned = false)
        val strictClient2 = ApiClient.getClient("https://192.168.1.50:6464", allowSelfSigned = false)
        assertSame(strictClient1, strictClient2)

        val selfSignedClient = ApiClient.getClient("https://192.168.1.50:6464", allowSelfSigned = true)
        assertNotSame(strictClient1, selfSignedClient)

        ApiClient.reset()
        assertFalse(ApiClient.allowSelfSignedCerts)
        assertNull(ApiClient.pinnedFingerprint)
    }

    @Test
    fun authRepositoryRestoresSelfSignedTlsAndPinAcrossColdRestart() {
        val prefs = context.getSharedPreferences("shellguard_auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("pref_allow_self_signed_tls", true)
            .putString("pref_pinned_cert_sha256", "AA:BB:CC:DD")
            .commit()

        val repo = AuthRepository(context)
        assertTrue(repo.getAllowSelfSignedCerts())
        assertEquals("AA:BB:CC:DD", repo.getPinnedCertFingerprint())
        assertTrue(ApiClient.allowSelfSignedCerts)
        assertEquals("AA:BB:CC:DD", ApiClient.pinnedFingerprint)

        repo.logout()
        assertFalse(ApiClient.allowSelfSignedCerts)
        assertNull(ApiClient.pinnedFingerprint)
        assertNull(repo.getPinnedCertFingerprint())
    }

    private fun fakeCert(encodedBytes: ByteArray): X509Certificate {
        return object : X509Certificate() {
            override fun getEncoded(): ByteArray = encodedBytes
            override fun hasUnsupportedCriticalExtension(): Boolean = false
            override fun getCriticalExtensionOIDs(): MutableSet<String>? = null
            override fun getNonCriticalExtensionOIDs(): MutableSet<String>? = null
            override fun getExtensionValue(oid: String?): ByteArray? = null
            override fun checkValidity() {}
            override fun checkValidity(date: Date?) {}
            override fun getVersion(): Int = 3
            override fun getSerialNumber(): BigInteger = BigInteger.ONE
            override fun getIssuerDN(): Principal = Principal { "CN=ShellGuard" }
            override fun getSubjectDN(): Principal = Principal { "CN=ShellGuard" }
            override fun getNotBefore(): Date = Date(0)
            override fun getNotAfter(): Date = Date(Long.MAX_VALUE)
            override fun getTBSCertificate(): ByteArray = encodedBytes
            override fun getSignature(): ByteArray = encodedBytes
            override fun getSigAlgName(): String = "SHA256withRSA"
            override fun getSigAlgOID(): String = "1.2.840.113549.1.1.11"
            override fun getSigAlgParams(): ByteArray? = null
            override fun getIssuerUniqueID(): BooleanArray? = null
            override fun getSubjectUniqueID(): BooleanArray? = null
            override fun getKeyUsage(): BooleanArray? = null
            override fun getBasicConstraints(): Int = -1
            override fun verify(key: PublicKey?) {}
            override fun verify(key: PublicKey?, sigProvider: String?) {}
            override fun toString(): String = "FakeX509Certificate"
            override fun getPublicKey(): PublicKey = throw UnsupportedOperationException()
        }
    }
}
