package com.example.eform.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_favorite_forms",
    primaryKeys = ["userId", "formId"], // Composite primary key
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"], // Pastikan 'id' adalah primary key di UserEntity
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FormEntity::class,
            parentColumns = ["id"], // Pastikan 'id' adalah primary key di FormEntity
            childColumns = ["formId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["formId"])]
)
data class UserFavoriteFormEntity(
    val userId: Int,
    val formId: Int
)