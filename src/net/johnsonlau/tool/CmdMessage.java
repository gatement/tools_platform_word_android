package net.johnsonlau.tool;

public class CmdMessage {
	private String mCmd;
	private String mValue;

	public CmdMessage(String cmd, String value) {
		mCmd = cmd;
		mValue = value;
	}
	
	public String getCmd()
	{
		return mCmd;
	}
	
	public String getValue()
	{
		return mValue;
	}
}
