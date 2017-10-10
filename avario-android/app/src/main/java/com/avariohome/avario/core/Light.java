package com.avariohome.avario.core;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Light {
    private static final String TAG = "Light";
    private static volatile Light instance;
    public ArrayList<Algo> algos;

    private Light(){
        algos = new ArrayList<>();
    }

    public static Light getInstance(){
        if (instance == null){
            synchronized (Light.class){
                if (instance == null){
                    instance = new Light();
                }
            }
        }
        return instance;
    }

    public static void updateAlgo(String name, String payload){
        try {
            Algo alg = new Algo();
            alg.name = name;
            boolean skip = false;
            for (int i = 0; i < getInstance().algos.size(); i++){
                if (getInstance().algos.get(i).name.equals(alg.name)){
                    Log.v(TAG, "Skipping " + name);
                    skip = true;
                    if (payload != null && !payload.isEmpty()){
                        Log.v(TAG, "Updating " + name + " payload " + payload);
                        payload = payload.replace("\\", "");
                        JSONObject jsonPayload = new JSONObject(payload);
                        getInstance().algos.get(i).option = jsonPayload.getString("option");
                        getInstance().algos.get(i).payload = payload;
                    }
                    Log.v(TAG, "New payload value: "
                            + getInstance().algos.get(i).payload +
                    "\nNew options value " + getInstance().algos.get(i).option);

                    break;
                }
            }
            if (!skip){
                Log.v(TAG, "Adding " + name + " to list.");
                getInstance().algos.add(alg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String parseAlgoEntityID(List<JSONObject> entityJSONs) throws JSONException {
        String entityIds = null;
        for (JSONObject item : entityJSONs) {
            if (entityIds == null){
                entityIds = item.getString("entity_id");
            } else {
                entityIds = entityIds + "," + item.getString("entity_id");
            }
        }
        return entityIds;
    }

    public static boolean isStateSelected(String entityIDs, String state){
        boolean result = false;
        Log.v(TAG, "Check state");
        ArrayList<Algo> tempAlgo = getInstance().algos;
        for (Algo item : tempAlgo) {
            if (item.name.equals(entityIDs)){
                Log.v(TAG, "Name " + item.name
                        + "\nOption " + item.option + ", " + state);
                if (item.option != null && item.option.equalsIgnoreCase(state)){
                    result =  true;
                }
                break;
            }
        }

        return result;
    }

    public static void addAlgo(ArrayList<Algo> algos){
        if (algos != null){
            getInstance().algos.addAll(algos);
        }
    }

    public static void addAlgo(Algo algo){
        getInstance().algos.add(algo);
    }

    public static class Algo{
        public String name;
        public String option;
        public String payload;
    }
}
