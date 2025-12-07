package com.mark.discordbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TMDb {

    private static String API_KEY;
    private static final String BASE_URL = "https://api.themoviedb.org/3";

    public TMDb(String apikey){
        API_KEY = apikey;
    }

    private JsonObject makeRequest(String urlStr) {
        try {
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

    public JsonArray searchMovies(String query, Integer year){
        try{
            query = query.replace(" ", "%20");
            String urlStr = BASE_URL + "/search/movie?api_key=" + API_KEY + "&query=" + query + (year != null ? "&year=" + year : "");
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
            String urlStr =  BASE_URL + "/movie/" + id +
                    "?api_key=" + API_KEY;

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

    public int getRuntime(int movieId){
        String url = BASE_URL + "/movie/" + movieId + "?api_key=" + API_KEY;
        JsonObject obj = makeRequest(url);

        if(obj.has("runtime") && !obj.get("runtime").isJsonNull()){
            return obj.get("runtime").getAsInt();
        }
        return 0;
    }

}
