package com.example.srolly

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.srolly.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        binding.signupButton.setOnClickListener {
            registerUser()
        }

        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }
    }

    private fun registerUser() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

        // Input validation
        if (name.isEmpty()) {
            binding.nameInput.error = "Full name is required"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Invalid email format"
            return
        }

        if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            binding.confirmPasswordInput.error = "Passwords do not match"
            return
        }

        // Show loading UI
        binding.signupButton.isEnabled = false
        binding.signupButton.text = ""
        binding.signupProgressBar.visibility = View.VISIBLE

        // Firebase signup
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Hide loading UI regardless of result
                binding.signupButton.isEnabled = true
                binding.signupButton.text = "Signup"
                binding.signupProgressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener

                    val userRef = firebaseDatabase.getReference("users").child(userId)
                    val userData = mapOf(
                        "name" to name,
                        "email" to email
                    )

                    userRef.setValue(userData)
                    Toast.makeText(requireContext(), "Account created!", Toast.LENGTH_SHORT).show()
                    Log.d("SignupFlow", "User info saved. Navigating now...")

                    if (isAdded && findNavController().currentDestination?.id == R.id.signupFragment) {
                        val bundle = Bundle().apply {
                            putString("userName", name)
                        }
                        findNavController().navigate(R.id.action_signupFragment_to_homeFragment , bundle)
                    }

                } else {
                    val errorMessage = when (val exception = task.exception) {
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> {
                            "An account with this email already exists."
                        }

                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> {
                            "Password is too weak. Please choose a stronger one."
                        }

                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                            "Invalid email format."
                        }

                        else -> {
                            exception?.localizedMessage ?: "Signup failed. Please try again."
                        }
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
