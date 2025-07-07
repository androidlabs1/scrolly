package com.example.srolly.ADAPTORS

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.srolly.DATA_CLASS.AppUsage
import com.example.srolly.R
import java.util.concurrent.TimeUnit

class AppUsageAdapter(private val appList: List<AppUsage>) : RecyclerView.Adapter<AppUsageAdapter.UsageViewHolder>() {

    class UsageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val timeUsed: TextView = itemView.findViewById(R.id.timeUsed)
        val launchCount: TextView = itemView.findViewById(R.id.launchCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_usage, parent, false)
        return UsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val app = appList[position]
        Log.d("AppUsageAdapter", "Binding: ${app.appName}, Time: ${app.totalTimeUsed}, Launches: ${app.launchCount}")
        holder.appIcon.setImageDrawable(app.icon)
        holder.appName.text = app.appName
        holder.timeUsed.text = "Used: ${formatTime(app.totalTimeUsed)}"
        holder.launchCount.text = "Opened: ${app.launchCount} times"


    }

    override fun getItemCount(): Int = appList.size

    private fun formatTime(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }
}
