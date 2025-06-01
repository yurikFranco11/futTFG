package com.example.futtfg.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.futtfg.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserPurchasesViewModel : ViewModel() {
    private val _userPurchases = MutableLiveData<List<Product>>()
    val userPurchases: LiveData<List<Product>> = _userPurchases

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun loadUserPurchases() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Debes iniciar sesión para ver tus compras"
            return
        }

        _isLoading.value = true
        db.collection("purchases")
            .whereEqualTo("buyerId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val purchasesList = documents.mapNotNull { document ->
                    try {
                        Product(
                            id = document.id,
                            name = document.getString("productName") ?: "",
                            description = document.getString("description") ?: "",
                            price = document.getDouble("price") ?: 0.0,
                            category = document.getString("category") ?: "",
                            selectedImageUrl = document.getString("productImage") ?: "",
                            sellerId = document.getString("sellerId") ?: "",
                            sellerName = "" // No necesitamos el nombre del vendedor en el historial
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _userPurchases.value = purchasesList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar tus compras: ${e.message}"
                _isLoading.value = false
            }
    }
} 