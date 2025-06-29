package com.example.srolly

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.srolly.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            loginUser()
        }

        binding.signupText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        binding.guestText.setOnClickListener {
            val sharedPrefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("is_guest", true).apply()

            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }


        binding.forgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
    }

    private fun loginUser() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Invalid email format"
            binding.emailInput.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            binding.passwordInput.requestFocus()
            return
        }

        // Show loading state
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "" // optional: remove text while loading
        binding.loginProgressBar.visibility = View.VISIBLE

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Hide loading state
                binding.loginButton.isEnabled = true
                binding.loginButton.text = "Login"
                binding.loginProgressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // ✅ Reset guest flag
                    requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("is_guest", false).apply()
                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    val errorMessage = when (val exception = task.exception) {
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found with this email."
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect password."
                        else -> exception?.localizedMessage ?: "Login failed. Please try again."
                    }

                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
