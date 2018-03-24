package edu.cmu.cs.lti.discoursedb.remote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
//import org.codehaus.jackson.map.ObjectMapper;
//import org.codehaus.jackson.map.util.JSONPObject;

//import com.google.gson.JsonObject;



// https://stackoverflow.com/questions/21223084/how-do-i-use-an-ssl-client-certificate-with-apache-httpclient
// https://stackoverflow.com/questions/22296312/convert-certificate-from-pem-into-jks
//
// Converted cid.pem to pk12 format with:
// openssl pkcs12 -export -in cid.pem -out cert.p12
// It asks for a password; that should match what is used here.

public class QueryProxy {
	String user;       // Email address of user, used to authorize discoursedb access
	String site;       // Discoursedb REST interface base URL
	String password;   // Password to the cert.p12 file
	String path;       // Path to the cert.p12 file (must be local)
	String browser;    // URL to user interface for query builder

	String resourceDir = "";
	String authHeader;
	CredentialsProvider provider;
	HttpClient client;
	SSLContext  sslContext;
	
	static final int CONNECT_TIMEOUT = 5000;     // 5 seconds
	static final int GET_DATA_TIMEOUT = 30000;   // 5 minutes
	
	
	static String help = "Need a file called discoursedb.query.properties containing properties BROWSER, SITE, KEYSTOREPATH, and KEYSTOREPASS";

	KeyStore readStore() throws IOException  {
        try (InputStream keyStoreStream = new FileInputStream(new File(resourceDir + path))) {
        		KeyStore keyStore = KeyStore.getInstance("PKCS12"); // or "PKCS12"
            keyStore.load(keyStoreStream, password.toCharArray());
            return keyStore;
        } catch (Exception e) {
			e.printStackTrace();
			throw(new IOException("Must have the " + path + " file in the classpath"));
		}
    }
	
	void setup() throws  Exception {
		sslContext = SSLContexts.custom()
                .loadKeyMaterial(readStore(),password.toCharArray()) // use null as second param if you don't have a separate key password
                .build();
		String auth = user + ":";
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
		authHeader = "Basic " + new String(encodedAuth);
	}
	
	
    
    void loadProps() throws IOException  {
	    	Properties prop = new Properties();
	    	InputStream input = null;
    		try {
    				input = new FileInputStream(new File(resourceDir + "discoursedb.query.properties"));
    				
				prop.load(input);
				site = prop.getProperty("SITE");
		    		path = prop.getProperty("KEYSTOREPATH");
		    		password = prop.getProperty("KEYSTOREPASS");
		    		browser = prop.getProperty("BROWSER");
		    		
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				IOException oops = new IOException(help,e);
				throw(oops);
			}
    }
    
	
	public QueryProxy(String googleID, String resourceDir) throws  Exception {
		user = googleID;
		this.resourceDir = resourceDir;
		loadProps();
		setup();
	}
	
	public String prop_list() throws ClientProtocolException, IOException {
		return callRestApi("/browsing/prop_list",null);
	}
	
	JsonArray rawQueries = null;
	public List<SavedQuery> getDiscoursedbQueries(String database) throws ClientProtocolException, IOException {
		rawQueries = callRestApiJson("/browsing/prop_list",null).readArray();
		return SavedQuery.parseListing(rawQueries,  database);
	}
	
	public String directBrowserUrl() {
		return browser;
	}
	
	public List<String> databases() throws ClientProtocolException, IOException {
		JsonObject jresult = callRestApiJson("/browsing/databases",null).readObject();
		
		// {"_embedded":{"browsingDatabasesResources":[{"databases":["openfl"]}]}}
		JsonArray items = jresult.getJsonObject("_embedded")
				.getJsonArray("browsingDatabasesResources")
				.getJsonObject(0)
				.getJsonArray("databases");
		ArrayList<String> result = new ArrayList<>();
		for( JsonValue i: items) {
			result.add(i.toString());
		}
			
		return result;
	}
	
	
	
	private Set<String> getAnnotationTypes(JsonArray ra) {
		Set<String> annos = new HashSet<String>();
		for(JsonValue item: ra) {
			annos.add(( (JsonString) ((JsonObject)item).get("type")).getString());
		}
		return annos;
	}
	
