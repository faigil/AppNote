package com.marvis.appnote.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfo(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String = "",
    @ColumnInfo(name = "version_name") val versionName: String = "",
    @ColumnInfo(name = "version_code") val versionCode: Long = 0,
    @ColumnInfo(name = "is_system_app") val isSystemApp: Boolean = false,
    @ColumnInfo(name = "install_time") val installTime: Long = 0,
    @ColumnInfo(name = "update_time") val updateTime: Long = 0,
    @ColumnInfo(name = "target_sdk") val targetSdk: Int = 0,
    @ColumnInfo(name = "min_sdk") val minSdk: Int = 0,
    @ColumnInfo(name = "apk_path") val apkPath: String = "",
    @ColumnInfo(name = "apk_size") val apkSize: Long = 0,          // bytes
    @ColumnInfo(name = "data_size") val dataSize: Long = 0,         // bytes
    @ColumnInfo(name = "uid") val uid: Int = 0,
    @ColumnInfo(name = "permissions") val permissions: String = "",  // JSON array
    @ColumnInfo(name = "signatures") val signatures: String = "",    // signature hashes
    @ColumnInfo(name = "launch_activity") val launchActivity: String = "",
    @ColumnInfo(name = "last_refreshed") val lastRefreshed: Long = 0
)