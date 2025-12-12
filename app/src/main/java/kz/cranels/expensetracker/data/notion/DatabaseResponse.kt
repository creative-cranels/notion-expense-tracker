package kz.cranels.expensetracker.data.notion

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DatabaseResponse(
    val properties: Properties
)

@JsonClass(generateAdapter = true)
data class Properties(
    @Json(name = "Category")
    val category: CategoryProperty
)

@JsonClass(generateAdapter = true)
data class CategoryProperty(
    val relation: Relation
)

@JsonClass(generateAdapter = true)
data class Relation(
    @Json(name = "database_id")
    val databaseId: String
)
