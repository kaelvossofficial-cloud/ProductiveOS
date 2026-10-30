package com.productiveos.launcher.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query(
        """
        SELECT * FROM apps 
        WHERE isHidden = 0 
        ORDER BY LOWER(COALESCE(NULLIF(customLabel, ''), label)) ASC
        """
    )
    fun observeVisibleApps(): Flow<List<AppEntity>>

    @Query(
        """
        SELECT * FROM apps 
        WHERE isPinned = 1 AND isHidden = 0 
        ORDER BY pinOrder ASC, LOWER(COALESCE(NULLIF(customLabel, ''), label)) ASC
        """
    )
    fun observePinnedApps(): Flow<List<AppEntity>>

    @Query(
        """
        SELECT * FROM apps 
        WHERE isHidden = 1 
        ORDER BY LOWER(COALESCE(NULLIF(customLabel, ''), label)) ASC
        """
    )
    fun observeHiddenApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps")
    suspend fun getAllAppsSnapshot(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): AppEntity?

    @Upsert
    suspend fun upsertAll(apps: List<AppEntity>)

    @Upsert
    suspend fun upsertApp(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName NOT IN (:installedPackages)")
    suspend fun deleteRemovedApps(installedPackages: List<String>)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE apps SET isPinned = :pinned, pinOrder = :pinOrder WHERE packageName = :packageName")
    suspend fun updatePinned(packageName: String, pinned: Boolean, pinOrder: Int)

    @Query("UPDATE apps SET customLabel = :customLabel WHERE packageName = :packageName")
    suspend fun updateCustomLabel(packageName: String, customLabel: String?)

    @Query("UPDATE apps SET isFrictionEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateFriction(packageName: String, enabled: Boolean)

    @Query(
        """
        UPDATE apps 
        SET isHidden = :hidden, 
            isPinned = CASE WHEN :hidden = 1 THEN 0 ELSE isPinned END 
        WHERE packageName = :packageName
        """
    )
    suspend fun updateHidden(packageName: String, hidden: Boolean)

    @Query("UPDATE apps SET launchCount = launchCount + 1, lastUsedTime = :timestamp WHERE packageName = :packageName")
    suspend fun incrementLaunchCount(packageName: String, timestamp: Long)
}

