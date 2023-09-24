package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = Report::class)
    suspend fun insertReport(report: Report): Long

    @Query("SELECT * FROM Report ORDER BY timestamp DESC")
    fun getAll(): List<Report>

    @Transaction
    @Query("SELECT * FROM Report WHERE appPackageName = :reportId ORDER BY timestamp DESC")
    fun getReportWithSubReports(reportId: Int): ReportWithSubReport

    @Query("Select * FROM Report")
    fun getAllObservable(): Flow<List<Report>>
}

data class ReportWithSubReport(
    @Embedded
    val report: Report,
    @Relation(
        parentColumn = "reportId",
        entityColumn = "reportId"
    )
    val subReports: List<SubReport>
)

