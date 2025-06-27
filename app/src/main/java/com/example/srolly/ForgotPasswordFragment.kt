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
import com.example.srolly.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.resetPasswordButton.setOnClickListener {
            sendPasswordResetEmail()
        }
    }

    private fun sendPasswordResetEmail() {
        val email = binding.forgotEmailInput.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.forgotEmailInput.error = "Enter a valid email"
            return
        }

        // Show progress and disable button
        binding.resetPasswordButton.isEnabled = false
        binding.resetProgressBar.visibility = View.VISIBLE

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                // Hide progress and re-enable button
                binding.resetProgressBar.visibility = View.GONE
                binding.resetPasswordButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "If this email exists, a reset link has been sent.", Toast.LENGTH_LONG).show()
                    binding.forgotEmailInput.setText("")

                    binding.root.postDelayed({
                        if (isAdded) {
                            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
                        }
                    }, 1500)
                } else {
                    val errorMsg = task.exception?.message ?: "Something went wrong"
                    Log.e("ForgotPassword", "Reset error: $errorMsg", task.exception)
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
