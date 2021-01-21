package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonManager {
    private static Gson gson;

    private GsonManager() {
    }

    public static synchronized Gson getInstance() {
        if (gson == null) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        return gson;
    }
}
