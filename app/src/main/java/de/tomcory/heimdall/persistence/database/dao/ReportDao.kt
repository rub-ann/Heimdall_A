package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.tomcory.heimdall.persistence.database.entity.Report

interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(vararg report: Report)

    @Query("SELECT * FROM Report")
    suspend fun getAll(): List<Report>

    @Transaction
    @Query("SELECT * FROM Report WHERE packageName = :packageName")
    suspend fun getReportWithApp(packageName: String): List<Report>
}


