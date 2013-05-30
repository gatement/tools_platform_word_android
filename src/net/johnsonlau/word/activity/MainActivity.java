package net.johnsonlau.word.activity;

import java.util.ArrayList;
import java.util.Locale;

import net.johnsonlau.tool.CmdMessage;
import net.johnsonlau.tool.MyListItem;
import net.johnsonlau.tool.TouchListView;
import net.johnsonlau.word.Config;
import net.johnsonlau.word.R;
import net.johnsonlau.word.db.DbAdapter;
import net.johnsonlau.word.db.DbOpenHelper;
import net.johnsonlau.word.io.SessionProxy;
import net.johnsonlau.word.io.WordProxy;
import net.johnsonlau.word.model.Word;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MainActivity extends ListActivity implements OnTouchListener,
		OnInitListener {
	private static final int MENU_ID_SETTINGS = Menu.FIRST;
	private static final int MENU_ID_ABOUT = Menu.FIRST + 1;
	private static final int MENU_ID_REVERT = Menu.FIRST + 2;
	private static final int MENU_ID_SYNC = Menu.FIRST + 3;
	private static final int MENU_ID_DELETE = Menu.FIRST + 10;

	private TextView mMsgTextView;
	private TextView mWordCountTextView;
	private ViewFlipper mViewFlipper;
	private ViewFlipper mSlideFlipper;
	private Button mBottomButton;

	private DbAdapter mDbAdapter;
	private Handler mMainHandler;

	private float mDownXValue;
	private float mDownYValue;

	private long mCurrentWordRowId = 0;
	private int mCurrentWordIndex = 0;
	private String mCurrentWordText;

	private String mServiceUrl;
	private String mUserId;
	private String mUserPwd;

	private TextView mWordTextView0;
	private TextView mPronunciationTextView0;
	private TextView mTranslationTextView0;
	private ScrollView mScrollView0;
	private TextView mWordTextView1;
	private TextView mPronunciationTextView1;
	private TextView mTranslationTextView1;
	private ScrollView mScrollView1;
	private Button mWordVisibilityButton;
	private Button mTranslationVisibilityButton;
	private Button mSlideDeleteButton;

	private MyArrayAdapter mMyArrayAdapter = null;
	private ArrayList<MyListItem> mDataArray = null;
	private TouchListView mTouchListView = null;

	private static final int REQ_TTS_STATUS_CHECK = 0;
	private boolean mTtsIsReady = false;
	private TextToSpeech mTts;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		checkTts();

		initMembers();
		bindEvents();
		populateData();
	}

	// == initialization methods
	// ===============================================================

	private void checkTts() {
		// ensure TTS is ready
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
	}

	private void initMembers() {
		mDbAdapter = new DbAdapter(this).open();

		mTouchListView = (TouchListView) getListView();

		mMsgTextView = (TextView) findViewById(R.id.main_msg);
		mWordCountTextView = (TextView) findViewById(R.id.main_word_count);
		mViewFlipper = (ViewFlipper) findViewById(R.id.main_flipper);
		mSlideFlipper = (ViewFlipper) findViewById(R.id.main_slide_flipper);
		mBottomButton = (Button) findViewById(R.id.main_bottom_btn);

		mWordTextView0 = (TextView) findViewById(R.id.main_slide_word0);
		mPronunciationTextView0 = (TextView) findViewById(R.id.main_slide_pronunciation0);
		mTranslationTextView0 = (TextView) findViewById(R.id.main_slide_translation0);
		mScrollView0 = (ScrollView) findViewById(R.id.main_slide_scroll0);

		mWordTextView1 = (TextView) findViewById(R.id.main_slide_word1);
		mPronunciationTextView1 = (TextView) findViewById(R.id.main_slide_pronunciation1);
		mTranslationTextView1 = (TextView) findViewById(R.id.main_slide_translation1);
		mScrollView1 = (ScrollView) findViewById(R.id.main_slide_scroll1);

		mWordVisibilityButton = (Button) findViewById(R.id.main_slide_hide_word);
		mTranslationVisibilityButton = (Button) findViewById(R.id.main_slide_hide_translation);
		mSlideDeleteButton = (Button) findViewById(R.id.main_slide_delete_word);

		mMainHandler = new Handler() {
			public void handleMessage(Message msg) {
				CmdMessage message = (CmdMessage) msg.obj;

				if (message.getCmd() == "PopulateData") {
					populateData();
				} else if (message.getCmd() == "Message") {
					mMsgTextView.setText(message.getValue());
				}
			}
		};
	}

	private void bindEvents() {
		registerForContextMenu(getListView());

		mTouchListView.setDropListener(onDrop);
		mTouchListView.setRemoveListener(onRemove);

		mScrollView0.setOnTouchListener((OnTouchListener) this);
		mScrollView1.setOnTouchListener((OnTouchListener) this);

		mBottomButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Button btn = (Button) view;
				if (btn.getText().equals("LIST")) {
					btn.setText("SLIDE");
					mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(
							MainActivity.this, R.anim.push_right_in));
				} else {
					btn.setText("LIST");
					mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(
							MainActivity.this, R.anim.push_left_in));
				}
				mViewFlipper.showNext();
			}
		});

		mWordVisibilityButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (mWordTextView0.getVisibility() == View.VISIBLE) {
					mWordTextView0.setVisibility(View.INVISIBLE);
					mWordTextView1.setVisibility(View.INVISIBLE);
					mPronunciationTextView0.setVisibility(View.INVISIBLE);
					mPronunciationTextView1.setVisibility(View.INVISIBLE);

					if (mTranslationTextView0.getVisibility() == View.INVISIBLE) {
						mTranslationTextView0.setVisibility(View.VISIBLE);
						mTranslationTextView1.setVisibility(View.VISIBLE);
					}
				} else {
					mWordTextView0.setVisibility(View.VISIBLE);
					mWordTextView1.setVisibility(View.VISIBLE);
					mPronunciationTextView0.setVisibility(View.VISIBLE);
					mPronunciationTextView1.setVisibility(View.VISIBLE);
				}
			}
		});

		mTranslationVisibilityButton
				.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						if (mTranslationTextView0.getVisibility() == View.VISIBLE) {
							mTranslationTextView0.setVisibility(View.INVISIBLE);
							mTranslationTextView1.setVisibility(View.INVISIBLE);
							if (mWordTextView0.getVisibility() == View.INVISIBLE) {
								mWordTextView0.setVisibility(View.VISIBLE);
								mWordTextView1.setVisibility(View.VISIBLE);
								mPronunciationTextView0
										.setVisibility(View.VISIBLE);
								mPronunciationTextView1
										.setVisibility(View.VISIBLE);
							}
						} else {
							mTranslationTextView0.setVisibility(View.VISIBLE);
							mTranslationTextView1.setVisibility(View.VISIBLE);
						}
					}
				});

		mSlideDeleteButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				new AlertDialog.Builder(MainActivity.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setMessage(
								"Are you sure you want to delete this word?")
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										deleteWord(mCurrentWordRowId,
												mCurrentWordText);
									}

								}).setNegativeButton("No", null).show();
			}
		});

	}

	public void populateData() {
		Cursor cursor = mDbAdapter.fetchAllWords();
		startManagingCursor(cursor);

		mDataArray = new ArrayList<MyListItem>();
		while (cursor.moveToNext()) {
			int rowId = cursor.getInt(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_ROWID));
			String text = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_WORD));
			int displayOrder = cursor
					.getInt(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER));
			MyListItem item = new MyListItem(rowId, text, displayOrder);

			mDataArray.add(item);
		}

		mMyArrayAdapter = new MyArrayAdapter();
		setListAdapter(mMyArrayAdapter);

		loadWord();
	}

	// == override methods
	// =====================================================================

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ID_SYNC, 0, R.string.main_menu_sync);
		menu.add(0, MENU_ID_REVERT, 1, R.string.main_menu_revert);
		menu.add(0, MENU_ID_SETTINGS, 2, R.string.main_menu_settings);
		menu.add(0, MENU_ID_ABOUT, 3, R.string.main_menu_about);

		return true;
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, MENU_ID_DELETE, 0, R.string.main_list_menu_delete);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		mMsgTextView.setText("");

		switch (item.getItemId()) {

		case MENU_ID_SETTINGS:
			goToSettingsActivity();
			return true;

		case MENU_ID_ABOUT:
			goToAboutActivity();
			return true;

		case MENU_ID_REVERT:
			new RevertChangesThread().start();
			return true;

		case MENU_ID_SYNC:
			new SyncThread().start();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public boolean onContextItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case MENU_ID_DELETE:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			if (info.targetView != null) {
				TextView textView = (TextView) info.targetView
						.findViewById(R.id.main_list_item);
				if (textView != null) {
					String word = textView.getText().toString();
					long rowId = Long.parseLong(String.valueOf(textView
							.getTag().toString()));

					deleteWord(rowId, word);
				}
			}
			return true;
		}

		return super.onContextItemSelected(item);
	}

	public boolean onTouch(View arg0, MotionEvent arg1) {
		boolean result = false;

		switch (arg1.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			mDownXValue = arg1.getX();
			mDownYValue = arg1.getY();
			break;
		}

		case MotionEvent.ACTION_UP: {
			float currentX = arg1.getX();
			float currentY = arg1.getY();

			if (Math.abs(mDownXValue - currentX) > 70) {
				if (mDownXValue < currentX) {

					mCurrentWordIndex--;
					loadWord();

					mSlideFlipper.setInAnimation(AnimationUtils.loadAnimation(
							this, R.anim.push_left_in));
					mSlideFlipper.showPrevious();
				} else {
					mCurrentWordIndex++;
					loadWord();

					mSlideFlipper.setInAnimation(AnimationUtils.loadAnimation(
							this, R.anim.push_right_in));
					mSlideFlipper.showNext();
				}

				result = true;
			} else if (Math.abs(mDownXValue - currentX) < 1
					&& Math.abs(mDownYValue - currentY) < 1) {
				speak(mCurrentWordText);

				result = true;
			}
			break;
		}
		}

		return result;
	}

	public void onInit(int status) {
		// TTS Engine is initialized
		if (status == TextToSpeech.SUCCESS) {
			// set voice language
			int result = mTts.setLanguage(Locale.US);

			// if it bad voice data
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				mTtsIsReady = false;
				Log.i(Config.LOG_TAG, "TTS Language is not available.");
			} else {
				mTtsIsReady = true;
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_TTS_STATUS_CHECK) {
			switch (resultCode) {
			// TTS Engine is available
			case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS: {
				mTts = new TextToSpeech(this, this);
			}
				break;

			// miss or bad voice data
			case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME: {
				// install voice data
				Intent dataIntent = new Intent();
				dataIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(dataIntent);
			}
				break;

			// fail to check
			case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
			default:
				Log.i(Config.LOG_TAG, "TTS is not available.");
				break;
			}
		}
	}

	protected void onPause() {
		super.onPause();

		// stop TTS is paused
		if (mTts != null) {
			mTts.stop();
		}
	}

	protected void onDestroy() {
		super.onDestroy();

		mDbAdapter.close();

		// release TTS
		if (mTts != null) {
			mTts.shutdown();
		}
	}

	// == sync
	// =================================================================================

	private void sync(boolean isReverting) {
		try {
			boolean success = true;

			getSettings();

			// -- create session
			// ----------------------------------------------------------
			sendMessage(new CmdMessage("Message", "Start creating session..."));

			String sessionId = "";
			try {
				sessionId = SessionProxy.createSession(mServiceUrl, mUserId,
						mUserPwd);
			} catch (Exception ex) {
				Log.i(Config.LOG_TAG,
						"createSession exception: " + ex.getMessage());
				sendMessage(new CmdMessage("Message", "Authorization error."));
				success = false;
			}
			if (sessionId == "") {
				sendMessage(new CmdMessage("Message", "Authorization error."));
				success = false;
			}

			Log.i(Config.LOG_TAG, "sessionId: " + sessionId);

			// -- sync deleted words to remote
			// -------------------------------------------------------------
			if (success && !isReverting) {
				sendMessage(new CmdMessage("Message",
						"Start synchronizing local deleted words to remote..."));
				success = syncLocalDeletedWordsToRemote(sessionId);

			}
			if (success) // purge local records
			{
				Log.i(Config.LOG_TAG, "success synchronizing deleted words to remote!");
				mDbAdapter.purgeDeletedWords();
			}

			// -- upload remote
			// words--------------------------------------------------
			if (success && !isReverting) {
				sendMessage(new CmdMessage("Message",
						"Start updating remote word position from local..."));
				success = updateRemoteWordsPosition(sessionId);

			}
			if (success) // purge local records
			{
				Log.i(Config.LOG_TAG, "success uploading remote words!");
				sendMessage(new CmdMessage("Message",
						"Start purging local records..."));
				mDbAdapter.purgeWords();
			}

			// -- download words
			// --------------------------------------------------------
			if (success) {
				sendMessage(new CmdMessage("Message",
						"Start downloading remote words..."));
				success = downloadAllWords(sessionId);

				Log.i(Config.LOG_TAG, "success downloading words!");
			}

			// -- finish
			// ------------------------------------------------------------
			if (success) {
				sendMessage(new CmdMessage("Message", "Sync succeeded."));
			}

		} catch (Exception ex) {
			Log.i(Config.LOG_TAG, "sync exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Sync error."));
		}
	}

	private boolean syncLocalDeletedWordsToRemote(String sessionId) {
		boolean result = true;

		try {
			result = WordProxy.syncLocalDeletedWordsToRemote(mServiceUrl,
					sessionId, mDbAdapter);
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG, "syncLocalDeletedWordsToRemote exception: "
					+ ex.getMessage());
			sendMessage(new CmdMessage("Message",
					"sync local deleted words to remote error."));
			result = false;
		}

		return result;
	}

	private boolean updateRemoteWordsPosition(String sessionId) {
		boolean result = true;

		try {
			result = WordProxy.updateWordsPosition(mServiceUrl, sessionId,
					mDbAdapter);
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG,
					"updateRemoteWords exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Update remote words error."));
			result = false;
		}

		return result;
	}

	private boolean downloadAllWords(String sessionId) {
		// -- get words -----------------------------------------------
		JSONArray words = null;
		try {
			words = WordProxy.getAllWords(mServiceUrl, sessionId);
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG,
					"downloadAllWords exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Download words error."));
			return false;
		}
		if (words == null) {
			sendMessage(new CmdMessage("Message", "Download words error."));
			return false;
		}

		// -- save words to local DB ----------------------------------
		try {
			for (int i = 0; i < words.length(); i++) {
				JSONObject word = words.getJSONObject(i);
				mDbAdapter.insertWord(
						word.getString(DbOpenHelper.TABLE_WORDS_ID),
						word.getString(DbOpenHelper.TABLE_WORDS_WORD),
						word.getString(DbOpenHelper.TABLE_WORDS_PRONUNCIATION),
						word.getString(DbOpenHelper.TABLE_WORDS_TRANSLATION),
						word.getInt(DbOpenHelper.TABLE_WORDS_DISPLAY_ORDER),
						word.getString(DbOpenHelper.TABLE_WORDS_LAST_UPDATED));
			}
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG,
					"insertWordToDB exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Insert words error."));
			return false;
		}

		return true;
	}

	// == new threads
	// ==========================================================================

	private class SyncThread extends Thread {
		public void run() {
			sync(false);
			sendMessage(new CmdMessage("PopulateData", ""));
		}
	}

	private class RevertChangesThread extends Thread {
		public void run() {
			sync(true);
			sendMessage(new CmdMessage("PopulateData", ""));
		}
	}

	// == listview event handler
	// ====================================================================

	private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		public void drop(int from, int to) {
			if (from != to) {
				MyListItem fromItem = mMyArrayAdapter.getItem(from);
				mMyArrayAdapter.remove(fromItem);
				mMyArrayAdapter.insert(fromItem, to);

				int fromIndex = to > from ? from : to;
				int toIndex = to > from ? to : from;

				int order = 0;
				if (fromIndex == 0) {
					order = mMyArrayAdapter.getItem(fromIndex)
							.getDisplayOrder();
				} else {
					order = mMyArrayAdapter.getItem(fromIndex - 1)
							.getDisplayOrder() - 1;
				}

				for (int i = fromIndex; i <= toIndex; i++) {
					MyListItem item = mMyArrayAdapter.getItem(i);
					item.setDisplayOrder(i);
					mDbAdapter.updateWordDisplayOrder(item.getRowId(), order);
					order--;
				}
			}
		}
	};

	private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		public void remove(int which) {
			View v = mTouchListView.getChildAt(which
					- mTouchListView.getFirstVisiblePosition());
			v.showContextMenu();
		}
	};

	protected void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		mCurrentWordIndex = position;
		showWord();
	}

	// == helpers
	// =============================================================================

	private void speak(String msg) {
		if (mTtsIsReady) {
			mTts.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	private void goToAboutActivity() {
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}

	private void goToSettingsActivity() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void deleteWord(long rowId, String word) {
		String text = word + " is deleted";
		mDbAdapter.insertDeletedWord(rowId);
		mDbAdapter.deleteWord(rowId);

		populateData();

		Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
	}

	private void sendMessage(CmdMessage msg) {
		Message message = mMainHandler.obtainMessage();
		message.obj = msg;
		mMainHandler.sendMessage(message);
	}

	private void getSettings() {
		Cursor settingsCursor = mDbAdapter.fetchSettings();
		settingsCursor.moveToFirst();
		startManagingCursor(settingsCursor);
		mServiceUrl = settingsCursor.getString(settingsCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_SERVICE));
		mUserId = settingsCursor.getString(settingsCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_USER_ID));
		mUserPwd = settingsCursor.getString(settingsCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_USER_PWD));
	}

	public void showWordCount() {
		int count = mDbAdapter.getWordCount();
		String text = String.valueOf(mCurrentWordIndex + 1) + "/"
				+ String.valueOf(count);

		mWordCountTextView.setText(text);
	}

	private void showWord() {
		mBottomButton.setText("SLIDE");
		mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(
				MainActivity.this, R.anim.push_right_in));
		mViewFlipper.showNext();

		loadWord();
	}

	private void loadWord() {
		Word word = mDbAdapter.fetchWordByIndex(mCurrentWordIndex);

		if (word != null) {
			mCurrentWordRowId = word.getRowId();
			mCurrentWordIndex = word.getIndex();
			mCurrentWordText = word.getWord();

			mWordTextView0.setText(word.getWord());
			mWordTextView1.setText(word.getWord());
			mPronunciationTextView0.setText(word.getPronunciation());
			mPronunciationTextView1.setText(word.getPronunciation());
			mTranslationTextView0.setText(word.getTranslation());
			mTranslationTextView1.setText(word.getTranslation());

			showWordCount();
		}
	}

	// == sub-classes
	// =============================================================================

	class MyArrayAdapter extends ArrayAdapter<MyListItem> {
		MyArrayAdapter() {
			super(MainActivity.this, R.layout.main_list_item, mDataArray);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.main_list_item, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.main_list_item);

			label.setText(mDataArray.get(position).getValue());
			label.setTag(mDataArray.get(position).getRowId());

			return (row);
		}
	}
}