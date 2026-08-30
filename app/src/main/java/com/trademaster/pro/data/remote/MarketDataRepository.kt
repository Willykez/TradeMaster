package com.trademaster.pro.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trademaster.pro.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

// Thin wrapper around the live quote API. Every call site treats failure as
// a normal, expected case (no key configured, rate-limited, offline) and
// falls back gracefully -- see TradeRepository.refreshTicker -- so a bad
// network day never breaks the dashboard, it just stops looking "live" for
// a bit.
class MarketDataRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val api: TwelveDataApi = Retrofit.Builder()
        .baseUrl("https://api.twelvedata.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TwelveDataApi::class.java)

    val hasApiKey: Boolean get() = BuildConfig.TWELVE_DATA_API_KEY.isNotBlank()

    /** Returns pair -> price for every symbol that came back with a valid quote. */
    suspend fun fetchQuotes(symbols: List<String>): Result<Map<String, Double>> {
        if (!hasApiKey) return Result.failure(IllegalStateException("No TWELVE_DATA_API_KEY configured"))
        return try {
            val response = api.getPrices(symbols.joinToString(","), BuildConfig.TWELVE_DATA_API_KEY)
            val prices = response.mapNotNull { (symbol, dto) ->
                dto.price?.toDoubleOrNull()?.let { symbol to it }
            }.toMap()
            if (prices.isEmpty()) {
                Result.failure(IllegalStateException("Quote API returned no usable prices (rate-limited or invalid key?)"))
            } else {
                Result.success(prices)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
