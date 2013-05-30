package net.johnsonlau.word.db;

import net.johnsonlau.tool.DateTime;
import net.johnsonlau.word.model.Word;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapter {

	private final Context mContext;
	private DbOpenHelper mDbOpenHelper;
	private SQLiteDatabase mDb;

	public DbAdapter(Context content) {
		this.mContext = content;
	}

	public DbAdapter open() throws SQLException {
		mDbOpenHelper = new DbOpenHelper(mContext);
		mDb = mDbOpenHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbOpenHelper.close();
	}

	// == settings ===============================

	public boolean updateSettings(String service, String userId, String userPwd) {
		ContentValues args = new ContentValues();
		args.put(DbOpenHelper.TABLE_SETTINGS_SERVICE, service);
		args.put(DbOpenHelper.TABLE_SETTINGS_USER_ID, userId);
		args.put(DbOpenHelper.TABLE_SETTINGS_USER_PWD, userPwd);

		return mDb.update(DbOpenHelper.TABLE_SETTINGS, args, null, null) > 0;
	}

	public Cursor fetchSettings() throws SQLException {
		Cursor cursor = mDb.query(true, DbOpenHelper.TABLE_SETTINGS,
				new String[] { DbOpenHelper.TABLE_SETTINGS_SERVICE,
						DbOpenHelper.TABLE_SETTINGS_USER_ID,
						DbOpenHelper.TABLE_SETTINGS_USER_PWD }, null, null,
				null, null, null, null);

		return cursor;
	}

	// == words ==================================

	public Cursor fetchAllWords() throws SQLException {
		return mDb.query(true, DbOpenHelper.TABLE_WORDS, new String[] {
				DbOpenHelper.TABLE_WORDS_ROWID, DbOpenHelper.TABLE_WORDS_ID,
				DbOpenHelper.TABLE_WORDS_WORD,
				DbOpenHelper.TABLE_WORDS_PRONUNCIATION,
				DbOpenHelper.TABLE_WORDS_TRANSLATION,
				DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER,
				DbOpenHelper.TABLE_WORDS_LAST_UPDATED }, null, null, null,
				null, DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER + " DESC", null);
	}

	public void purgeWords() {
		mDb.delete(DbOpenHelper.TABLE_WORDS, null, null);
	}

	public long insertWord(String id, String word, String pronunciation,
			String translation, int display_order, String updated) {
		ContentValues values = new ContentValues();
		values.put(DbOpenHelper.TABLE_WORDS_ID, id);
		values.put(DbOpenHelper.TABLE_WORDS_WORD, word);
		values.put(DbOpenHelper.TABLE_WORDS_PRONUNCIATION, pronunciation);
		values.put(DbOpenHelper.TABLE_WORDS_TRANSLATION, translation);
		values.put(DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER, display_order);
		values.put(DbOpenHelper.TABLE_WORDS_LAST_UPDATED, updated);

		return mDb.insert(DbOpenHelper.TABLE_WORDS, null, values);
	}

	public Cursor fetchWord(long rowId) throws SQLException {
		Cursor cursor = mDb.query(true, DbOpenHelper.TABLE_WORDS, new String[] {
				DbOpenHelper.TABLE_WORDS_ROWID, DbOpenHelper.TABLE_WORDS_ID,
				DbOpenHelper.TABLE_WORDS_WORD,
				DbOpenHelper.TABLE_WORDS_PRONUNCIATION,
				DbOpenHelper.TABLE_WORDS_TRANSLATION,
				DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER },
				DbOpenHelper.TABLE_WORDS_ROWID + "=" + rowId, null, null, null,
				null, "1");

		return cursor;
	}

	public Word fetchWordByIndex(int index) throws SQLException {
		Cursor cursor = fetchAllWords();
		int count = cursor.getCount();

		Word result = null;

		if (count > 0) {
			if (index >= count) {
				index = 0;
			} else if (index < 0) {
				index = count - 1;
			}

			int i = -1;
			do {
				cursor.moveToNext();
				i++;
			} while (i < index);

			long rowId = cursor.getLong(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_ROWID));
			String word = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_WORD));
			String pronunciation = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_PRONUNCIATION));
			String translation = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_TRANSLATION));
			cursor.close();

			result = new Word(rowId, index, word, pronunciation, translation);
		}

		return result;
	}

	public int getWordCount() throws SQLException {
		Cursor cursor = fetchAllWords();
		return cursor.getCount();
	}

	public boolean deleteWord(long rowId) {
		return mDb.delete(DbOpenHelper.TABLE_WORDS,
				DbOpenHelper.TABLE_WORDS_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateWordDisplayOrder(long rowId, int displayOrder) {
		ContentValues args = new ContentValues();
		args.put(DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER, displayOrder);
		args.put(DbOpenHelper.TABLE_WORDS_LAST_UPDATED,
				DateTime.getUtcDateTimeString());

		return mDb.update(DbOpenHelper.TABLE_WORDS, args,
				DbOpenHelper.TABLE_WORDS_ROWID + "=" + rowId, null) > 0;
	}

	// == deleted_words ====================

	public Cursor fetchAllDeletedWords() throws SQLException {
		return mDb.query(true, DbOpenHelper.TABLE_DELETED_WORDS,
				new String[] { DbOpenHelper.TABLE_DELETED_WORDS_ID }, null,
				null, null, null, null, null);
	}

	public void purgeDeletedWords() {
		mDb.delete(DbOpenHelper.TABLE_DELETED_WORDS, null, null);
	}

	public long insertDeletedWord(long rowId) {
		Cursor wordCursor = fetchWord(rowId);

		wordCursor.moveToFirst();
		String id = wordCursor.getString(wordCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_ID));
		wordCursor.close();

		ContentValues values = new ContentValues();
		values.put(DbOpenHelper.TABLE_DELETED_WORDS_ID, id);

		return mDb.insert(DbOpenHelper.TABLE_DELETED_WORDS, null, values);
	}
}
