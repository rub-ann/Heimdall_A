package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.coroutines.flow.Flow

@Dao
interface SubReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubReport(subReport: SubReport): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubReport(subReport: List<SubReport>): List<Long>

    @Query("SELECT * FROM SubReport ORDER BY packageName ASC")
    fun getAll(): List<SubReport>

    @Query("SELECT * FROM SubReport WHERE packageName = :packageName")
    fun getSubReportsByPackageName(packageName: String): List<SubReport>

    @Query("SELECT * FROM SubReport WHERE packageName = :packageName AND module = :moduleName")
    fun getSubReportsByPackageNameAndModule(packageName: String, moduleName: String): SubReport

    @Query("Select * FROM SubReport")
    fun getAllObservable(): Flow<List<SubReport>>
}
