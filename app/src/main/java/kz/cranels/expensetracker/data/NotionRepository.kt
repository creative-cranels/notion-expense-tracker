package kz.cranels.expensetracker.data

import android.util.Log
import kz.cranels.expensetracker.data.local.Category
import kz.cranels.expensetracker.data.local.CategoryDao
import kz.cranels.expensetracker.data.notion.NotionApiService
import kz.cranels.expensetracker.data.notion.request.CreatePageRequest
import kz.cranels.expensetracker.data.notion.request.DateContent
import kz.cranels.expensetracker.data.notion.request.DateProperty
import kz.cranels.expensetracker.data.notion.request.ExpenseProperties
import kz.cranels.expensetracker.data.notion.request.NumberProperty
import kz.cranels.expensetracker.data.notion.request.ParentDatabase
import kz.cranels.expensetracker.data.notion.request.RelationId
import kz.cranels.expensetracker.data.notion.request.RelationProperty
import kz.cranels.expensetracker.data.notion.request.TextContent
import kz.cranels.expensetracker.data.notion.request.TitleContent
import kz.cranels.expensetracker.data.notion.request.TitleProperty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class NotionRepository(
    private val notionApiService: NotionApiService,
    private val categoryDao: CategoryDao
) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun syncCategories(token: String, databaseId: String): Boolean {
        return try {
            // Step 1: Get the ID of the related categories database
            val dbResponse = notionApiService.getDatabase(
                token = "Bearer $token",
                databaseId = databaseId
            )

            if (dbResponse.isSuccessful && dbResponse.body() != null) {
                val categoriesDatabaseId = dbResponse.body()!!.properties.category.relation.databaseId

                // Step 2: Query the categories database to get the list of categories
                val categoryListResponse = notionApiService.queryDatabase(
                    token = "Bearer $token",
                    databaseId = categoriesDatabaseId
                )

                if (categoryListResponse.isSuccessful && categoryListResponse.body() != null) {
                    val categories = categoryListResponse.body()!!.results.mapNotNull {
                        val id = it.id
                        val name = it.properties.name.title.firstOrNull()?.plainText
                        if (name != null) {
                            Category(id = id, name = name)
                        } else {
                            null
                        }
                    }

                    // Step 3: Clear old categories and save the new ones to the local database
                    categoryDao.clearAll()
                    categoryDao.insertAll(categories)
                    Log.d("NotionRepository", "Successfully synced ${categories.size} categories.")
                    true
                } else {
                    Log.e("NotionRepository", "Failed to query categories database: ${categoryListResponse.errorBody()?.string()}")
                    false
                }
            } else {
                Log.e("NotionRepository", "Failed to get database info: ${dbResponse.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("NotionRepository", "An error occurred during category sync", e)
            false
        }
    }

    suspend fun createExpense(
        token: String,
        databaseId: String,
        description: String,
        amount: Double,
        categoryId: String,
        date: Date
    ): Boolean {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val request = CreatePageRequest(
                parent = ParentDatabase(databaseId = databaseId),
                properties = ExpenseProperties(
                    name = TitleProperty(listOf(TitleContent(TextContent(description)))),
                    amount = NumberProperty(amount),
                    category = RelationProperty(listOf(RelationId(categoryId))),
                    date = DateProperty(DateContent(isoFormat.format(date)))
                )
            )

            val response = notionApiService.createExpense(
                token = "Bearer $token",
                request = request
            )

            if (response.isSuccessful) {
                Log.d("NotionRepository", "Successfully created expense.")
                true
            } else {
                Log.e("NotionRepository", "Failed to create expense: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("NotionRepository", "An error occurred during expense creation", e)
            false
        }
    }
}
