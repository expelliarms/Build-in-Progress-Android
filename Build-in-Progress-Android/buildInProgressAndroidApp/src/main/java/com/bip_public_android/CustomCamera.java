package com.bip_public_android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.helperClass.CameraPreview;
import com.example.helperClass.Constant;
import com.example.helperClass.PictureHorizontalLayout;
import com.example.helperClass.Timer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * <code> public class extends Main implements SurfaceHolder.Callback </code>
 * <p>
 * Customized camera activity used to either select photos from the gallery or
 * take multiple photos to be uploaded to the projects.
 */
public class CustomCamera extends SherlockActivity {

    private static final String TAG = "CustomCamerActivity";
    protected static final int MEDIA_TYPE_IMAGE = 1;
    protected static final int MEDIA_TYPE_VIDEO = 2;
    protected static final int REQUEST_CODE_GALLERY = 10;
    protected static final int ACTION_TAKE_VIDEO = 20;

    // Orientation Listener to ensure that the pictures are saved
    // in the correct orientation
    private OrientationEventListener mOrientationEventListener;

    private int mOrientation = -1;

    // Orientation to be saved when the picture is being saved to a file
    private static final int ORIENTATION_PORTRAIT_NORMAL = 1;
    private static final int ORIENTATION_PORTRAIT_INVERTED = 2;
    private static final int ORIENTATION_LANDSCAPE_NORMAL = 3;
    private static final int ORIENTATION_LANDSCAPE_INVERTED = 4;

    // Camera stuff
    private Camera camera;
    /** Holder used to display the camera */
    private CameraPreview cameraPreview;
    /** Recorder for taking videos */
    private MediaRecorder recorder;
    /** Frame layout holding the camera's preview view */
    private FrameLayout previewHolder;
    // View to display time whenever the user is recording
    public TextView timerView;

    /** Button to take picture */
    private ImageButton shutter;
    /** Button to open the gallery */
    private ImageButton gallery;
    /** Button to toggle taking pictures and recording */
    private ImageButton switchCamera;
    // holder for buttons
    private LinearLayout buttonHolder;

    int navBarHeight = 0;
    /** ActionBarSherlockMenu */
    private Menu menu;
    private MenuItem saveMenuItem;

    /**
     * Boolean used to check if the camera is either taking a picture or
     * recording
     */
    private boolean inRecordMode = false;
    /** constant to check if the camera is in the preview mode */
    private boolean isRecording = false;
    /** layout to add the image horizontally */ 
    private PictureHorizontalLayout myHorizontalLayout;
    /**
     * Vibrator used to let the user know they switched between camera and video
     */
    private Vibrator vibrator;
    /** Uri used to let the media recorder know where to store the video */
    private Uri videoUri;
    /**
     * Click listener for the shutter of the activity, handles recording or
     * taking pictures *? private ShutterClick shutterClickListener = new
     * ShutterClick(); /** Handler used to display time on the timer view
     */
    private Handler handler = new Handler();
    /** Timer for the user to see amount of time recording */
    private Timer timer;

    /** Click listener to handle either recording or taking a picture */
    private ShutterClick shutterClickListener = new ShutterClick();

    /** Get Device manufacturer to set image rotation accordingly */
    String device = android.os.Build.MANUFACTURER;
    String model = android.os.Build.MODEL;
    int version = android.os.Build.VERSION.SDK_INT;
    int device_rotation = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        //getSupportActionBar().hide();
        
        
        Log.d(TAG, "device manufacturer: " + device);
        /*if (device.equals("HUAWEI") || device.equals("motorola") || model.contains("Nexus")) {
            //Log.d(TAG, "setting device rotation");
            device_rotation = 90;
        }*/
        Log.d(TAG, "default orientation = " + getResources().getConfiguration().orientation);
        super.onCreate(savedInstanceState);
        setContentView(com.bip_public_android.R.layout.activity_custom_camera);

        // add back button to action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setFormat(PixelFormat.UNKNOWN);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Setting up the preview to display the camera
        cameraPreview = new CameraPreview(CustomCamera.this);
        recorder = new MediaRecorder();

