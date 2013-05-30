package net.johnsonlau.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

public class HttpRequest 
{
	public static String doPost(String url, List<NameValuePair> params, String cookie) 
	{
		String result = "";
		
		try
		{
			if(url.startsWith("https"))
			{
				result = HttpRequest.doHttpsRequest(url, params, cookie, "POST");
			}
			else
			{
				result = HttpRequest.doHttpPost(url, params, cookie);			
			}
		}
		catch (Exception e)
		{			
		}
		
		return result;		
	}

	public static String doGet(String url, String cookie) 
	{
		String result = "";
		
		try
		{
			if(url.startsWith("https"))
			{
				result = HttpRequest.doHttpsRequest(url, null, cookie, "GET");
			}
			else
			{
				result = HttpRequest.doHttpGet(url, cookie);			
			}
		}
		catch (Exception e)
		{			
		}
		
		return result;
	}
	
	//== helpers ================================================================

    private static X509TrustManager x509TrustManager = new X509TrustManager()
    {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException 
		{
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException 
		{	
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() 
		{
			return null;
		} 
    }; 
    
	private static String doHttpGet(String url, String cookie) throws IOException 
	{
		String result = "";

		HttpGet httpGet = new HttpGet(url);
		if (!Utilities.isEmptyOrNull(cookie)) 
		{
			httpGet.setHeader("Cookie", cookie);
		}

		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 60000);
		HttpConnectionParams.setSoTimeout(httpParams, 300000);

		HttpClient httpClient = new DefaultHttpClient(httpParams);
		HttpResponse response = httpClient.execute(httpGet);

		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) 
		{
			InputStream inputStream = response.getEntity().getContent();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
			BufferedReader buffer = new BufferedReader(inputStreamReader);

			String inputLine = null;
			while ((inputLine = buffer.readLine()) != null) 
			{
				result += inputLine + "\n";
			}
			
			inputStreamReader.close();
		}

		return result;
	}

	private static String doHttpPost(String url, List<NameValuePair> params, String cookie) throws IOException 
	{
		String result = "";

		AbstractHttpEntity formEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
		formEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		formEntity.setContentEncoding("UTF-8");

		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(formEntity);

		if (!Utilities.isEmptyOrNull(cookie)) 
		{
			httpPost.setHeader("Cookie", cookie);
		}

		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 60000);
		HttpConnectionParams.setSoTimeout(httpParams, 300000);

		HttpClient httpClient = new DefaultHttpClient(httpParams);
		HttpResponse response = httpClient.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) 
		{
			InputStream inputStream = response.getEntity().getContent();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
			BufferedReader buffer = new BufferedReader(inputStreamReader);

			String inputLine = null;
			while ((inputLine = buffer.readLine()) != null)
			{
				result += inputLine + "\n";
			}
			
			inputStreamReader.close();
		}

		return result;
	}
	
	private static String doHttpsRequest(String path, List<NameValuePair> params, String cookie, String RequestMethod) throws IOException, NoSuchAlgorithmException, KeyManagementException 
	{
		String result = "";	
		
		StringBuilder entityBuilder = new StringBuilder("");
        if(params !=null && !params.isEmpty())
        {
            for(NameValuePair entry : params)
            {
                entityBuilder.append(entry.getName()).append('=');
                entityBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                entityBuilder.append('&');
            }

            entityBuilder.deleteCharAt(entityBuilder.length() - 1);
        }
        
        byte[] entity = entityBuilder.toString().getBytes();

        URL url = new URL(path);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        // Trust all certificates
        if (conn instanceof HttpsURLConnection) 
        {   
             SSLContext context = SSLContext.getInstance("TLS");   
             context.init(new KeyManager[0], new TrustManager[]{HttpRequest.x509TrustManager}, new SecureRandom()); 

             ((HttpsURLConnection) conn).setSSLSocketFactory(context.getSocketFactory());   
             ((HttpsURLConnection) conn).setHostnameVerifier(new AllowAllHostnameVerifier());   
        }

        conn.setConnectTimeout(60000);
        conn.setRequestMethod(RequestMethod);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(entity.length));
        conn.setRequestProperty("Cookie", cookie);
        
        if(RequestMethod == "POST")
        {
	        OutputStream outStream = conn.getOutputStream();
	        outStream.write(entity);
	        outStream.flush();
	        outStream.close();
        }
		
        InputStream inputStream = conn.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
		BufferedReader buffer = new BufferedReader(inputStreamReader);

		String inputLine = null;
		while ((inputLine = buffer.readLine()) != null)
		{
			result += inputLine + "\n";
		}
		
		inputStreamReader.close();
				
		return result;
	}
}
