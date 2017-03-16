package edu.cmu.pslc.learnsphere.imports.xAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import java.util.*;

public class JsonFlattener {
    public Map<String, String> parse(JSONObject jsonObject)throws Exception {
        Map<String, String> flatJson = new HashMap<String, String>();
        flatten(jsonObject, flatJson, "");
        return flatJson;
    }

    public List<Map<String, String>> parse(JSONArray jsonArray)throws Exception {
        List<Map<String, String>> flatJson = new ArrayList<Map<String, String>>();
        int length = jsonArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Map<String, String> stringMap = parse(jsonObject);
            flatJson.add(stringMap);
        }
        return flatJson;
    }

    public List<Map<String, String>> parseJson(String json) throws Exception {
        List<Map<String, String>> flatJson = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            flatJson = new ArrayList<Map<String, String>>();
            flatJson.add(parse(jsonObject));
        } catch (JSONException je) {
            flatJson = handleAsArray(json);
        }
        return flatJson;
    }

    private List<Map<String, String>> handleAsArray(String json) throws Exception {
        List<Map<String, String>> flatJson = null;
        try {
            JSONArray jsonArray = new JSONArray(json);
            flatJson = parse(jsonArray);
        } catch (Exception e) {
            throw new Exception("Json might be malformed: ",e);
        }
        return flatJson;
    }

    private void flatten(JSONArray obj, Map<String, String> flatJson, String prefix)throws Exception {
        int length = obj.length();
        for (int i = 0; i < length; i++) {
            if (obj.get(i).getClass() == JSONArray.class) {
                JSONArray jsonArray = (JSONArray) obj.get(i);
                if (jsonArray.length() < 1) continue;
                flatten(jsonArray, flatJson, prefix + i);
            } else if (obj.get(i).getClass() == JSONObject.class) {
                JSONObject jsonObject = (JSONObject) obj.get(i);
                flatten(jsonObject, flatJson, prefix + (i + 1));
            } else {
                String value = obj.get(i).toString();
                if (value != null)
                    flatJson.put(prefix + (i + 1), value);
            }
        }
    }

    private void flatten(JSONObject obj, Map<String, String> flatJson, String prefix)throws Exception {
        Iterator iterator = obj.keys();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            
            if (obj.get(key).getClass() == JSONObject.class) {
                JSONObject jsonObject = (JSONObject) obj.get(key);
                String prefixNew= prefix.equals("")? key : prefix+"-"+key;
                flatten(jsonObject, flatJson, prefixNew);
            } else if (obj.get(key).getClass() == JSONArray.class) {
                JSONArray jsonArray = (JSONArray) obj.get(key);
                if (jsonArray.length() < 1) continue;
                String prefixNew= prefix.equals("")? key : prefix+"-"+key;
                flatten(jsonArray, flatJson, prefixNew);
            } else {
                String value = obj.get(key).toString();
                if (value != null && !value.equals("null"))
                	flatJson.put(prefix.equals("")? key : prefix+"-"+key, value);
            }
        }
    }
}

