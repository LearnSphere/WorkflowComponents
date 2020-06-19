/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 *
 * @author Liang Zhang
 *
 * 
 */
public class BaseClient {
    protected URL _host;
    protected String username;
    protected String password;
    protected String authString;
    
    public BaseClient(String uri, String username, String password) throws MalformedURLException {
        init(new URL(uri), username, password);
    }
    
    protected void init(URL uri, String user, String password) throws MalformedURLException{
        String holder =uri.toString();
        if(holder.endsWith("/")){
            URL newUri=new URL(holder.substring(0,holder.length()-1));
            this._host=newUri;
        }else{
            this._host=uri;
        }
        this.username=user;
        this.password=password;
        
        //define the authencation (basic)
        String auth = username + ":" + password;
        byte[] encodedAuth=Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));    
        this.authString="Basic "+new String(encodedAuth);
    }
    
    protected HttpURLConnection initializaConnection(URL url) throws IOException{
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.addRequestProperty("X-Experience-API-Version", "1.0.3");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", this.authString);
        return conn;
    }
    
    protected String readFromConnection(HttpURLConnection conn) throws IOException{
        if(conn.getResponseCode()>=400){
            String server = conn.getURL().toString();
            int statusCode = conn.getResponseCode();
            InputStream inputStream=new BufferedInputStream(conn.getErrorStream());
            StringBuilder stringBuilder=new StringBuilder();
            InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String strl;
            while((strl=bufferedReader.readLine())!=null)
                stringBuilder.append(strl);
            throw new IOException(
            String.format("Server (%s) Responded with %d: %s", new Object[] { server, Integer.valueOf(statusCode), stringBuilder.toString() }));
        }
        
        InputStream in=new BufferedInputStream(conn.getInputStream());
        StringBuilder sb= new StringBuilder();
        InputStreamReader reader=new InputStreamReader(in);
        BufferedReader br=new BufferedReader(reader);
        String line;
        while((line=br.readLine())!=null)
            sb.append(line);
        br.close();
        reader.close();
        conn.disconnect();
        return sb.toString();
    }
    
}
