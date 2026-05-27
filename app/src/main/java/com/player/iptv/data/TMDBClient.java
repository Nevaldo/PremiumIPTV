package com.player.iptv.data;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class TMDBClient {

    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    public static final String API_KEY = "2a331bc8c4d6de257eb5b9c7f1e96966";
    public static final String LANGUAGE = "pt-BR";

    private static TMDBApi api;

    public static TMDBApi getApi() {
        if (api == null) {
            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(TMDBApi.class);
        }
        return api;
    }
}
