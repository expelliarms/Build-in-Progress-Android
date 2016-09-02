package com.bip_public_android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.helperClass.ImageFetcher;

/**
 * Customized camera interface: user can select multiple images from their image
 * gallery and pass these images back to the custom camera activity. 
 */
public class CustomizedGallery extends SherlockFragmentActivity implements OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>{

	//    private static final String TAG = "CustomizedGallery";
	/** selected picture array */
	private SparseBooleanArray thumbnailSelection = new SparseBooleanArray();

	/** Array of all the selected image's paths */
	private SparseArray<String> arrPath = new SparseArray<String>();

	/** ImageAdapter for the array Bitmap */
	private ImageAdapter imageAdapter;

	/** Grid view to hold all the images */
	private GridView imageGrid;

	/** Images already stored from the camera */
	private ArrayList<String> imageFromCam = new ArrayList<String>();
	
	MenuItem saveMenuItem; // saveIcon 
	
	private int imageCounter = 0; // keeping track of the number of selecdted images

	private Cursor imageCursor, actualImageCursor;
	private int image_column_index, actual_image_column_index;
	private int colWidth;

	private static final int CURSORLOADER_THUMBS = 0;
	private static final int CURSORLOADER_REAL = 1;

	private Set<String> fileNames = new HashSet<String>();

	private final ImageFetcher fetcher = new ImageFetcher();

	private boolean shouldRequestThumb = true;

	public static final String IMAGE_INTENT = "image";
	public static final String GALLERY_INTENT = "gallery";

	private String[] projection = {
			MediaStore.Files.FileColumns._ID, 
			MediaStore.Files.FileColumns.DATA,
			MediaStore.Files.FileColumns.DATE_ADDED,
			MediaStore.Files.FileColumns.MEDIA_TYPE,
			MediaStore.Files.FileColumns.MIME_TYPE,
			MediaStore.Files.FileColumns.TITLE
	}; // used for query ContentResolver for mediafiles

	private String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
			+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
			+ " OR " 
			+ MediaStore.Files.FileColumns.MEDIA_TYPE + "="
			+ MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;  // used to select images and videos from contentResolver

	private Uri queryUri = MediaStore.Files.getContentUri("external");  
	
