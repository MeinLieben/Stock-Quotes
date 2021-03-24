package com.example.stockquotes

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.stockquotes.news.NewsViewAdapter
import com.example.stockquotes.stock.StockViewAdapter
import com.finnhub.api.apis.DefaultApi
import com.finnhub.api.infrastructure.ApiClient
import com.finnhub.api.infrastructure.ApiClient.Companion.client
import com.finnhub.api.models.CompanyProfile2
import com.finnhub.api.models.News
import com.finnhub.api.models.StockCandles
import com.google.gson.Gson
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.squareup.moshi.Types
import kotlinx.android.synthetic.main.activity_company.*
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*


class CompanyActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private val apiClient = DefaultApi()
    private val gson = Gson()
    private lateinit var activeTextView: TextView
    private var favouriteTickers: MutableList<String> = mutableListOf()
    private lateinit var ticker: String
    private lateinit var profile: CompanyProfile2
    private lateinit var newsList: List<News>
    private lateinit var yearCandles: StockCandles
    private lateinit var monthCandles: StockCandles
    private lateinit var weekCandles: StockCandles
    private lateinit var newsAdapter: NewsViewAdapter


    private val listenerChart = View.OnClickListener {
        if (activeTextView != textViewChart) {
            switchTextView(textViewChart, activeTextView)
            activeTextView = textViewChart
            setLayoutsVisibility()
        }
    }

    private val listenerProfile = View.OnClickListener {
        if (activeTextView != textViewProfile) {
            switchTextView(textViewProfile, activeTextView)
            activeTextView = textViewProfile
            setLayoutsVisibility()
        }
    }

    private val listenerNews = View.OnClickListener {
        if (activeTextView != textViewNews) {
            switchTextView(textViewNews, activeTextView)
            activeTextView = textViewNews
            setLayoutsVisibility()
        }
    }

    private fun switchTextView(textView1: TextView, textView2: TextView) {
        textView1.typeface = Typeface.DEFAULT_BOLD
        textView1.textSize = 24F
        textView1.setTextColor(ContextCompat.getColor(this, R.color.heavy_black))
        textView2.typeface = Typeface.DEFAULT
        textView2.textSize = 18F
        textView2.setTextColor(ContextCompat.getColor(this, R.color.light_black))
    }

    private fun setLayoutsVisibility() {
        scrollViewChart.visibility = View.INVISIBLE
        scrollViewProfile.visibility = View.INVISIBLE
        newsLayout.visibility = View.INVISIBLE
        when (activeTextView) {
            textViewChart -> scrollViewChart.visibility = View.VISIBLE
            textViewProfile -> scrollViewProfile.visibility = View.VISIBLE
            textViewNews -> newsLayout.visibility = View.VISIBLE
        }
    }

    private fun switchStarVisibility() {
        val visibleStarOff = favouriteStarOff.visibility
        favouriteStarOff.visibility = favouriteStarOn.visibility
        favouriteStarOn.visibility = visibleStarOff
    }

    private val listenerFavouriteStarOff = View.OnClickListener {
        switchStarVisibility()
        favouriteTickers.add(ticker)
    }
    private val listenerFavouriteStarOn = View.OnClickListener {
        switchStarVisibility()
        favouriteTickers.remove(ticker)
    }

    private fun initFavourite() {
        if (preferences.contains(getString(R.string.favourite_list)))
            favouriteTickers = preferences.getStringSet(getString(R.string.favourite_list), setOf())
                ?.toMutableList() ?: mutableListOf()
        if (ticker in favouriteTickers) switchStarVisibility()
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    private fun initPoints(candles: StockCandles) {
        val prices = candles.c ?: listOf()
        val times = candles.t ?: listOf()
        val points = prices.map {
            val index = prices.indexOf(it)
            DataPoint(Date(times[index] * 1000), it.toDouble())
        }.toTypedArray()
        points.sortBy { it.x }
        val series = LineGraphSeries(points)
        series.isDrawDataPoints = true
        series.setOnDataPointTapListener { _, dataPoint ->
            val locale = Locale.getDefault()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
            val date = Date(dataPoint.x.toLong())
            val toast = Toast.makeText(
                this,
                "${dateFormat.format(date)}, $${dataPoint.y.format(2)}",
                Toast.LENGTH_SHORT
            )
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
        if (times.isNotEmpty()) {
            graph.viewport.setMinX((times.first() * 1000).toDouble())
            graph.viewport.setMaxX((times.last() * 1000).toDouble())
            graph.viewport.isXAxisBoundsManual = true
        }
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false
        graph.removeAllSeries()
        graph.addSeries(series)
    }

    private fun initProfile() {
        textViewProfileContent.text = String.format(
            getString(R.string.profileContent),
            profile.ipo,
            profile.phone?.toDouble()?.toLong(),
            profile.shareOutstanding,
            profile.weburl,
            profile.finnhubIndustry)
    }

    private fun initNews() {
        newsAdapter = NewsViewAdapter(newsList)
        newsView.adapter = newsAdapter
        newsAdapter.notifyDataSetChanged()
    }

    private fun initData() {
        ticker = intent.getStringExtra(StockViewAdapter.StockHolder.TICKER).toString()
        textViewTicker.text = ticker
        textViewStockName.text =
            intent.getStringExtra(StockViewAdapter.StockHolder.STOCK_NAME).toString()
        textViewCurrentPrice.text =
            intent.getStringExtra(StockViewAdapter.StockHolder.CURRENT_PRICE).toString()
        textViewPriceInrease.text =
            intent.getStringExtra(StockViewAdapter.StockHolder.PRICE_INCREASE).toString()
        initFavourite()
        if (textViewPriceInrease.text.first() == '+')
            textViewPriceInrease.setTextColor(ContextCompat.getColor(this, R.color.green))
        else textViewPriceInrease.setTextColor(ContextCompat.getColor(this, R.color.red))

        val nowadays = getInstance()
        val yearAgo = getInstance()
        val monthAgo = getInstance()
        val weekAgo = getInstance()
        yearAgo.add(YEAR, -1)
        monthAgo.add(MONTH, -1)
        weekAgo.add(WEEK_OF_YEAR, -1)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                yearCandles = apiClient.stockCandles(
                    ticker,
                    "M",
                    yearAgo.timeInMillis / 1000,
                    nowadays.timeInMillis / 1000,
                    null
                )
                monthCandles = apiClient.stockCandles(
                    ticker,
                    "D",
                    monthAgo.timeInMillis / 1000,
                    nowadays.timeInMillis / 1000,
                    null
                )
                weekCandles = apiClient.stockCandles(
                    ticker,
                    "60",
                    weekAgo.timeInMillis / 1000,
                    nowadays.timeInMillis / 1000,
                    null
                )
                CoroutineScope(Dispatchers.Main).launch {
                    initPoints(yearCandles)
                    yearButton.visibility = View.VISIBLE
                    monthButton.visibility = View.VISIBLE
                    weekButton.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    messageError()
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                profile = apiClient.companyProfile2(ticker, null, null)
                CoroutineScope(Dispatchers.Main).launch {
                    initProfile()
                    if (activeTextView == textViewProfile) scrollViewProfile.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                messageError()
            }
        }

        val locale = Locale.getDefault()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", locale)
        val request = Request.Builder()
            .url(
                "${getString(R.string.url_request_news)}?symbol=${ticker}&from=${
                    dateFormat.format(
                        weekAgo.time
                    )
                }&to=${dateFormat.format(nowadays.time)}&token=${getString(R.string.api_key3)}"
            )
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                messageError()
            }

            override fun onResponse(call: Call, response: Response) {
                newsList = gson.fromJson(
                    response.body?.string(),
                    Types.newParameterizedType(List::class.java, News::class.java)
                )
                CoroutineScope(Dispatchers.Main).launch {
                    initNews()
                }
            }
        })

    }

    override fun onBackPressed() {
        onStop()
        super.onBackPressed()
    }

    private fun messageError() {
        val toast = Toast.makeText(
            applicationContext,
            "Error loading data. Try again in a minute.",
            Toast.LENGTH_LONG
        )
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company)
        preferences = getSharedPreferences(getString(R.string.settings), Context.MODE_PRIVATE)

        activeTextView = textViewProfile
        textViewChart.setOnClickListener(listenerChart)
        textViewProfile.setOnClickListener(listenerProfile)
        textViewNews.setOnClickListener(listenerNews)

        favouriteStarOff.setOnClickListener(listenerFavouriteStarOff)
        favouriteStarOn.setOnClickListener(listenerFavouriteStarOn)

        prevActivity.setOnClickListener { onBackPressed() }
        yearButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                initPoints(yearCandles)
            }
        }
        monthButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                initPoints(monthCandles)
            }
        }
        weekButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                initPoints(weekCandles)
            }
        }

        ApiClient.apiKey["token"] = getString(R.string.api_key3)
        initData()
    }

    override fun onStop() {
        val editor = preferences.edit()
        editor.putStringSet(getString(R.string.favourite_list), favouriteTickers.toSet()).apply()
        super.onStop()
    }
}