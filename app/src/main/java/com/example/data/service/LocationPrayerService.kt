package com.example.data.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LivePrayerTimesResult(
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val sunrise: String = "",
    val cityName: String,
    val countryName: String,
    val calculationMethod: String,
    val latitude: Double,
    val longitude: Double,
    val fetchedAtFormatted: String,
    val sourceUrl: String = "https://www.assabile.com/prayer-times"
)

/**
 * LocationPrayerService
 *
 * Location-based service utilizing device GPS coordinates to fetch live, dynamic
 * prayer timings from assabile.com (official Abu Dhabi Awqaf calculation method).
 */
class LocationPrayerService(private val context: Context) {

    companion object {
        private const val TAG = "LocationPrayerService"
        // Official Abu Dhabi Coordinates
        const val ABU_DHABI_LAT = 24.4539
        const val ABU_DHABI_LNG = 54.3773
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _lastResult = MutableStateFlow<LivePrayerTimesResult?>(null)
    val lastResult: StateFlow<LivePrayerTimesResult?> = _lastResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Checks if location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Obtains the user's current GPS location coordinates or falls back to Abu Dhabi.
     */
    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Location permission not granted, defaulting to Abu Dhabi coordinates")
            return@withContext Pair(ABU_DHABI_LAT, ABU_DHABI_LNG)
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            // Try current fresh location
            val location: Location? = try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .await()
            } catch (e: Exception) {
                null
            }

            if (location != null) {
                return@withContext Pair(location.latitude, location.longitude)
            }

            // Fallback to last known location
            val lastLoc: Location? = fusedClient.lastLocation.await()
            if (lastLoc != null) {
                return@withContext Pair(lastLoc.latitude, lastLoc.longitude)
            }

            // Fallback to LocationManager
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = gpsLoc ?: netLoc
            if (best != null) {
                return@withContext Pair(best.latitude, best.longitude)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting GPS location: ${e.message}")
        }

        Pair(ABU_DHABI_LAT, ABU_DHABI_LNG)
    }

    /**
     * Fetches live prayer timings from assabile.com for given or GPS coordinates.
     */
    suspend fun fetchLivePrayerTimes(
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<LivePrayerTimesResult> = withContext(Dispatchers.IO) {
        _isFetching.value = true
        _errorMessage.value = null

        val (lat, lng) = if (latitude != null && longitude != null) {
            Pair(latitude, longitude)
        } else {
            getDeviceLocation()
        }

        val url = "https://www.assabile.com/api/prayer-times?lat=$lat&lng=$lng"
        Log.d(TAG, "Fetching live timings from: $url")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ImtiazLifestyle-Android/1.0 (https://www.assabile.com/prayer-times)")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Failed to fetch from assabile.com (HTTP ${response.code})"
                _errorMessage.value = err
                _isFetching.value = false
                return@withContext Result.failure(Exception(err))
            }

            val bodyStr = response.body?.string() ?: ""
            if (bodyStr.isBlank()) {
                val err = "Empty response from assabile.com"
                _errorMessage.value = err
                _isFetching.value = false
                return@withContext Result.failure(Exception(err))
            }

            val json = JSONObject(bodyStr)

            // Extract ville or lieu (location)
            val villeObj = json.optJSONObject("ville")
            val lieuObj = json.optJSONObject("lieu")
            val tzCity = lieuObj?.optString("tz")?.substringAfterLast("/")?.replace("_", " ")
            val fallbackCity = villeObj?.optString("nom") ?: tzCity ?: "Abu Dhabi"
            val fallbackCountry = villeObj?.optString("pays") ?: lieuObj?.optString("pays") ?: "United Arab Emirates"

            // Reverse geocode from real GPS coordinates
            var cityName = fallbackCity
            var countryName = fallbackCountry
            try {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addrs = geocoder.getFromLocation(lat, lng, 1)
                if (!addrs.isNullOrEmpty()) {
                    val addr = addrs[0]
                    val local = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    if (!local.isNullOrBlank()) cityName = local
                    if (!addr.countryName.isNullOrBlank()) countryName = addr.countryName
                }
            } catch (e: Exception) {
                Log.d(TAG, "Geocoder lookup: ${e.message}")
            }

            val method = json.optString("methode", "dubai (Awqaf UAE)")

            // Extract jours array
            val joursArr = json.optJSONArray("jours")
            if (joursArr == null || joursArr.length() == 0) {
                val err = "No prayer times returned in assabile.com response"
                _errorMessage.value = err
                _isFetching.value = false
                return@withContext Result.failure(Exception(err))
            }

            val todayObj = joursArr.getJSONObject(0)
            val fajr = todayObj.optString("fajr", "05:01")
            val dhuhr = todayObj.optString("dhuhr", "12:12")
            val asr = todayObj.optString("asr", "15:31")
            val maghrib = todayObj.optString("maghrib", "18:04")
            val isha = todayObj.optString("icha", "19:17")
            val sunrise = todayObj.optString("chourouk", "06:14")

            val timeFormat = SimpleDateFormat("HH:mm, MMM d", Locale.getDefault())
            val fetchedAt = timeFormat.format(Date())

            val result = LivePrayerTimesResult(
                fajr = fajr,
                dhuhr = dhuhr,
                asr = asr,
                maghrib = maghrib,
                isha = isha,
                sunrise = sunrise,
                cityName = cityName,
                countryName = countryName,
                calculationMethod = method,
                latitude = lat,
                longitude = lng,
                fetchedAtFormatted = fetchedAt
            )

            _lastResult.value = result
            _isFetching.value = false
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from assabile.com: ${e.message}", e)
            val err = "Error connecting to assabile.com: ${e.localizedMessage ?: "Network error"}"
            _errorMessage.value = err
            _isFetching.value = false
            Result.failure(e)
        }
    }
}
