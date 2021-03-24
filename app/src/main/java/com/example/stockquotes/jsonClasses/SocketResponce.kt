package com.example.stockquotes.jsonClasses.socketResponse

import com.google.gson.annotations.SerializedName

data class SocketResponse(
    @SerializedName("data") var data: List<Data>,
    @SerializedName("type") var type: String
)

data class Data(

    @SerializedName("c") var c: List<String>,
    @SerializedName("p") var p: Float,
    @SerializedName("s") var s: String,
    @SerializedName("t") var t: Long,
    @SerializedName("v") var v: Float

)