package com.mark.discordbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TMDb {

    private final String apiKey;

    public TMDb(String apiKey){
        this.apiKey = apiKey;
    }

    public JsonArray searchMovies(String query, Integer year){
        try{
            query = query.replace(" ", "%20");
            String urlStr = "https://api.themoviedb.org/3/search/movie?api_key=" + apiKey + "&query=" + query + (year != null ? "&year=" + year : "");
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())
            ).getAsJsonObject();

            return root.getAsJsonArray("results");

        } catch (Exception e){
            e.printStackTrace();
            return new JsonArray();
        }
    }

    public JsonObject getMovieById(String id) {
        try {
            String urlStr = "https://api.themoviedb.org/3/movie/" + id +
                    "?api_key=" + apiKey;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            return JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())
            ).getAsJsonObject();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
