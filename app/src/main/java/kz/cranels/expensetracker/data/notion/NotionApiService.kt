package kz.cranels.expensetracker.data.notion

import kz.cranels.expensetracker.data.notion.request.CreatePageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface NotionApiService {
    @GET("v1/databases/{databaseId}")
    suspend fun getDatabase(
        @Header("Authorization") token: String,
        @Header("Notion-Version") notionVersion: String = "2022-06-28",
        @Path("databaseId") databaseId: String
    ): Response<DatabaseResponse>

    @POST("v1/databases/{databaseId}/query")
    suspend fun queryDatabase(
        @Header("Authorization") token: String,
        @Header("Notion-Version") notionVersion: String = "2022-06-28",
        @Path("databaseId") databaseId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap() // Notion API requires an empty JSON body for a full query
    ): Response<CategoryListResponse>

    @POST("v1/pages")
    suspend fun createExpense(
        @Header("Authorization") token: String,
        @Header("Notion-Version") notionVersion: String = "2022-06-28",
        @Body request: CreatePageRequest
    ): Response<Unit> // We don't need to parse the response body for this request
}
