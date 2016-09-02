package com.savagelook.android;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

public class Lazy {
	public static class Ex {
		public static String getStackTrace(java.lang.Exception e) {
			StringWriter sWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(sWriter));
			return sWriter.getBuffer().toString();
		}
	}
	
	public static class Str {
		public static String urlEncode(String value, boolean trim) {
			try {
				return java.net.URLEncoder.encode(value.trim(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;	
		}
		
		public static String urlEncode(String value) {
			return urlEncode(value, true);
		}
	}
}
