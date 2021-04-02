package com.example.stockquotes

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockquotes.jsonClasses.socketResponse.SocketResponse
import com.example.stockquotes.stock.Stock
import com.example.stockquotes.stock.StockViewAdapter
import com.finnhub.api.apis.DefaultApi
import com.finnhub.api.infrastructure.ApiClient
import com.google.gson.Gson
import com.squareup.moshi.Types
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.*
import java.util.Calendar.getInstance


class MainActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private var stocks: MutableList<Stock> = mutableListOf()
    private lateinit var tickers: List<String>
    private var favouriteTickers: MutableList<String> = mutableListOf()
    private lateinit var stocksAdapter: StockViewAdapter
    private var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)
    private val apiClient = DefaultApi()
    private val gson = Gson()
    private var refreshTime: Calendar = getOpeningTime()
    private var isFavouriteView = false

    private val listenerFavourite = View.OnClickListener {
        val favouriteStocks = stocks.filter { it.ticker in favouriteTickers }
        val favouriteStocksAdapter = StockViewAdapter(favouriteStocks, favouriteTickers)
        stockView.adapter = favouriteStocksAdapter
        switchTextView(textViewFavourite, textViewStocks)
        isFavouriteView = true
        searchView.clearFocus()
        searchView.setQuery(searchView.query, true)
    }
    private val listenerStocks = View.OnClickListener {
        stockView.adapter = stocksAdapter
        switchTextView(textViewStocks, textViewFavourite)
        isFavouriteView = false
        searchView.clearFocus()
        searchView.setQuery(searchView.query, true)
    }

    private fun switchTextView(textView1: TextView, textView2: TextView) {
        textView1.typeface = Typeface.DEFAULT_BOLD
        textView1.textSize = 24F
        textView1.setTextColor(ContextCompat.getColor(this, R.color.heavy_black))
        textView2.typeface = Typeface.DEFAULT
        textView2.textSize = 18F
        textView2.setTextColor(ContextCompat.getColor(this, R.color.light_black))
    }

    private val searchListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            return onQueryTextChange(query)
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            val filterStocks = stocks.filter {
                (if (isFavouriteView) it.ticker in favouriteTickers else true) &&
                        (it.ticker.contains(newText ?: "", ignoreCase = true) ||
                                it.stockName.contains(newText ?: "", ignoreCase = true))
            }
            val filterAdapter = StockViewAdapter(filterStocks, favouriteTickers)
            stockView.adapter = filterAdapter
            return false
        }

    }


    private val webSocketListener = object : WebSocketListener() {

        inner class RequestClass(val type: String = "subscribe", var symbol: String = "")

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            val result = gson.fromJson(text, SocketResponse::class.java)
            stocks.forEach { stock ->
                stock.currentPrice =
                    result.data.find { it.s == stock.ticker }?.p ?: stock.currentPrice
                stock.priceIncrease = stock.currentPrice - stock.prevDayPrice
                stock.percentPriceIncrease =
                    if (stock.prevDayPrice > 0) stock.priceIncrease * 100 / stock.prevDayPrice else 0.0f
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            val requestClass = RequestClass()
            stocks.forEach {
                requestClass.symbol = it.ticker
                webSocket.send(gson.toJson(requestClass))
            }
        }
    }


    private fun initStockData() {

        stocks.forEach {
            val stock = CoroutineScope(Dispatchers.IO).async {
                try {
                    it.getData(apiClient)
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        messageError()
                    }
                }
                it
            }
            CoroutineScope(Dispatchers.Main).launch {
                stocksAdapter.notifyItemChanged(stocks.indexOf(stock.await()))
            }
        }
    }

    private fun refreshQuotes() {
        refreshTime = getInstance()
        stocks.forEach {
            val stock = CoroutineScope(Dispatchers.IO).async {
                try {
                    it.refreshQuotes(apiClient)
                } catch (e: Exception) {
                    messageError()
                    refreshTime = getOpeningTime()
                }
                it
            }
            CoroutineScope(Dispatchers.Main).launch {
                stocksAdapter.notifyItemChanged(stocks.indexOf(stock.await()))
            }
        }
    }

    private fun getOpeningTime(): Calendar {
        val timeZone = TimeZone.getTimeZone("GMT+1:00")
        val time = getInstance(timeZone)
        time.set(Calendar.HOUR_OF_DAY, 0)
        time.set(Calendar.HOUR, 0)
        time.set(Calendar.MINUTE, 0)
        time.set(Calendar.SECOND, 0)
        return time
    }

    private fun initStockList() {
        val openingTime = getOpeningTime()
        val request = Request.Builder()
            .url("${getString(R.string.url_request)}?token=${getString(R.string.api_key)}")
            .build()

        when {
            preferences.contains(getString(R.string.stock_list)) -> {
                stocks = gson.fromJson(
                    preferences.getString(getString(R.string.stock_list), ""),
                    Types.newParameterizedType(List::class.java, Stock::class.java)
                )
                tickers = stocks.map { it.ticker }
                favouriteTickers = preferences.getStringSet(getString(R.string.favourite_list), setOf())
                    ?.toMutableList() ?: mutableListOf()
                if (!preferences.contains(getString(R.string.time))) refreshQuotes()
                else {
                    refreshTime = gson.fromJson(
                        preferences.getString(getString(R.string.time), ""),
                        Calendar::class.java
                    )
                    if (openingTime.timeInMillis >= refreshTime.timeInMillis && isNetworkAvailable(this)) refreshQuotes()
                }
                stocksAdapter = StockViewAdapter(stocks, favouriteTickers)
                stockView.adapter = stocksAdapter

                if(isNetworkAvailable(this))
                    ApiClient.client.newWebSocket(request, webSocketListener)
                else messageNoConnection(this)
            }
            isNetworkAvailable(this) -> {
                CoroutineScope(Dispatchers.IO).launch{
                    try {
                        initTickerList()
                    }
                    catch (e: Exception){
                        messageError()
                        delay(3500)
                        finish()
                    }
                    stocks = tickers.map { Stock(it) } as MutableList<Stock>
                    CoroutineScope(Dispatchers.Main).launch {
                        stocksAdapter = StockViewAdapter(stocks, favouriteTickers)
                        stockView.adapter = stocksAdapter
                        ApiClient.apiKey["token"] = getString(R.string.api_key2)
                        initStockData()
                        refreshQuotes()
                    }
                    ApiClient.client.newWebSocket(request, webSocketListener)
                }
            }
            else -> messageNoConnection(this)
        }
    }

    private fun initTickerList() {
        tickers = apiClient.indicesConstituents(getString(R.string.DJI)).constituents?.sorted() ?: listOf()
    }

    private fun messageError() {
        val toast = Toast.makeText(
            applicationContext,
            "Error loading data. Try again later.",
            Toast.LENGTH_LONG
        )
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        stockView.layoutManager = linearLayoutManager

        ApiClient.apiKey["token"] = getString(R.string.api_key)

        preferences = getSharedPreferences(getString(R.string.settings), Context.MODE_PRIVATE)

        textViewFavourite.setOnClickListener(listenerFavourite)
        textViewStocks.setOnClickListener(listenerStocks)

        searchView.setOnQueryTextListener(searchListener)
        searchView.setOnQueryTextFocusChangeListener { v, hasFocus -> if (!hasFocus) v.clearFocus() }
    }

    override fun onResume() {
        super.onResume()
        initStockList()
    }

    override fun onPause() {
        val editor = preferences.edit()
        editor.putString(getString(R.string.stock_list), gson.toJson(stocks)).apply()
        editor.putStringSet(getString(R.string.favourite_list), favouriteTickers.toSet()).apply()
        editor.putString(getString(R.string.time), gson.toJson(refreshTime)).apply()
        super.onPause()
    }
}