	private final static String TAG = "CustomizedGallery";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.bip_public_android.R.layout.activity_customized_gallery);
		fileNames.clear();

		// add back button to action bar
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		Log.d(TAG, "customized gallery!!!!!!!");
		MediaPreview.log();
		setUpViews();

	}

	
	/**
	 * Set up views associated with the activity and the associated listeners
	 * to the views
	 */
	private void setUpViews() {
	    
	    getSupportActionBar().setTitle("");

		//Setting up 3 columns
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		int width = outMetrics.widthPixels;
		colWidth = width/3;

		imageGrid = (GridView) findViewById(com.bip_public_android.R.id.PhoneImageGrid_customized_gallery);
		imageGrid.setColumnWidth(colWidth);
		imageGrid.setOnScrollListener(new OnScrollListener() {
			private int lastFirstItem = 0;
			private long timestamp = System.currentTimeMillis();

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if(scrollState == SCROLL_STATE_IDLE){
					shouldRequestThumb = true;
					imageAdapter.notifyDataSetChanged();                    
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				float dt = System.currentTimeMillis()-timestamp;
				if (firstVisibleItem != lastFirstItem){
					double speed = 1 / dt * 1000;
					lastFirstItem = firstVisibleItem;
					timestamp = System.currentTimeMillis();
					shouldRequestThumb = speed < visibleItemCount;
				}

			}
		});

		imageAdapter = new ImageAdapter(this);
		imageGrid.setAdapter(imageAdapter);

		LoaderManager.enableDebugLogging(false);
		getSupportLoaderManager().initLoader(CURSORLOADER_THUMBS, null, this);
		getSupportLoaderManager().initLoader(CURSORLOADER_REAL, null, this);		
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.add(Menu.NONE, com.bip_public_android.R.id.save, Menu.NONE, "save")
		.setIcon(android.R.color.transparent)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		saveMenuItem = menu.findItem(com.bip_public_android.R.id.save);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		// Handle item selection
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		case com.bip_public_android.R.id.save:
			final int len = thumbnailSelection.size();
			int cnt = 0;
			for (int i = 0; i < len; i++) {
				cnt++;
				imageFromCam.add(arrPath.get(thumbnailSelection.keyAt(i)));
			}
			if (cnt == 0) {
				setResult(RESULT_CANCELED);
				finish();
			} else {
				// send these pictures to new activity
				Intent returnIntent = getIntent();
				returnIntent.putStringArrayListExtra(GALLERY_INTENT, imageFromCam);
				setResult(RESULT_OK,returnIntent); 
				finish();
			}
			break;
		}
		return true;
	}

	/**
	 * The adapter to handle to underlying array of pictures
	 *
	 */
	public class ImageAdapter extends BaseAdapter {
		private final Bitmap mPlaceHolderBitmap;
		private LayoutInflater mInflater;

		public ImageAdapter(Context c) {
			Bitmap tmpHolderBitmap = BitmapFactory.decodeResource(getResources(), com.bip_public_android.R.drawable.loading_icon);
			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mPlaceHolderBitmap = Bitmap.createScaledBitmap(tmpHolderBitmap, colWidth, colWidth, false);
			if(tmpHolderBitmap != mPlaceHolderBitmap){
				tmpHolderBitmap.recycle();
				tmpHolderBitmap = null;
			}
		}

		@Override
		public int getCount() {
			if(imageCursor !=null){
				return imageCursor.getCount();
			}else{
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		// create a new ImageView for each item referenced by the Adapter
		public View getView(final int position, View convertView, ViewGroup parent) {
		    Log.d(TAG, "in get view");
			final ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(com.bip_public_android.R.layout.galleryitem, null);
				holder.imageView = (ImageView) convertView
						.findViewById(com.bip_public_android.R.id.thumbImage_gallery_item);
				holder.checkbox = (CheckBox) convertView
						.findViewById(com.bip_public_android.R.id.itemCheckBox_gallery_item);
				holder.playIcon = (ImageView) convertView.findViewById(com.bip_public_android.R.id.videoPlayIcon_gallery_item);
				convertView.setTag(holder);
			}
			// picture is already there
			else {
				holder = (ViewHolder) convertView.getTag();
			}

			// Select image by either clicking the imageview or the checkbox
			holder.imageView.setOnClickListener(new selectImage(position, holder));
			holder.checkbox.setOnClickListener(new selectImage(position, holder));

			holder.imageView.setImageBitmap(null);

			if (!imageCursor.moveToPosition(position)) {
				return convertView;
			}

			if (image_column_index == -1) {
				return convertView;
			}

			final int id = imageCursor.getInt(image_column_index);
			int mediaType = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
			final boolean isVideo = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
			final String mediaPath =  imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
			Log.d(TAG, "image path: " + mediaPath);

			if (isChecked(position)) {
				holder.checkbox.setChecked(true);
			} else {
				holder.checkbox.setChecked(false);
			}
			if (shouldRequestThumb) {
				fetcher.fetch(Integer.valueOf(id), mediaPath, holder.imageView, colWidth, isVideo);
			}
			if(isVideo){
				holder.playIcon.setVisibility(View.VISIBLE);
			}else{
				holder.playIcon.setVisibility(View.INVISIBLE);
			}

			return convertView;

		}


	}

	public class selectImage implements View.OnClickListener{
		int pos;
		ViewHolder hold;

		public selectImage(int pos, ViewHolder hold){
			this.pos = pos;
			this.hold = hold;
		}

		@Override
		public void onClick(View v){
			boolean imageExists = thumbnailSelection.get(pos);
			if (imageExists){
				// remote image from arrays
				thumbnailSelection.delete(pos);
				arrPath.delete(pos);
				imageCounter--;
			}else{
				// add image to arrays
			    imageCounter++;
				thumbnailSelection.put(pos, !thumbnailSelection.get(pos));
				Cursor mediaCursor = getContentResolver().query(queryUri, projection, selection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
				mediaCursor.moveToPosition(pos);
				int mediaType = mediaCursor.getInt(mediaCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
				Boolean isVideo = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
				int dataColumnIndex;
				if(isVideo){
					dataColumnIndex = mediaCursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA);
				}else{
					dataColumnIndex = mediaCursor.getColumnIndex(MediaStore.Images.Media.DATA);
				}	            
				arrPath.put(pos, mediaCursor.getString(dataColumnIndex));
			}
			
			// update title 
			
            if(imageCounter == 1){
                getSupportActionBar().setTitle(imageCounter + " image selected   ");
                saveMenuItem.setIcon(com.bip_public_android.R.drawable.ic_navigation_forward_highlight);
            }else if(imageCounter >1){
                getSupportActionBar().setTitle(imageCounter + " images selected");
                saveMenuItem.setIcon(com.bip_public_android.R.drawable.ic_navigation_forward_highlight);
            }else{
                getSupportActionBar().setTitle("");
                saveMenuItem.setIcon(android.R.color.transparent); 
            }
			
			hold.checkbox.setChecked(thumbnailSelection.get(pos));	        
		}
	}


	/**
	 * The view holder to store the check box and imageView 
	 */
	private class ViewHolder {
		private CheckBox checkbox;
		private ImageView imageView;
		private ImageView playIcon;

	}

	private String getImageName(int position) {
		actualImageCursor.moveToPosition(position);
		String name = null;

		try {
			name = actualImageCursor.getString(actual_image_column_index);
		} catch (Exception e) {
			return null;
		}
		return name;
	}

	private void setChecked(int position, boolean b) {
		thumbnailSelection.put(position, b);
	}

	public boolean isChecked(int position) {
		boolean ret = thumbnailSelection.get(position);
		return ret;
	}

	public void cancelClicked(View ignored) {
		setResult(RESULT_CANCELED);
		finish();
	}

	public void selectClicked(View ignored) {
		Intent data = new Intent();
		if (fileNames.isEmpty()) {
			this.setResult(RESULT_CANCELED);
		} else {

			ArrayList<String> al = new ArrayList<String>();
			al.addAll(fileNames);
			Bundle res = new Bundle();
			res.putStringArrayList("MULTIPLEFILENAMES", al);
			if (imageCursor != null) {
				res.putInt("TOTALFILES", imageCursor.getCount());
			}

			data.putExtras(res);
			this.setResult(RESULT_OK, data);
		}
		finish();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		String name = getImageName(position);

		if (name == null) {
			return;
		}

		boolean isChecked = !isChecked(position);

		setChecked(position, isChecked);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int cursorID, Bundle arg1) {
		CursorLoader cl = null;

		ArrayList<String> media = new ArrayList<String>();
		switch (cursorID) {

		case CURSORLOADER_THUMBS:
			media.add(MediaStore.Files.FileColumns._ID);
			break;
		case CURSORLOADER_REAL:
			media.add(MediaStore.Files.FileColumns.DATA);
			break;
		default:
			break;
		}
		Log.d(TAG, "CREATING CURSOR");
        cl = new CursorLoader(CustomizedGallery.this, queryUri, projection, selection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
        
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor == null) {
			// NULL cursor. This usually means there's no image database yet..
		    Log.d(TAG, "NO IMAGES FOUND");
			return;
		}
		Log.d(TAG, "CURSOR_COUNT: " + Integer.toString(cursor.getCount()));
		Log.d(TAG, "loading images");
		switch (loader.getId()) {
		case CURSORLOADER_THUMBS:
			imageCursor = cursor;
			image_column_index = imageCursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
			imageAdapter.notifyDataSetChanged();
			break;
		case CURSORLOADER_REAL:
			actualImageCursor = cursor;
			actual_image_column_index = actualImageCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
			break;
		default:
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == CURSORLOADER_THUMBS) {
			imageCursor = null;
		} else if (loader.getId() == CURSORLOADER_REAL) {
			actualImageCursor = null;
		}
	}


}