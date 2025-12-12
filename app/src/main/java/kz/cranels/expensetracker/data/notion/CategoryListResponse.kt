package kz.cranels.expensetracker.data.notion

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CategoryListResponse(
    val results: List<CategoryPage>
)

@JsonClass(generateAdapter = true)
data class CategoryPage(
    val id: String,
    val properties: CategoryPageProperties
)

@JsonClass(generateAdapter = true)
data class CategoryPageProperties(
    @Json(name = "Name")
    val name: CategoryNameProperty
)

@JsonClass(generateAdapter = true)
data class CategoryNameProperty(
    val title: List<CategoryTitle>
)

@JsonClass(generateAdapter = true)
data class CategoryTitle(
    @Json(name = "plain_text")
    val plainText: String
)
