package net.johnsonlau.word.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.johnsonlau.tool.HttpRequest;
import net.johnsonlau.tool.Utilities;
import net.johnsonlau.word.Config;
import net.johnsonlau.word.db.DbAdapter;
import net.johnsonlau.word.db.DbOpenHelper;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.util.Log;

public class WordProxy {

	public static JSONArray getAllWords(String serviceUrl, String sessionId)
			throws IOException, JSONException {

		JSONArray result = null;

		String url = serviceUrl + Config.URL_WORD_LIST;
		String cookie = Config.SESSION_COOKIE_NAME + "=" + sessionId;
		String response = HttpRequest.doGet(url, cookie);

		Log.i(Config.LOG_TAG, "getAllWords return: " + response);

		JSONObject obj = new JSONObject(response);
		if (obj.getBoolean("success")) {
			result = obj.getJSONArray("data");
		}

		return result;
	}

	public static boolean updateWordsPosition(String serviceUrl,
			String sessionId, DbAdapter dbAdapter) throws IOException,
			JSONException {

		boolean result = false;

		// -- prepare data ----------------------------------------------
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		Cursor wordsCursor = dbAdapter.fetchAllWords();
		while (wordsCursor.moveToNext()) {
			String id = wordsCursor.getString(wordsCursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_ID));
			String displayOrder = wordsCursor
					.getString(wordsCursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER));

			String lastUpdated = wordsCursor
					.getString(wordsCursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_LAST_UPDATED));

			nameValuePairs.add(new BasicNameValuePair(id, displayOrder + ","
					+ lastUpdated));
		}
		wordsCursor.close();

		// -- start update --------------------------------------------
		if (nameValuePairs.size() > 0) // in case nothing to update
		{
			String url = serviceUrl + Config.URL_WORD_UPDATE_WORDS_POSITION;
			String cookie = Config.SESSION_COOKIE_NAME + "=" + sessionId;
			String response = HttpRequest.doPost(url, nameValuePairs, cookie);

			Log.i(Config.LOG_TAG, "updateWordsPosition return: " + response);

			JSONObject obj = new JSONObject(response);
			result = obj.getBoolean("success");
		} else {
			result = true;
		}

		return result;
	}

	public static boolean syncLocalDeletedWordsToRemote(String serviceUrl,
			String sessionId, DbAdapter dbAdapter) throws IOException,
			JSONException {

		boolean result = false;

		// -- get deleted ids ----------------------------------------------
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		String ids = "";
		Cursor wordsCursor = dbAdapter.fetchAllDeletedWords();
		while (wordsCursor.moveToNext()) {
			String id = wordsCursor
					.getString(wordsCursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_DELETED_WORDS_ID));

			if (Utilities.isEmptyOrNull(ids)) {
				ids = id;
			} else {
				ids += "," + id;
			}
		}
		wordsCursor.close();
		nameValuePairs.add(new BasicNameValuePair("ids", ids));

		Log.i(Config.LOG_TAG, "deleting: " + ids);

		// -- start synchronizing --------------------------------------------
		if (!Utilities.isEmptyOrNull(ids)) // in case no words to delete
		{
			String url = serviceUrl + Config.URL_WORD_DELETE_WORDS;
			String cookie = Config.SESSION_COOKIE_NAME + "=" + sessionId;
			String response = HttpRequest.doPost(url, nameValuePairs, cookie);

			Log.i(Config.LOG_TAG, "syncLocalDeletedWordsToRemote return: "
					+ response);

			JSONObject obj = new JSONObject(response);
			result = obj.getBoolean("success");
		} else {
			result = true;
		}

		return result;
	}
}
