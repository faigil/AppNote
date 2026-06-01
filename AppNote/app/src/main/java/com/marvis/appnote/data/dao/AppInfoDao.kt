package com.marvis.appnote.data.dao

import androidx.room.*
import com.marvis.appnote.data.entity.AppInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM app_info ORDER BY app_name COLLATE NOCASE ASC")
    fun getAllApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE is_system_app = 1 ORDER BY app_name COLLATE NOCASE ASC")
    fun getSystemApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE is_system_app = 0 ORDER BY app_name COLLATE NOCASE ASC")
    fun getUserApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE package_name = :pkg")
    suspend fun getByPackage(pkg: String): AppInfo?

    @Query("SELECT * FROM app_info WHERE app_name LIKE '%' || :query || '%' OR package_name LIKE '%' || :query || '%' ORDER BY app_name COLLATE NOCASE ASC")
    fun searchApps(query: String): Flow<List<AppInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppInfo>)

    @Query("DELETE FROM app_info")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM app_info")
    suspend fun count(): Int
}