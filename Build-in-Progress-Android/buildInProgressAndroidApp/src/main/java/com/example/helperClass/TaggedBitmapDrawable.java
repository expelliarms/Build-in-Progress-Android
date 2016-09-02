package com.example.helperClass;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class TaggedBitmapDrawable extends BitmapDrawable{

	private String url;
	private int id;
	private String filename;
	
	public TaggedBitmapDrawable(Resources resources, Bitmap bitmap, String url, int id, String filename){
		super(resources, bitmap);
		this.url = url;
		this.id = id;
		this.filename = filename;
	}
	
	/**
	 * @return url of this TaggedBitmapDrawable
	 */
	public String getUrl(){
		return url;
	}
	
	/**
	 * @return id of this TaggedBitmapDrawable
	 */
	public int getId(){
		return id;
	}
	
	/**
	 * @return filename of this TaggedBitmapDrawable
	 */
	public String getFilename(){
		return filename;
	}
	
}
