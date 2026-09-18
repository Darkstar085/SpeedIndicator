package com.sipun.netspeedindicator.core.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class Result {
        Success,
        FileMissing,
        PermissionRequired,
        Failed
    }

    fun installDownloadedUpdate(): Result {
        val update = UpdateManager.getDownloadedUpdate(context) ?: return Result.FileMissing
        val apk = File(File(context.filesDir, "updates"), update.fileName)
        if (!apk.isFile) return Result.FileMissing

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return Result.PermissionRequired
        }

        return try {
            val apkUri = FileProvider.getUriForFile(context, context.packageName + ".files", apk)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                clipData = ClipData.newRawUri("APK", apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (installIntent.resolveActivity(context.packageManager) == null) {
                Result.Failed
            } else {
                context.startActivity(installIntent)
                Result.Success
            }
        } catch (_: Exception) {
            Result.Failed
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_SECURITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
