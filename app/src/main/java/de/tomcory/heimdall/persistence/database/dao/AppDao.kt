package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report

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
    suspend fun getAppWithReports(packageName: String): AppWithReports

    @Transaction
    @Query("SELECT * FROM App")
    suspend fun getAllAppWithReports(): List<AppWithReports>

}

data class AppWithReports(
    @Embedded val app: App,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName"
    )
    val reports: List<Report>
): Comparable<AppWithReports> {
    override operator fun compareTo(other: AppWithReports): Int {
        if (this.app.packageName > other.app.packageName) return 1
        if (this.app.packageName < other.app.packageName) return -1
        if ((this.reports.lastOrNull()?.mainScore ?: 0.0) > (other.reports.lastOrNull()?.mainScore ?: 0.0)) return 1
        if ((this.reports.lastOrNull()?.mainScore ?: 0.0) < (other.reports.lastOrNull()?.mainScore ?: 0.0)) return -1
        return 0
    }
}
