package com.alaotach.eli5.assistant

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import com.alaotach.eli5.ShareImageGenerator
import java.io.File

class ShareTransparentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val caption = intent.getStringExtra(EXTRA_CAPTION) ?: ""

        if (imagePath.isNullOrBlank()) {
            finish()
            return
        }

        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            finish()
            return
        }

        val imageUri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            clipData = ClipData.newRawUri("ELI5 Translation", imageUri)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, "ELI5 Screen Translation")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        ShareImageGenerator.grantPermissionsToReceivers(this, shareIntent, imageUri)

        val chooserIntent = Intent.createChooser(shareIntent, "Share Translated Screen").apply {
            clipData = ClipData.newRawUri("ELI5 Translation", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(chooserIntent)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_CAPTION = "extra_caption"
    }
}
