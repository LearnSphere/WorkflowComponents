package edu.cmu.pslc.learnsphere.analysis.DSSPP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class DssppUtil {

	//Creating query
	
	public double getSimilarity(String[] text1, String[] text2, String SS ,String domain) throws Exception {
		String payload = null;
		//String url = "http://dsspp.skoonline.org/comparetext?";
		String url = "//dsspp.skoonline.org/comparetext?";

		payload = "json={";
		payload += "\"type\":1,";
		//payload += "\"minWeight\":" + minWeight +",";
		payload += "\"text1\":\"" + generateString(text1).trim() + "\",";
		payload += "\"text2\":\"" + generateString(text2).trim() + "\",";
		//payload += "\"minStrength\":" + minStrength +",";
		//payload += "\"wc\":0,";
		//payload += "\"etop\":10,";
		//payload += "\"category\":\"" + category + "\",";
		//payload += "\"ttop\":50,";
		payload += "\"SS\":\"" + SS + "\",";
		//payload += "\"userGuid\":\"3ae566a4-3208-4715-bd48-dd73808fe350\",";
		//payload += "\"minRankby\":" + minRankby +",";
		//payload += "\"include_ttop\":true,";
		payload += "\"guid\":\"3ae566a4-3208-4715-bd48-dd73808fe350\",";
		//payload += "\"include_etop\":true,";
		//payload += "\"format\":\"json\",";
		payload += "\"domain\":\"" + domain + "\"}";

		String urlTotal = url + payload;
		return HttpGetRes(urlTotal);

}

	// HTTP GET request
	private double HttpGetRes(String url1) throws Exception {
		final String USER_AGENT = "Mozilla/5.0";
		
		URI obj_r = new URI("http",url1, null);
		
		//URI obj_r = new URI("http","search.barnesandnoble.com",url1, null);
		
		URL obj = obj_r.toURL();
	
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
		
		if (con.getResponseCode() == 200) {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			//BufferedReader reader = new BufferedReader(new InputStreamReader(((HttpURLConnection) (new URL(urlString)).openConnection()).getInputStream(), Charset.forName("UTF-8")));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// String json = EntityUtils.toString(response, "UTF-8");
			JSONParser parser = new JSONParser();
			Object resultObject = parser.parse(response.toString());
			JSONObject objJson = (JSONObject) resultObject;
			
			try {
				double score= Double.parseDouble(String.valueOf(objJson.get("score")));
				//System.out.println(score);
				return score;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				return -1.0;
			}
        } else {
        	return -1.0;
        }



	}
	
	public String generateString (String[] arr)
	{
		StringBuilder builder = new StringBuilder();
		for(String s : arr) {
		    builder.append(s+' ');
		}
		return builder.toString();
	} 

}