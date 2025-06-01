package com.example.futtfg.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.futtfg.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class ProductDetailViewModel : ViewModel() {
    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _purchaseSuccess = MutableLiveData<Boolean>()
    val purchaseSuccess: LiveData<Boolean> = _purchaseSuccess

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun loadProduct(productId: String) {
        _isLoading.value = true
        db.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->
                _isLoading.value = false
                if (document != null && document.exists()) {
                    val product = document.toObject(Product::class.java)?.apply {
                        id = document.id
                    }
                    _product.value = product
                } else {
                    _error.value = "Producto no encontrado"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Error al cargar el producto: ${e.message}"
            }
    }

    fun purchaseProduct(product: Product) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Debes iniciar sesión para comprar"
            return
        }

        _isLoading.value = true

        // Crear el documento de compra con todos los campos
        val purchase = hashMapOf(
            "buyerId" to currentUser.uid,
            "price" to product.price,
            "productDescription" to product.description,
            "productId" to product.id,
            "productImage" to product.selectedImageUrl,
            "productName" to product.name,
            "purchaseDate" to Date(),
            "sellerId" to product.sellerId,
            "status" to "completed",
            "transactionId" to generateTransactionId()
        )

        db.runTransaction { transaction ->
            // Verificar que el producto aún existe y está disponible
            val productRef = db.collection("products").document(product.id)
            val productSnapshot = transaction.get(productRef)
            
            if (!productSnapshot.exists()) {
                throw Exception("El producto ya no está disponible")
            }

            // Crear la compra
            val purchaseRef = db.collection("purchases").document()
            transaction.set(purchaseRef, purchase)
            
            // Eliminar el producto
            transaction.delete(productRef)
        }.addOnSuccessListener {
            _isLoading.value = false
            _purchaseSuccess.value = true
        }.addOnFailureListener { e ->
            _isLoading.value = false
            _error.value = "Error al realizar la compra: ${e.message}"
        }
    }

    private fun generateTransactionId(): String {
        return "f" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 31)
    }
} 