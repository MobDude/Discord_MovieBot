package com.mark.discordbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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

    private final HttpClient httpClient;

    /**
     * Base URL for TMDb API v3.
     */
    private static final String BASE_URL = "https://api.themoviedb.org/3";

    /**
     * Timeout duration in milliseconds.
     */
    //private static final int TIMEOUT_MS = 5000;

    /**
     * Constructs a new TMDb API client.
     * @param apikey the TMDb API key
     */
    public TMDb(String apikey){
        if (apikey == null || apikey.isBlank()) {
            throw new IllegalArgumentException("TMDb API key must not be null or blank");
        }
        this.apiKey = apikey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * Executes a GET request against the TMDb API and parses the response as JSON.
     * @param urlStr the full request URL
     * @return the parsed {@link JsonObject}, or {@code null} if the request fails
     */
    private JsonObject makeRequest(String urlStr) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlStr)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200) return null;
            return JsonParser.parseString(response.body()).getAsJsonObject();

        } catch (Exception e) {
            System.err.println("TMDb request failed: " + e.getMessage());
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
        String url = String.format("%s/search/movie?api_key=%s&query=%s%s", BASE_URL, apiKey, encodedQuery, (year !=null ? "&year=" + year : ""));

        JsonObject root = makeRequest(url);
        return (root != null && root.has("results")) ? root.getAsJsonArray("results") : new JsonArray();
    }

    /**
     * Retrieves the runtime of a movie in minutes.
     * @param movieId the TMDb movie ID
     * @return the runtime in minutes, or {@code 0} if unavailable
     */
    public int getRuntime(int movieId){
        JsonObject obj = makeRequest(BASE_URL + "/movie/" + movieId + "?api_key=" + apiKey);
        return (obj != null && obj.has("runtime") && !obj.get("runtime").isJsonNull())
                ? obj.get("runtime").getAsInt() : 0;
    }

    /**
     * Retrieves full movie details from TMDb by movie ID.
     * @param id the TMDb movie ID
     * @return a {@link JsonObject} containing movie details, or {@code null} on failure
     */
    public JsonObject getMovieById(String id) {
        return makeRequest(BASE_URL + "/movie/" + id + "?api_key=" + apiKey);
    }



}
