package com.example.futtfg.ui.home

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
import com.example.futtfg.databinding.FragmentHomeBinding
import com.example.futtfg.adapters.ProductAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        homeViewModel.loadProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToProductDetailFragment(product.id)
                )
            },
            onEditClick = { product ->
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToNavigationSell(product.id)
                )
            },
            onBuyClick = { product ->
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToProductDetailFragment(product.id)
                )
            }
        )

        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        homeViewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
            updateEmptyState(products.isEmpty())
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        homeViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.loadProducts()
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