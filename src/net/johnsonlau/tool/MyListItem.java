package net.johnsonlau.tool;

public class MyListItem {
	private long mRowId;
	private String mValue;
	private int mDisplayOrder;

	public MyListItem(long rowId, String value, int displayOrder) {
		mRowId = rowId;
		mValue = value;
		mDisplayOrder = displayOrder;
	}
	
	public long getRowId()
	{
		return mRowId;
	}
	
	public String getValue()
	{
		return mValue;
	}
	
	public int getDisplayOrder()
	{
		return mDisplayOrder;
	}
	
	public int setDisplayOrder(int displayOrder)
	{
		return mDisplayOrder = displayOrder;
	}
}
