package net.johnsonlau.word.model;

public class Word {
	private long mRowId;
	private int mIndex;
	private String mWord;
	private String mPronunciation;
	private String mTranslation;

	public Word(long rowId, int index, String word, String pronunciation, String translation) {
		mRowId = rowId;
		mIndex = index;
		mWord = word;
		mPronunciation = pronunciation;
		mTranslation = translation;
	}

	public long getRowId() {
		return mRowId;
	}

	public int getIndex() {
		return mIndex;
	}

	public String getWord() {
		return mWord;
	}

	public String getPronunciation() {
		return mPronunciation;
	}

	public String getTranslation() {
		return mTranslation;
	}
}
