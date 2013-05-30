package net.johnsonlau.tool;

public class Utilities {
	public static boolean isEmptyOrNull(String str) {
		if (str == null) {
			return true;
		} else if (str.trim().equals("")) {
			return true;
		} else {
			return false;
		}
	}
}
