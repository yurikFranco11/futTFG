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
import com.example.futtfg.adapters.ProductAdapter
import com.example.futtfg.databinding.FragmentUserPurchasesBinding

class UserPurchasesFragment : Fragment() {
    private var _binding: FragmentUserPurchasesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UserPurchasesViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserPurchasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[UserPurchasesViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        viewModel.loadUserPurchases()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { /* No hacemos nada al hacer clic en el producto */ },
            onEditClick = { /* No se usa en compras */ },
            onBuyClick = { /* No se usa en compras */ }
        ).apply {
            setShowButtons(false) // Desactivar botones en el historial de compras
            setClickable(false) // Desactivar el clic en los items
        }

        binding.recyclerViewPurchases.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        viewModel.userPurchases.observe(viewLifecycleOwner) { products ->
            if (products != null) {
                productAdapter.submitList(products)
                updateEmptyState(products.isEmpty())
            }
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
            viewModel.loadUserPurchases()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmpty.isVisible = isEmpty
        binding.recyclerViewPurchases.isVisible = !isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 