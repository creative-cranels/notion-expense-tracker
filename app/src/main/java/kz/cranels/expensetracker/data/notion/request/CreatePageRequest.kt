package kz.cranels.expensetracker.data.notion.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePageRequest(
    val parent: ParentDatabase,
    val properties: ExpenseProperties
)

@JsonClass(generateAdapter = true)
data class ParentDatabase(
    @Json(name = "database_id")
    val databaseId: String
)

@JsonClass(generateAdapter = true)
data class ExpenseProperties(
    @Json(name = "Name")
    val name: TitleProperty,
    @Json(name = "Amount")
    val amount: NumberProperty,
    @Json(name = "Category")
    val category: RelationProperty,
    @Json(name = "Date")
    val date: DateProperty
)

@JsonClass(generateAdapter = true)
data class TitleProperty(
    val title: List<TitleContent>
)

@JsonClass(generateAdapter = true)
data class TitleContent(
    val text: TextContent
)

@JsonClass(generateAdapter = true)
data class TextContent(
    val content: String
)

@JsonClass(generateAdapter = true)
data class NumberProperty(
    val number: Double
)

@JsonClass(generateAdapter = true)
data class RelationProperty(
    val relation: List<RelationId>
)

@JsonClass(generateAdapter = true)
data class RelationId(
    val id: String
)

@JsonClass(generateAdapter = true)
data class DateProperty(
    val date: DateContent
)

@JsonClass(generateAdapter = true)
data class DateContent(
    val start: String // ISO 8601 date string
)
