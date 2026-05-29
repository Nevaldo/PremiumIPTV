package com.player.iptv.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TmdbModels {

    public static class TmdbSearchResponse {
        public List<TmdbMovieResult> results;
    }

    public static class TmdbMovieResult {
        public int id;
        public String title;
        @SerializedName("poster_path") public String posterPath;
        @SerializedName("backdrop_path") public String backdropPath;
        public String overview;
        @SerializedName("release_date") public String releaseDate;
        @SerializedName("vote_average") public double voteAverage;
        @SerializedName("genre_ids") public List<Integer> genreIds;
    }

    public static class TmdbMovieDetails {
        public int id;
        public String title;
        @SerializedName("original_title") public String originalTitle;
        @SerializedName("poster_path") public String posterPath;
        @SerializedName("backdrop_path") public String backdropPath;
        public String overview;
        @SerializedName("release_date") public String releaseDate;
        public int runtime;
        @SerializedName("vote_average") public double voteAverage;
        public List<Genre> genres;
        public Credits credits;
        @SerializedName("production_companies") public List<ProductionCompany> productionCompanies;
    }

    public static class ProductionCompany {
        public int id;
        public String name;
        @SerializedName("logo_path") public String logoPath;
    }

    public static class Genre {
        public int id;
        public String name;
    }

    public static class Credits {
        public List<Cast> cast;
    }

    public static class Cast {
        public int id;
        public String name;
        public String character;
        @SerializedName("profile_path") public String profilePath;
        @SerializedName("known_for_department") public String knownForDepartment;
    }

    public static class TmdbSeriesSearchResponse {
        public List<TmdbSeriesResult> results;
    }

    public static class TmdbSeriesResult {
        public int id;
        public String name;
        @SerializedName("poster_path") public String posterPath;
        @SerializedName("backdrop_path") public String backdropPath;
        public String overview;
        @SerializedName("first_air_date") public String firstAirDate;
        @SerializedName("vote_average") public double voteAverage;
        @SerializedName("genre_ids") public List<Integer> genreIds;
    }

    public static class TmdbSeriesDetails {
        public int id;
        public String name;
        @SerializedName("poster_path") public String posterPath;
        @SerializedName("backdrop_path") public String backdropPath;
        public String overview;
        @SerializedName("first_air_date") public String firstAirDate;
        @SerializedName("last_air_date") public String lastAirDate;
        @SerializedName("number_of_seasons") public int numberOfSeasons;
        @SerializedName("number_of_episodes") public int numberOfEpisodes;
        @SerializedName("vote_average") public double voteAverage;
        public List<Genre> genres;
        public Credits credits;
        public List<Season> seasons;
    }

    public static class Season {
        @SerializedName("season_number") public int seasonNumber;
        public String name;
        @SerializedName("poster_path") public String posterPath;
    }
}
