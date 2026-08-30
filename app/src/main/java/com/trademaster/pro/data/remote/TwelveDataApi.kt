package com.trademaster.pro.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

// Twelve Data's free-tier batch quote endpoint. One request can carry many
// symbols, which matters on a free plan: our whole 10-pair ticker fits in a
// single call instead of ten, keeping us well under the rate limit even
// polling every 15s. Docs: https://twelvedata.com/docs#price
interface TwelveDataApi {
    @GET("price")
    suspend fun getPrices(
        @Query("symbol") commaSeparatedSymbols: String,
        @Query("apikey") apiKey: String
    ): Map<String, PriceDto>
}

// Twelve Data returns a single flat object (no "symbol" wrapper) when only
// one symbol is requested, and a map keyed by symbol for multiple -- we
// always request 2+ symbols so we can rely on the map shape here.
@JsonClass(generateAdapter = true)
data class PriceDto(
    val price: String? = null,
    @Json(name = "code") val errorCode: Int? = null,
    @Json(name = "message") val errorMessage: String? = null
)
