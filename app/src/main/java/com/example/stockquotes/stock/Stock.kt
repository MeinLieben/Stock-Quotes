package com.example.stockquotes.stock

import com.finnhub.api.apis.DefaultApi

class Stock(
    val ticker: String,
    var stockName: String = "",
    var currentPrice: Float = 0.0f,
    var prevDayPrice: Float = 0.0f,
    var priceIncrease: Float = 0.0f,
    var percentPriceIncrease: Float = 0.0f,
    var logoURL: String = ""
) {


    fun getData(apiClient: DefaultApi) {
        val companyProfile = apiClient.companyProfile2(ticker, null, null)
        stockName = companyProfile.name.toString()
        logoURL = companyProfile.logo.toString()
    }

    fun refreshQuotes(apiClient: DefaultApi) {
        val quote = apiClient.quote(ticker)
        currentPrice = quote.c ?: 0.0f
        prevDayPrice = quote.pc ?: 0.0f
        priceIncrease = currentPrice - prevDayPrice
        percentPriceIncrease =
            if (prevDayPrice > 0) priceIncrease * 100 / prevDayPrice else 0.0f

    }


}