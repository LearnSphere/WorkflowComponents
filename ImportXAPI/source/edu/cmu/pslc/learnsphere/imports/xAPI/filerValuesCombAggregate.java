/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Liang Zhang
 */
public class filerValuesCombAggregate {
    String queryMode;
    String filterByUntil;
    String filterBySince;
    String group;
    
    public filerValuesCombAggregate() {
    }

    public filerValuesCombAggregate(String queryMode, String filterByUntil, String filterBySince, String group) {
        this.queryMode = queryMode;
        this.filterByUntil = filterByUntil;
        this.filterBySince = filterBySince;
        this.group = group;
    }
    
    //$match,$group,
    public JSONArray sqlUrlWithFilter(String filterByUntil, String filterBySince, String matchFilter, String groupingKey) throws ParseException, JSONException{
        JSONArray dataSqlOptionArray = new JSONArray();
        
        ArrayList valuesSplitListAgrMa = new ArrayList(); //match list
        //ArrayList valuesSplitListAgrGr = new ArrayList(); //group list
        ArrayList valuesSplitListAgr = new ArrayList();
        
        //Transform the data format
        String filterByUntilAgr = null;
        String filterBySinceAgr = null;
        
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = dt.parse(filterByUntil);
        SimpleDateFormat dt1 = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zZ (zzzz)");
        filterByUntilAgr=dt1.format(date);
        
        SimpleDateFormat dts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date dates = dts.parse(filterBySince);
        SimpleDateFormat dts1 = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zZ (zzzz)");
        filterBySinceAgr=dts1.format(dates);
        
        //Parse as query url
        
        JSONObject filterByUntilAgrObj = new JSONObject().put(matchFilter,new JSONObject().put("$lt",new JSONObject().put("$parseDate",new JSONObject().put("date",filterByUntilAgr))));
        JSONObject filterBySinceAgrObj = new JSONObject().put(matchFilter,new JSONObject().put("$gt",new JSONObject().put("$parseDate",new JSONObject().put("date",filterBySinceAgr))));
        
        valuesSplitListAgrMa.add(filterByUntilAgrObj);
        valuesSplitListAgrMa.add(filterBySinceAgrObj);
        
        //JSONObject filterByMatch=new JSONObject().put("$match",new JSONObject().put("$and",valuesSplitListAgrMa));
        JSONObject filterByMatch=new JSONObject().put("$match",new JSONObject().put(matchFilter, new JSONObject().put("$gte",0)));
        
        JSONObject filterByGroupObGr=new JSONObject();
        filterByGroupObGr.put("_id",groupingKey);
        filterByGroupObGr.put("count",new JSONObject().put("$sum",1));
        //filterByGroupObGr.put("time","statement.timestamp");
        filterByGroupObGr.put("score",new JSONObject().put("$avg","$statement.result.score.scaled"));
        
        JSONObject filterByGroupAgr=new JSONObject().put("$group",filterByGroupObGr);
        
        JSONObject filterByProjAgr=new JSONObject();
        filterByProjAgr.put("count","$count");
        //filterByProjAgr.put("score","$score");
        //filterByProjAgr.put("name","$_id.name");
        //filterByProjAgr.put("_id",0);
        JSONObject filterByProjectAgr=new JSONObject().put("$project",filterByProjAgr);
                
        //dataSqlOptionArray.put(filterByMatch);
        dataSqlOptionArray.put(filterByGroupAgr);
        //dataSqlOptionArray.put(filterByProjectAgr);
        
        return dataSqlOptionArray;
    }
    
}
