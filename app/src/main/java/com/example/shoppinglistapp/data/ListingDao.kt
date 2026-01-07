package com.example.shoppinglistapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ListingDao {
    @Query("SELECT * FROM shopping_lists")
    fun getAllLists(): LiveData<List<Whislist>>
    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    fun getItemsForList(listId: Int): LiveData<List<ListItem>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    suspend fun getItemsForListSync(listId: Int): List<ListItem>

    @Insert
    suspend fun insertList(list: Whislist)

    @Delete
    suspend fun deleteList(list: Whislist)


    @Insert
    suspend fun insertItem(item: ListItem)

    @Update
    suspend fun updateItem(item: ListItem)

    @Delete
    suspend fun deleteItem(item: ListItem)
}