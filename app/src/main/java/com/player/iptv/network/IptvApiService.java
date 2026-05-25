package com.player.iptv.network;

import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface IptvApiService {

    @FormUrlEncoded
    @POST("player_api.php")
    Single<Response<ResponseBody>> authenticate(
        @Field("username") String username,
        @Field("password") String password
    );

    @GET("player_api.php")
    Single<Response<ResponseBody>> getData(
        @Query("username") String username,
        @Query("password") String password,
        @Query("action") String action
    );

    @GET("player_api.php")
    Single<Response<ResponseBody>> getSeriesInfo(
        @Query("username") String username,
        @Query("password") String password,
        @Query("action") String action,
        @Query("series_id") int seriesId
    );
}
