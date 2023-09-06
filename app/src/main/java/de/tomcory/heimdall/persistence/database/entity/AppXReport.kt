package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

class AppXReport {
    @Entity(primaryKeys = ["packageName", "reportId"])
    data class AppXReport(
            @ColumnInfo(index = true)
            val packageName: String,
            @ColumnInfo(index = true, name = "reportId")
            val reportId: Int
    )

    data class AppWithReports(
            @Embedded val app: App,
            @Relation(
                    parentColumn = "packageName",
                    entityColumn = "reportId",
            )
            val reports: List<Report>
    )
}