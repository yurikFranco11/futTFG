package com.example.futtfg.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.futtfg.databinding.FragmentProductDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProductDetailFragment : Fragment() {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()
    private lateinit var viewModel: ProductDetailViewModel
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ProductDetailViewModel::class.java]

        setupToolbar()
        setupObservers()
        setupBuyButton()
        
        viewModel.loadProduct(args.productId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupBuyButton() {
        binding.buttonBuy.setOnClickListener {
            viewModel.product.value?.let { product ->
                viewModel.purchaseProduct(product)
            }
        }
    }

    private fun setupObservers() {
        viewModel.product.observe(viewLifecycleOwner) { product ->
            if (product != null) {
                binding.apply {
                    textViewProductName.text = product.name
                    textViewPrice.text = String.format("%.2f €", product.price)
                    textViewCategory.text = product.category
                    textViewDescription.text = product.description
                    textViewSellerInfo.text = "Vendido por: ${product.sellerName}"

                    if (product.selectedImageUrl.isNotEmpty()) {
                        Picasso.get()
                            .load(product.selectedImageUrl)
                            .into(imageViewProduct)
                    }

                    // Mostrar/ocultar botón de comprar según el vendedor
                    val currentUser = auth.currentUser
                    buttonBuy.isVisible = currentUser != null && currentUser.uid != product.sellerId
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.buttonBuy.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.purchaseSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "¡Compra realizada con éxito!", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 