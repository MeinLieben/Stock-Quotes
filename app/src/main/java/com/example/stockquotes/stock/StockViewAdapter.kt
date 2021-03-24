package com.example.stockquotes.stock


import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.stockquotes.CompanyActivity
import com.example.stockquotes.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.stock_view.view.*


class StockViewAdapter(
    private val stocks: List<Stock>,
    private var favouriteTickers: MutableList<String>
) :
    RecyclerView.Adapter<StockViewAdapter.StockHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockHolder {
        val inflatedView = parent.inflate(R.layout.stock_view, false)
        return StockHolder(inflatedView, parent.context, favouriteTickers)
    }

    override fun onBindViewHolder(holder: StockHolder, position: Int) {
        val itemStock = stocks[position]
        holder.bindStock(itemStock)
    }

    override fun getItemCount() = stocks.size

    class StockHolder(
        private val view: View,
        private val context: Context,
        private val favouriteList: MutableList<String>,
        private var stock: Stock? = null
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {


        init {
            view.setOnClickListener(this)
            view.favouriteStarOff.setOnClickListener {
                view.favouriteStarOff.visibility = View.INVISIBLE
                view.favouriteStarOn.visibility = View.VISIBLE
                favouriteList.add(view.textViewTicker.text.toString())
            }
            view.favouriteStarOn.setOnClickListener {
                view.favouriteStarOn.visibility = View.INVISIBLE
                view.favouriteStarOff.visibility = View.VISIBLE
                favouriteList.remove(view.textViewTicker.text.toString())
            }
        }


        override fun onClick(v: View) {
            val companyIntent = Intent(context, CompanyActivity::class.java)
            companyIntent.putExtra(TICKER, v.textViewTicker.text)
            companyIntent.putExtra(STOCK_NAME, v.textViewStockName.text)
            companyIntent.putExtra(CURRENT_PRICE, v.textViewCurrentPrice.text)
            companyIntent.putExtra(PRICE_INCREASE, v.textViewPriceInrease.text)
            context.startActivity(companyIntent)
        }

        companion object {
            const val TICKER = "ticker"
            const val STOCK_NAME = "stock name"
            const val CURRENT_PRICE = "current price"
            const val PRICE_INCREASE = "price increase"
        }

        private fun Float.format(digits: Int) = "%.${digits}f".format(this)
        fun bindStock(stock: Stock) {
            this.stock = stock
            view.textViewTicker.text = stock.ticker
            view.textViewStockName.text = stock.stockName
            if (stock.ticker in favouriteList) {
                view.favouriteStarOff.visibility = View.INVISIBLE
                view.favouriteStarOn.visibility = View.VISIBLE
            } else {
                view.favouriteStarOn.visibility = View.INVISIBLE
                view.favouriteStarOff.visibility = View.VISIBLE
            }
            var textPriceIncrease = if (stock.priceIncrease < 0) {
                view.textViewPriceInrease.setTextColor(ContextCompat.getColor(context, R.color.red))
                ""
            } else {
                view.textViewPriceInrease.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.green
                    )
                )
                "+"
            }
            textPriceIncrease += "$${stock.priceIncrease.format(2)} ($textPriceIncrease${
                stock.percentPriceIncrease.format(
                    2
                )
            }%)"
            view.textViewPriceInrease.text = textPriceIncrease
            val textCurrentPrice = "$${stock.currentPrice.format(2)}"
            view.textViewCurrentPrice.text = textCurrentPrice

            if (stock.logoURL == "") {
                view.logo.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.no_image))
            } else {
                Picasso.with(context)
                    .load(stock.logoURL)
                    .into(view.logo, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                        }

                        override fun onError() {
                            view.logo.setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.no_image
                                )
                            )
                        }
                    })
            }
        }
    }
}

