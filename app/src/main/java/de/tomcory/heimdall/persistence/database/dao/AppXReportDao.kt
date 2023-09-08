package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.tomcory.heimdall.persistence.database.entity.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.AppXReport

@Dao
interface AppXReportDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg crossRef: AppXReport)

    @Transaction
    @Query("SELECT * FROM App WHERE packageName = :packageName")
    suspend fun getAppWithReports(packageName: String): AppWithReports

    @Transaction
    @Query("SELECT * FROM App")
    suspend fun getAppWithReports(): List<AppWithReports>

    @Transaction
    @Query("SELECT * FROM App")
    fun getAppsandReports(): List<AppWithReports>
}