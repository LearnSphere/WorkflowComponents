/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *make the logic realtions of filter options into query url.
 * @author Liang Zhang
 */
public class StatementClientVeracity {
    public JSONArray filterByOption(JSONObject sqlUrlWithFilter,String lrsUrl, String username, String password) throws JSONException, MalformedURLException, IOException {    
        JSONArray stsArray=new JSONArray();  //Statements Array
        //create the query link
        String encodeDataSql=URLEncoder.encode(sqlUrlWithFilter.toString(), StandardCharsets.UTF_8.toString());
        String getDataUrl=lrsUrl+"search?query="+encodeDataSql;
        //set the authencation (basic)
        String auth = username + ":" + password;
        byte[] encodedAuth=Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));        
        String authHeaderValue = "Basic " + new String(encodedAuth);
        
        //set the request method and properties using HttpURLConnection
        URL obj=new URL(getDataUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", authHeaderValue);
        
        int responseCode=con.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_OK){
            System.out.println("GET Response Code :: " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
            String inputLine;
            StringBuffer sts = new StringBuffer();
            
            while ((inputLine = in.readLine()) != null) {
                    sts.append(inputLine);
            }
            in.close();
            stsArray = new JSONArray(sts.toString()); 
        }else{
            System.out.println("GET request not worked");
        }
       return(stsArray);         
    }
     
}
