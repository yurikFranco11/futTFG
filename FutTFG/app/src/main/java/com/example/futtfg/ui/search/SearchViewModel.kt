package com.example.futtfg.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.futtfg.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val _searchResults = MutableLiveData<List<Product>>()
    val searchResults: LiveData<List<Product>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var searchJob: Job? = null
    private val db = FirebaseFirestore.getInstance()
    
    val categories = listOf(
        "Running",
        "Ropa",
        "Suplementación",
        "Botas de fútbol",
        "Equipaciones",
        "Artículos"
    ).sorted()

    init {
        // Verificar productos existentes al iniciar
        checkExistingProducts()
    }

    private fun checkExistingProducts() {
        db.collection("products").get()
            .addOnSuccessListener { documents ->
                Log.d("SearchViewModel", "Total productos en la base de datos: ${documents.size()}")
                documents.forEach { doc ->
                    val product = doc.toObject(Product::class.java)
                    Log.d("SearchViewModel", "Producto encontrado - ID: ${doc.id}, " +
                        "Nombre: ${product.name}, " +
                        "Categoría: ${product.category}, " +
                        "Vendido: ${product.isSold}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("SearchViewModel", "Error al verificar productos: ${e.message}")
            }
    }

    fun searchProducts(query: String, selectedCategory: String?) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce para evitar demasiadas consultas
            _isLoading.value = true
            Log.d("SearchViewModel", "Iniciando búsqueda - query: $query, categoría: $selectedCategory")
            
            try {
                // Primero, obtener todos los productos no vendidos
                var productsRef = db.collection("products")
                    .whereEqualTo("isSold", false)

                // Añadir filtro por categoría si está seleccionada
                if (!selectedCategory.isNullOrEmpty()) {
                    Log.d("SearchViewModel", "Aplicando filtro por categoría: $selectedCategory")
                    productsRef = productsRef.whereEqualTo("category", selectedCategory)
                }

                // Ordenar por fecha
                productsRef = productsRef.orderBy("createdAt", Query.Direction.DESCENDING)

                productsRef.get()
                    .addOnSuccessListener { documents ->
                        Log.d("SearchViewModel", "Documentos recibidos: ${documents.size()}")
                        
                        documents.forEach { doc ->
                            val product = doc.toObject(Product::class.java)
                            Log.d("SearchViewModel", "Producto en resultados - ID: ${doc.id}, " +
                                "Nombre: ${product.name}, " +
                                "Categoría: ${product.category}, " +
                                "Vendido: ${product.isSold}")
                        }
                        
                        val products = documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Product::class.java).also { product ->
                                    product.id = doc.id
                                }
                            } catch (e: Exception) {
                                Log.e("SearchViewModel", "Error al procesar producto: ${e.message}")
                                null
                            }
                        }
                        
                        Log.d("SearchViewModel", "Productos procesados: ${products.size}")

                        // Filtrar por búsqueda de texto si es necesario
                        val filteredProducts = if (query.isNotEmpty()) {
                            products.filter { product ->
                                product.name.lowercase().contains(query.lowercase())
                            }
                        } else {
                            products
                        }

                        Log.d("SearchViewModel", "Productos filtrados finales: ${filteredProducts.size}")
                        _searchResults.value = filteredProducts
                        _isLoading.value = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("SearchViewModel", "Error en la consulta: ${e.message}")
                        _error.value = "Error al buscar productos: ${e.message}"
                        _searchResults.value = emptyList()
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error inesperado: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
                _searchResults.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }
} 