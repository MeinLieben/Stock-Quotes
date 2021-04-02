package com.example.stockquotes

import android.content.Context
import android.net.ConnectivityManager
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
fun isNetworkAvailable(activity: AppCompatActivity):Boolean{
    val connectivityManager=activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo=connectivityManager.activeNetworkInfo
    return  networkInfo!=null && networkInfo.isConnected
}

fun messageNoConnection(activity: AppCompatActivity) {
    val toast = Toast.makeText(
        activity,
        "No Internet connection...",
        Toast.LENGTH_LONG
    )
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}