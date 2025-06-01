package com.example.futtfg.ui.sell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.futtfg.R
import com.example.futtfg.databinding.FragmentSellBinding
import com.squareup.picasso.Picasso

class SellFragment : Fragment() {

    private var _binding: FragmentSellBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SellViewModel
    private var imageUrl: String? = null
    private val args: SellFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SellViewModel::class.java]

        setupViews()
        setupObservers()

        args.productId?.let { productId ->
            viewModel.loadProduct(productId)
            binding.buttonPublish.text = getString(R.string.save_changes)
            binding.textViewTitle.text = getString(R.string.edit_product)
            binding.editTextImageUrl.isEnabled = false
            binding.editTextImageUrl.alpha = 0.5f
        }
    }

    private fun setupViews() {
        val categoriesAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            viewModel.categories
        )
        binding.autoCompleteCategory.setAdapter(categoriesAdapter)

        binding.editTextImageUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = binding.editTextImageUrl.text.toString()
                if (url.isNotEmpty()) {
                    loadImagePreview(url)
                }
            }
        }

        binding.buttonPublish.setOnClickListener {
            val name = binding.editTextProductName.text.toString()
            val description = binding.editTextProductDescription.text.toString()
            val priceText = binding.editTextProductPrice.text.toString()
            val category = binding.autoCompleteCategory.text.toString()

            if (validateInputs(name, description, priceText, category)) {
                val price = priceText.toDouble()
                val imageUrl = binding.editTextImageUrl.text.toString()
                
                if (args.productId != null) {
                    viewModel.updateProduct(args.productId!!, name, description, price, category, imageUrl) { success ->
                        if (success) {
                            Toast.makeText(context, getString(R.string.product_updated), Toast.LENGTH_LONG).show()
                            findNavController().popBackStack()
                        }
                    }
                } else {
                    if (imageUrl.isEmpty()) {
                        binding.layoutImageUrl.error = getString(R.string.image_url_required)
                        return@setOnClickListener
                    }
                    viewModel.uploadProduct(name, description, price, category, imageUrl) { success ->
                        if (success) {
                            clearFields()
                            Toast.makeText(context, getString(R.string.product_published), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun loadImagePreview(url: String) {
        Picasso.get()
            .load(url)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(binding.imageViewProduct, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    imageUrl = url
                    binding.layoutImageUrl.error = null
                }
                override fun onError(e: Exception?) {
                    Toast.makeText(context, getString(R.string.invalid_image_url), Toast.LENGTH_SHORT).show()
                    imageUrl = null
                }
            })
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.buttonPublish.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.currentProduct.observe(viewLifecycleOwner) { product ->
            product?.let {
                binding.editTextProductName.setText(it.name)
                binding.editTextProductDescription.setText(it.description)
                binding.editTextProductPrice.setText(it.price.toString())
                binding.autoCompleteCategory.setText(it.category, false)
                binding.editTextImageUrl.setText(it.selectedImageUrl)
                loadImagePreview(it.selectedImageUrl)
                imageUrl = it.selectedImageUrl
            }
        }
    }

    private fun validateInputs(
        name: String,
        description: String,
        price: String,
        category: String
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.layoutProductName.error = getString(R.string.name_required)
            isValid = false
        } else {
            binding.layoutProductName.error = null
        }

        if (description.isEmpty()) {
            binding.layoutProductDescription.error = getString(R.string.description_required)
            isValid = false
        } else {
            binding.layoutProductDescription.error = null
        }

        if (price.isEmpty()) {
            binding.layoutProductPrice.error = getString(R.string.price_required)
            isValid = false
        } else {
            try {
                price.toDouble()
                binding.layoutProductPrice.error = null
            } catch (e: NumberFormatException) {
                binding.layoutProductPrice.error = getString(R.string.error_invalid_price)
                isValid = false
            }
        }

        if (category.isEmpty() || !viewModel.categories.contains(category)) {
            binding.layoutProductCategory.error = getString(R.string.select_valid_category)
            isValid = false
        } else {
            binding.layoutProductCategory.error = null
        }

        return isValid
    }

    private fun clearFields() {
        binding.editTextProductName.text?.clear()
        binding.editTextProductDescription.text?.clear()
        binding.editTextProductPrice.text?.clear()
        binding.editTextImageUrl.text?.clear()
        binding.autoCompleteCategory.text?.clear()
        binding.imageViewProduct.setImageResource(0)
        
        binding.layoutProductName.error = null
        binding.layoutProductDescription.error = null
        binding.layoutProductPrice.error = null
        binding.layoutProductCategory.error = null
        binding.layoutImageUrl.error = null
        
        imageUrl = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 