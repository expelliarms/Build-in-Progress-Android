package com.bip_public_android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.example.helperClass.Constant;
import com.example.helperClass.ImageVideoResource;
import com.example.helperClass.MediaPreviewAdapter;
import com.example.helperClass.Multimedia;
import com.example.helperClass.PreviewFragment;
import com.example.helperClass.TaggedBitmapDrawable;
import com.google.android.youtube.player.YouTubePlayerFragment;

/**
 * <code> public class extends Activity </code>
 * <p>
 * Activity used to select or delete a single image from a list of images
 * 
 * @author trannguyen
 * 
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MediaPreview extends SherlockFragmentActivity {

    private String auth_token;

    /** Check button to return to the prev activity */
    private ImageButton okay;
    /** Delete image */
    private ImageButton delete;

    /** URL of the media file, if its is non-local */
    private String mediaURL;
    private String bip_image_url; // also for video
    private String filename;

    /** Position of image in the array */
    private int position;
    private int mediaID; // imageID from Build in Progress website

    private boolean isLocal;
    private boolean isFromVideo; // whether the media file is a video

    private YouTubePlayerFragment playerFragment; // for holding youtube video
    MediaController videoController; // for playing local video
    boolean videoControllerExists = false;

    private ProgressBar mediaProgress;
    private boolean isVimeoVideo = false;

    public static int bottomButtonHeight; // for placing video controller over buttons

    private FrameLayout mediaHolder;
    private boolean wasVideo;

    private final static String TAG = "MediaPreview";
    
    // Swipe View stuff
	public static ArrayList<ImageVideoResource> mediaRes =
			new ArrayList<ImageVideoResource>(); // list of ImageVideoResource of
												   // all the media in the step
    public final String EMPTYVIDEOPATH = ""; // empty string to be given as the videoURL input
                                    // for an ImageVideoResource that is an image
    public static ArrayList<String> mediaSources =
    		new ArrayList<String>(); // list of URLs of all the media in the step
    private MediaPreviewAdapter adapter;
	private ViewPager pager;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "oncreate mediapreview");
        super.onCreate(savedInstanceState);
        setContentView(com.bip_public_android.R.layout.activity_media_preview); // contains viewpager
    }


    @Override
    protected void onStart(){
    	super.onStart();
    	Log.d(TAG, "onstart mediapreview");

        BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;

		new SetAllBitmap().execute();

        okay = (ImageButton) findViewById(com.bip_public_android.R.id.okay_imagePreview);
        delete = (ImageButton) findViewById(com.bip_public_android.R.id.delete_imagePreview);
        okay.setBackgroundResource(com.bip_public_android.R.drawable.custom_button_blue);
        delete.setBackgroundResource(com.bip_public_android.R.drawable.custom_button_blue);
        setUpViews();

        // add back button to action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setting up the preview (for local images and videos)
        //need to initialize the pager here, so we can make it invisible if there are local images/videos
        pager = (ViewPager) findViewById(com.bip_public_android.R.id.view_pager);
        //Intent i = getIntent();
        //Log.d(TAG, "VCJ is local: " + isLocal + " and local from intent: " + i.getBooleanExtra("local", false));

        /*String callingClass = i.getStringExtra("callingActivity");
        Log.d(TAG, "VCJ Media Preview calling activity : " + callingClass);*/

        //if (callingClass.equals(CustomCamera.class.getSimpleName())) { // if the calling activity is Custom Camera
        if (isLocal) { // if the calling activity is Custom Camera
            setUpPreview(); // need it for local pictures and images
        } else {
            // want to see the ViewPager
            pager.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Sets up the okay and delete buttons and the s3TextureView
     */
	public void setUpViews() {
    	Log.d(TAG, "setting up views HERE");
        // Setting the image buttons
        /*okay = (ImageButton) findViewById(R.id.okay_imagePreview);
        delete = (ImageButton) findViewById(R.id.delete_imagePreview);
        okay.setBackgroundResource(R.drawable.custom_button_grey);
        delete.setBackgroundResource(R.drawable.custom_button_grey);*/
        //bottomButtons = (LinearLayout) findViewById(R.id.bottom_button_image_preview);

        bottomButtonHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 53, getResources().getDisplayMetrics());

        final Intent i = getIntent();
        // Obtaining all the paths
        mediaURL = i.getStringExtra("imageURL");

        // hide title from toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        isLocal = i.getBooleanExtra("local", false);
        isFromVideo = i.getBooleanExtra("isFromVideo", false);
        //videoRotation = i.getIntExtra("videoRotation", 0);
        //mainContentView = (FrameLayout) findViewById(R.id.main_content);
        mediaProgress = (ProgressBar) findViewById(com.bip_public_android.R.id.media_progress);

        // Getting the image position in the array
        position = i.getIntExtra("position", -1);
        mediaID = i.getIntExtra("imageId", -1);

        if (!isLocal) {
            bip_image_url = Constant.IMAGE_URL + mediaID; // update IMAGE_URL with imageID
            if (isFromVideo) {
                mediaURL = i.getStringExtra("videoURL");
            }
        }
        // vcj: disable buttons, until list is populated
        /*okay.setBackgroundResource(R.drawable.custom_button_blue);
        delete.setBackgroundResource(R.drawable.custom_button_blue);*/
        okay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
               	StepDisplay.setAlreadyLoaded(true);
                Intent returnIntent = getIntent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
            }
        });

        delete.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	Intent intent = getIntent();
            	boolean notUploaded = intent.getBooleanExtra("notUploaded", false);

            	AlertDialog dialog = new AlertDialog.Builder(
                        MediaPreview.this).create();

            	if(notUploaded){ // image only in small thumbnail
            		// Don't show dialog
            		Intent returnIntent = getIntent();
                    returnIntent.putExtra("deleted", true);
                    returnIntent.putExtra("position", position);
                    setResult(RESULT_OK, returnIntent);
                    finish();

            	} else { // image uploaded , Set buttons for media from website
                    if (!isLocal) {
                        // Deleting from view pager
                        dialog.setTitle("Delete");
                        dialog.setMessage("Are you sure you want to delete?");
                        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        if (!isLocal) {
                                            // delete image from website
                                            //Log.d(TAG, "VCJ about to call deleteImageTask for bip_image_url: "+bip_image_url);
                                        	new DeleteImageTask(MediaPreview.this)
                                        		.execute(bip_image_url + "?auth_token=" + auth_token);
                                        }
                                        dialog.dismiss();
                                    }
                                });
                        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        dialog.dismiss();
                                    }
                                });
                        dialog.show();
                    } else { // Deleting newly uploaded media that isn't in the viewpager
                        // Technically, it's not local either (but we set local to true in Method.java)
                        dialog.setTitle("Delete");
                        dialog.setMessage("Are you sure you want to delete?");
                        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                                new DialogInterface.OnClickListener() {

                        			@Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    	int index = mediaURL.length()-1;
                                    	while (!mediaURL.substring(index, index+1).equals("/")) {
                                    		index--;
                                    	}
                                    	filename = mediaURL.substring(index+1);

                                    	new DeleteNewImageTask(MediaPreview.this).execute(filename);

                                    	Intent returnIntent = getIntent();
                                        returnIntent.putExtra("deleted", true);
                                        returnIntent.putExtra("position", position);
                                        setResult(RESULT_OK, returnIntent);
                                        finish();

                                        dialog.dismiss();
                                    }
                                });

                        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                    	dialog.show();
                    }
            	}

            }
        });
        delete.bringToFront();
        okay.bringToFront();
    }

	/**
     * Method used to set up the image view in the preview image. If the image
     * is store locally, it will get the picture form within the phone and set
     * it as the image. If it is on the website, it will have to download it and
     * then display it.
     */
	private void setUpPreview() {
    	Log.d(TAG, "setUpPreview");
        mediaHolder = (FrameLayout) findViewById(com.bip_public_android.R.id.media_holder);
        // vcj: only happens with local images/videos now
        Log.d(TAG, "IS LOCAL");
        if (mediaURL.endsWith("jpg") || mediaURL.endsWith("jpeg")
                || mediaURL.endsWith("png") || mediaURL.endsWith("gif")) {
            addLocalImage(mediaHolder);
        } else {// It is a video
            playLocalVideo(mediaHolder);
        }

    }

    /**
     * Loads all the images with their urls and creates TaggedBitmapDrawable
     * objects for each image. Also sets up the pager and adapter
     * for swipe view.
     * @author eunice
     *
     */
    public class SetAllBitmap extends AsyncTask <Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "setAllBitmap mediaSources = " + mediaSources);
			Log.d(TAG, "setAllBitmap mediaRes BEFORE= " + mediaRes);
            //Log.d(TAG,"VCJ mediaSources: "+mediaSources);
            //Log.d(TAG,"VCJ Project: "+mediaSources);

            clearImages(); //vcj add
            ArrayList<Multimedia> multimediaList = ProjectDisplay.currentlySelected.getMultimediaList();
            ArrayList<Integer> mediaIDlist = new ArrayList<Integer>();
            for (Multimedia media : multimediaList) {
                String url;
                mediaIDlist.add(media.getId());
                if(media.isFromVideo()) {
                    //Log.d(TAG, "Video path vcj: "+media.getVideoPath());
                    url = media.getVideoPath();
                } else {
                    //Log.d(TAG, "image path vcj: "+media.getPath());
                    url = media.getPath();
                }
                if (!mediaSources.contains(url)) {
                    // only add it if it's not already in there
                    mediaSources.add(url);
                }

            }

            ArrayList<TaggedBitmapDrawable> drawableList = new ArrayList<TaggedBitmapDrawable>(mediaSources.size());
            for (String src : mediaSources) {
                Log.d(TAG, "src = " + src);
                ImageView image = new ImageView(MediaPreview.this);
                URL url;
                try {
                    // Get image from url
                    if (src.indexOf("http")!=0) {
                        // srcs without the http, need it to connect
                        mediaSources.set(mediaSources.indexOf(src), "http:" +src);
                        src = "http:" +src;
                    }
                    url = new URL(src);
                    //Log.d(TAG, "src = " + src);
                    HttpGet httpRequest;
                    httpRequest = new HttpGet(url.toURI());
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = httpclient
                            .execute(httpRequest);
                    HttpEntity entity = response.getEntity();
                    BufferedHttpEntity b_entity = new BufferedHttpEntity(entity);
                    InputStream input = b_entity.getContent();

                    // Create bitmap from image
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    image.setImageBitmap(bitmap);
                    Resources resources = getResources();

                    // Get attributes from url
                    // Should not get attributes from the url, since the url might be a reference to a remix item
                    String filename = src.substring(src.lastIndexOf("/")+1,src.length());
                    int id = mediaIDlist.get(mediaSources.indexOf(src));
                    //int id = Integer.parseInt(stringId);

                    // Create BitmapDrawable object and add it to mediaRes
                    TaggedBitmapDrawable drawable =
                            new TaggedBitmapDrawable(resources, bitmap, src, id, filename);
                    drawableList.add(drawable);
                } catch (Exception e) {
                    //Log.d(TAG, "VCJ some error: "+e.getMessage());
                    e.printStackTrace();
                }
            }

            //Log.d(TAG, "VCJ: Media Preview currently selected project media list size : "+ProjectDisplay.currentlySelected.getMultimediaList().size());
            //Log.d(TAG, "VCJ: Media Preview media list is double stepDisplay for some reason List: "+ProjectDisplay.currentlySelected.getMultimediaList());
            for (int i =0; i < ProjectDisplay.currentlySelected.getMultimediaList().size(); i++) {
                Multimedia item = ProjectDisplay.currentlySelected.getMultimediaList().get(i);
                //Log.d(TAG, "VCJ video  : "+item.getVideoPath());
                if (item.getVideoPath()!=null){
                    //it's a video
                    mediaRes.add(new ImageVideoResource(item.getVideoPath(), item.getVideoRotation(), drawableList.get(i)));
                } else {
                    mediaRes.add(new ImageVideoResource(EMPTYVIDEOPATH, 0, drawableList.get(i))); // VCJ TODO change this later so that it's not set to 0
                }
            }
            Log.d(TAG, "setAllBitmap mediaRes AFTER = " + mediaRes);
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
            if (!isLocal) {
                position = getIntent().getIntExtra("position", -1);

                // Initialize pager and adapter for swipe view
                //Log.d(TAG, "VCJ Position of image: " + position);
                adapter = new MediaPreviewAdapter(getSupportFragmentManager(), mediaRes.size(), position);
                pager.setAdapter(adapter);
                pager.setCurrentItem(position);
                pager.setOffscreenPageLimit(3);
                //pager.setOffscreenPageLimit(3);
                pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

                    // Gets called every time a new image page is loaded
                    @Override
                    public void onPageSelected(int position) {
                        // set mediaID and bip_image_url to info from current image
                        //Log.d(TAG, "position = " + position);
                        //Log.d(TAG, "number of multimedia = " + ProjectDisplay.currentlySelected.getMultimediaList().size());

                        Multimedia currentMedia = ProjectDisplay.currentlySelected.getMultimediaList().get(position);
                        mediaID = currentMedia.getId();
                        bip_image_url = Constant.IMAGE_URL + mediaID;

                        int currentItem = pager.getCurrentItem();
                        PreviewFragment currentFragment = (PreviewFragment) adapter.instantiateItem(pager, currentItem);
                        // Set the visible hint to true on page selected
                        currentFragment.setUserVisibleHint(true);

                    }

                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        //Log.d(TAG, "VCJ Media Preview: on Page Scrolled Page: "+position+" Out of "+pager.getChildCount()+" Current item:"+pager.getCurrentItem());
                        // Position is the item being touched and dragged. At the end it calls this function on the selected page (position==currentlySelected).
                        int currentItem = pager.getCurrentItem();
                        PreviewFragment positionFragment = (PreviewFragment) adapter.instantiateItem(pager, position);
                        PreviewFragment currentFragment = (PreviewFragment) adapter.instantiateItem(pager, currentItem);
                        if (position != currentItem) {
                            ArrayList<PreviewFragment> fragments = new ArrayList<PreviewFragment>();
                            fragments.add(positionFragment);
                            fragments.add(currentFragment);
                            for (PreviewFragment fragment : fragments) {
                                if (fragment.isVideo) {
                                    if (fragment.s3VideoPlayer != null) {
                                        if (fragment.s3VideoPlayer.isPlaying()) {
                                            fragment.s3VideoPlayer.pause();
                                        }
                                        if (fragment.videoController != null){
                                            fragment.videoController.hide(); // hide regardless, but pause if the video is playing
                                        }
                                    }
                                    if (fragment.playerFragment != null) {
                                        //fragment.playerFragment.pause();
                                        getSupportFragmentManager().beginTransaction().remove(fragment.playerFragment).commit();
                                        fragment.videoIcon.setVisibility(View.VISIBLE);
                                        fragment.videoIcon.bringToFront();
                                    }

                                    //if (fragment) // stop the vimeo videos on a swipe
                                }
                            }
                        } else {
                            //don't start the video b/c you'll play a video when swiping
                            if (positionFragment.isVideo && positionFragment.videoController != null) {
                                positionFragment.videoController.show();
                            }
                        }
                    }
                });


            }

            SharedPreferences preferences = getSharedPreferences("CurrentUser",
                    MODE_PRIVATE);
            auth_token = preferences.getString("AuthToken", "");

            // add back button to action bar
            //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		}
    }


    public static void addMedia(ImageVideoResource res, String source){
    	Log.d(TAG, "ADDING media BEFORE!!!!!!! "); // empty -- should not be empty!!!
    	Log.d(TAG, "add Media mediaRes = " + mediaRes);
    	Log.d(TAG, "add Media mediaSources = " + mediaSources);
        mediaRes.add(res);
    	mediaSources.add(source);
    	Log.d(TAG, "ADDING media AFTER!!!!!!!!! ");
    	Log.d(TAG, "add Media mediaRes = " + mediaRes);
    	Log.d(TAG, "add Media mediaSources = " + mediaSources);
    }



	/**
	 * Delete new image from step
	 * @author eunice
	 *
	 */
	public class DeleteNewImageTask extends AsyncTask<String, Void, HttpEntity> {

		private Context context;

		public DeleteNewImageTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute(){
			StepDisplay.setAlreadyLoaded(false);
		}

		@Override
		protected HttpEntity doInBackground(String... params) {

			String filename = params[0];

			// Read json file
			DefaultHttpClient defaultClient = new DefaultHttpClient();
			String url = Constant.PROJECT_URL + ProjectDisplay.getId() + "/steps.json";
			HttpGet httpGetRequest = new HttpGet(url);
			HttpResponse httpResponse;
			try {
				httpResponse = defaultClient.execute(httpGetRequest);
				BufferedReader reader = new BufferedReader (
						new InputStreamReader( httpResponse.getEntity().getContent(), "UTF-8"));
				String line = reader.readLine();
				String json = "";
				while (line != null) {
					json += line;
					line = reader.readLine();
				}

				// get the id from json, should be right before
				// first occurrence of filename
				int endIndex = json.indexOf(filename)-1; // index of the / between mediaID and filename
				int startIndex = endIndex;
				if(json.contains(filename)){
					while(!json.substring(startIndex-1, startIndex).equals("/")) {
						startIndex--;
					}
				} else {
					return null;
				}
				int mediaID = Integer.parseInt(json.substring(startIndex, endIndex));

				// Delete from mediaSources
				String deleteUrl;

                deleteUrl = Constant.AWS_IMAGE_URL + mediaID + "/" + filename;
				/*

				*/
				mediaSources.remove(deleteUrl);
                Log.d(TAG, "Delete new image : deleteURL: "+deleteUrl);
				// same as DeleteImageTask
				DefaultHttpClient client = new DefaultHttpClient();
				HttpDelete delete;

                delete = new HttpDelete(Constant.IMAGE_URL + mediaID + "?auth_token=" + auth_token);

		        delete.setHeader("Content-Type", "application/json");

		        HttpResponse response = null;

		        try {
		        	response = client.execute(delete);
		            Log.d(TAG, "delete new image response = " + response );
		        } catch (HttpResponseException e) {
		            e.printStackTrace();
		            Log.e("ClientProtocol", "" + e);
		        } catch (IOException e) {
		            e.printStackTrace();
		            Log.e("IO", "" + e);
	            }
	            HttpEntity result = null;
	            if (response != null) {
		            result = response.getEntity();
		        }
	            return result;

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;

		}

		protected void onPostExecute() {
            if (!isFromVideo) {
                Toast.makeText(getApplicationContext(), "Image Deleted",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Video Deleted",
                        Toast.LENGTH_LONG).show();
            }

            //Log.d(TAG, "VCJ delete new image onPostExecute");

            Intent returnIntent = getIntent();
            returnIntent.putExtra("imageId", mediaID);
            returnIntent.putExtra("deleted", true);
            returnIntent.putExtra("position", position);

            setResult(RESULT_OK, returnIntent);
            finish();
		}
	}


    /**
     * Delete Image from Step
     */

    public class DeleteImageTask extends AsyncTask<String, Void, HttpEntity> {

        private ProgressDialog dialog;
    	private Context context;

        public DeleteImageTask(Context context) {
        	this.context = context;
            dialog = new ProgressDialog(context);
        }

        protected void onPreExecute() {
        	Log.d(TAG, "delete image onPreExecute");
            if (!wasVideo) {
            //if (!isFromVideo) {
                this.dialog.setMessage("Deleting image..");
            } else {
                this.dialog.setMessage("Deleting video..");
            }
            this.dialog.setCancelable(false);
            this.dialog.show();
        }

        @Override
        protected HttpEntity doInBackground(String... urls) {

            DefaultHttpClient client = new DefaultHttpClient();
            HttpDelete delete = new HttpDelete(urls[0]);

            delete.setHeader("Content-Type", "application/json");

            HttpResponse response = null;

            try {
                response = client.execute(delete);
                Log.d(TAG, " delete image response = " + response );
            } catch (HttpResponseException e) {
                e.printStackTrace();
                Log.e("ClientProtocol", "" + e);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("IO", "" + e);
            }
            HttpEntity result = null;
            if (response != null) {
                result = response.getEntity();
            }
            return result;
        }

        protected void onPostExecute(HttpEntity result) {

            if (result != null) {
                // go back to the project screen
                dialog.dismiss();
                if (!wasVideo) {
                //if (!isFromVideo) {
                    Toast.makeText(getApplicationContext(), "Image Deleted",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Video Deleted",
                            Toast.LENGTH_LONG).show();
                }

                //Log.d(TAG, "VCJ image onPostExecute");

                Intent returnIntent = getIntent();
                returnIntent.putExtra("imageId", mediaID);
                returnIntent.putExtra("deleted", true);
                returnIntent.putExtra("position", position);
                //Log.d(TAG, "calling Activity media preview test : "+this.getClass().getSimpleName() + " and "+MediaPreview.class.getSimpleName());
                returnIntent.putExtra("callingActivity", this.getClass().getSimpleName());

                setResult(RESULT_OK, returnIntent);
                finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            /*if (isVimeoVideo) {
                if (vimeoPlayer.inCustomView()) {
                    vimeoPlayer.hideCustomView();
                }
            }*/
            finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void addLocalImage(FrameLayout mediaHolder) {
    	Log.d(TAG, "adding local image");
        // bitmap that will hold the image we will display
        Bitmap bmp;
        ImageView image = new ImageView(MediaPreview.this);
        image.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        // getting bitmap
        bmp = BitmapFactory.decodeFile(mediaURL);
        image.setImageBitmap(bmp);

        // Adding the image to the holder
        mediaHolder.addView(image);
        mediaProgress.setVisibility(View.GONE);

    }

    private void playLocalVideo(FrameLayout mediaHolder) {
        // Getting access to the mediacontroller
        videoController = new MediaController(this);
        videoControllerExists = true;

        // getting the height of the buttons
        //Resources r = getResources();
        /*bottomButtonHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 53, r.getDisplayMetrics());*/
        //Log.d(TAG, "VCJ playLocalVideo MediaPreview: bottomButton"+bottomButtonHeight+" r.getDisplayMetrics: "+r.getDisplayMetrics());
        if(android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN){
            videoController.setPadding(0, 0, 0, bottomButtonHeight);
        }

        // Setting the video view
        final VideoView video = new VideoView(MediaPreview.this);
        video.setVideoPath(mediaURL);
        video.setMediaController(videoController);
        //Log.d(TAG, "VCJ video rotation: " + video.getRotation());

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int h = displaymetrics.heightPixels;
        int w = displaymetrics.widthPixels;

        LayoutParams videoLayoutParams = new LayoutParams(w, h);
        videoLayoutParams.gravity = Gravity.CENTER;
        video.setLayoutParams(videoLayoutParams);

        // Add to holder
        mediaProgress.setVisibility(View.GONE);
        mediaHolder.addView(video);
        video.start();
        video.bringToFront();
        videoController.requestFocus();
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

/*
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isVimeoVideo) {
            vimeoPlayer.saveState(outState);
        }

    }*/

    @Override
    public void onResume(){
    	super.onResume();
    	Log.d(TAG, "onresume media preview");
    }
/*    @Override
    public void onStop() {
        super.onStop();
        if (isVimeoVideo) {
            vimeoPlayer.stopLoading();
        }
        if (isAmazon) {
            //s3VideoPlayer.stop();
            // s3VideoPlayer.reset();
            // s3VideoPlayer.release();
        }
        Log.d(TAG, "ONSTOP!!!!!!!! ");
    	Log.d(TAG, "onstop mediaRes = " + mediaRes+" onstop mediaSources = " + mediaSources);
    }*/


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            /*if (isVimeoVideo) {
                if (vimeoPlayer.inCustomView()) {
                    vimeoPlayer.hideCustomView();
                }
            }*/
            videoControllerExists = false;
            if (adapter != null && pager != null) {
                PreviewFragment fragment = (PreviewFragment) adapter.instantiateItem(pager, pager.getCurrentItem());
                if (fragment.playVideo != null) {
                    //Log.d(TAG, "VCJ attempting to cancel fragment");
                    fragment.playVideo.cancel(true);
                }
            }

            super.onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    public static void log(){
    	Log.d(TAG, "logging!!!!!!!! ");
    	Log.d(TAG, "mediaRes = " + mediaRes+" mediaSources = " + mediaSources);
    }

/*    @Override
    protected void onDestroy() {
        *//*if (s3VideoPlayer != null) {
            Log.d(TAG, "STOPPING S3 VIDEO");
            s3VideoPlayer.stop();
            s3VideoPlayer.release();
            super.onDestroy();
        }*//*
        super.onDestroy();
    }*/

    /**
     * Clears the list of image resources when StepDisplay is closed.
     * This method is called from StepDisplay onDestroy().
     */
    public static void clearImages() {
    	mediaRes.clear();
    	mediaSources.clear();
        Log.d(TAG, "mediaRes cleared and mediaSources cleared");
    }

}