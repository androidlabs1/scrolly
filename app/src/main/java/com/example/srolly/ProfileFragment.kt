package com.example.srolly

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.srolly.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        val isGuest = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_guest", false)

        binding.profileLoader.visibility = View.VISIBLE
        binding.profileContentLayout.visibility = View.GONE

        if (isGuest || firebaseAuth.currentUser == null) {
            // Guest Mode UI
            binding.profileInitial.text = "G"
            binding.fullNameText.text = "Guest User"
            binding.emailText.text = "Not logged in"

            binding.logoutButton.setOnClickListener {
                Toast.makeText(requireContext(), "You are already logged out", Toast.LENGTH_SHORT).show()
            }

            binding.editNameButton.setOnClickListener {
                Toast.makeText(requireContext(), "Login to edit your name", Toast.LENGTH_SHORT).show()
            }

            binding.profileLoader.visibility = View.GONE
            binding.profileContentLayout.visibility = View.VISIBLE
        } else {
            // Logged-In User
            val user = firebaseAuth.currentUser!!
            val userId = user.uid
            val userEmail = user.email ?: "Not available"

            val userRef = firebaseDatabase.getReference("users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

                    binding.profileInitial.text = initial
                    binding.fullNameText.text = name
                    binding.emailText.text = userEmail

                    binding.profileLoader.visibility = View.GONE
                    binding.profileContentLayout.visibility = View.VISIBLE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                    binding.profileLoader.visibility = View.GONE
                }
            })

            binding.logoutButton.setOnClickListener {
                requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_guest", false).apply()

                firebaseAuth.signOut()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                requireActivity().recreate()
            }

            binding.editNameButton.setOnClickListener {
                Toast.makeText(requireContext(), "Edit name feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
