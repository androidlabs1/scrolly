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
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    // Using the backing property so that we never accidentally use binding when it's null.
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
        Log.d("ProfileFragment", "onViewCreated triggered")

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        // Setup logout and edit buttons
        binding.logoutButton.setOnClickListener {
            handleLogout()
        }
        binding.editNameButton.setOnClickListener {
            handleEditName()
        }
    }

    private fun loadUserProfile() {
        val isGuest = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_guest", false)

        binding.profileLoader.visibility = View.VISIBLE
        binding.profileContentLayout.visibility = View.GONE

        if (isGuest || firebaseAuth.currentUser == null) {
            displayGuestProfile()
        } else {
            displayUserProfile()
        }
    }

    private fun displayGuestProfile() {
        binding.profileInitial.text = "G"
        binding.fullNameText.text = "Guest User"
        binding.emailText.text = "Not logged in"
        binding.profileLoader.visibility = View.GONE
        binding.profileContentLayout.visibility = View.VISIBLE
    }

    private fun displayUserProfile() {
        val user = firebaseAuth.currentUser!!
        val userRef = firebaseDatabase.getReference("users").child(user.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ensure the binding is still valid
                if (!isAdded || _binding == null) return

                val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

                binding.profileInitial.text = initial
                binding.fullNameText.text = name
                binding.emailText.text = user.email ?: "Not available"

                binding.profileLoader.visibility = View.GONE
                binding.profileContentLayout.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileFragment", "Failed to load user data", error.toException())
                if (!isAdded || _binding == null) return
                binding.profileLoader.visibility = View.GONE
                binding.profileContentLayout.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleLogout() {
        val isGuest = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_guest", false)
        if (isGuest) {
            requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("is_guest", false).apply()
        } else {
            firebaseAuth.signOut()
        }
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        // Insert your navigation logic to go back to the login screen if needed.
    }

    private fun handleEditName() {
        val isGuest = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_guest", false)
        if (isGuest || firebaseAuth.currentUser == null) {
            Toast.makeText(requireContext(), "Login to edit your name", Toast.LENGTH_SHORT).show()
            return
        }
        val editText = EditText(requireContext()).apply {
            hint = "Enter new name"
            setText(binding.fullNameText.text.toString())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUserName(newName)
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUserName(newName: String) {
        val user = firebaseAuth.currentUser ?: return
        val userRef = firebaseDatabase.getReference("users").child(user.uid)
        userRef.child("name").setValue(newName)
            .addOnSuccessListener {
                binding.fullNameText.text = newName
                binding.profileInitial.text = newName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        Log.d("ProfileFragment", "onResume called")
        loadAppUsage()
    }

    private fun loadAppUsage() {
        if (!hasUsageAccessPermission()) {
            Toast.makeText(
                requireContext(),
                "Please allow usage access to view app usage",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        Log.d("AppUsage", "Permission granted, loading usage data...")
        val usageList = getAppUsageStats()
        Log.d("AppUsage", "Usage List Size: ${usageList.size}")

        binding.usageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.usageRecyclerView.adapter = AppUsageAdapter(usageList)
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        Log.d("PermissionCheck", "Mode: $mode")
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getAppUsageStats(): List<AppUsage> {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        // Using last 7 days to get better data
        val startTime = endTime - TimeUnit.DAYS.toMillis(7)
        Log.d("AppUsage", "Querying usage stats from $startTime to $endTime")

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        Log.d("AppUsageRaw", "Retrieved ${usageStatsList?.size ?: 0} items from UsageStatsManager")

        val packageManager = requireContext().packageManager
        val usageMap = mutableMapOf<String, AppUsage>()

        usageStatsList?.forEach { usageStats ->
            val packageName = usageStats.packageName
            val totalTime = usageStats.totalTimeInForeground
            if (totalTime < 1000) {
                Log.d("AppUsage", "$packageName - zero usage")
                return@forEach
            }

            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)

                // Skip if it's our own app or a system app
                if (isSystemApp(appInfo, packageName)) {
                    Log.d("AppUsage", "Skipping system app: $packageName")
                    return@forEach
                }

                val label = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                Log.d("AppUsage", "Including: $packageName ($label) - $totalTime ms")

                val previous = usageMap[packageName]
                usageMap[packageName] = AppUsage(
                    packageName = packageName,
                    appName = label,
                    icon = icon,
                    totalTimeUsed = totalTime + (previous?.totalTimeUsed ?: 0),
                    launchCount = (previous?.launchCount ?: 0) + getLaunchCountForApp(usageStatsManager, packageName, startTime, endTime)
                )
            } catch (e: Exception) {
                Log.e("AppUsage", "Error processing $packageName: ${e.message}")
            }
        }

        val finalList = usageMap.values.sortedByDescending { it.totalTimeUsed }.take(15)
        Log.d("AppUsage", "Final filtered app count: ${finalList.size}")
        return finalList
    }

    private fun getLaunchCountForApp(
        usageStatsManager: UsageStatsManager,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Int {
        var count = 0
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName == packageName && event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w("AppUsage", "Failed to retrieve events for $packageName: ${e.message}")
        }
        return if (count > 0) count else 1 // Default to 1 if no events found
    }

    private fun isSystemApp(appInfo: ApplicationInfo, packageName: String): Boolean {
        val excludedPackages = listOf(
            requireContext().packageName,
            "com.android.systemui",
            "com.android.settings",
            "android",
            "com.android.launcher",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.google.android.packageinstaller",
            "com.miui.securitycenter",
            "com.miui.securitycore",
            "com.miui.home",
            "com.miui.weather2",
            "com.xiaomi.permissioncontroller",
            "com.xiaomi.calendar",
            "com.miui.yellowpage",
            "com.miui.misound",
            "com.miui.msa.global",
            "com.miui.guardprovider",
            "com.miui.notification",
            "com.google.android.ext.services",
            "com.google.android.projection.gearhead",
            "com.google.android.gm",
            "com.google.android.contacts",
            "com.mediatek.ims",
            "org.mipay.android.manager",
            "com.milink.service",
            "com.miui.daemon"
        )

        val pm = requireContext().packageManager

        // Exclude by known package names
        if (packageName in excludedPackages) {
            Log.d("AppFilter", "Excluded system/utility app: $packageName")
            return true
        }

        // Exclude if app has no launch intent (not user-launchable)
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Log.d("AppFilter", "Excluded: $packageName (no launcher intent)")
            return true
        }

        // Optionally: You can exclude apps with zero usage, but you already do that by checking `totalTimeInForeground < 1000`
        return false
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
