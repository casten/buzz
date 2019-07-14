package com.casten.buzz;

import org.json.JSONException;
import org.json.JSONObject;

public class BuzzProto {
    public static String createBuzzPayload(int which) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("effect", which);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  jo.toString() + "\n";
    }

    public static String createBuzzPayload() {
        return createBuzzPayload(53);
    }

}
