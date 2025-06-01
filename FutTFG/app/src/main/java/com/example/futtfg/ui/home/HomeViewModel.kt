package com.example.futtfg.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.futtfg.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadProducts()
    }

    fun loadProducts() {
        _isLoading.value = true
        db.collection("products")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val productsList = documents.mapNotNull { document ->
                    try {
                        Product.fromMap(document.data, document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                _products.value = productsList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar los productos: ${e.message}"
                _isLoading.value = false
            }
    }

    fun deleteProduct(productId: String, onComplete: (Boolean) -> Unit) {
        if (auth.currentUser == null) {
            _error.value = "Debes iniciar sesión para eliminar productos"
            onComplete(false)
            return
        }

        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                val product = document.data?.let { Product.fromMap(it, document.id) }
                if (product?.sellerId == auth.currentUser?.uid) {
                    // El usuario actual es el vendedor, puede eliminar el producto
                    db.collection("products").document(productId)
                        .delete()
                        .addOnSuccessListener {
                            loadProducts() // Recargar la lista
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            _error.value = "Error al eliminar el producto: ${e.message}"
                            onComplete(false)
                        }
                } else {
                    _error.value = "No tienes permiso para eliminar este producto"
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                _error.value = "Error al verificar el producto: ${e.message}"
                onComplete(false)
            }
    }

    fun isCurrentUserProduct(sellerId: String): Boolean {
        return auth.currentUser?.uid == sellerId
    }
}