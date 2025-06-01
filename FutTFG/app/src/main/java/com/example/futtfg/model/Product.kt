package com.example.futtfg.model

data class Product(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val selectedImageUrl: String = "",
    val currentProductId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSold: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "price" to price,
            "category" to category,
            "selectedImageUrl" to selectedImageUrl,
            "currentProductId" to currentProductId,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "timestamp" to timestamp,
            "isSold" to isSold
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String = ""): Product {
            return Product(
                id = id,
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                category = map["category"] as? String ?: "",
                selectedImageUrl = map["selectedImageUrl"] as? String ?: "",
                currentProductId = map["currentProductId"] as? String ?: "",
                sellerId = map["sellerId"] as? String ?: "",
                sellerName = map["sellerName"] as? String ?: "",
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                isSold = map["isSold"] as? Boolean ?: false
            )
        }
    }
} 