package com.example.helperClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bip_public_android.MediaPreview;
/**
 * This class will create a horizontal layout that let the user dynamically
 * add/view items (primarily picture) to the layout horizontally
 * @author trannguyen
 *
 */
/**
 * This class will create a horizontal layout that let the user dynamically
 * add/view items (primarily picture) to the layout horizontally
 * @author trannguyen
 *
 */
public class PictureHorizontalLayout extends LinearLayout {
	/**Request code used to match the preview request code	 */
	private final static int REQUEST_CODE = 15;
	    
    private final static String TAG = "PictureHorizontalLayout";
	
	/** The width and height of the thumbnail image to add to the layout*/
	public final static int WIDTH = 105;
	public final static int HEIGHT = 105;
	
	/** The context that this layout is added in */
	Activity myContext;
	
	
	/** The array contains the internal model */
	private ArrayList<String> itemList = new ArrayList<String>();

	public PictureHorizontalLayout(Context context) {
		super(context);
		myContext = (Activity)context;
	}
	
	public PictureHorizontalLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		myContext = (Activity)context;
	}
	
	/** set the array to the item List */
	public void setArrayList(ArrayList<String> array){		
		itemList = array;
		for(String mediaPath: array){
			this.add(Uri.parse(mediaPath));
		}
	}
	
	/**
	 * Method used to retrieve the image from the path and 
	 * store that to the item list and add the image to the layout
	 * @param uri of the image
	 */
	public void add(Uri uri) {
		Log.d(TAG, "Whoa looke what we got here a uri with path: " + uri.getPath());
		int newIdx = itemList.size();
		new ImageAdder().execute(newIdx, uri);
	}


	/**
	 * Method used to decode a Bitmap from a path and resize the image to
	 * a specified height and width.
	 * @param path - String of the path to the image
	 * @param reqWidth - int width to reduce image to
	 * @param reqHeight - int height to reduce image to
	 * @return Bitmap - the decoded bitmap using the reqWidth and reqHeight
	 */
	public Bitmap decodeSampledBitmapFromUri(String path, int reqWidth,
			int reqHeight) {
		Bitmap bm = null;

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		bm = BitmapFactory.decodeFile(path, options);

		return bm;
	}
	
	/**
	 * Method used to resize an image to a specific height and width
	 * @param filePath - String path of the image to be resized
	 * @param targetWidth - int new width to resize image
	 * @param targetHeight - int new height to resize image
	 * @return Bitmap - resized Bitmap
	 */
	public static Bitmap resizeBitMapImage(String filePath, int targetWidth,
			int targetHeight) {
		Bitmap bitMapImage = null;
		Bitmap originalBitmap = BitmapFactory.decodeFile(filePath);
		bitMapImage = Bitmap.createScaledBitmap(originalBitmap, WIDTH, HEIGHT, true);

		return bitMapImage;
	}

	/**
	 * Method used to calculate the in sample size of a bitmap
	 * @param options - BitmapFactory.Options option used to calculate the sample size
	 * from the outHeight and outWidth
	 * @param reqWidth - int the new reduced width of the image
	 * @param reqHeight - int the new reduced height of the image
	 * @return int representing the ratio of the sample size to be reduced
	 */
	public int calculateInSampleSize
		(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 2;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}

		return inSampleSize;
	}

	@Override
	public void removeViewAt(int index) {
		super.removeViewAt(index);
		if (this.itemList != null) {
			Log.d(TAG, "VCJ PictureHorizontalLayout itemList: " + this.itemList + " and size: "+this.itemList.size() + " "+ this.itemList.isEmpty());
		} else {
			Log.d(TAG, "VCJ The item list for PictureHorizontalLayout is null!!!!!!!!!! NOOOOO");
		}
		this.itemList.remove(index);
	}
	
	@Override
	public void removeAllViews(){
		super.removeAllViews();
		if(this.itemList!= null){
			this.itemList.clear();
		}
	}
	
	public ArrayList<String> getItemList(){
		return this.itemList;
	}
	
	// convert exif orientation to degrees
	private static int exifToDegrees(int exifOrientation) {        
	    if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; } 
	    else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; } 
	    else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }            
	    return 0;    
	 }
	
	/**
	 * Retrieve the image from the path index and adds it to the layout
	 * @param i index of the the image to be retrieved
	 */
	public class ImageAdder extends AsyncTask<Object, Void, View>{


		@Override
		protected void onPostExecute(View result) {
			if(result!=null){
				PictureHorizontalLayout.this.addView(result);
			}
		}

		@Override
		protected View doInBackground(Object... params) {
			Integer i = (Integer)params[0];
			Uri myUri = (Uri)params[1];
			File myFile = new File(myUri.toString());
			String path = myFile.getAbsolutePath();
			itemList.add(path);
			Bitmap bm = null;
			
			View displayView = null;
			
			final String imageUrl;
			if (i < itemList.size()) {
				//TODO might have to make sure that other types of images can be added
				if(path.endsWith("jpg") || path.endsWith("jpeg")||path.endsWith("png")||path.endsWith("gif")){
					try {
                        ExifInterface exif = new ExifInterface(path);
                        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        int rotationInDegrees = exifToDegrees(rotation);
                        Log.d(TAG, "rotation in Degrees: " + rotationInDegrees);
                        Matrix matrix = new Matrix();
                        if(rotation != 0f) {
                            matrix.preRotate(rotationInDegrees);
                            Log.d(TAG, "creating new bitmap");
                     
                            bm = BitmapFactory.decodeFile(path);
                            bm = Bitmap.createBitmap(bm, 0, 0, WIDTH, HEIGHT, matrix, true);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
					bm = resizeBitmap(BitmapFactory.decodeFile(path), HEIGHT);
					// dynamically create the ImageView to add to the layout
					displayView = new ImageView(myContext);
										
					displayView.setLayoutParams(new LayoutParams(WIDTH, HEIGHT));
					((ImageView) displayView).setScaleType(ImageView.ScaleType.CENTER_CROP);
					((ImageView) displayView).setImageBitmap(bm);
					Log.d(TAG, "image dimensions: width = " + displayView.getWidth() + " height - " + displayView.getHeight());
					displayView.setPadding(4, 4, 4, 4);
					//Setting tag of the view as the string containing path
					displayView.setTag(path);
					imageUrl = (String) displayView.getTag();
					
				}else{
					bm =  ThumbnailUtils.createVideoThumbnail(path,
					MediaStore.Images.Thumbnails.MICRO_KIND);
					LayoutInflater inflater = (LayoutInflater)getContext().getSystemService
						      (Context.LAYOUT_INFLATER_SERVICE);
					//If we have a video set to be the video preview layout
					displayView = inflater.inflate(com.bip_public_android.R.layout.video_preview, null);
					displayView.setLayoutParams(new LayoutParams(WIDTH, HEIGHT));
					ImageView video_img = (ImageView)displayView.findViewById(com.bip_public_android.R.id.video_image_preview);
					video_img.setScaleType(ImageView.ScaleType.CENTER_CROP);
					video_img.setImageBitmap(bm);
					Log.d(TAG, "video_img dimensions: width - " + video_img.getWidth() + " height - " + video_img.getHeight());
					ImageView video_icon = (ImageView)displayView.findViewById(com.bip_public_android.R.id.video_icon);
					video_icon.setVisibility(View.VISIBLE);
					displayView.setPadding(4, 4, 4, 4);
					//Setting tag of the view as the string containing path
					displayView.setTag(path);
					imageUrl = (String) displayView.getTag();
					
				}
				
				displayView.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						Intent x = new Intent(myContext, MediaPreview.class);
						x.putExtra("local", true);
						x.putExtra("position", PictureHorizontalLayout.this.indexOfChild(v));
						x.putExtra("imageURL", imageUrl);
						Log.d(TAG, "VCJ PHL setting calling activity: " + myContext.getClass().getSimpleName());
						x.putExtra("callingActivity", myContext.getClass().getSimpleName());

						x.putExtra("notUploaded", true); // not added to PictureHorizontalLayout
														// of StepDisplay yet, only in small thumbnail
						myContext.startActivityForResult(x, REQUEST_CODE);
						
					}
					
				});

			}
			
			return displayView;
		}
		
	}

    /**
     * Resize image 
     * @author: ttseng
     */
    public static Bitmap resizeBitmap(Bitmap bm, int newHeight){
        int width = bm.getWidth();
        //Log.d(TAG, "width: " + width);
        int height = bm.getHeight();
        //Log.d(TAG, "height: " + height);
        float scaleFactor= ((float) newHeight)/height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactor, scaleFactor);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        //Log.d(TAG, "resized image height: " + resizedBitmap.getHeight());
        return resizedBitmap;
    }
	
}
