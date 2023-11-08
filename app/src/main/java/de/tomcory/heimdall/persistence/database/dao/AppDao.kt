package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.AppWithTrackers
import de.tomcory.heimdall.persistence.database.entity.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(vararg app: App)

    @Query("UPDATE App SET isInstalled = 0 WHERE packageName = :packageName")
    suspend fun updateIsInstalled(packageName: String)

    @Query("SELECT * FROM App")
    suspend fun getAll(): List<App>

    @Query("SELECT * FROM App WHERE packageName= :packageName")
    suspend fun getAppByName(packageName: String): App

    @Transaction
    @Query("SELECT * FROM App WHERE packageName = :packageName")
    suspend fun getAppWithReport(packageName: String): AppWithReports

    @Transaction
    @Query("SELECT * FROM App")
    suspend fun getAllAppWithReports(): List<AppWithReports>

    @Transaction
    @Query("SELECT * FROM App WHERE isInstalled AND NOT isSystem")
    suspend fun getInstalledUserAppWithReports(): List<AppWithReports>

    @Transaction
    @Query("SELECT * FROM App")
    fun getAllAppWithReportsObservable(): Flow<List<AppWithReports>>

    @Transaction
    @Query("SELECT * FROM App WHERE packageName = :packageName")
    suspend fun getAppWithTrackersFromPackageName(packageName: String): AppWithTrackers

}

data class AppWithReports(
    @Embedded val app: App,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "appPackageName"
    )
    val reports: List<Report>,
) : Comparable<AppWithReports> {
    fun getLatestReport(): Report? {
        return reports.maxByOrNull { it.timestamp }
    }

    override operator fun compareTo(other: AppWithReports): Int {
        if (this.app.packageName > other.app.packageName) return 1
        if (this.app.packageName < other.app.packageName) return -1
        if ((this.getLatestReport()?.mainScore ?: 0.0) > (other.getLatestReport()?.mainScore
                ?: 0.0)
        ) return 1
        if ((this.getLatestReport()?.mainScore ?: 0.0) < (other.getLatestReport()?.mainScore
                ?: 0.0)
        ) return -1
        return 0
    }
}

