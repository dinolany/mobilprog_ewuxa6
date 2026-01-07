package com.example.shoppinglistapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ListItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listId: Int,
    val name: String,
    val category: String,
    val quantity: Int,
    val unit: String,
    val isBought: Boolean = false,
    val isUrgent: Boolean = false
)