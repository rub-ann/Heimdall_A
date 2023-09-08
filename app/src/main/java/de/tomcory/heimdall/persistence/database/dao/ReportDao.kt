package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.tomcory.heimdall.persistence.database.entity.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(vararg report: Report)

    @Query("SELECT * FROM Report")
    fun getAll(): List<Report>

    @Transaction
    @Query("SELECT * FROM Report WHERE packageName = :packageName")
    fun getAppWithReports(packageName: String): List<Report>

    @Query("Select * FROM Report")
    fun getAllObservable(): Flow<List<Report>>
}


