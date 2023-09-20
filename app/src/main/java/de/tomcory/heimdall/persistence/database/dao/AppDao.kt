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
    suspend fun getAppWithReport(packageName: String): AppWithReport

    @Transaction
    @Query("SELECT * FROM App")
    suspend fun getAllAppWithReports(): List<AppWithReport>

    @Transaction
    @Query("SELECT * FROM App")
    fun getAllAppWithReportsObservable(): Flow<List<AppWithReport>>

}

data class AppWithReport(
    @Embedded val app: App,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName"
    )
    val report: Report?
): Comparable<AppWithReport> {
    override operator fun compareTo(other: AppWithReport): Int {
        if (this.app.packageName > other.app.packageName) return 1
        if (this.app.packageName < other.app.packageName) return -1
        if ((this.report?.mainScore ?: 0.0) > (other.report?.mainScore ?: 0.0)) return 1
        if ((this.report?.mainScore ?: 0.0) < (other.report?.mainScore ?: 0.0)) return -1
        return 0
    }
}
