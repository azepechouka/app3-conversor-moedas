package com.example.conversordemoedas.data.api

import com.example.conversordemoedas.data.model.cotacao
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AwesomeApi {

    @GET("json/last/{coinPair}")
    suspend fun getcotacao(@Path("coinPair") coinPair: String):
            Response<Map<String, cotacao>>
}