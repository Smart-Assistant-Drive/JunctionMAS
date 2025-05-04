package Utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class JsonSerializer {

    public static JsonObject stringToJsonObjectGson(String jsonString) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(jsonString, JsonObject.class);
        } catch (JsonParseException e) {
            System.out.println("Errore durante la conversione della stringa in JsonObject: " + e.toString());
            throw new IllegalArgumentException();
        }
    }

}
