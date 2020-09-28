/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *helps us do the request to LRS, and retrieve statements from LRS.
 * @author Liang Zhang
 */
public class filterValuesComb {
    public JSONObject sqlUrlWithFilter(List<String> filters, Map<String,String> filterOptionPathsMap,List<String> filterValues) throws JSONException{
        
        JSONObject sqlUrlWithFilter= new JSONObject();
        Map<String, String> hmapSql = combineListsIntoOrderedMap(filters,filterValues);
        
        ArrayList valuesSplitList = new ArrayList();
        //JSONObject dataSqlActor = new JSONObject();
        JSONObject dataSqlVerb = new JSONObject();
        JSONObject dataSqlSince = new JSONObject();
        JSONObject dataSqlUntil = new JSONObject();
        JSONObject dataSqlActivity = new JSONObject();
        JSONObject dataSqlStsid = new JSONObject();        
        
        //Combine all these query opptions for creating data query url string
        for (String filter : filters){
            if (hmapSql.containsKey(filter)){
                JSONObject dataSqlOption = new JSONObject();
                dataSqlOption=dataSqlByOption(hmapSql,filterOptionPathsMap,filter);
                valuesSplitList.add(dataSqlOption);
            }
        }
        
        //make different situation for separate loop: size of filter functions is 1 or >1
        if(filterValues.size()>1&&filters.size()>1&&(filterValues.size()==filters.size())){
            sqlUrlWithFilter.put("$and",valuesSplitList); 
        }
        if(filterValues.size()==1&&filterValues.size()==1){ 
           JSONArray jsonArray = new JSONArray(valuesSplitList);
           sqlUrlWithFilter=jsonArray.getJSONObject(0);
        }
        
        return(sqlUrlWithFilter);
    }
     
    Map<String,String> combineListsIntoOrderedMap (List<String> keys, List<String> values) {
        if (keys.size() != values.size())
            throw new IllegalArgumentException ("Cannot combine lists with dissimilar sizes");
        Map<String,String> map = new HashMap<String,String>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }
    
    JSONObject dataSqlByOption(Map<String, String> hmapSql, Map<String,String> filterOptionPathsMap, String filterByOption) throws JSONException{
        JSONObject dataSqlOption = new JSONObject();
        String filterByOptionString = hmapSql.get(filterByOption);
        String filterOptionPath = filterOptionPathsMap.get(filterByOption);
        if (hmapSql.containsKey(filterByOption)){
            //get each filter values
            JSONObject filterByOptionSql = new JSONObject();
            if(filterByOptionString.contains(";")){
               String[] valuesSplit= filterByOptionString.split(";");
               ArrayList valuesSplitList = new ArrayList(Arrays.asList(valuesSplit));
               //eliminates leading and trailing spaces for elements of filter values list
               for(int j = 0; j<valuesSplitList.size();j++){
                 valuesSplitList.set(j, valuesSplitList.get(j).toString().trim());
               }
               
               //constitute the json object for querying
               filterByOptionSql.put("$in",valuesSplitList);
               dataSqlOption.put(filterOptionPath,filterByOptionSql);
               
            }else{
                if(filterByOption.equals("filterBySince")||filterByOption.equals("filterByUntil")){
                   String newTimeStamp = null;
                    try {
                        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        Date date = dt.parse(filterByOptionString.trim());
                        SimpleDateFormat dt1 = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zZ (zzzz)");
                        newTimeStamp=dt1.format(date);
                    } catch (ParseException ex) {
                        Logger.getLogger(filterValuesComb.class.getName()).log(Level.SEVERE, null, ex);
                    }
                   JSONObject filterByOptionString01= new JSONObject();
                   filterByOptionString01.put("date",newTimeStamp);
                   JSONObject filterByOptionString02= new JSONObject();
                   filterByOptionString02.put("$parseDate",filterByOptionString01);
                   JSONObject filterByOptionString03= new JSONObject();
                   filterByOptionString03.put("$gt", filterByOptionString02);
                   dataSqlOption.put(filterOptionPath,filterByOptionString03);
               }else{
                   dataSqlOption.put(filterOptionPath,filterByOptionString.trim());
                }
            }
        }
        return dataSqlOption;
    }
    
}
