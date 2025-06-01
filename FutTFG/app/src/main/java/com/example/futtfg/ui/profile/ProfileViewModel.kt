package com.example.futtfg.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.futtfg.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date
import java.util.UUID
import com.google.firebase.firestore.Query
import com.google.firebase.storage.ktx.storage

class ProfileViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage.reference
    
    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _userData = MutableLiveData<Map<String, String>>(emptyMap())
    val userData: LiveData<Map<String, String>> = _userData

    private val _profilePhotoUrl = MutableLiveData<String>()
    val profilePhotoUrl: LiveData<String> = _profilePhotoUrl

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userProducts = MutableLiveData<List<Product>>()
    val userProducts: LiveData<List<Product>> = _userProducts

    private val _userPurchases = MutableLiveData<List<Product>>()
    val userPurchases: LiveData<List<Product>> = _userPurchases

    private val _purchaseSuccess = MutableLiveData<Boolean>()
    val purchaseSuccess: LiveData<Boolean> = _purchaseSuccess

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        _user.value = auth.currentUser
        auth.currentUser?.let { user ->
            loadUserData(user.uid)
        }
    }

    private fun loadUserData(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val data = document.data
                if (data != null) {
                    _userData.value = mapOf(
                        "name" to (data["name"] as? String ?: ""),
                        "lastName" to (data["lastName"] as? String ?: ""),
                        "email" to (data["email"] as? String ?: ""),
                        "photoUrl" to (data["photoUrl"] as? String ?: "")
                    )
                    _profilePhotoUrl.value = data["photoUrl"] as? String ?: ""
                }
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar datos: ${e.message}"
            }
    }

    fun loadUserProducts() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Debes iniciar sesión para ver tus productos"
            return
        }

        _isLoading.value = true
        db.collection("products")
            .whereEqualTo("sellerId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val productsList = documents.mapNotNull { document ->
                    try {
                        Product.fromMap(document.data, document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                _userProducts.value = productsList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar tus productos: ${e.message}"
                _isLoading.value = false
            }
    }

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
                            id = document.getString("productId") ?: "",
                            name = document.getString("productName") ?: "",
                            description = document.getString("productDescription") ?: "",
                            price = (document.get("price") as? Number)?.toDouble() ?: 0.0,
                            selectedImageUrl = document.getString("productImage") ?: "",
                            sellerId = document.getString("sellerId") ?: "",
                            isSold = true
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }

                _userPurchases.value = purchasesList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Error al cargar tus compras: ${e.message}"
                _isLoading.value = false
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkAuthState()
                    onComplete(true)
                } else {
                    _error.value = task.exception?.message ?: "Error al iniciar sesión"
                    onComplete(false)
                }
            }
    }

    fun registerUser(name: String, lastName: String, email: String, password: String, photoUrl: String, onComplete: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        saveUserData(user.uid, name, lastName, email, photoUrl) { success ->
                            if (success) {
                                checkAuthState()
                            }
                            onComplete(success)
                        }
                    } else {
                        _error.value = "Error al crear el usuario"
                        onComplete(false)
                    }
                } else {
                    _error.value = task.exception?.message ?: "Error al registrar usuario"
                    onComplete(false)
                }
            }
    }

    private fun saveUserData(userId: String, name: String, lastName: String, email: String, photoUrl: String, onComplete: (Boolean) -> Unit) {
        val userData = hashMapOf(
            "name" to name,
            "lastName" to lastName,
            "email" to email,
            "photoUrl" to photoUrl
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                _error.value = "Error al guardar datos: ${e.message}"
                onComplete(false)
            }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
        _userData.value = emptyMap()
    }

    fun purchaseProduct(product: Product, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Debes iniciar sesión para comprar productos"
            onComplete(false)
            return
        }

        if (product.sellerId == currentUser.uid) {
            _error.value = "No puedes comprar tu propio producto"
            onComplete(false)
            return
        }

        if (product.isSold) {
            _error.value = "Este producto ya ha sido vendido"
            onComplete(false)
            return
        }

        _isLoading.value = true

        val purchase = hashMapOf(
            "productId" to product.id,
            "buyerId" to currentUser.uid,
            "sellerId" to product.sellerId,
            "price" to product.price,
            "purchaseDate" to Date(),
            "productName" to product.name,
            "productDescription" to product.description,
            "productImage" to product.selectedImageUrl,
            "status" to "completed",
            "transactionId" to UUID.randomUUID().toString()
        )

        db.runTransaction { transaction ->
            val productRef = db.collection("products").document(product.id)
            val productSnapshot = transaction.get(productRef)
            
            if (productSnapshot.getBoolean("isSold") == true) {
                throw Exception("El producto ya ha sido vendido")
            }

            transaction.update(productRef, "isSold", true)

            val purchaseRef = db.collection("purchases").document()
            transaction.set(purchaseRef, purchase)
        }.addOnSuccessListener {
            _purchaseSuccess.value = true
            _isLoading.value = false
            onComplete(true)
        }.addOnFailureListener { e ->
            _error.value = when {
                e.message?.contains("ya ha sido vendido") == true -> "Este producto ya ha sido vendido"
                else -> "Error al realizar la compra: ${e.message}"
            }
            _isLoading.value = false
            onComplete(false)
        }
    }

    fun uploadProfilePhoto(imageUri: Uri, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Debes iniciar sesión para cambiar la foto"
            onComplete(false)
            return
        }

        _isLoading.value = true
        val photoRef = storage.child("profile_photos/${currentUser.uid}")

        photoRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                photoRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                // Actualizar la URL en Firestore
                db.collection("users").document(currentUser.uid)
                    .update("photoUrl", downloadUri.toString())
                    .addOnSuccessListener {
                        _profilePhotoUrl.value = downloadUri.toString()
                        _isLoading.value = false
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        _error.value = "Error al actualizar la foto: ${e.message}"
                        _isLoading.value = false
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                _error.value = "Error al subir la foto: ${e.message}"
                _isLoading.value = false
                onComplete(false)
            }
    }
} 