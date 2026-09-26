package com.lyq2010.leesmusic.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

@Suppress("DEPRECATION")
object UpdateInstaller {
    private val flags get() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    private fun signers(info: PackageInfo): Set<String> =
        (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
            .orEmpty().map { it.toCharsString() }.toSet()

    fun verify(context: Context, file: File, manifest: UpdateManifest) {
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val candidate = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: error("安装包无法读取")
        check(candidate.packageName == context.packageName) { "安装包不属于此应用" }
        val code = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        check(code == manifest.versionCode && candidate.versionName == manifest.version) { "安装包版本不匹配" }
        val signatures = signers(installed)
        check(signatures.isNotEmpty() && signatures == signers(candidate)) { "安装包签名与当前应用不同，无法覆盖更新" }
    }

    /** Returns false when the user must first grant permission, then explicitly retry installation. */
    fun install(context: Context, file: File, manifest: UpdateManifest): Boolean {
        verify(context, file, manifest)
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        return true
    }
}
