package com.example.futtfg

import android.app.Application
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class FutTFGApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Verificar Google Play Services
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("FutTFG", "Google Play Services no está disponible: $resultCode")
        }
    }
} 