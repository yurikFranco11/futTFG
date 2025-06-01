package com.example.futtfg.ui.sell

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.futtfg.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SellViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    private val _currentProduct = MutableLiveData<Product?>()
    val currentProduct: LiveData<Product?> = _currentProduct

    val categories = listOf(
        "Running",
        "Ropa",
        "Suplementación",
        "Botas de fútbol",
        "Equipaciones",
        "Artículos"
    )

    fun loadProduct(productId: String) {
        _isLoading.value = true
        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val product = Product.fromMap(document.data!!, document.id)
                    _currentProduct.value = product
                } else {
                    _error.value = "No se encontró el producto"
                }
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar el producto: ${e.message}"
                _isLoading.value = false
            }
    }

    fun uploadProduct(
        name: String,
        description: String,
        price: Double,
        category: String,
        imageUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (auth.currentUser == null) {
            _error.value = "Debes iniciar sesión para publicar un producto"
            onComplete(false)
            return
        }

        _isLoading.value = true
        val productId = db.collection("products").document().id
        val sellerId = auth.currentUser!!.uid

        db.collection("users").document(sellerId).get()
            .addOnSuccessListener { document ->
                val sellerName = "${document.getString("name")} ${document.getString("lastName")}"
                
                val product = Product(
                    id = productId,
                    name = name,
                    description = description,
                    price = price,
                    category = category,
                    selectedImageUrl = imageUrl,
                    currentProductId = productId,
                    sellerId = sellerId,
                    sellerName = sellerName
                )

                db.collection("products").document(productId)
                    .set(product.toMap())
                    .addOnSuccessListener {
                        _isLoading.value = false
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        _error.value = "Error al guardar el producto: ${e.message}"
                        _isLoading.value = false
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                _error.value = "Error al obtener datos del vendedor: ${e.message}"
                _isLoading.value = false
                onComplete(false)
            }
    }

    fun updateProduct(
        productId: String,
        name: String,
        description: String,
        price: Double,
        category: String,
        selectedImageUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        _isLoading.value = true
        
        val updates = mapOf(
            "name" to name,
            "description" to description,
            "price" to price,
            "category" to category,
            "selectedImageUrl" to selectedImageUrl
        )

        db.collection("products").document(productId)
            .update(updates)
            .addOnSuccessListener {
                _success.value = true
                _isLoading.value = false
                onComplete(true)
            }
            .addOnFailureListener { e ->
                _error.value = "Error al actualizar el producto: ${e.message}"
                _isLoading.value = false
                onComplete(false)
            }
    }
} 