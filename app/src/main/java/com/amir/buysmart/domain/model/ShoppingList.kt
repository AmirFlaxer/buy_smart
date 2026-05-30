package com.amir.buysmart.domain.model

data class ShoppingList(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val inviteCode: String = "",
    val customLocations: List<String> = emptyList()
)
