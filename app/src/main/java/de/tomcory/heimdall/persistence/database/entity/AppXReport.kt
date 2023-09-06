package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

class AppXReport {
    data class AppWithReports(
            @Embedded val app: App,
            @Relation(
                    parentColumn = "packageName",
                    entityColumn = "reportId",
            )
            val reports: List<Report>
    )
}