package com.example.futtfg.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.futtfg.databinding.ItemProductBinding
import com.example.futtfg.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onEditClick: (Product) -> Unit,
    private val onBuyClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val auth = FirebaseAuth.getInstance()
    private var showButtons = true
    private var isClickable = true

    fun setShowButtons(show: Boolean) {
        showButtons = show
        notifyDataSetChanged()
    }

    fun setClickable(clickable: Boolean) {
        isClickable = clickable
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                textViewProductName.text = product.name
                textViewProductPrice.text = String.format("%.2f €", product.price)
                textViewProductCategory.text = product.category

                // Cargar la imagen usando Picasso
                if (product.selectedImageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(product.selectedImageUrl)
                        .into(imageViewProduct)
                }

                // Configurar visibilidad y listeners de los botones
                if (showButtons) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        if (product.sellerId == currentUser.uid) {
                            // El usuario actual es el vendedor
                            buttonEdit.visibility = View.VISIBLE
                            buttonBuy.visibility = View.GONE
                            buttonEdit.setOnClickListener { onEditClick(product) }
                        } else {
                            // El usuario actual es un potencial comprador
                            buttonEdit.visibility = View.GONE
                            buttonBuy.visibility = View.VISIBLE
                            buttonBuy.setOnClickListener { onBuyClick(product) }
                        }
                    } else {
                        // No hay usuario logueado
                        buttonEdit.visibility = View.GONE
                        buttonBuy.visibility = View.GONE
                    }
                } else {
                    // Ocultar todos los botones en modo historial
                    buttonEdit.visibility = View.GONE
                    buttonBuy.visibility = View.GONE
                }

                // Configurar el click listener del item completo solo si está habilitado
                root.isClickable = isClickable
                if (isClickable) {
                    root.setOnClickListener { onProductClick(product) }
                } else {
                    root.setOnClickListener(null)
                }
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
} 