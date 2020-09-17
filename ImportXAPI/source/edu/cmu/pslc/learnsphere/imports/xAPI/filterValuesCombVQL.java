/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    
    public JSONObject sqlUrlWithFilter (String filterValue,String processValue) throws JSONException, ParseException{
        JSONObject dataSqlOptionObj = new JSONObject();
        dataSqlOptionObj.put("filter",JSONObject.NULL);
        //dataSqlOptionObj.put("filter",new JSONObject().put("actor.name", "Guest of Memphis"));
        //ANDed together
        JSONObject multiFilterObj=new JSONObject()
                .put("actor.name","Guest of Memphis")
                .put("object.id", "7aaf118c-f174-3eba-9ec5-680cd791a020");
        //dataSqlOptionObj.put("filter",multiFilterObj);
        
        JSONObject multiFilterObj01=new JSONObject().put("actor.mbox","mailto:414a5a5b-3d66-4ae6-99ed-5552ffccfe47@author.x-in-y.com");
        //JSONObject multiFilterObj02=new JSONObject().put("object.id","7aaf118c-f174-3eba-9ec5-680cd791a020");
        JSONArray filterArray = new JSONArray() 
            .put(multiFilterObj01);
            //.put(multiFilterObj02);
        //dataSqlOptionObj = new JSONObject().put("filter",new JSONObject().put("$or",filterArray));
        
        JSONObject multiFilterObjUntil=new JSONObject();
        JSONObject multiFilterObjSince=new JSONObject();
        String filterByUntil = "2020-09-09T00:28:07.551Z";
        String filterBySince = "2020-09-08T22:29:52.104Z";
        
        JSONArray filterArrayTime = new JSONArray()
                .put(new JSONObject().put("timestamp",new JSONObject().put("$lte",new JSONObject().put("$parseDate",new JSONObject().put("date",filterByUntil)))))
                .put(new JSONObject().put("timestamp",new JSONObject().put("$gte",new JSONObject().put("$parseDate",new JSONObject().put("date",filterBySince)))));  
            
        dataSqlOptionObj = new JSONObject().put("filter",new JSONObject().put("$and",filterArrayTime));
        
        JSONArray processArray = new JSONArray();
        JSONObject processObj=new JSONObject().put("$frequentValues",new JSONObject().put("path", processValue));
        processArray.put(processObj);
        dataSqlOptionObj.put("process",processArray);
        
        //dataSqlOptionObj.put("filter",new JSONObject().put("actor.mbox","mailto:91ce1aad-07ec-42e9-93d1-0e8ae49abe71@author.x-in-y.com"));
        //dataSqlOptionObj.put("process","");
        
        return dataSqlOptionObj;
    }
    
    
}
