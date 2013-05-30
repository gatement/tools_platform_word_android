package net.johnsonlau.word.activity;

import net.johnsonlau.tool.Utilities;
import net.johnsonlau.word.R;
import net.johnsonlau.word.db.DbAdapter;
import net.johnsonlau.word.db.DbOpenHelper;
import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SettingsActivity extends Activity {
	private DbAdapter mDbAdapter;

	private EditText mServiceUrlEditText;
	private EditText mUserEditText;
	private EditText mPasswordEditText;
	private TextView mMsgTextView;
	private Button mSaveButton;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		initMembers();
		bindEvents();
		populateData();
	}

	// == initialization methods
	// ===============================================================

	private void initMembers() {
		mDbAdapter = new DbAdapter(this).open();

		mMsgTextView = (TextView) findViewById(R.id.settings_msg);
		mServiceUrlEditText = (EditText) findViewById(R.id.settings_service_url);
		mUserEditText = (EditText) findViewById(R.id.settings_user);
		mPasswordEditText = (EditText) findViewById(R.id.settings_password);
		mSaveButton = (Button) findViewById(R.id.settings_save);
	}

	private void bindEvents() {
		this.mSaveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				// get setting values
				String service = mServiceUrlEditText.getText().toString()
						.trim();
				String user = mUserEditText.getText().toString().trim();
				String password = mPasswordEditText.getText().toString().trim();

				if (Utilities.isEmptyOrNull(service)
						|| Utilities.isEmptyOrNull(user)
						|| Utilities.isEmptyOrNull(password)) {
					mMsgTextView.setText("Please fill out all requred fields.");
					return;
				}

				// save settings, clear records, and return
				mDbAdapter.updateSettings(service, user, password);
				mDbAdapter.purgeWords();
				mDbAdapter.purgeDeletedWords();

				goToMainActivity();
			}
		});
	}

	private void populateData() {
		try {
			Cursor settingsCursor = mDbAdapter.fetchSettings();
			settingsCursor.moveToFirst();
			startManagingCursor(settingsCursor);

			mServiceUrlEditText
					.setText(settingsCursor.getString(settingsCursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_SERVICE)));
			mUserEditText
					.setText(settingsCursor.getString(settingsCursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_USER_ID)));
			mPasswordEditText
					.setText(settingsCursor.getString(settingsCursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_USER_PWD)));
		} catch (SQLException ex) {
			mMsgTextView.setText("Load settings error!");
		}
	}

	// == override methods
	// =====================================================================
	protected void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	// == helpers
	// ============================================================================

	private void goToMainActivity() {
		setResult(RESULT_OK);
		finish();
	}
}