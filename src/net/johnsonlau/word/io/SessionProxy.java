package net.johnsonlau.word.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.johnsonlau.tool.HttpRequest;
import net.johnsonlau.word.Config;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class SessionProxy {

	public static String createSession(String serviceUrl, String userId,
			String userPwd) throws IOException, JSONException {

		String result = "";

		String url = serviceUrl + Config.URL_SESSION_CREATE;

		Log.i(Config.LOG_TAG, "createSession request: " + url);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("id", userId));
		nameValuePairs.add(new BasicNameValuePair("pwd", userPwd));

		String response = HttpRequest.doPost(url, nameValuePairs, null);

		Log.i(Config.LOG_TAG, "createSession return: " + response);

		JSONObject obj = new JSONObject(response);
		if (obj.getBoolean("success")) {
			result = obj.getString("data");
		}

		return result;
	}
}