        setUpViews();

    }

    /**
     * Set up the views and click listeners for this activity
     */
    private void setUpViews() {
        
        // Getting views
        previewHolder = (FrameLayout) findViewById(com.bip_public_android.R.id.camerapreview);
        timerView = (TextView) findViewById(com.bip_public_android.R.id.camera_timer);
        gallery = (ImageButton) findViewById(com.bip_public_android.R.id.gallery_customized_camera);
        myHorizontalLayout = (PictureHorizontalLayout) findViewById(com.bip_public_android.R.id.mygallery);
        
        // put the views behind the navigation bar
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN){
            Log.d(TAG, "adjusting to account for navigation bar");
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            
            // apply margin to horizontal scroll view to take into account height of status bar
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            {
                // NOTE: fix the hardcoded margin scale factor (1.5)
                Resources resources = getApplication().getResources();
                int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    navBarHeight =  resources.getDimensionPixelSize(resourceId);
                    Log.d(TAG, "navBarHeight: " + navBarHeight);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) myHorizontalLayout.getLayoutParams();
                    params.setMargins(0, (int) Math.round(navBarHeight*1.5), 0, 0);
                    Log.d(TAG, "ADJUST MARGINS OF HORIZONTAL LAYOUT");
                    myHorizontalLayout.setLayoutParams(params);
                }
