package com.pakdrive

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity

class PermissionHandler {
    companion object{
        val permissionRequestCode=101

        fun askNotificationPermission(context: Context, requestPermissionLauncher: ActivityResultLauncher<String>) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    }
                    context is Activity && context.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    else -> {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }


        fun showEnableGpsDialog(context: Activity) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Enable GPS")
                .setMessage("Please enable GPS to use this feature.")
                .setPositiveButton("Settings") { which, dialog ->
                    val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(settingsIntent)
                    which.dismiss()
                    context.finish()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(context, "it is necessary for app functionality.", Toast.LENGTH_SHORT).show()
                    context.finish()
                }
                .setCancelable(false)
            val dialog = builder.create()
            dialog.show()
        }

    }
}