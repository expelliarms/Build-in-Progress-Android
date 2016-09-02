package com.bip_public_android;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.helperClass.Constant;
import com.example.helperClass.Method;
import com.example.helperClass.Method.DeleteStepTask;
import com.example.helperClass.Method.GetImageFromURL;
import com.example.helperClass.Method.UpdateStepNameTask;
import com.example.helperClass.Method.UploadImage;
import com.example.helperClass.Method.UploadVideo;
import com.example.helperClass.Multimedia;
import com.example.helperClass.PictureHorizontalLayout;
import com.savagelook.android.UrlJsonAsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StepDisplay extends SherlockFragmentActivity {
	
	// for SwipeView of image gallery
	private HorizontalScrollView horizontalScroll;
	
	//private ViewPager mPager;
	//private static HashMap<Integer, String> mediaURLs = new HashMap<Integer, String>();

    private String stepURL; 
    private String stepURLjson;

    private String auth_token;
    private int userID;

    // Boolean to tell the project activity if the step was updated
    private boolean changed = false;
    private boolean changedImages = false;
    private boolean updatedDate = false;
    private static boolean alreadyLoaded = false;

    // Previous and next steps

    // Information about the step
    private int projectID;
    private String stepDescription = "null";
    private static int numMedia;
    private int stepID;


    private final int PREVIEW_IMAGE_HEIGHT = 285; // height of preview-sized
                                                  // image from website
    private final int PREVIEW_IMAGE_WIDTH = 380;

    private final static String stepDescriptionHelpText = "Type a description of your step here!";

    // Views
    private LinearLayout stepImages;
    private ViewSwitcher stepNameSwitcher;
    private ViewSwitcher stepDescriptionSwitcher;
    private EditText stepNameEditText;
    private EditText stepDescriptionEditText;
    private CheckBox lastStepCheckBox;
    private TextView stepDescriptionView;
    private TextView stepName;
    private Menu editMenu;

    private boolean editMode = false;
    private boolean editTextClicked = false;
    private boolean backPressedFromActionBar = false;
    
    private boolean fromProjects = false;

    private final static String DEFAULT_DESCRIPTION = "";

    private final static String TAG = "StepDisplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!Method.isNetworkAvailable(this)) {
            // Displaying a nicer connection error dialog
            Toast.makeText(
                    this,
                    "No internet connection available. Check your connection settings",
                    Toast.LENGTH_LONG).show();
            Intent noInternet = new Intent(this, AllProjectDisplay.class);
            noInternet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(noInternet);
            finish();
        }
        setContentView(com.bip_public_android.R.layout.activity_step_display);
        
        horizontalScroll = (HorizontalScrollView) findViewById(com.bip_public_android.R.id.step_image_horizontal_scroll);
        //urls = new ArrayList<String>();
        
        // Getting info about step
        projectID = getIntent().getIntExtra("projectID", -1);
        fromProjects = getIntent().getBooleanExtra("fromProjects", false);

        //Log.d(TAG, "VCJ!  position " + ProjectDisplay.currentlySelected.getPosition());
        int stepPosition = ProjectDisplay.currentlySelected.getPosition();
        stepID = ProjectDisplay.currentlySelected.getId();
        stepURL = Constant.PROJECT_URL + projectID + "/steps/" + stepPosition;
        stepURLjson = stepURL + ".json";
        stepID = ProjectDisplay.currentlySelected.getId();

        numMedia = 0;

        // set action bar title to step name
        getSupportActionBar().setTitle(
                ProjectDisplay.currentlySelected.getName());

        // add back button to action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setUpViews();

    }

	private void setUpViews() {
        // Getting preferences and auth_token
        SharedPreferences preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
        auth_token = preferences.getString("AuthToken", "");
        userID = preferences.getInt("UserID", -1);

        stepImages = (LinearLayout) findViewById(com.bip_public_android.R.id.step_image_holder);
        stepNameSwitcher = (ViewSwitcher) findViewById(com.bip_public_android.R.id.step_name_switcher);
        stepDescriptionSwitcher = (ViewSwitcher) findViewById(com.bip_public_android.R.id.step_description_switcher);
        stepNameEditText = (EditText) stepNameSwitcher
                .findViewById(com.bip_public_android.R.id.step_name_edit_view);
        stepDescriptionEditText = (EditText) stepDescriptionSwitcher
                .findViewById(com.bip_public_android.R.id.step_description_edit_view);
//        stepPublishedDate = (TextView) findViewById(R.id.step_date_published);
        stepName = (TextView) findViewById(com.bip_public_android.R.id.step_name);
        Button addImageButton = (Button) findViewById(com.bip_public_android.R.id.step_display_add_photo);
        Button deleteStepButton = (Button) findViewById(com.bip_public_android.R.id.delete_step_button);

        // set step name
        stepName.setText(ProjectDisplay.currentlySelected.getName());

        // hide title from toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // delete step when user clicks "delete step" button
        deleteStepButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (Method.isNetworkAvailable(getApplicationContext())) {
                    new AlertDialog.Builder(StepDisplay.this)
                    	.setTitle("Delete Step")
                        .setMessage(
                        		"Are you sure you want to delete step "
                                 + ProjectDisplay.currentlySelected
                                 .getName() + "?")
                        .setPositiveButton("Yes",
                                 new DialogInterface.OnClickListener() {

                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                        deleteStepFromAPI(stepURL + "?auth_token=" + auth_token+"&step_id=" + stepID);
                                 }
                        }).setNegativeButton("Cancel", null).show();
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "No internet connection available. Check your connection settings",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        stepName.setOnClickListener(new editStepModeListener());

        // step description clicked - toggle to edit mode
        stepDescriptionSwitcher.setOnClickListener(new editStepModeListener());

        addImageButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	Log.d(TAG, "step display upload");
                MediaPreview.log();
                PackageManager pm = getApplicationContext().getPackageManager();

                if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                    Intent i = new Intent(StepDisplay.this, CustomCamera.class);
                    startActivityForResult(i, Constant.REQUEST_CODE_ADD_PIC);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Your device does not appear to have a camera",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onBackPressed() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(stepNameEditText.getWindowToken(), 0);

        // check if user is pressing back from edit
        if (!backPressedFromActionBar && editMode) {
            stepNameSwitcher.setDisplayedChild(0);
            stepDescriptionSwitcher.setDisplayedChild(0);

            // cycle edit icon
            MenuItem editMenuItem = editMenu.findItem(com.bip_public_android.R.id.edit);
            onOptionsItemSelected(editMenuItem);

        } else {
            Intent returnIntent = getIntent();
            returnIntent.putExtra("changed_images", changedImages);
            returnIntent.putExtra("updatedDate", updatedDate);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
        
        MediaPreview.clearImages();
        Log.d(TAG, "images cleared on back pressed");

    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(TAG, "step display onstop");
    }
    @Override
    public void onResume() {
        super.onResume();        
        // scroll to the first newly uploaded image on resume
        /*horizontalScroll.postDelayed(new Runnable() {
            public void run() {
                horizontalScroll.scrollTo(numMedia * PREVIEW_IMAGE_WIDTH, 0);
            }
        }, 100L);*/

        if (!alreadyLoaded || fromProjects) {
            Log.d(TAG, "loading step from api from onresume");
            loadStepFromAPI(stepURLjson+ "?auth_token=" + auth_token);
            fromProjects = false;
        }
    }

    public static void setAlreadyLoaded(boolean value) {
    	alreadyLoaded = value;
    }
    
    // Load project steps from build in progress website
    public void loadStepFromAPI(String url) {
        if (Method.isNetworkAvailable(getApplicationContext())) {
            // vcj : getStepTask adds the urls, so you should clear the ones you have now it will add the new ones
            Log.d(TAG, "Reloading the step. multimedia size :"+ProjectDisplay.currentlySelected.getMultimediaList().size());
            GetStepTask getProjectStepsTask = new GetStepTask(StepDisplay.this);
            getProjectStepsTask.setMessageLoading("Loading step..");
            getProjectStepsTask.execute(url);
            MediaPreview.clearImages();
        } else {
            Toast.makeText(getApplicationContext(),
                    "No internet connection. Check connection settings",
                    Toast.LENGTH_LONG).show();
        }
    }

    // run delete step task to delete step from build in progress website
    private void deleteStepFromAPI(String url) {
        Log.d(TAG, "delete url: "+url);
        new DeleteStepTask().execute(url);
        Intent intent = getIntent();
        intent.putExtra("deleted", true);
        intent.putExtra("updatedDate", true);
        setResult(RESULT_OK, intent);
        finish();
    }


    // fetch step from project page
    public class GetStepTask extends UrlJsonAsyncTask {

        private ProgressDialog dialog;

        public GetStepTask(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(StepDisplay.this);
            dialog.setMessage("Loading step..");
            dialog.show();
            dialog.setCanceledOnTouchOutside(false);
        }

        @Override
        protected void onPostExecute(JSONObject json) {

            boolean isRemix;

            try {
                Log.d(TAG, "OnPostExecute: fetching step assets");
                JSONObject jsonStep = json.getJSONObject("data").getJSONObject(
                        "step");
                Log.d(TAG,"JSON : " + jsonStep);

                isRemix = jsonStep.getBoolean("remix?");

                String getJsonImagePath;
                String getJsonVideoPath;
                if (isRemix) { // if it is a remixed project we need to get the
                    // remixed images
                    getJsonImagePath = "remix_image_path";
                    getJsonVideoPath = "remix_video_path";
                } else { // we get the normal image and video paths
                    getJsonImagePath = "image_path";
                    getJsonVideoPath = "video";
                }

                stepDescription = jsonStep.getString("description");

                JSONArray jsonArray = jsonStep.getJSONArray("images");
                // List of multimedia to add to a step
                ArrayList<Multimedia> multList = new ArrayList<Multimedia>();

                if (jsonArray != null) {
                    int len = jsonArray.length();
                    for (int j = 0; j < len; j++) {

                        JSONObject multObj = jsonArray.getJSONObject(j);

                        int imagePosition = multObj.getInt("position");

                        String previewUrl = multObj
                                .getJSONObject(getJsonImagePath)
                                .getJSONObject("preview").getString("url");
                        String thumbUrl = multObj
                                .getJSONObject(getJsonImagePath)
                                .getJSONObject("thumb").getString("url");
                        String imgUrl = multObj.getJSONObject(getJsonImagePath)
                                .getString("url");
                        String sqThumbUrl = multObj
                                .getJSONObject(getJsonImagePath)
                                .getJSONObject("square_thumb").getString("url");


                        int imageId = multObj.getInt("id");
                        //urls.add(imgUrl);

                        boolean hasVideo = true;
                        boolean isFromEmbeddedVideo = false;
                        String videoUrl = null;
                        int videoRotation = 0; // default to 0

                        try {
                            JSONObject videoObj = multObj
                                    .getJSONObject(getJsonVideoPath);

                            // If the video has an embedded video path
                            isFromEmbeddedVideo = videoObj.getString(
                                    "embed_url").length() > 4;


                            Log.d(TAG, "video JSON: " + videoObj.toString()+" is from embedded video: "+isFromEmbeddedVideo+" embed url : "+videoObj.getString("embed_url"));

                            if (isFromEmbeddedVideo) {
                                videoUrl = videoObj.getString("embed_url");
                                //Log.d(TAG, "VCJ what is an embed_url: " +videoObj.getString("embed_url"));
                            } else {
                                //Log.d(TAG, "VCJ is remix : "+isRemix);
                                if (isRemix) {
                                    videoRotation = videoObj.getJSONObject("video_path").getInt("rotation");
                                } else {
                                    videoRotation = videoObj.getInt("rotation");
                                }
                                //Log.d(TAG, "VCJ what is rotation: " + videoRotation);
                                videoUrl = videoObj.getJSONObject("video_path").getString("url");
                                //Log.d(TAG, "VCJ what is a regular path: " +videoUrl);
                            }

                        } catch (JSONException e) {
                            // If we have exception, the image had no video
                            // associated to it
                            //Log.d(TAG, "VCJ video exception: "+e.getMessage());
                            hasVideo = false;
                        }

                        Multimedia stepMedia;
                        // If the image has a video associated with it

                        if (hasVideo) {
                            // If the video is embedded from youtube/vimeo
                            if (isFromEmbeddedVideo) {
                                stepMedia = new Multimedia(imageId,
                                        imagePosition, imgUrl, previewUrl,
                                        thumbUrl, sqThumbUrl, videoUrl);
                            }// The video is located on the amazon server
                            else {
                                stepMedia = new Multimedia(imageId,
                                        imagePosition, imgUrl, previewUrl,
                                        thumbUrl, sqThumbUrl, videoUrl, videoRotation);
                            }
                        } else {
                            // Create the video using only the image constructor
                            stepMedia = new Multimedia(imageId, imagePosition,
                                    imgUrl, previewUrl, thumbUrl, sqThumbUrl);
                        }

                        // Adding media to the entire list of step's multimedea
                        multList.add(stepMedia);
                        if (multList.size() > numMedia) {
                            numMedia = multList.size();
                        }
                    }
                }
                ProjectDisplay.currentlySelected.setMultimediaList(multList);

                // Setting step description
                ProjectDisplay.currentlySelected
                        .setDescription(stepDescription);

                //add the step description to the textview
                stepDescriptionView = (TextView) findViewById(com.bip_public_android.R.id.step_description);

                Log.w(TAG, "stepDescription: " + stepDescription);
                Log.w(TAG,
                        "empty stepDescription? : " + stepDescription.isEmpty());

                if (stepDescriptionView != null) {
                    if (stepDescription.equals("null") || stepDescription.isEmpty()) {
                        stepDescriptionView.setText(stepDescriptionHelpText);
                        // show the default help description text
                    } else {
                        stepDescriptionView.setText(Html
                                .fromHtml(stepDescription));
                    }
                }

            } catch (Exception e) {
                //Log.d(TAG, "Try failed. json not found : " + e.getMessage());
            } finally {

                // can't call this code if there is no json return
                super.onPostExecute(json);
                //Log.d(TAG, "VCJ onPostExecute finally entered");

                // Sets the boolean to true so that we don't reload page again
                alreadyLoaded = true;

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                // Adding images to the step
                stepNameSwitcher.setDisplayedChild(0);

                stepDescriptionSwitcher.setDisplayedChild(0);

                // Removing all views from the step images to not duplicate
                // images
                stepImages.removeAllViews();

                if (ProjectDisplay.currentlySelected.getMultimediaList().size() > 0) {
                    Log.d(TAG, "more than one existing images: size: "+ProjectDisplay.currentlySelected.getMultimediaList().size());
                    ArrayList<Multimedia> stepMedia = ProjectDisplay.currentlySelected
                            .getMultimediaList();
                    int totalImages = stepMedia.size();
                    View[] sortedImageViews = new View[totalImages];
                    int count =0;
                    for (Multimedia multimedia : stepMedia) {
                        // Adding the preview image as the path
                        String path = multimedia.getPreviewPath();

                        View displayView;
                        if (multimedia.isFromVideo()) {
                            LayoutInflater inflater = (LayoutInflater) StepDisplay.this
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            // If we have a video set to be the video preview
                            // layout
                            displayView = inflater.inflate(
                                    com.bip_public_android.R.layout.video_preview, null);
                            ImageView video_img = (ImageView) displayView
                                    .findViewById(com.bip_public_android.R.id.video_image_preview);
                            new GetImageFromURL(video_img, StepDisplay.this,
                                    null).execute(path);
                        } else {
                            displayView = new ImageView(StepDisplay.this);
                            new GetImageFromURL((ImageView) displayView,
                                    StepDisplay.this, null).execute(path);
                        }

                        LayoutParams imageParams = new LayoutParams(
                                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT);
                        displayView.setLayoutParams(imageParams);
                        // Setting id of image as tag
                        displayView.setTag(multimedia);
                        displayView.setPadding(4, 4, 4, 4);
                        sortedImageViews[count] = displayView;
                        count++;
                        //sortedImageViews[imagePos] = displayView;

                    }
                    for (final View image : sortedImageViews) {
                        stepImages.addView(image);
                        image.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.d(TAG, "creating the Media preview: " + ProjectDisplay.currentlySelected.getMultimediaList().size());
                                Intent intent = new Intent(StepDisplay.this, MediaPreview.class);
                                Multimedia tag = (Multimedia) image.getTag();
                                intent.putExtra("position", (Integer) tag.getPosition());
                                intent.putExtra("imageId", (Integer) tag.getId());
                                intent.putExtra("imageURL", tag.getPath());
                                intent.putExtra("isFromVideo", (Boolean) tag.isFromVideo());
                                //Log.d(TAG, "VCJ STEP DISPLAY setting calling activity: "+StepDisplay.class.getSimpleName());
                                intent.putExtra("callingActivity", StepDisplay.class.getSimpleName());
                                if (tag.isFromVideo()) {
                                    String video_url = tag.getVideoPath();
                                    intent.putExtra("videoURL", video_url);
                                    intent.putExtra("videoRotation", (Integer) tag.getVideoRotation());
                                }

                                startActivityForResult(intent,
                                        Constant.REQUEST_CODE_PREVIEW);
                            }
                        });
                    }
                }

                // Adding extra images that have not uploaded to website
                if (ProjectDisplay.currentlySelected.getCurrentlyUploading() != null) {
                    Log.d(TAG, "adding extra images that have not been uploaded to the website..");
                    for (final String localpath : ProjectDisplay.currentlySelected
                            .getCurrentlyUploading()) {
                        Bitmap bmp;
                        if (localpath.endsWith("jpg")
                                || localpath.endsWith("jpeg")
                                || localpath.endsWith("png")
                                || localpath.endsWith("gif")) {
                            bmp = PictureHorizontalLayout.resizeBitmap(
                                    BitmapFactory.decodeFile(localpath),
                                    PREVIEW_IMAGE_HEIGHT);
                        } else {
                            bmp = ThumbnailUtils.createVideoThumbnail(
                                    localpath,
                                    MediaStore.Images.Thumbnails.MINI_KIND);
                        }

                        final ImageView image = new ImageView(StepDisplay.this);
                        image.setImageBitmap(bmp);
                        image.setPadding(4, 4, 4, 4);

                        image.setLayoutParams(new LayoutParams(
                                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT));
                        image.setAlpha(Constant.UPLOADING_ALPHA);
                        image.setScaleType(ScaleType.FIT_XY);
                        stepImages.addView(image);

                    }
                    //Log.d(TAG, "VCJ step images (horizontal view) children after if: "+stepImages.getChildCount());
                }

                // end of finally block
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(Menu.NONE, com.bip_public_android.R.id.refresh, Menu.NONE, "refresh")
                .setIcon(com.bip_public_android.R.drawable.ic_action_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(Menu.NONE, com.bip_public_android.R.id.edit, Menu.NONE, "edit")
                .setIcon(com.bip_public_android.R.drawable.ic_action_edit).setTitle("edit")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        this.editMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getTitle() == "edit") {
            editMode = false;
        }
        // Handle item selection
        switch (item.getItemId()) {
        case com.bip_public_android.R.id.refresh:
            loadStepFromAPI(stepURLjson + "?auth_token=" + auth_token);
            return true;
        case com.bip_public_android.R.id.delete_step:
            new AlertDialog.Builder(this)
                    .setTitle("Delete Step")
                    .setMessage(
                            "Are you sure you want to delete step "
                                    + ProjectDisplay.currentlySelected
                                            .getName() + "?")
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    Log.d(TAG, "delete step ID : "+stepID);
                                    deleteStepFromAPI(stepURL + "?auth_token="
                                            + auth_token+"&step_id="+stepID);
                                }
                            });
            return true;
        case com.bip_public_android.R.id.edit:
            if (editMode) {
                // edit action bar icon
                item.setIcon(com.bip_public_android.R.drawable.ic_action_edit);
                item.setTitle("edit");

                editMode = false;
                saveStep();

            } else {
                editMode = true;
                item.setIcon(com.bip_public_android.R.drawable.ic_action_accept);
                item.setTitle("save");
                if (!editTextClicked) {
                    // user clicked on menu icon, not directly on editText
                    editStep(null);
                }
            }
            return true;
        case android.R.id.home:
            backPressedFromActionBar = true;
            this.onBackPressed();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case Constant.REQUEST_CODE_PREVIEW:
                int deletedID = data.getIntExtra("imageId", -1);
                // Removing the mapping for the deleted image from the step
                //Log.d(TAG,"VCJ deleted ID: "+deletedID);
                if (deletedID != -1) {
                    if (ProjectDisplay.currentlySelected.getMultimediaList()
                            .size() > 0) {

                        Multimedia deletedMedia = ProjectDisplay.currentlySelected
                                .findMultimediaById(deletedID);
                        //Log.d(TAG, "VCJ trying to delete media with id : " + deletedID + " from media list :" + ProjectDisplay.currentlySelected.getMultimediaList());

                        // Removing the media from the step
                        ProjectDisplay.currentlySelected
                                .removeMultimedia(deletedMedia);
                        //Log.d(TAG, "VCJ calling activity result ok : " + data.getStringExtra("callingActivity"));
                        //Log.d(TAG, "VCJ position "+data.getIntExtra("position", -1));
                    }
                }

                changedImages = true;
                //Log.d(TAG, "VCJ about to reload the step");
                loadStepFromAPI(stepURLjson + "?auth_token=" + auth_token);
                break;
            	
            case Constant.REQUEST_CODE_ADD_PIC:
                changedImages = true;

                Log.d(TAG, "OnActivityResult: entering add pic");
                if (Method.isNetworkAvailable(this)) {
                    if (userID != -1) {
                    	
                        List<String> toAdd = data
                                .getStringArrayListExtra(Constant.IMAGE_INTENT);
                        
                        for (final String localpath : toAdd) {
                            // Adding the image path to both the extra images
                            // and currently uploading for the step
                            ProjectDisplay.currentlySelected
                                    .addExtraImage(localpath);
                            ProjectDisplay.currentlySelected
                                    .addCurrentlyUploading(localpath);

                            Bitmap bmp;

                            View displayView;

                            if (localpath.endsWith("jpg")
                                    || localpath.endsWith("jpeg")
                                    || localpath.endsWith("png")
                                    || localpath.endsWith("gif")) {
                                bmp = PictureHorizontalLayout.resizeBitmap(
                                        BitmapFactory.decodeFile(localpath),
                                        285);
                                
                                // Adding a simple image view for an image
                                displayView = new ImageView(StepDisplay.this);

                                ((ImageView) displayView).setImageBitmap(bmp);
                                ((ImageView) displayView).setScaleType(ScaleType.CENTER_CROP);

                                // Uploading image with testing upload image
                                // with the uploading notification
                                //Multimedia tag = (Multimedia) displayView.getTag();
                                // if the size hasn't been updated yet, you can give it the correct index
                                int position = ProjectDisplay.currentlySelected.getMultimediaList().size() + toAdd.indexOf(localpath);
                                //Log.d(TAG, "VCJ multimedia size: " + ProjectDisplay.currentlySelected.getMultimediaList().size() + " and index of item: " + toAdd.indexOf(localpath));

                                new UploadImage(StepDisplay.this,
                                        ProjectDisplay.currentlySelected,
                                        ProjectDisplay.stepAdapter,
                                        (ImageView) displayView,
                                        position)
                                        .execute("" + ProjectDisplay.currentlySelected.getId(),
                                                localpath, auth_token, "" + projectID, userID);
                            } else {
                                bmp = ThumbnailUtils.createVideoThumbnail(
                                        localpath,
                                        MediaStore.Images.Thumbnails.MINI_KIND);

                                LayoutInflater inflater = (LayoutInflater) this
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                // If we have a video set to be the video preview layout
                                displayView = inflater.inflate(
                                        com.bip_public_android.R.layout.video_preview, null);
                                ImageView video_img = (ImageView) displayView
                                        .findViewById(com.bip_public_android.R.id.video_image_preview);
                                video_img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                video_img.setImageBitmap(bmp);
//Log.d(TAG, "VCJ multimedia size: " + ProjectDisplay.currentlySelected.getMultimediaList().size() + " and index of item: " + toAdd.indexOf(localpath));

                                new UploadVideo(StepDisplay.this,
                                        ProjectDisplay.currentlySelected,
                                        ProjectDisplay.stepAdapter, video_img)
                                        .execute(""+ ProjectDisplay.currentlySelected.getId(),
                                                localpath, auth_token, "" + projectID, userID);

                                /*new UploadVideo(StepDisplay.this,
                                        ProjectDisplay.currentlySelected,
                                        ProjectDisplay.stepAdapter, displayView)
                                        .execute(""+ ProjectDisplay.currentlySelected.getId(),
                                                localpath, auth_token, "" + projectID, userID);*/
                            }
                                                       
                            // Setting the final params for the display view
                            displayView.setPadding(4, 4, 4, 4);
                            displayView.setLayoutParams(new LayoutParams(
                                    PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT));
                            displayView.setAlpha(Constant.UPLOADING_ALPHA);

                            ImageView videoIcon = (ImageView) displayView
                                    .findViewById(com.bip_public_android.R.id.video_icon);
                            if (videoIcon != null) {
                                int videoIconDimension = (int) Math
                                        .round(PREVIEW_IMAGE_HEIGHT * 0.5);
                                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                        videoIconDimension, videoIconDimension);
                                layoutParams.addRule(
                                        RelativeLayout.CENTER_IN_PARENT,
                                        RelativeLayout.TRUE);
                                videoIcon.setLayoutParams(layoutParams);
                                Log.d(TAG, "OnActivityResult: setting video icon to visible videoIconDimension: " + videoIconDimension);
                                videoIcon.setVisibility(View.VISIBLE);
                            }

                            stepImages.addView(displayView);

                            // Increasing the amount of files uploading
                            Constant.currentlyUploading.incrementAndGet();

                            // Intent to call if the user clicks the
                            // notification
                            String files_uploading_string;
                            if( Constant.currentlyUploading.get() == 1){
                               files_uploading_string = "file";
                            }else{
                                files_uploading_string = "files";
                            }
                            Intent notIntent = new Intent(this,
                                    AllProjectDisplay.class);
                            Builder uploadBuilder = new NotificationCompat.Builder(
                                    StepDisplay.this)
                                    .setSmallIcon(
                                            com.bip_public_android.R.drawable.logo)
                                    .setContentTitle(
                                            "Uploading to Build in Progress")
                                    .setContentText(
                                            Constant.currentlyUploading.get()
                                                    + " " + files_uploading_string + " uploading")
                                    .setTicker("Uploading to Build in Progress")
                                    .setOnlyAlertOnce(true);
                            Notification uploadNotif = uploadBuilder.build();
                            uploadNotif.contentIntent = PendingIntent
                                    .getActivity(this, 0, notIntent, 0);
                            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(
                                    Constant.NOTIFICATION_UPLOADING_ID,
                                    uploadNotif);
                        }

                        //Log.d(TAG, "VCJ step images (horizontal view) children before refresh: " + stepImages.getChildCount());

                        horizontalScroll.postDelayed(new Runnable() {
                            public void run() {
                                horizontalScroll.scrollTo(numMedia * PREVIEW_IMAGE_WIDTH, 0);
                            }
                        }, 100L);

                    } else {
                        Toast.makeText(
                                this,
                                "Couldn't find user id. Try restarting the app.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(
                            this,
                            "No internet connection available. Check connection settings",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
            
    }



    public String getFormattedDate(String monthString, int day, int hour,
            int minute) {
        boolean pm = false;
        StringBuilder formatted = new StringBuilder(monthString);
        formatted.append(" ").append(day);
        if (hour > 12) {
            hour = hour - 12;
            pm = true;
        }
        formatted.append(" at ").append(hour);

        if (minute < 10)
            formatted.append(":0").append(minute);
        else
            formatted.append(":").append(minute);

        if (pm)
            formatted.append(" PM");
        else
            formatted.append(" AM");

        return formatted.toString();
    }

    public static Bitmap resizeImage(Context act, int resId, int newWidth, int newHeight) {
        // load the origial Bitmap
        Bitmap BitmapOrg = BitmapFactory.decodeResource(act.getResources(),
                resId);
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        // calculate the scale
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
    }

    // save the step
    public void saveStep() {
        Log.w(TAG, "saving step");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(stepNameEditText.getWindowToken(), 0);
        boolean descriptionChanged = ((stepDescriptionEditText.getText()
                .length() > 0) && !ProjectDisplay.currentlySelected
                .getDescription().equals(
                        Html.toHtml(stepDescriptionEditText.getText())));
        boolean nameChanged = !ProjectDisplay.currentlySelected.getName()
                .equals(stepNameEditText.getText().toString());

        if (stepNameEditText.getText().toString().length() > 0) {
            /*
             * Only toggle stepNameSwitcher back to TextView if we're not on the
             * project overview step (or if only the check box for last step was
             * selected)
             */
            if (stepNameSwitcher.getCurrentView().getId() == stepNameEditText
                    .getId()
                    || stepDescriptionSwitcher.getCurrentView().getId() == stepDescriptionEditText
                            .getId()) {
                // Assuring that the stepname has changed in order to update

                if (nameChanged || descriptionChanged) {
                    changed = true;
                    stepDescription = Html.toHtml(stepDescriptionEditText
                            .getText());

                    // Can't update the project overview's step name, gives
                    // errors
                    ProjectDisplay.currentlySelected.setName(stepNameEditText
                            .getText().toString());
                    ProjectDisplay.currentlySelected
                            .setDescription(stepDescription);
                }
                stepDescriptionEditText.setText("");
                stepNameEditText.setText("");
                stepNameSwitcher.setDisplayedChild(0);
            }

            // Reverting back to unchangeable state
            stepDescriptionSwitcher.setDisplayedChild(0);

            // Assuring we changed the step before trying to update
            if (changed) {
                if (Method.isNetworkAvailable(getApplicationContext())) {
                    // update title of project in textview
                    stepName.setText(ProjectDisplay.currentlySelected.getName());

                    String stepDescriptionText = ProjectDisplay.currentlySelected
                            .getDescription();

                    if (descriptionChanged) {
                        stepDescriptionText = stepDescriptionText.replace(
                                "<u>", "");
                        stepDescriptionText = stepDescriptionText.replace(
                                "</u>", "");
                        stepDescriptionView.setText(Html
                                .fromHtml(stepDescriptionText));
                    } else {
                        stepDescriptionText = DEFAULT_DESCRIPTION;
                    }

                    // set action bar title to project name
                    getSupportActionBar().setTitle(
                            ProjectDisplay.currentlySelected.getName());
                    changed = false;

                    new UpdateStepNameTask(StepDisplay.this).execute(stepURL,
                            auth_token,
                            ProjectDisplay.currentlySelected.getName(),
                            stepDescriptionText);
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "No internet connection available. Please check connection settings",
                            Toast.LENGTH_SHORT).show();
                }
            }

        } else {
            Toast.makeText(StepDisplay.this, "Please add a step name",
                    Toast.LENGTH_LONG).show();
        }
    }
    

    // enter the edit mode for a step
    public void editStep(View v) {
        // only allow user to edit the name of the step if it's not the project
        // overview step
        
        Log.d(TAG, "stepName: " + ProjectDisplay.currentlySelected.getName());
        stepNameEditText.setText(ProjectDisplay.currentlySelected.getName());
        stepNameSwitcher.setDisplayedChild(1);

        stepDescriptionSwitcher.setDisplayedChild(1);

        if (!stepDescription.equals("null")) {
            Log.d(TAG, "stepDescription: " + stepDescription);
            stepDescriptionEditText.setText(Html.fromHtml(stepDescription));
        } // else don't fill the step description - leave hint text


        if (v != null) {
            // autofocus on whatever view was clicked
            if (v.getId() == com.bip_public_android.R.id.step_name) {
                Log.d(TAG, "clicked on step name");
                stepNameEditText.requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(stepNameEditText,
                                InputMethodManager.SHOW_FORCED);
            } else if (v.getId() == com.bip_public_android.R.id.step_description_switcher) {
                Log.d(TAG, "clicked on step description");
                stepDescriptionEditText.requestFocus();
                Log.d(TAG, "show keyboard");
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(stepDescriptionEditText,
                                InputMethodManager.SHOW_FORCED);
            }
        }

    }

    // toggle edit mode for step (when edit button clicked)
    public class editStepModeListener implements OnClickListener {

        public void onClick(View v) {
            Log.d(TAG, "clicked view: " + v.getId());
            editMode = true;
            MenuItem editMenuItem = editMenu.findItem(com.bip_public_android.R.id.edit);
            editTextClicked = true;
            onOptionsItemSelected(editMenuItem);
            editStep(v);
            editTextClicked = false;
        }
    }


    public static void setImageOnClickListener(ImageView image, Context context, String filepath, int position) {
        final Context mContext= context;
        final String mFilepath = filepath;
        final int mPosition = position;

        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(mContext, MediaPreview.class);
                    intent.putExtra("imageURL", mFilepath); // imageURL refers to either
                    // website url or local path
                    // for image or video
                    if (mFilepath.endsWith("jpg")
                            || mFilepath.endsWith("jpeg")
                            || mFilepath.endsWith("png")
                            || mFilepath.endsWith("gif")) {
                        intent.putExtra("isFromVideo", false);
                    } else {
                        intent.putExtra("isFromVideo", true);
                    }
                    intent.putExtra("local", true); // don't add newly uploaded images to view pager
                    //intent.putExtra("local", false); // it's uploaded, so it's not a local image anymore
                    intent.putExtra("position", mPosition);
                    intent.putExtra("callingActivity", mContext.getClass().getSimpleName());
                    Log.d(TAG, "UPLOAD: position = " + mPosition);
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    Log.d(TAG, "FAILED");
                }
            }
        });
    }
}
