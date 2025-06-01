package com.example.futtfg.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.futtfg.databinding.FragmentProfileBinding
import com.squareup.picasso.Picasso
import com.example.futtfg.R

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            // Mostrar la imagen seleccionada inmediatamente
            Picasso.get()
                .load(imageUri)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(binding.imageViewProfile)

            // Subir la imagen
            viewModel.uploadProfilePhoto(imageUri) { success ->
                if (success) {
                    Toast.makeText(context, "Foto actualizada con éxito", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        
        setupViews()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            updateUIState(user != null)
        }

        viewModel.userData.observe(viewLifecycleOwner) { userData ->
            if (userData.isNotEmpty()) {
                binding.textViewUserName.text = "${userData["name"]} ${userData["lastName"]}"
                binding.textViewUserEmail.text = userData["email"]
                
                // Cargar la foto de perfil desde la URL guardada
                val photoUrl = userData["photoUrl"]
                if (!photoUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(binding.imageViewProfile)
                } else {
                    binding.imageViewProfile.setImageResource(R.drawable.default_profile)
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.buttonChangePhoto.isEnabled = !isLoading
        }

        // Observar cambios en la URL de la foto de perfil
        viewModel.profilePhotoUrl.observe(viewLifecycleOwner) { url ->
            if (url.isNotEmpty()) {
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(binding.imageViewProfile)
            }
        }
    }

    private fun setupViews() {
        // Configurar el botón de cambiar foto
        binding.buttonChangePhoto.setOnClickListener {
            getContent.launch("image/*")
        }

        // Configurar la vista previa de la foto en el registro
        binding.editTextPhotoUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s?.toString() ?: ""
                if (url.isNotEmpty()) {
                    Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(binding.imageViewRegisterPreview)
                } else {
                    binding.imageViewRegisterPreview.setImageResource(R.drawable.default_profile)
                }
            }
        })

        // Configuración de la navegación entre formularios
        binding.buttonShowRegister.setOnClickListener {
            binding.cardLogin.visibility = View.GONE
            binding.cardRegister.visibility = View.VISIBLE
        }

        binding.buttonShowLogin.setOnClickListener {
            binding.cardLogin.visibility = View.VISIBLE
            binding.cardRegister.visibility = View.GONE
        }

        // Configuración del inicio de sesión
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextLoginEmail.text.toString()
            val password = binding.editTextLoginPassword.text.toString()
            
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.loginUser(email, password) { success ->
                    if (success) {
                        clearLoginFields()
                    }
                }
            } else {
                Toast.makeText(context, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración del registro
        binding.buttonRegister.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val lastName = binding.editTextLastName.text.toString()
            val email = binding.editTextRegisterEmail.text.toString()
            val password = binding.editTextRegisterPassword.text.toString()
            val photoUrl = binding.editTextPhotoUrl.text.toString()
            
            if (name.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.registerUser(name, lastName, email, password, photoUrl) { success ->
                    if (success) {
                        clearRegisterFields()
                    }
                }
            } else {
                Toast.makeText(context, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración de botones del perfil
        binding.buttonMyProducts.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionNavigationProfileToUserProducts())
        }

        binding.buttonMyPurchases.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionNavigationProfileToUserPurchases())
        }

        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun updateUIState(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            binding.cardLogin.visibility = View.GONE
            binding.cardRegister.visibility = View.GONE
            binding.cardProfile.visibility = View.VISIBLE
        } else {
            binding.cardLogin.visibility = View.VISIBLE
            binding.cardRegister.visibility = View.GONE
            binding.cardProfile.visibility = View.GONE
        }
    }

    private fun clearLoginFields() {
        binding.editTextLoginEmail.text?.clear()
        binding.editTextLoginPassword.text?.clear()
    }

    private fun clearRegisterFields() {
        binding.editTextName.text?.clear()
        binding.editTextLastName.text?.clear()
        binding.editTextRegisterEmail.text?.clear()
        binding.editTextRegisterPassword.text?.clear()
        binding.editTextPhotoUrl.text?.clear()
        binding.imageViewRegisterPreview.setImageResource(R.drawable.default_profile)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAuthState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 