package com.productiveos.launcher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val label: String,
    val customLabel: String? = null,
    val isPinned: Boolean = false,
    val pinOrder: Int = 999,
    val isHidden: Boolean = false,
    val isFrictionEnabled: Boolean = false,
    val launchCount: Int = 0,
    val lastUsedTime: Long = 0L
) {
    val displayName: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}

