package com.player.iptv.data;

import com.player.iptv.model.TmdbModels.TmdbMovieDetails;
import com.player.iptv.model.TmdbModels.TmdbSearchResponse;
import com.player.iptv.model.TmdbModels.TmdbSeriesDetails;
import com.player.iptv.model.TmdbModels.TmdbSeriesSearchResponse;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TMDBApi {

    @GET("search/movie")
    Single<TmdbSearchResponse> searchMovie(
            @Query("query") String query,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    @GET("movie/{id}")
    Single<TmdbMovieDetails> getMovieDetails(
            @Path("id") int id,
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("append_to_response") String append
    );

    @GET("search/tv")
    Single<TmdbSeriesSearchResponse> searchSeries(
            @Query("query") String query,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    @GET("tv/{id}")
    Single<TmdbSeriesDetails> getSeriesDetails(
            @Path("id") int id,
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("append_to_response") String append
    );
}
