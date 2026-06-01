package com.marvis.appnote.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.storage.StorageManager
import com.marvis.appnote.data.dao.AppInfoDao
import com.marvis.appnote.data.entity.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

class AppInfoRepository(
    private val context: Context,
    private val appInfoDao: AppInfoDao
) {
    fun getAllApps(): Flow<List<AppInfo>> = appInfoDao.getAllApps()
    fun getSystemApps(): Flow<List<AppInfo>> = appInfoDao.getSystemApps()
    fun getUserApps(): Flow<List<AppInfo>> = appInfoDao.getUserApps()
    fun searchApps(query: String): Flow<List<AppInfo>> = appInfoDao.searchApps(query)

    suspend fun refreshAll(): Int = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
        val appInfoList = packages.map { pkg -> pkg.toAppInfo(pm) }

        appInfoDao.deleteAll()
        appInfoDao.insertAll(appInfoList)
        appInfoList.size
    }

    private fun PackageInfo.toAppInfo(pm: PackageManager): AppInfo {
        val isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

        // Permissions
        val permArray = JSONArray()
        requestedPermissions?.forEach { permArray.put(it) }

        // Signatures
        val sigHashes = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.signingCertificateHistory?.forEach { cert ->
                val md = MessageDigest.getInstance("SHA-256")
                val hash = md.digest(cert.encoded)
                sigHashes.add(hash.joinToString("") { "%02x".format(it) })
            }
        }

        // APK size
        val apkFile = File(applicationInfo.sourceDir)
        val apkSize = if (apkFile.exists()) apkFile.length() else 0L

        // Data size
        val dataSize = try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            val uuid = storageManager?.getUuidForPath(context.filesDir)
            if (uuid != null) {
                storageManager.getCacheQuotaBytes(uuid, applicationInfo.uid)
            } else 0L
        } catch (_: Exception) { 0L }

        // Launch activity
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val launchActivity = launchIntent?.component?.className ?: ""

        return AppInfo(
            packageName = packageName,
            appName = applicationInfo.loadLabel(pm).toString(),
            versionName = versionName ?: "",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                longVersionCode
            } else {
                @Suppress("DEPRECATION") versionCode.toLong()
            },
            isSystemApp = isSystem,
            installTime = packageInfo.firstInstallTime,
            updateTime = packageInfo.lastUpdateTime,
            targetSdk = applicationInfo.targetSdkVersion,
            minSdk = applicationInfo.minSdkVersion,
            apkPath = applicationInfo.sourceDir,
            apkSize = apkSize,
            dataSize = dataSize,
            uid = applicationInfo.uid,
            permissions = permArray.toString(),
            signatures = sigHashes.joinToString(","),
            launchActivity = launchActivity,
            lastRefreshed = System.currentTimeMillis()
        )
    }
}