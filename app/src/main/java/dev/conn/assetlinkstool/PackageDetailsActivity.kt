package dev.conn.assetlinkstool

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.package_details_activity.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalArgumentException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class PackageDetailsActivity : AppCompatActivity() {
    companion object {
        val EXTRA_KEY_PACKAGE = "package"

        private val HEX_CHAR_LOOKUP = "0123456789ABCDEF".toCharArray()
        private val DIR_NAME = "asset_links"
        private val FILE_NAME = "assetlinks.json"
        private val AUTHORITY = "dev.conn.assetlinkstool.fileprovider"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.package_details_activity)

        val packageName = intent.getStringExtra(EXTRA_KEY_PACKAGE)
        packageNameTextView.setText(packageName)

        val packageSignature = getCertificateSHA256FingerprintForPackage(packageName)
        packageSignatureTextView.setText(prettyPrintSignature(packageSignature))

        // TODO: Nullness and error safety.

        val assetLink = createAssetLinks(packageName, packageSignature)
        assetLinkTextView.setText(assetLink)

        copySignatureButton.setOnClickListener { addToClipboard(packageSignature) }
        copyAssetLinkButton.setOnClickListener { addToClipboard(assetLink) }
        shareSignatureButton.setOnClickListener {
            val intent = createShareIntent("Signature for $packageName", packageSignature)
            startActivity(Intent.createChooser(intent, "Share Signature for $packageName"))
        }
        // TODO: Share should share as a file.
        shareAssetLinkButton.setOnClickListener {
            val intent = createShareAssetLinkIntent(assetLink) ?: return@setOnClickListener

            startActivity(Intent.createChooser(intent, "Share Asset Link for $packageName"))
        }
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        try {
            return packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Computes the SHA256 certificate for the given package name. The app with the given package
     * name has to be installed on device. The output will be a 30 long HEX string with : between
     * each value.
     * @param packageName The package name to query the signature for.
     * @return The SHA256 certificate for the package name.
     */
    @SuppressLint("PackageManagerGetSignatures")
    // https://stackoverflow.com/questions/39192844/android-studio-warning-when-using-packagemanager-get-signatures
    fun getCertificateSHA256FingerprintForPackage(packageName: String): String {
        val packageInfo = getPackageInfo(packageName)

        val input = ByteArrayInputStream(packageInfo.signatures[0].toByteArray())
        var hexString: String? = null
        try {
            val certificate = CertificateFactory.getInstance("X509").generateCertificate(input) as X509Certificate
            hexString = bytesToHex(
                MessageDigest.getInstance("SHA256").digest(certificate.getEncoded())
            )
        } catch (e: CertificateEncodingException) {
            throw java.lang.RuntimeException(e)
        } catch (e: CertificateException) {
            // This shouldn't happen.
            throw java.lang.RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw java.lang.RuntimeException(e)
        }

        return hexString
    }

    fun bytesToHex(byteArray: ByteArray): String {
        val hexString = StringBuilder(byteArray.size * 3 - 1)

        for (i in byteArray.indices) {
            val value = byteArray[i].toUByte().and(0xff.toUByte()).toInt()

            hexString.append(HEX_CHAR_LOOKUP[value ushr 4])
            hexString.append(HEX_CHAR_LOOKUP[value and 0x0f])
            if (i < byteArray.size - 1) hexString.append(':')
        }

        return hexString.toString()
    }

    fun createAssetLinks(name: String, signature: String): String {
        return "[{\n" +
                "  \"relation\": [\"delegate_permission/common.handle_all_urls\"],\n" +
                "  \"target\": {\n" +
                "    \"namespace\": \"android_app\",\n" +
                "    \"package_name\": \"$name\",\n" +
                "    \"sha256_cert_fingerprints\": [\n" +
                "      \"$signature\"\n" +
                "    ]\n" +
                "  }\n" +
                "}]"
    }

    fun addToClipboard(data: String) {
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.primaryClip = ClipData.newPlainText("Source Test", data)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun createShareIntent(title: String, text: String): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.setType("text/plain")
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        return intent
    }

    fun prettyPrintSignature(hex: String): String {
        if (hex.length < 72) return hex

        // Yeah this is hacky...
        return hex.substring(0, 24) + "\n" +
                hex.substring(24, 48) + "\n" +
                hex.substring(48, 72) + "\n" +
                hex.substring(72) + " "
    }

    fun createAssetLinksFile(assetLinks: String): Uri? {
        try {
            val dir = File(filesDir, DIR_NAME)

            if (!dir.exists()) {
                dir.mkdir()
            }

            val file = File(dir, FILE_NAME)

            val os = FileOutputStream(file)
            os.write(assetLinks.toByteArray())
            os.close()

            return FileProvider.getUriForFile(this, AUTHORITY, file)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not create file to share, try copying instead.", Toast.LENGTH_LONG).show()
        }
        return null
    }

    fun createShareAssetLinkIntent(assetLinks: String): Intent? {
        val uri = createAssetLinksFile(assetLinks)

        if (uri == null) return null

        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        intent.setType("application/json")
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(Intent.EXTRA_SUBJECT, "Digital Asset Link")

        return intent
    }
}
