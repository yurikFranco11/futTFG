package com.example.futtfg.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.futtfg.R
import com.example.futtfg.adapters.ProductAdapter
import com.example.futtfg.databinding.FragmentSearchBinding
import com.google.android.material.snackbar.Snackbar

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SearchViewModel
    private lateinit var productAdapter: ProductAdapter
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        
        setupRecyclerView()
        setupSearchInput()
        setupCategoryFilter()
        setupSwipeRefresh()
        setupObservers()
        
        // Realizar búsqueda inicial
        viewModel.searchProducts("", null)
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                findNavController().navigate(
                    SearchFragmentDirections.actionNavigationSearchToProductDetailFragment(product.id)
                )
            },
            onEditClick = { product ->
                findNavController().navigate(
                    SearchFragmentDirections.actionNavigationSearchToNavigationSell(product.id)
                )
            },
            onBuyClick = { product ->
                findNavController().navigate(
                    SearchFragmentDirections.actionNavigationSearchToProductDetailFragment(product.id)
                )
            }
        )
        
        binding.searchResultsRecyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupSearchInput() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchProducts(s?.toString() ?: "", selectedCategory)
            }
        })
    }

    private fun setupCategoryFilter() {
        // Agregar opción "Todas las categorías" al inicio de la lista
        val allCategories = listOf("Todas las categorías") + viewModel.categories
        
        val categoriesAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            allCategories
        )
        
        binding.categoryFilterAutoComplete.apply {
            setAdapter(categoriesAdapter)
            setText("Todas las categorías", false)
            setOnItemClickListener { _, _, position, _ ->
                selectedCategory = if (position == 0) null else allCategories[position]
                viewModel.searchProducts(binding.searchEditText.text.toString(), selectedCategory)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.searchProducts(binding.searchEditText.text.toString(), selectedCategory)
        }
    }

    private fun setupObservers() {
        viewModel.searchResults.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
            updateEmptyState(products.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateTextView.visibility = if (isEmpty) {
            binding.searchResultsRecyclerView.visibility = View.GONE
            View.VISIBLE
        } else {
            binding.searchResultsRecyclerView.visibility = View.VISIBLE
            View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 