//                navBarHeight = (int) Math.round(TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics())*1.5);
//                Log.d(TAG, "navbarHeight: " + navBarHeight);
            }            
        }
        

        // Getting the buttons and overlaying them on top of the camera preview
        buttonHolder = (LinearLayout) findViewById(com.bip_public_android.R.id.button_holder_customized_cam);
        buttonHolder.bringToFront();
        // Horizontal scroll view to display images when they overflow from the
        // horizontal layout
        HorizontalScrollView pictureScroller = (HorizontalScrollView) findViewById(com.bip_public_android.R.id.gallery_scroll_view);
        // Moving scroller to the front in order for the images to be seen
        pictureScroller.bringToFront();

        // Adding the preview to the holder
        previewHolder.addView(cameraPreview);

        // Setting images from the intent
        if (getIntent().getStringArrayListExtra(Constant.IMAGE_INTENT) != null) {
            for (String path : getIntent().getStringArrayListExtra(
                    Constant.IMAGE_INTENT)) {
                myHorizontalLayout.add(Uri.parse(path));
            }
        }

        // Adding the gallery button functionality
        gallery.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Assuring that the app isn't recording
                if (!isRecording) {
                    Intent intent = new Intent(v.getContext(),
                            CustomizedGallery.class);
                    startActivityForResult(intent, REQUEST_CODE_GALLERY);
                }

            }
        });

        shutter = (ImageButton) findViewById(com.bip_public_android.R.id.shutter_customized_camera);

        /*
         * Setting custom on click listener for shutter in order for images to
         * be processed and added to the files before a new photo can be taken
         */
        shutter.setOnClickListener(shutterClickListener);

        switchCamera = (ImageButton) findViewById(com.bip_public_android.R.id.video_customized_camera);

        /*
         * On click listener for recording a video. Sends the activity into the
         * already installed camera with a custom file Uri for saving the video
         */
        switchCamera.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                vibrator.vibrate(100);
                // Can't switch while recording
                if (!isRecording) {
                    if (!inRecordMode) {
                        // Setting record mode to true so we know what state we
                        // are in
                        inRecordMode = true;
                        // Calling change rotation to swap the images
                        changeRotation(mOrientation, 0);
                    } else {
                        timerView.setVisibility(View.INVISIBLE);
                        inRecordMode = false;
                        // Calling change rotation to swap images
                        changeRotation(mOrientation, 0);
                        if (android.os.Build.VERSION.SDK_INT < 14) {
                            try {
                                camera.reconnect();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        });
        
    }

    /**
     * Method used to set the camera preview's parameters to match the phone's
     * width and set the height accordingly to assure that there are no aspect
     * ratio issues.
     */
    private void setHolderParameters() {
        //Log.d(TAG, "setting camera layout parameters");
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels+navBarHeight;
        //Log.d(TAG, "screen width and height: " + width + " " + height);
        
        Size mPreviewSize = CameraPreview.getOptimalPreviewSize(camera
                .getParameters().getSupportedPreviewSizes(), width, height);
        if(camera == null) {Log.d(TAG, "camera is null");}
        //Log.d(TAG, "optimal size: " + mPreviewSize.width + " " + mPreviewSize.height);
        
        // set size of frame layout
        LayoutParams previewParams = new LayoutParams(mPreviewSize.height, mPreviewSize.width);
        TypedValue tv = new TypedValue();
        getApplicationContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
        
        previewParams.setMargins(0, actionBarHeight, 0, -navBarHeight);
        
        previewHolder.setLayoutParams(previewParams);

        Camera.Parameters parameters = camera.getParameters();
        Log.d(TAG, "parameters = " + parameters.toString());
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.set("orientation", "portrait"); // default orientation to
                                                   // portrait
        
        camera.setParameters(parameters);
        Log.d(TAG, "setting display orientation");
        camera.setDisplayOrientation(90);
    }

    @Override
    protected void onPause() {

        // Assure we stop recording if the user was recording when the app
        // paused
        if (isRecording) {
            stopRecording();
        }

        super.onPause();
        // Release camera stuff
        releaseCameraAndPreview();
        releaseMediaRecorder();
        // Disable the orientation listener
        mOrientationEventListener.disable();
    }

    @Override
    protected void onStop() {
        if (isRecording) {
            stopRecording();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Opening the camera to preview it
        openCameraAsync();

        // Adding rotation listener to the activity to ensure that our pictures
        // are taken in the
        // correct orientation
        if (mOrientationEventListener == null) {
            mOrientationEventListener = new OrientationEventListener(this,
                    SensorManager.SENSOR_DELAY_NORMAL) {

                @Override
                public void onOrientationChanged(int orientation) {

                    // determine our orientation based on sensor response
                    int lastOrientation = mOrientation;
                    //if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (cameraPreview.getDeviceDefaultOrientation() == Configuration.ORIENTATION_PORTRAIT) {
                        if (orientation >= 315 || orientation < 45) {
                            if (mOrientation != ORIENTATION_PORTRAIT_NORMAL) {
                                Log.d(TAG, "portrait normal");
                                mOrientation = ORIENTATION_PORTRAIT_NORMAL;
                            }
                        } else if (orientation < 315 && orientation >= 225) {
                            if (mOrientation != ORIENTATION_LANDSCAPE_NORMAL) {
                                Log.d(TAG, "landscape normal");
                                mOrientation = ORIENTATION_LANDSCAPE_NORMAL;
                            }
                        } else if (orientation < 225 && orientation >= 135) {
                            if (mOrientation != ORIENTATION_PORTRAIT_INVERTED) {
                                Log.d(TAG, "portrait interverted");
                                mOrientation = ORIENTATION_PORTRAIT_INVERTED;
                            }
                        } else { // orientation <135 && orientation > 45
                            if (mOrientation != ORIENTATION_LANDSCAPE_INVERTED) {
                                Log.d(TAG, "landscape interverted");
                                mOrientation = ORIENTATION_LANDSCAPE_INVERTED;
                            }
                        }
                    } else {
                        if (orientation >= 315 || orientation < 45) {
                            if (mOrientation != ORIENTATION_LANDSCAPE_NORMAL) {
                                mOrientation = ORIENTATION_LANDSCAPE_NORMAL;
                            }
                        } else if (orientation < 315 && orientation >= 225) {
                            if (mOrientation != ORIENTATION_PORTRAIT_INVERTED) {
                                mOrientation = ORIENTATION_PORTRAIT_INVERTED;
                            }
                        } else if (orientation < 225 && orientation >= 135) {
                            if (mOrientation != ORIENTATION_LANDSCAPE_INVERTED) {
                                mOrientation = ORIENTATION_LANDSCAPE_INVERTED;
                            }
                        } else { // orientation <135 && orientation > 45
                            if (mOrientation != ORIENTATION_PORTRAIT_NORMAL) {
                                mOrientation = ORIENTATION_PORTRAIT_NORMAL;
                            }
                        }
                    }

                    if (lastOrientation != mOrientation) {
                        changeRotation(mOrientation, lastOrientation);
                    }
                    //Log.d(TAG, "mOrientation: "+mOrientation);
                }
            };
        }
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }

    }

    /**
     * Create a new file to store the image in the storage, based on the current
     * time the picture was taken in order to have a unique address for the
     * picture.
     * 
     * @param type
     *            the type of the file (i.e IMAGE, VIDEO)
     * @return the File for storage
     */
    private File getOutputMediaFile(int type) {
        String imageDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getPath()
                + "/Build in Progress";
        File mediaStorageDir = new File(imageDirectory);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CustomizedCam", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("MM-dd-yyyy-HH-mm-ss",
                Locale.getDefault()).format(new Date());
        File mediaFile = null;
        if (type == MEDIA_TYPE_IMAGE) {
            // Creating the jpg image to be saved under the customizedcamera
            // folder
            mediaFile = new File(mediaStorageDir.getPath(), "IMG_" + timeStamp
                    + ".jpg");
            Log.d(TAG, "adding image to MediaStore");
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mediaFile)));
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath(), "VID_" + timeStamp
                    + ".mp4");
            videoUri = Uri.parse(mediaFile.toString());
            Log.d(TAG, "adding image to MediaStore");
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mediaFile)));
        }

        return mediaFile;
    }

    /**
     * Call back to take action when picture is taken. Gets the picture's file
     * address based on the time the picture was taken and writes it to the
     * CustomCamera folder under pictures.
     */
    private PictureCallback myPictureCallback_JPG = new PictureCallback() {
    	
        @Override
        public void onPictureTaken(byte[] data, Camera cam) {
            Log.d(TAG, "saving picture");
            Log.d(TAG, "device = " + device + ", defaultorientation = " + getResources().getConfiguration().orientation + ", version = " + version);
            Log.d(TAG,
                    "portrait image? "
                            + Boolean.toString(cameraPreview
                                    .getDeviceDefaultOrientation() == Configuration.ORIENTATION_PORTRAIT));

            // Degrees to rotate image by when saving
            int degree = 0;

            // do not rotate image, just put rotation info in
            if (cameraPreview.getDeviceDefaultOrientation() == Configuration.ORIENTATION_PORTRAIT) {
                Log.d(TAG, "default orientation is portrait");
                switch (mOrientation) {
                case ORIENTATION_LANDSCAPE_INVERTED:
                    Log.d(TAG, "orientation landscape inverted");
                    degree = 90;
                    break;
                case ORIENTATION_PORTRAIT_NORMAL:
                    Log.d(TAG, "orientation portrait normal");
                    degree = 0;
                    break;
                case ORIENTATION_LANDSCAPE_NORMAL:
                    Log.d(TAG, "orientation landscape normal");
                    degree = 270;
                    break;
                case ORIENTATION_PORTRAIT_INVERTED:
                    Log.d(TAG, "orientation portrait inverted");
                    degree = 180;
                    break;
                }
            } else {
                Log.d(TAG, "default orientation is landscape");
                switch (mOrientation) {
                case ORIENTATION_LANDSCAPE_INVERTED:
                    degree = 180;
                	break;
                case ORIENTATION_PORTRAIT_NORMAL:
                    degree = 270;
                	break;
                case ORIENTATION_LANDSCAPE_NORMAL:
                    degree = 0;
                    break;
                case ORIENTATION_PORTRAIT_INVERTED:
                    degree = 90;
                    break;
                }
            }

            // apply rotation offset if using Huawei phone
            degree = degree - device_rotation;

            Bitmap bMap;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 6;
            options.inDither = false; // Disable Dithering mode
            options.inPurgeable = true; // Tell to gc that whether it needs free
            // memory, the Bitmap can be cleared
            options.inInputShareable = true; // Which kind of reference will be
            // used to recover the Bitmap
            // data after being clear, when
            // it will be used in the future
            options.inTempStorage = new byte[32 * 1024];
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            bMap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            Log.d(TAG, "check and rotate, degree = " + degree);
        	Log.d(TAG, "bMap width = " + bMap.getWidth() + ", height = " + bMap.getHeight());
        	
        	// if image rotated 90 degrees, rotate back 90 degrees
        	if (bMap.getWidth() >= bMap.getHeight()){
        		degree += 90;
        	}
        	
            if (degree != 0) {
            	Log.d(TAG, "rotating by " + degree + " degrees");
                bMap = rotate(bMap, degree);
            }

            // Getting the picture's unique file to be added to the folder
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            
          

            /*
             * This catches errors when creating the output file to put the
             * picture in
             */
            if (pictureFile == null) {
                Log.d(TAG,
                        "Error creating media file, check storage permissions");

                // After the camera has finished taking the picture
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        // Remove white border from preview
                        cameraPreview.setBackgroundResource(0);
                    }

                });
                // Setting the onclicklistener back on the shutter
                shutter.setOnClickListener(shutterClickListener);

                // Restarting the preview as soon as picture is done
                camera.startPreview();

                Toast.makeText(getApplicationContext(),
                        "Error saving file. Try restarting the camera.",
                        Toast.LENGTH_LONG).show();

                return;
            }

            FileOutputStream out = null;
            try {
                // Saving the image
                out = new FileOutputStream(pictureFile);
                bMap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                if (bMap != null) {
                    bMap.recycle();
                    bMap = null;
                    Log.d(TAG, "SAVED IMAGE");
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    // Assuring we always close the output stream
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "new image path: " + pictureFile.getPath());
            
            /*try{
            	ExifInterface exif = new ExifInterface(pictureFile.getPath());
            	Log.d(TAG, "tag orientation = " + Integer.toString(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)));
            } catch (Exception e){
            	
            }*/
            
            myHorizontalLayout.add(Uri.parse(pictureFile.getPath()));
            MenuItem check = menu.findItem(com.bip_public_android.R.id.save);
            check.setIcon(com.bip_public_android.R.drawable.ic_action_accept_highlight_3);

            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[] { Environment.getExternalStorageDirectory()
                            .toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);

                            camera.startPreview();

                            // After the camera has finished taking the picture
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Remove white border from preview
                                    Log.d(TAG, "removing white background");
                                    cameraPreview.setBackgroundResource(0);
                                    
                                    // highlight check
                                    MenuItem check = menu.findItem(com.bip_public_android.R.id.save);
                                    check.setIcon(com.bip_public_android.R.drawable.ic_action_accept_highlight_3);
                                }
                            });
                            // Setting the onclicklistener back on the shutter
                            shutter.setOnClickListener(shutterClickListener);
                        }
                    });

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Don't care if the result code is cancel
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        switch (requestCode) {
        case REQUEST_CODE_GALLERY:

            // Add all the images from the gallery to the horizontal picture
            // layout
            if (intent.getStringArrayListExtra(Constant.GALLERY_INTENT) != null) {
                ArrayList<String> imagePath = intent
                        .getStringArrayListExtra(Constant.GALLERY_INTENT);
                for (String i : imagePath) {
                    myHorizontalLayout.add(Uri.parse(i));
                    MenuItem check = menu.findItem(com.bip_public_android.R.id.save);
                    check.setIcon(com.bip_public_android.R.drawable.ic_action_accept_highlight_3);
                }
            }
            break;

        // Remove items that have been deleted by the preview
        case Constant.REQUEST_CODE_PREVIEW:
            if (intent.getBooleanExtra("deleted", false)) {
                int position = intent.getIntExtra("position", 0);
                Log.d(TAG, "VCJ CustomCamera Delete at position: "+position);
                myHorizontalLayout.removeViewAt(position);
            }
            // If they removed all of the items set the check back to grey
            if (myHorizontalLayout.getItemList().size() == 0) {
                MenuItem check = menu.findItem(com.bip_public_android.R.id.save);
                check.setIcon(android.R.color.transparent);
            }
            break;
        }

    }

    /**
     * OnClickListener for the camera's shutter button. The shutterclicklistener
     * assures that the camera will take pictures or record videos depending on
     * what mode the user is currently on.
     * 
     * @author jan
     * 
     */
    private class ShutterClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Shutter clicked");
            if (!inRecordMode) {
                shutter.setOnClickListener(null);

                // Adding white border to the preview
                cameraPreview.setBackgroundResource(com.bip_public_android.R.drawable.white_border_1);

                // Take picture
                camera.takePicture(null, null, myPictureCallback_JPG);
            } else {
                if (isRecording) {
                    // stop recording
                    stopRecording();
                } else {
                    // begin recording
                    startRecording();
                }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(Menu.NONE, com.bip_public_android.R.id.save, Menu.NONE, "save")
                .setIcon(android.R.color.transparent)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        saveMenuItem = menu.findItem(com.bip_public_android.R.id.save);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case com.bip_public_android.R.id.save:
            if (myHorizontalLayout.getItemList().size() > 0) {
                Intent i = getIntent();
                i.putExtra("projectID", getIntent()
                        .getIntExtra("projectID", -1));
                i.putExtra("projectTitle",
                        getIntent().getStringExtra("projectTitle"));
                i.putStringArrayListExtra(Constant.IMAGE_INTENT,
                        myHorizontalLayout.getItemList());
                setResult(RESULT_OK, i);
                finish();
            }
            break;

        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCameraAndPreview() {
        cameraPreview.setCamera(null);
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    /**
     * Sets the images in the buttons in the correct orientation based on how
     * the user has the device rotated
     * 
     * @param orientation
     * @param lastOrientation
     */
    private void changeRotation(int orientation, int lastOrientation) {
        if (!inRecordMode) {
            switch (orientation) {
            case ORIENTATION_LANDSCAPE_INVERTED:
                shutter.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.device_access_camera, 270));
                gallery.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.content_picture, 270));
                switchCamera.setImageDrawable(getRotatedImage(com.bip_public_android.R.drawable.video,
                        270));
                break;
            case ORIENTATION_PORTRAIT_NORMAL:
                shutter.setImageResource(com.bip_public_android.R.drawable.device_access_camera);
                gallery.setImageResource(com.bip_public_android.R.drawable.content_picture);
                switchCamera.setImageResource(com.bip_public_android.R.drawable.video);
                break;
            case ORIENTATION_LANDSCAPE_NORMAL:
                shutter.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.device_access_camera, 90));
                gallery.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.content_picture, 90));
                switchCamera.setImageDrawable(getRotatedImage(com.bip_public_android.R.drawable.video,
                        90));
                break;
            case ORIENTATION_PORTRAIT_INVERTED:
                shutter.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.device_access_camera, 180));
                gallery.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.content_picture, 180));
                switchCamera.setImageDrawable(getRotatedImage(com.bip_public_android.R.drawable.video,
                        180));
                break;
            }
        } else {
            switch (orientation) {
            case ORIENTATION_LANDSCAPE_INVERTED:
                shutter.setImageDrawable(getRotatedImage(com.bip_public_android.R.drawable.video, 270));
                gallery.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.content_picture, 270));
                switchCamera.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.device_access_camera, 270));
                timerView.setRotation(270);
                break;
            case ORIENTATION_PORTRAIT_NORMAL:
                shutter.setImageResource(com.bip_public_android.R.drawable.video);
                gallery.setImageResource(com.bip_public_android.R.drawable.content_picture);
                switchCamera.setImageResource(com.bip_public_android.R.drawable.device_access_camera);
                timerView.setRotation(0);
                break;
            case ORIENTATION_LANDSCAPE_NORMAL:
                shutter.setImageDrawable(getRotatedImage(com.bip_public_android.R.drawable.video, 90));
                gallery.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.content_picture, 90));
                switchCamera.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.device_access_camera, 90));
                timerView.setRotation(90);
                break;
            case ORIENTATION_PORTRAIT_INVERTED:
                shutter.setImageDrawable(getRotatedImage(com.bip_public_android.R.drawable.video, 180));
                gallery.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.content_picture, 180));
                switchCamera.setImageDrawable(getRotatedImage(
                        com.bip_public_android.R.drawable.device_access_camera, 180));
                timerView.setRotation(180);
                break;
            default:

            }
        }
    }

    /**
     * Rotates given Drawable
     * 
     * @param drawableId
     *            Drawable Id to rotate
     * @param degrees
     *            Rotate drawable by Degrees
     * @return Rotated Drawable
     */
    private Drawable getRotatedImage(int drawableId, int degrees) {
        Bitmap original = BitmapFactory.decodeResource(getResources(),
                drawableId);
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        
        Bitmap rotated = Bitmap.createBitmap(original, 0, 0,
                original.getWidth(), original.getHeight(), matrix, true);
        
        
        return new BitmapDrawable(CustomCamera.this.getResources(), rotated);
    }

    /**
     * Method used to rotate a bitmap by a number of degrees
     * 
     * @param bitmap
     *            to rotate
     * @param degree
     *            number of degrees to rotate image by
     * @return rotated bitmap
     */
    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    /**
     * Method used to initialize the recorder. Correctly sets up the camera
     * specifications in the correct order.
     * 
     * @return true if everything set up correctly, false otherwise
     */
    private boolean prepareVideoRecorder() {
    	Log.d(TAG, "in prepareVideoRecorder()");
        // It is very important to unlock the camera before doing setCamera
        // or it will results in a black preview
        if (camera == null) {
            camera = getCameraInstance();
        }

        if (recorder == null) {
            recorder = new MediaRecorder();
        }

        // Have to stop preview before starting to record
        camera.stopPreview();
        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        recorder.setCamera(camera);

        // Step 2: Set sources
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        recorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO)
                .getAbsolutePath());

        // No limit. Don't forget to check the space on disk.
        recorder.setMaxDuration(50000);
        recorder.setVideoFrameRate(30);
        recorder.setVideoEncodingBitRate(3000000);
        recorder.setAudioEncodingBitRate(8000);

        // Step 5: Set the preview output
        recorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());

        // Setting the camera's orientation
        int degree = 0;
        // do not rotate image, just put rotation info in
        switch (mOrientation) {
        case ORIENTATION_LANDSCAPE_INVERTED:
            degree = 180;
            break;
        case ORIENTATION_PORTRAIT_NORMAL:
            degree = 90;
            break;
        case ORIENTATION_LANDSCAPE_NORMAL:
            degree = 0;
            break;
        case ORIENTATION_PORTRAIT_INVERTED:
            degree = 270;
            break;
        }

        recorder.setOrientationHint(degree);

        // Step 6: Prepare configured MediaRecorder
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            // This is thrown if the previous calls are not called with the
            // proper order
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            e.printStackTrace();
            return false;
        }
        // Everything went successfully
        return true;
    }

    /**
     * Open the camera asynchronously to reduce the lag when opening activity
     */
    public void openCameraAsync() {
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... arg0) {
                if (!isFinishing()) {
                    // Resuming camera and display when resuming
                    if (camera == null) {
                        Log.d(TAG, "Resuming with a null camera");
                        camera = getCameraInstance();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                setHolderParameters();
                cameraPreview.setCamera(camera);

                // Calling surface created so that the preview of the camera is
                // correct
                cameraPreview.surfaceCreated(cameraPreview.getHolder());
            }
        }.execute();
    }

    private boolean releaseMediaRecorder() {
        if (recorder != null) {
            recorder.reset(); // clear recorder configuration
            recorder.release(); // release the recorder object
            recorder = null;
            if (camera != null) {
                camera.lock(); // lock camera for later use
                return true;
            }
            return false;
        }
        return false;
    }

    private void startRecording() {

        // initialize video camera
        if (prepareVideoRecorder()) {
            // Assuring the app doesn't go to sleep while recording
            timerView.setKeepScreenOn(true);

            // "Adding" timer whenever we start to record
            timerView.setVisibility(View.VISIBLE);

            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            recorder.start();

            // inform the user that recording has started
            shutter.setBackgroundResource(com.bip_public_android.R.drawable.custom_button_red);
            isRecording = true;

            // TODO:Testing timer stuff
            startTimer();
        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // inform user
            Toast.makeText(getApplicationContext(),
                    "Error preparing camera for recording", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void stopRecording() {
        // Allowing the phone to go to sleep while not recording
        timerView.setKeepScreenOn(false);

        isRecording = false;
        stopTimer();
        // stop recording and release camera
        recorder.stop(); // stop the recording
        releaseMediaRecorder(); // release the MediaRecorder object
        if (android.os.Build.VERSION.SDK_INT < 14) {
            camera.lock(); // take camera access back from MediaRecorder
            camera.startPreview();
        }

        if (videoUri != null) {
            myHorizontalLayout.add(videoUri);
            MenuItem check = menu.findItem(com.bip_public_android.R.id.save);
            check.setIcon(com.bip_public_android.R.drawable.ic_action_accept_highlight_3);
        } else {
            Log.d(TAG, "error saving video");
        }
        // inform the user that recording has stopped
        shutter.setBackgroundResource(com.bip_public_android.R.drawable.custom_button_blue);

        // Making the timer invisible again
        timerView.setVisibility(View.INVISIBLE);
    }

    private void startTimer() {
        timer = new Timer(timerView);
        timer.startTimer();
    }

    private void stopTimer() {
        timer.stopTimer();
    }

    @Override
    public void onBackPressed() {
        // Can't exit the activity while recording
        if (!isRecording) {
            super.onBackPressed();
        }
    }
    
}
