/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Liang Zhang
 */
public class filterValuesCombVQL {
    String filter;
    String process;

    public filterValuesCombVQL() {
    }


    public JSONObject sqlUrlWithFilter (String filterValue,String processValue) throws JSONException{
        JSONObject dataSqlOptionObj = new JSONObject();
        //dataSqlOptionObj.put("filter",JSONObject.NULL);
        dataSqlOptionObj.put("filter",new JSONObject());
        
        JSONArray processArray = new JSONArray();
        JSONObject processObj=new JSONObject().put("$frequentValues",new JSONObject().put("path", processValue));
        processArray.put(processObj);
        
        dataSqlOptionObj.put("process",processArray);
        //dataSqlOptionObj.put("filter",new JSONObject().put("actor.mbox","mailto:91ce1aad-07ec-42e9-93d1-0e8ae49abe71@author.x-in-y.com"));
        //dataSqlOptionObj.put("process","");
        
        return dataSqlOptionObj;
    }
    
    
}
