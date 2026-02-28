package com.mark.matrixbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client for interacting with The Movie Database (TMDb) API,
 * <p>
 * This class provides helper methods to search for movies, retrieve movie details, and fetch metadata such as runtime.
 * </p>
 */
public class TMDb {

    /**
     * API key used to authenticate requests.
     */
    private final String apiKey;

    /**
     * Base URL for TMDb API v3.
     */
    private static final String BASE_URL = "https://api.themoviedb.org/3";

    /**
     * Timeout duration in milliseconds.
     */
    private static final int TIMEOUT_MS = 5000;

    /**
     * Constructs a new TMDb API client.
     * @param apikey the TMDb API key
     */
    public TMDb(String apikey){
        if (apikey == null || apikey.isBlank()) {
            throw new IllegalArgumentException("TMDb API key must not be null or blank");
        }
        this.apiKey = apikey;
    }

    /**
     * Executes a GET request against the TMDb API and parses the response as JSON.
     * @param urlStr the full request URL
     * @return the parsed {@link JsonObject}, or {@code null} if the request fails
     */
    private JsonObject makeRequest(String urlStr) {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            return JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())
            ).getAsJsonObject();

        } catch (Exception e) {
            System.err.println("TMDb request failed: " + urlStr);
            return null;
        }
    }

    /**
     * Searches TMDb for movies matching a query string.
     * @param query the movie title or partial title
     * @param year optional release year filter, or {@code null}
     * @return a {@link JsonArray} of search results, or an empty array if the request fails
     */
    public JsonArray searchMovies(String query, Integer year){

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String url = BASE_URL + "/search/movie"
                + "?api_key=" + apiKey
                + "&query=" + encodedQuery
                + (year != null ? "&year=" + year : "");

        JsonObject root = makeRequest(url);

        if ((root == null) || !root.has("results")){
            return new JsonArray();
        }

        return root.getAsJsonArray("results");
    }

    /**
     * Retrieves full movie details from TMDb by movie ID.
     * @param id the TMDb movie ID
     * @return a {@link JsonObject} containing movie details, or {@code null} on failure
     */
    public JsonObject getMovieById(String id) {
        String url = BASE_URL + "/movie/" + id
                + "?api_key=" + apiKey;

        return makeRequest(url);
    }

    /**
     * Retrieves the runtime of a movie in minutes.
     * @param movieId the TMDb movie ID
     * @return the runtime in minutes, or {@code 0} if unavailable
     */
    public int getRuntime(int movieId){
        String url = BASE_URL + "/movie/" + movieId
                + "?api_key=" + apiKey;

        JsonObject obj = makeRequest(url);

        if (obj != null && obj.has("runtime") && !obj.get("runtime").isJsonNull()) {
            return obj.get("runtime").getAsInt();
        }

        return 0;
    }

}
