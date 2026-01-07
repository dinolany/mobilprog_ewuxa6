package com.example.shoppinglistapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class Whislist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dueDate: String, // "yyyy-MM-dd" formátumban
    val totalItems: Int = 0,    // Kiszámolt érték
    val boughtItems: Int = 0    // Kiszámolt érték
)