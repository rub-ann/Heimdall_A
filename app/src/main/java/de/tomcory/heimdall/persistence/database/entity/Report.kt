package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Data class and database entity for storing privacy reports about apps.
 * Primary key is [reportId] is generated automatically on insert and should not be set manually.
 * Expecting [appPackageName] as app name that is reported about and [timestamp] usually as current time in Milliseconds.
 * [mainScore] is the total score of the app and should be between 0 and 1.
 *
 * Offers [toString] for pretty debugging output.
 * For querying together with app info or sub-reports, consider [de.tomcory.heimdall.persistence.database.dao.AppWithReports].or [de.tomcory.heimdall.persistence.database.dao.ReportWithSubReport].
 */
@Serializable
@Entity
data class Report(
        // auto generate id
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(index = true)
        val reportId: Long = 0,
        val appPackageName: String,

        // generate Timestamp from database if not set
        @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
        val timestamp: Long,
        val mainScore: Double
) {
        override fun toString(): String {
                return """
                        Score Evaluation Report of $reportId:
                                package: $appPackageName
                                timestamp: $timestamp
                                Score: $mainScore
                """.trimIndent()
        }
}