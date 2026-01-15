package com.quotevault.data.repository

import com.quotevault.data.remote.CategoryDto
import com.quotevault.data.remote.CollectionDto
import com.quotevault.data.remote.CollectionQuoteDto
import com.quotevault.data.remote.QuoteDto
import com.quotevault.data.remote.UserFavoriteDto
import com.quotevault.domain.model.Quote
import com.quotevault.domain.model.QuoteCollection
import com.quotevault.domain.model.RepoError
import com.quotevault.domain.repository.QuoteRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : QuoteRepository {

    override suspend fun getQuotes(
        page: Int,
        pageSize: Int,
        searchQuery: String?,
        category: String?,
        author: String?
    ): Result<List<Quote>> {
        return try {
            val from = (page - 1) * pageSize
            val to = from + pageSize - 1
            
            val userId = supabase.auth.currentUserOrNull()?.id
            android.util.Log.d("QuoteRepo", "getQuotes: page=$page, searchQuery=$searchQuery, category=$category, author=$author, userId=$userId")

            // strict filtering using !inner join if category is present
            // this ensures we only get quotes that match the category name relationship
            val columns = if (!category.isNullOrBlank()) {
                Columns.raw("*, categories!inner(id, name)") 
            } else {
                Columns.raw("*, categories(id, name)")
            }
            
            // escape special characters for sql like pattern
            val sanitizedQuery = searchQuery?.let { query ->
                query.replace("\\", "\\\\")
                     .replace("%", "\\%")
                     .replace("_", "\\_")
                     .replace("\"", "")
                     .replace("'", "''")
                     .trim()
            }

            val quoteDtos = supabase.postgrest.from("quotes").select(
                columns = columns
            ) {
                filter {
                    if (!sanitizedQuery.isNullOrBlank()) {
                        // search in text or author
                        or {
                           ilike("text", "%$sanitizedQuery%")
                           ilike("author", "%$sanitizedQuery%")
                        }
                    }
                    if (!category.isNullOrBlank()) {
                        eq("categories.name", category)
                    }
                    if (!author.isNullOrBlank()) {
                        eq("author", author)
                    }
                }
                order("created_at", order = Order.DESCENDING)
                range(from.toLong(), to.toLong())
            }.decodeList<QuoteDto>()
            
            android.util.Log.d("QuoteRepo", "getQuotes: Fetched ${quoteDtos.size} quotes")

            // fetch favorites map if logged in
            val favoriteIds = if (userId != null) {
                supabase.postgrest.from("favorites").select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<UserFavoriteDto>().map { it.quoteId }.toSet()
            } else {
                emptySet()
            }

            val quotes = quoteDtos.map { dto ->
                Quote(
                    id = dto.id,
                    content = dto.text,
                    author = dto.author,
                    category = dto.categoryData?.name ?: "Unknown",
                    isFavorite = favoriteIds.contains(dto.id)
                )
            }
            
            Result.success(quotes)
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "getQuotes failed", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(quote: Quote): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
            android.util.Log.d("QuoteRepo", "toggleFavorite: userId=$userId, quoteId=${quote.id}")
            
            if (userId == null) return Result.failure(RepoError.AuthRequired)
            
            // check db state for truth
            val exists = supabase.postgrest.from("favorites").select(head = true) {
                count(Count.EXACT)
                filter {
                     eq("user_id", userId)
                     eq("quote_id", quote.id)
                }
            }.countOrNull() ?: 0 > 0
            
            if (exists) {
                // Delete
                supabase.postgrest.from("favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("quote_id", quote.id)
                    }
                }
                android.util.Log.d("QuoteRepo", "toggleFavorite: Deleted")
            } else {
                // Insert
                val dto = UserFavoriteDto(
                    userId = userId,
                    quoteId = quote.id
                )
                supabase.postgrest.from("favorites").insert(dto) 
                android.util.Log.d("QuoteRepo", "toggleFavorite: Inserted")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "toggleFavorite failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getFavorites(): Result<List<Quote>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.failure(RepoError.AuthRequired)
            android.util.Log.d("QuoteRepo", "getFavorites: userId=$userId")
            
            // Fetch favorites
            val favorites = supabase.postgrest.from("favorites").select {
                filter { eq("user_id", userId) }
            }.decodeList<UserFavoriteDto>()
            
            android.util.Log.d("QuoteRepo", "getFavorites: Found ${favorites.size} favorite entries")

            if (favorites.isEmpty()) return Result.success(emptyList())
            
            val quoteIds = favorites.map { it.quoteId }
            val quoteDtos = supabase.postgrest.from("quotes").select(
                 columns = Columns.raw("*, categories(name)")
            ) {
                filter {
                    isIn("id", quoteIds)
                }
            }.decodeList<QuoteDto>()
            
            val quotes = quoteDtos.map { dto ->
                Quote(
                    id = dto.id,
                    content = dto.text,
                    author = dto.author,
                    category = dto.categoryData?.name ?: "Unknown",
                    isFavorite = true
                )
            }
            Result.success(quotes)
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "getFavorites failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getFavoritesCount(): Result<Int> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.success(0)
            val count = supabase.postgrest.from("favorites").select(head = true) {
                count(Count.EXACT)
                filter { eq("user_id", userId) }
            }.countOrNull() ?: 0
            Result.success(count.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<List<String>> {
        return try {
            val categories = supabase.postgrest.from("categories")
                .select()
                .decodeList<CategoryDto>()
                .map { it.name }
                .distinct()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuthors(): Result<List<String>> {
        return try {
            val authors = supabase.postgrest.from("quotes")
                .select(columns = Columns.raw("author"))
                .decodeList<QuoteDto>()
                .mapNotNull { it.author }
                .distinct()
                .sorted()
            Result.success(authors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createCollection(name: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.failure(RepoError.AuthRequired)
            val dto = CollectionDto(
                userId = userId,
                name = name
            )
            // 'lists' table
            supabase.postgrest.from("lists").insert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCollections(): Result<List<QuoteCollection>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.failure(RepoError.AuthRequired)
            // 'lists' table
            val dtos = supabase.postgrest.from("lists").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<CollectionDto>()
            
            val collections = dtos.map { 
                QuoteCollection(
                    id = it.id ?: "",
                    name = it.name,
                    userId = it.userId
                )
            }
            Result.success(collections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addQuoteToCollection(collectionId: String, quoteId: String): Result<Unit> {
        return try {
             // 'list_quotes' table
            val dto = CollectionQuoteDto(
                listId = collectionId,
                quoteId = quoteId
            )
            supabase.postgrest.from("list_quotes").upsert(dto, onConflict = "list_id, quote_id")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeQuoteFromCollection(collectionId: String, quoteId: String): Result<Unit> {
        return try {
            // 'list_quotes' table
            supabase.postgrest.from("list_quotes").delete {
                filter {
                    eq("list_id", collectionId)
                    eq("quote_id", quoteId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.failure(RepoError.AuthRequired)
            
            // first delete all quotes in this collection (cascade)
            supabase.postgrest.from("list_quotes").delete {
                filter {
                    eq("list_id", collectionId)
                }
            }
            
            // then delete the collection itself
            supabase.postgrest.from("lists").delete {
                filter {
                    eq("id", collectionId)
                    eq("user_id", userId)
                }
            }
            
            android.util.Log.d("QuoteRepo", "deleteCollection: Deleted collection $collectionId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "deleteCollection failed", e)
            Result.failure(e)
        }
    }

    override suspend fun renameCollection(collectionId: String, newName: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.failure(RepoError.AuthRequired)
            
            supabase.postgrest.from("lists").update({
                set("name", newName)
            }) {
                filter {
                    eq("id", collectionId)
                    eq("user_id", userId)
                }
            }
            
            android.util.Log.d("QuoteRepo", "renameCollection: Renamed $collectionId to $newName")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "renameCollection failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getDailyQuote(categories: List<String>): Result<Quote> {
        return try {
            val columns = if (categories.isNotEmpty()) {
                Columns.raw("*, categories!inner(name)") 
            } else {
                Columns.raw("*, categories(name)")
            }

            // count from 'quotes'
            val count = supabase.postgrest.from("quotes").select(head = true) { 
                count(Count.EXACT) 
                if (categories.isNotEmpty()) {
                    filter {
                         isIn("categories.name", categories)
                    }
                }
            }.countOrNull() ?: 0
            
            if (count == 0L) throw Exception("No quotes available")

            val today = java.time.LocalDate.now()
            val seed = (today.year * 366 + today.dayOfYear).toLong()
            val index = seed % count
            
            val dto = supabase.postgrest.from("quotes").select(
                  columns = columns
            ) {
                 if (categories.isNotEmpty()) {
                    filter {
                         isIn("categories.name", categories)
                    }
                }
                range(index, index)
                limit(1)
            }.decodeSingle<QuoteDto>()
            
            val userId = supabase.auth.currentUserOrNull()?.id
            val isFavorite = if (userId != null) {
                 val favCount = supabase.postgrest.from("favorites").select(head = true) {
                    count(Count.EXACT)
                    filter {
                        eq("user_id", userId)
                        eq("quote_id", dto.id)
                    }
                }.countOrNull() ?: 0
                favCount > 0
            } else false

            Result.success(Quote(dto.id, dto.text, dto.author, dto.categoryData?.name ?: "Unknown", isFavorite))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuote(id: String): Result<Quote> {
        return try {
            val dto = supabase.postgrest.from("quotes").select(
                  columns = Columns.raw("*, categories(name)")
            ) {
                filter {
                    eq("id", id)
                }
            }.decodeSingle<QuoteDto>()
            
             val userId = supabase.auth.currentUserOrNull()?.id
            val isFavorite = if (userId != null) {
                 val favCount = supabase.postgrest.from("favorites").select(head = true) {
                    count(Count.EXACT)
                    filter {
                        eq("user_id", userId)
                        eq("quote_id", dto.id)
                    }
                }.countOrNull() ?: 0
                favCount > 0
            } else false

            Result.success(Quote(dto.id, dto.text, dto.author, dto.categoryData?.name ?: "Unknown", isFavorite))
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "getQuote failed for id=$id", e)
            Result.failure(e)
        }
    }

    override suspend fun getCollectionQuotes(collectionId: String): Result<List<Quote>> {
        return try {
            // list_quotes -> quote_id
            val listQuotes = supabase.postgrest.from("list_quotes").select {
                filter { eq("list_id", collectionId) }
            }.decodeList<CollectionQuoteDto>()
            
            if (listQuotes.isEmpty()) return Result.success(emptyList())
            
            val quoteIds = listQuotes.map { it.quoteId }
            val quoteDtos = supabase.postgrest.from("quotes").select(
                 columns = Columns.raw("*, categories(name)")
            ) {
                filter {
                    isIn("id", quoteIds)
                }
            }.decodeList<QuoteDto>()
            
            // Need to check favorites status for these too
            val userId = supabase.auth.currentUserOrNull()?.id
            val favoriteIds = if (userId != null) {
                supabase.postgrest.from("favorites").select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<UserFavoriteDto>().map { it.quoteId }.toSet()
            } else {
                emptySet()
            }

            val quotes = quoteDtos.map { dto ->
                Quote(
                    id = dto.id,
                    content = dto.text,
                    author = dto.author,
                    category = dto.categoryData?.name ?: "Unknown",
                    isFavorite = favoriteIds.contains(dto.id)
                )
            }
            Result.success(quotes)
        } catch (e: Exception) {
             android.util.Log.e("QuoteRepo", "getCollectionQuotes failed", e)
             Result.failure(e)
        }
    }

    override suspend fun getCollection(collectionId: String): Result<QuoteCollection?> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.failure(RepoError.AuthRequired)
            
            val dto = supabase.postgrest.from("lists").select {
                filter {
                    eq("id", collectionId)
                    eq("user_id", userId)
                }
            }.decodeSingleOrNull<CollectionDto>()
            
            val collection = dto?.let {
                QuoteCollection(
                    id = it.id ?: "",
                    name = it.name,
                    userId = it.userId
                )
            }
            Result.success(collection)
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "getCollection failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllSavedQuoteIds(): Result<Set<String>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.failure(RepoError.AuthRequired)
            
            // Get all quote IDs from list_quotes where the list belongs to this user
            val collections = supabase.postgrest.from("lists").select {
                filter { eq("user_id", userId) }
            }.decodeList<CollectionDto>()
            
            val collectionIds = collections.mapNotNull { it.id }
            
            if (collectionIds.isEmpty()) {
                return Result.success(emptySet())
            }
            
            val listQuotes = supabase.postgrest.from("list_quotes").select {
                filter { isIn("list_id", collectionIds) }
            }.decodeList<CollectionQuoteDto>()
            
            val quoteIds = listQuotes.map { it.quoteId }.toSet()
            android.util.Log.d("QuoteRepo", "getAllSavedQuoteIds: Found ${quoteIds.size} saved quotes")
            Result.success(quoteIds)
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "getAllSavedQuoteIds failed", e)
            Result.failure(e)
        }
    }
}
