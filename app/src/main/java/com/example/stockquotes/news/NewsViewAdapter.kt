package com.example.stockquotes.news

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.stockquotes.R
import com.example.stockquotes.stock.inflate
import com.finnhub.api.models.News
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.news_view.view.*


class NewsViewAdapter(private val newsList: List<News>) :
    RecyclerView.Adapter<NewsViewAdapter.NewsHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsHolder {
        val inflatedView = parent.inflate(R.layout.news_view, false)
        return NewsHolder(inflatedView, parent.context)
    }

    override fun onBindViewHolder(holder: NewsHolder, position: Int) {
        val itemNews = newsList[position]
        holder.bindStock(itemNews)
    }

    override fun getItemCount() = newsList.size

    class NewsHolder(
        private val view: View,
        private val context: Context,
        private var news: News? = null
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {



        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(news?.url))
            context.startActivity(browserIntent)
        }

        fun bindStock(news: News) {
            this.news = news
            view.textViewHeadline.text = news.headline
            view.textViewSummary.text = news.summary
            if (news.image == "") {
                view.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.no_image))
            } else {
                Picasso.with(context)
                    .load(news.image)
                    .into(view.image, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                        }

                        override fun onError() {
                            view.image.setImageDrawable(
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
