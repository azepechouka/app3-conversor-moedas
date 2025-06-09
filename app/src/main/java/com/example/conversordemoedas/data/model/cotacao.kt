package com.example.conversordemoedas.data.model

import com.google.gson.annotations.SerializedName

data class cotacao(
    @SerializedName("code") val code: String,
    @SerializedName("codein") val codeIn: String,
    @SerializedName("name") val name: String,
    @SerializedName("high") val high: String,
    @SerializedName("low") val low: String,
    @SerializedName("varBid") val varBid: String,
    @SerializedName("pctChange") val pctChange: String,
    @SerializedName("bid") val bid: String,
    @SerializedName("ask") val ask: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("create_date") val createDate: String
)