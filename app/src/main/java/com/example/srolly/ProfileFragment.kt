package com.example.srolly

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.srolly.ADAPTORS.AppUsageAdapter
import com.example.srolly.DATA_CLASS.AppUsage
import com.example.srolly.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("ProfileFragment", "View created")

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        val isGuest = requireContext()
            .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_guest", false)

        binding?.apply {
            profileLoader.visibility = View.VISIBLE
            profileContentLayout.visibility = View.GONE
        }

        if (isGuest || firebaseAuth.currentUser == null) {
            Log.d("ProfileFragment", "Guest user detected")

            binding?.apply {
                profileInitial.text = "G"
                fullNameText.text = "Guest User"
                emailText.text = "Not logged in"

                editNameButton.setOnClickListener {
                    Toast.makeText(requireContext(), "Login to edit your name", Toast.LENGTH_SHORT).show()
                }

                profileLoader.visibility = View.GONE
                profileContentLayout.visibility = View.VISIBLE
            }
        } else {
            Log.d("ProfileFragment", "Authenticated user detected")

            val user = firebaseAuth.currentUser!!
            val userId = user.uid
            val userEmail = user.email ?: "Not available"

            val userRef = firebaseDatabase.getReference("users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

                    Log.d("Firebase", "User name: $name")

                    binding?.apply {
                        profileInitial.text = initial
                        fullNameText.text = name
                        emailText.text = userEmail

                        profileLoader.visibility = View.GONE
                        profileContentLayout.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Failed to load profile: ${error.message}")
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                    binding?.profileLoader?.visibility = View.GONE
                }
            })

            binding?.logoutButton?.setOnClickListener {
                requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_guest", false).apply()

                firebaseAuth.signOut()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                requireActivity().recreate()
            }

            binding?.editNameButton?.setOnClickListener {
                Toast.makeText(requireContext(), "Edit name feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        // Check permission
        if (!hasUsageAccessPermission()) {
            Log.w("Permission", "Usage access not granted")
            Toast.makeText(requireContext(), "Please allow usage access to view app usage", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        // Load usage data
        val usageList = getAppUsageStats()
        Log.d("UsageStats", "Loaded ${usageList.size} apps")

        val adapter = AppUsageAdapter(usageList)
        binding?.usageRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.usageRecyclerView?.adapter = adapter
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getAppUsageStats(): List<AppUsage> {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val packageManager = requireContext().packageManager
        val usageMap = mutableMapOf<String, AppUsage>()

        usageStatsList?.forEach { usageStats ->
            try {
                val appInfo = packageManager.getApplicationInfo(usageStats.packageName, 0)

                // ✅ Skip system apps
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return@forEach

                val label = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)

                val prev = usageMap[usageStats.packageName]
                val totalTime = usageStats.totalTimeInForeground
                val newLaunch = prev?.launchCount ?: 0

                usageMap[usageStats.packageName] = AppUsage(
                    packageName = usageStats.packageName,
                    appName = label,
                    icon = icon,
                    totalTimeUsed = totalTime + (prev?.totalTimeUsed ?: 0),
                    launchCount = newLaunch + 1
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // Ignore
            }
        }

        return usageMap.values
            .filter { it.totalTimeUsed > 0 } // Optional: hide unused apps
            .sortedByDescending { it.totalTimeUsed }
            .take(10)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
