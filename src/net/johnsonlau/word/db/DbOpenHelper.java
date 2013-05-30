package net.johnsonlau.word.db;

import net.johnsonlau.word.Config;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbOpenHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "data";
	private static final int DATABASE_VERSION = 1;

	// == table settings =============================================
	public static final String TABLE_SETTINGS = "settings";
	public static final String TABLE_SETTINGS_ROWID = "_id";
	public static final String TABLE_SETTINGS_SERVICE = "service";
	public static final String TABLE_SETTINGS_USER_ID = "user_id";
	public static final String TABLE_SETTINGS_USER_PWD = "user_pwd";
	
	private static final String TABLE_SETTINGS_CREATE = "CREATE TABLE "
			+ TABLE_SETTINGS 
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+ ", service TEXT NOT NULL" 
			+ ", user_id TEXT NOT NULL"
			+ ", user_pwd TEXT NOT NULL);";
	
	private static final String TABLE_SETTINGS_INITIALIZE = "INSERT INTO settings "
			+ "(service, user_id, user_pwd)"
			+ " VALUES('https://tools.johnson.uicp.net', '', '');";

	
	// == table words =================================================
	public static final String TABLE_WORDS = "words";
	public static final String TABLE_WORDS_ROWID = "_id";
	public static final String TABLE_WORDS_ID = "id";
	public static final String TABLE_WORDS_WORD = "word";
	public static final String TABLE_WORDS_PRONUNCIATION = "pronunciation";
	public static final String TABLE_WORDS_TRANSLATION = "translation";
	public static final String TABLE_WORDS_DISPLAY_ORDER = "display_order";
	public static final String TABLE_WORDS_LAST_UPDATED = "last_updated";
	
	private static final String TABLE_WORDS_CREATE = "CREATE TABLE "
			+ TABLE_WORDS 
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+ ", id TEXT NOT NULL" 
			+ ", word TEXT NOT NULL"
			+ ", pronunciation TEXT NOT NULL"
			+ ", translation TEXT NOT NULL"
			+ ", display_order INTEGER NOT NULL"
			+ ", last_updated DATETIME NOT NULL);";

	
	// == table deleted_words =========================================
	public static final String TABLE_DELETED_WORDS = "deleted_words";
	public static final String TABLE_DELETED_WORDS_ROWID = "_id";
	public static final String TABLE_DELETED_WORDS_ID = "id";
	
	private static final String TABLE_DELETED_WORDS_CREATE = "CREATE TABLE "
			+ TABLE_DELETED_WORDS 
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+ ", id TEXT NOT NULL);";

	
	// == ovreride methods =========================================

	DbOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_SETTINGS_CREATE);
		db.execSQL(TABLE_SETTINGS_INITIALIZE);
		db.execSQL(TABLE_WORDS_CREATE);
		db.execSQL(TABLE_DELETED_WORDS_CREATE);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
			upgradeToVersion2();
		}
		if (oldVersion == 2 && newVersion == 3) {
			upgradeToVersion3();
		}

		Log.i(Config.LOG_TAG, "Upgraded database " + DATABASE_NAME + " from version "
				+ oldVersion + " to " + newVersion);
	}

	private void upgradeToVersion2() {
		// do upgrading job
	}

	private void upgradeToVersion3() {
		// do upgrading job
	}
}
