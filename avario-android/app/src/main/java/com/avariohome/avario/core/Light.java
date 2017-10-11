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

    public static void addAllAlgo(ArrayList<Algo> alg){
        if (alg != null) {
            getInstance().algos.clear();
            getInstance().algos.addAll(alg);
        }

    }

    public static void addAlgo(String name, String payload){
        try {
            JSONObject jsonPayload = new JSONObject(payload);
            Algo alg = new Algo();
            alg.name = name;
            alg.option = StateArray.getInstance().
                    getSettingsDefaultLightAlgo(jsonPayload.getString("entity_id"));
            alg.entityID = jsonPayload.getString("entity_id");
            alg.payload = payload;
            boolean skip = false;
            for (int i = 0; i < getInstance().algos.size(); i++){
                if (getInstance().algos.get(i).name.equals(alg.name)){
                    Log.v(TAG, "Skipping " + name);
                    skip = true;
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
                        getInstance().algos.get(i).entityID = jsonPayload.getString("entity_id");
                    }
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

    public static boolean isStateSelected(String entityIDs, String state){
        boolean result = false;
        Log.v(TAG, "Check state");
        for (Algo item : getInstance().algos) {
            if (item.name.equals(entityIDs)){
                if (item.option != null && item.option.equalsIgnoreCase(state)){
                    result =  true;
                }
                break;
            }
        }

        return result;
    }

    public static String isPresentOnAlgoList(List<String> entityIds){
        String name = null;
        for (String item : entityIds) {
            if (name == null){
                name = item;
            } else {
                name = name + "," + item;
            }
        }
        for (Algo item : getInstance().algos) {
            if (item.name.equals(name)){
                return name;
            }
        }
        return null;
    }

    public static boolean hasSameOptions(String entityIDs, String option){
        boolean result = false;

        for (Algo item : getInstance().algos) {
            if (item.name.equals(entityIDs)){
                if (item.option != null && item.option.equalsIgnoreCase(option)){
                    result =  true;
                }
                break;
            }
        }

        return result;
    }

    public static class Algo{
        public String name;
        public String option;
        public String entityID;
        public String payload;
    }
}
