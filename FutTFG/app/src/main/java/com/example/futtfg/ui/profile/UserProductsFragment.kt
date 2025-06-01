package com.example.futtfg.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.futtfg.databinding.FragmentUserProductsBinding
import com.example.futtfg.adapters.ProductAdapter

class UserProductsFragment : Fragment() {

    private var _binding: FragmentUserProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        
        viewModel.loadUserProducts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                findNavController().navigate(
                    UserProductsFragmentDirections.actionUserProductsToProductDetail(product.id)
                )
            },
            onEditClick = { product ->
                findNavController().navigate(
                    UserProductsFragmentDirections.actionUserProductsToNavigationSell(product.id)
                )
            },
            onBuyClick = { product ->
                findNavController().navigate(
                    UserProductsFragmentDirections.actionUserProductsToProductDetail(product.id)
                )
            }
        )

        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        viewModel.userProducts.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
            updateEmptyState(products.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadUserProducts()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmpty.isVisible = isEmpty
        binding.recyclerViewProducts.isVisible = !isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 