	public void queryToCsv(SavedQuery selectorOutput, File outfile) throws ClientProtocolException, IOException {
		querytextToCsv(selectorOutput.querytext, outfile);
	}
	/*
	 * Read a bunch of records from DiscourseDB, format as CSV, and write to a file
	 * 
	 * Format of the file we're reading:
	 * 
	 * {"_embedded":{"browsingContributionResources":[
	 *   {"type":"GIT_COMMIT_MESSAGE",
	 *   "content":"Initial commit",
	 *   "title":"",
	 *   "contributor":"jgranick",
	 *   "discourseParts": ["openfl/actuate commit messages"],
	 *   "startTime":"2012-01-18T04:19:19Z",
	 *   "annotations":[{"type":"BadAnswer","range":"(all)","features":[]},{"type":"BadAnswer","range":"117-129","features":[]},{"type":"GoodAnswer","range":"100-107","features":[]}],
	 *   "parentId":0,
	 *   "contributionId":192,
	 *   "discoursePartIds":[10]},
	 *   ...    
	 *   	 */
	public void querytextToCsv(String querytext, File outfile) throws ClientProtocolException, IOException {
		// TODO: Handle annotations gracefully 
		
		
		// Open CSV file
		CSVFormat fmt = CSVFormat.MYSQL;
		FileWriter fileWriter = new FileWriter(outfile);
		CSVPrinter prt = new CSVPrinter(fileWriter, fmt);
		
		// Get column titles from query result (assume first row matches all rows)
		JsonObject jraw = query(querytext).readObject();
		JsonArray data = jraw.getJsonObject("_embedded")
				.getJsonArray("browsingContributionResources");
		JsonObject row1 = data.getJsonObject(0);
		ArrayList<String> colOrder = new ArrayList<String>();
		Set<String> annotations = new HashSet<String>();
		
		// First, collect all the keys of the first row of the table, excluding annotations
		for (String key: row1.keySet()) {
			colOrder.add(key);
		}
		
		// Next, collect together unique annotations
		for (JsonValue rowv: data) {
			JsonObject row = (JsonObject)rowv;
			for (String key: row.keySet()) {
				if (key.equals("annotations")) {
					for(JsonValue itemv: row.getJsonArray(key)) {
						JsonObject item = (JsonObject)itemv;
						annotations.add(( (JsonString) item.get("type")).getString());
					}
					
				}
			}
		}
		
		for (String anno : annotations) {
			colOrder.add("ann." + anno);
		}
		prt.printRecord(colOrder);
		
		// Get data from query result
		for (int d = 0;  d < data.size(); d+=1)
		{
		    	  JsonObject row = data.getJsonObject(d);
		    	  ArrayList<String> newrow = new ArrayList<String>();
		    	  for (String c: colOrder) {
		    		  if (c.startsWith("ann.")) {
		    			  
		    			  /* Annotations might have complex data associated with them.
		    			   * Here we just output a count of how many times this annotation
		    			   * applied to this contribution, ignoring character ranges and
		    			   * features.  That data is available in the "annotations" column,
		    			   * and we can add new columns representing these items if people request
		    			   * it.  But better to keep it simple for now
		    			   */
		    			  int count = 0;
		    			  
		    			  for(JsonValue itemv: row.getJsonArray("annotations")) {
						 JsonObject item = (JsonObject)itemv;
						 String key = ( (JsonString) item.get("type")).getString();
						 if (c.equals("ann." + key)) {
							 count += 1;
						 } 
					  }
		    			  newrow.add(Integer.toString(count));
		    		  } else {
		    			  JsonValue v = row.get(c);
		    			  if (v.getValueType() == javax.json.JsonValue.ValueType.STRING) {
		    				  newrow.add(((JsonString)row.get(c)).getString());
		    			  } else if (v.getValueType() == javax.json.JsonValue.ValueType.NUMBER) {
		    				  if (((JsonNumber)row.get(c)).isIntegral()) {
		    					  newrow.add(Long.toString(((JsonNumber)row.get(c)).longValue()));
		    				  } else {
		    					  newrow.add(Double.toString(((JsonNumber)row.get(c)).doubleValue()));
		    				  }
		    			  } else {
		    				  newrow.add(row.get(c).toString());
		    			  }
		    		  }
		    	  }
		    	  prt.printRecord(newrow);
		}
		
		
		// Close CSV file
		fileWriter.flush();
		fileWriter.close();
		prt.close();
	}
	
	public JsonReader query(String querytext) throws ClientProtocolException, IOException {
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("query", querytext));
        nvps.add(new BasicNameValuePair("length", "10000000")); // Get all rows; this is a dump
        	return callRestApiJson("/browsing/query", nvps);
    }
	
	private JsonReader callRestApiJson(String url, List<NameValuePair> parameters) throws ClientProtocolException, IOException {
		return javax.json.Json.createReader(new StringReader(callRestApi(url, parameters)));
	}
	
	
	private String callRestApi(String url, List<NameValuePair> parameters) throws ClientProtocolException, IOException {
		RequestConfig config = RequestConfig.custom()
		  .setConnectTimeout(CONNECT_TIMEOUT)                         
		  .setConnectionRequestTimeout(GET_DATA_TIMEOUT)     
		  .setSocketTimeout(CONNECT_TIMEOUT).build();       
		client = HttpClientBuilder.create().setSSLContext(sslContext).build();
		String params = "";
		if (parameters != null) {
			params = "?" + URLEncodedUtils.format(parameters, "utf-8");
		}
		HttpGet request = new HttpGet(site + url + params);
		request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

		HttpResponse response = client.execute(request);
		HttpEntity result = response.getEntity();
		String answer = EntityUtils.toString(result);
		
		EntityUtils.consume(result);
		return answer;
	}
	
	// https://stackoverflow.com/questions/8753583/download-a-file-through-an-http-get-in-java
	private void downloadRestApi(String url, List<NameValuePair> parameters, File fn) throws ClientProtocolException, IOException {
		client = HttpClientBuilder.create().setSSLContext(sslContext).build();
		String params = "";
		if (parameters != null) {
			params = "?" + URLEncodedUtils.format(parameters, "utf-8");
		}
		HttpGet request = new HttpGet(site + url + params);
		request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

		HttpResponse response = client.execute(request);
		InputStream result = response.getEntity().getContent();
		FileOutputStream fos = new FileOutputStream(fn);
		byte[] buffer = new byte[4096];
        int length; 
        while((length = result.read(buffer)) > 0) {
        		fos.write(buffer, 0, length);
        }
		fos.close();
	}

	
	
	
	
}
