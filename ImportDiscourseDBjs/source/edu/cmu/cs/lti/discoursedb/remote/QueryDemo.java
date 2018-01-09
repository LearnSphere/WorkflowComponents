package edu.cmu.cs.lti.discoursedb.remote;

import java.io.File;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;

// https://stackoverflow.com/questions/21223084/how-do-i-use-an-ssl-client-certificate-with-apache-httpclient
// https://stackoverflow.com/questions/22296312/convert-certificate-from-pem-into-jks
//
// Converted cid.pem to pk12 format with:
// openssl pkcs12 -export -in cid.pem -out cert.p12
// using password
// gadolinium
public class QueryDemo {
	static String user = "cbogartdenver@gmail.com";
	static String site = "https://erebor.lti.cs.cmu.edu:5398";
	
	private static final String KEYSTOREPATH = "/cert.p12"; // or .p12
    private static final String KEYSTOREPASS = "gadolinium";
    private static final String KEYPASS = "gadolinium";

   
    
	public static void main(String[] args) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, Exception  {
		QueryProxy p = new QueryProxy("cbogartdenver@gmail.com","");
		System.out.println("Browser URL is " + p.directBrowserUrl());
		System.out.println("Databases!");
		System.out.println(p.databases());
		System.out.println("Queries!");
		List<SavedQuery> lsq = p.getDiscoursedbQueries("openfl");
		System.out.println(lsq);
		System.out.println("Data!");
		String myquery = "{\"database\":\"openfl\",\"rows\":{\"discourse_part\":[{\"dpid\":\"9\",\"name\":\"openfl/actuate\"}]}}";
		assert lsq.get(0).querytext.equals(myquery) : "Did not retrieve expected query string";
		File f =  new File("xyzpdq.csv");
		f.delete();
		p.queryToCsv(lsq.get(0),f);
		assert f.exists() : "queryToCsv did not write to a file";
		assert f.length() > 2000 : "queryToCsv Returned too-short query results";
		System.out.println("Done");
	}
	/*
	public QueryDemo() {
	
	}
	
	void connect() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, Exception {
		SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(readStore(),KEYPASS.toCharArray()) // use null as second param if you don't have a separate key password
                .build();
		
        //HttpHost target = new HttpHost("https://erebor.lti.cs.cmu.edu",5398,"https");
		
        CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials basic = new UsernamePasswordCredentials(user, "");
		provider.setCredentials(AuthScope.ANY, basic);
		//AuthCache authCache = new BasicAuthCache();
		//authCache.put(target, new BasicScheme());

		//HttpClientContext context = HttpClientContext.create();
		//context.setCredentialsProvider(provider);
		//context.setAuthCache(authCache);
		
		HttpClient client = HttpClientBuilder.create().setSSLContext(sslContext).build();
		String propListUrl = site + "/browsing/prop_list";
		HttpGet request = new HttpGet(propListUrl);
		
		String auth = user + ":";
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
		String authHeader = "Basic " + new String(encodedAuth);
		request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
		//List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        //nvps.add(new BasicNameValuePair("query", "ftf"));

        //request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        
		HttpResponse response = client.execute(request);
		HttpEntity result = response.getEntity();
		
		System.out.println(EntityUtils.toString(result));
		EntityUtils.consume(result);;
	}
	
	void connect2() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
		System.out.println(this.getClass().getResource(KEYSTOREPATH));
		SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(
				this.getClass().getResource(KEYSTOREPATH),
                KEYSTOREPASS.toCharArray(), KEYPASS.toCharArray(),
                (aliases, socket) -> aliases.keySet().iterator().next()
        ).build();
		
		CloseableHttpClient httpclient = HttpClients
		        .custom().setSSLContext(sslContext).build();
		
		CloseableHttpResponse closeableHttpResponse = httpclient.execute(
		        new HttpGet("https://erebor.lti.cs.cmu.edu:5398/browsing/prop_list"));
		
		
		
		System.out.println(closeableHttpResponse.getStatusLine());
        HttpEntity entity = closeableHttpResponse.getEntity();
        try (InputStream content = entity.getContent();
	            ReadableByteChannel src = Channels.newChannel(content);
	            WritableByteChannel dest = Channels.newChannel(System.out)) {
	            ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
	            while (src.read(buffer) != -1) {
	                buffer.flip();
	                dest.write(buffer);
	                buffer.compact();
	            }
	            buffer.flip();
	            while (buffer.hasRemaining())
	                dest.write(buffer);
        		}
	}
	*/
}
