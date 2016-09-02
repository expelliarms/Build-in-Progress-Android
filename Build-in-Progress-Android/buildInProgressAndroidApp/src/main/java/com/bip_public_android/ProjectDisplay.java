package com.bip_public_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.example.helperClass.Constant;
import com.example.helperClass.Method;
import com.example.helperClass.Multimedia;
import com.example.helperClass.ParcelStep;
import com.example.helperClass.PictureHorizontalLayout;
import com.mobeta.android.dslv.DragSortListView;
import com.savagelook.android.UrlJsonAsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectDisplay extends SherlockActivity {

	private final static String TAG = "ProjectScreen";
	/**
	 * Public static step adapter in order for uploading images from
	 * step display to be able to update the correct step adapter
	 */
	public static CustomizedStepAdapter stepAdapter;
	/**
	 * Step that is currently being updated in the step display
	 * class
	 */
	public static ParcelStep currentlySelected;

	private int lastStepId = 0;

	private String PROJECT_STEPS_URL;
	private String PROJECT_STEPS_JSON_URL;

	public String projUploadURL;

	//	private final String projectHelp = "Note: Your project will become published on Build in Progress once you name your project and add a step with an image.";

	private String auth_token;
	/**Keeping track of whether the projects have been loaded or not*/
	private boolean alreadyLoaded = false;
	/**Boolean used to see if we added a step and want to scroll down to the bottom of the list*/
	private boolean addedStep = false;
	private boolean addedLabel = false;

	boolean backPressedFromActionBar = false;
	boolean editMode = false;

	//Project info
	private static int projectID;
	private String projectTitle;
	private String projectDescription;

	private final int SQUARE_IMAGE_DIMENSION = 105; // Dimension of square image thumbnail from website

	private SharedPreferences preferences;

	/** List containing all of the bitmaps associated with all steps in the data
	 * will only add images to positions that have actually have images associated
	 * with it
	 */
	private SparseArray<SparseArray<View>> tempStepsThumbnails = new SparseArray<SparseArray<View>>();
	private SparseArray<View> tempLabelThumbnails = new SparseArray<View>();

	/** step data for all of the steps except step Overview */
	private static  List<ParcelStep> stepList = new ArrayList<ParcelStep>();
	private static  List<ParcelStep> labelList = new ArrayList<ParcelStep>();

	private RadioButton stepButton;
	private RadioButton labelButton;

	private boolean viewSteps = true; // true if the toggle is on Steps, false if on Labels

	private ViewSwitcher projectTitleSwitcher;
	private ViewSwitcher editIconSwitcher;
	private EditText projectTitleEditText;
	private ImageButton editProjectTitle;
	private ImageButton saveProjectTitle;
	private TextView projectTitleView;
	private LinearLayout projectDescriptionHolder;
	private Button addStep;
	private ImageButton mapButton;
	private ImageButton listButton;
	private WebView webview;
	private ProgressBar webviewProgress;
	
	//	private TextView projectHelpView;
	private DragSortListView stepListView;

	boolean newProjectCreated;
	boolean mapMode;

	//TODO drag and drop for older versions
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if(!Method.isNetworkAvailable(this)){
			//Displaying a nicer connection error dialog
			Toast.makeText(this, "No internet connection available. Check your connection settings", Toast.LENGTH_LONG).show();
			Intent noInternet = new Intent(this, AllProjectDisplay.class);
			noInternet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(noInternet);
			finish();
		}

		setContentView(com.bip_public_android.R.layout.activity_project_screen);

		// get the auth_token from shared preferences
		preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
		auth_token = preferences.getString("AuthToken", "");

		// get the information from the intent
		Intent intent = getIntent();

		// retrieve project information from the ProjectActivity
		projectID = intent.getIntExtra("projectID", -1);
		projectTitle = intent.getStringExtra("projectTitle");
		newProjectCreated = intent.getBooleanExtra("newProjectCreated",false);
		PROJECT_STEPS_URL = Constant.PROJECT_URL+projectID+"/steps";
		PROJECT_STEPS_JSON_URL = PROJECT_STEPS_URL+".json";

		projUploadURL = PROJECT_STEPS_JSON_URL+"?auth_token=" + auth_token;

		// set the project name TextView to the name of the current project
		projectTitleView = (TextView) findViewById(com.bip_public_android.R.id.project_title);
		projectTitleView.setText(projectTitle);				
		
		mapMode = false; // list view is loaded by default
		
		setUpViews();
	}
	
	private void showActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);      
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);        
        
        View view = getLayoutInflater().inflate(com.bip_public_android.R.layout.project_action_bar, null);
        actionBar.setCustomView(view, lp);
        
        View v = actionBar.getCustomView();
        mapButton = (ImageButton) v.findViewById(com.bip_public_android.R.id.map);
        mapButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Log.d(TAG, "clicked on map");
                
                // hide map view and show list
                mapButton.setBackground(getResources().getDrawable(com.bip_public_android.R.drawable.right_rounded_rect_selected));
                mapButton.setImageDrawable(getResources().getDrawable(com.bip_public_android.R.drawable.map_white));
                listButton.setBackground(getResources().getDrawable(com.bip_public_android.R.drawable.left_rounded_rect_deselected));
                listButton.setImageDrawable(getResources().getDrawable(com.bip_public_android.R.drawable.list_blue));
                
                mapMode = true;
                
                // load web browser
                webview.setVisibility(View.VISIBLE);
                webview.getSettings().setJavaScriptEnabled(true);
                webview.getSettings().setLoadWithOverviewMode(true);
                webview.getSettings().setUseWideViewPort(true);
                
                webview.getSettings().setBuiltInZoomControls(true);
                webview.getSettings().setDisplayZoomControls(false);
                
                webview.setWebChromeClient(new WebChromeClient() {
                    public void onProgressChanged(WebView view, int progress) 
                       {
//                        Log.d(TAG, "progress: " + progress);
                       if(progress < 100 && webviewProgress.getVisibility() == ProgressBar.GONE){
                           webviewProgress.setVisibility(ProgressBar.VISIBLE);
                       }
                       webviewProgress.setProgress(progress);
                       if(progress == 100) {
                           webviewProgress.setVisibility(ProgressBar.GONE);
                       }
                    }
                });
                
                webview.setWebViewClient(new WebViewClient(){
                    public void onPageFinished(WebView view, String url){
                        String tree_string = "tree_width=";
                        int tree_width = Integer.parseInt(url.substring(url.lastIndexOf(tree_string)+tree_string.length()));
                        if(tree_width>10){
                            // rotate view
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        }
                    }
                });
                
                String mobile_url = Constant.PROJECT_URL + projectID + "/steps/mobile?auth_token=" + auth_token;
                Log.d(TAG, "mobile_url: " + mobile_url);
                webview.loadUrl(mobile_url);                
            }
        });
        
        listButton = (ImageButton) v.findViewById(com.bip_public_android.R.id.list);
        listButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Log.d(TAG, "clicked on list");
                
                mapMode = false;
                
                mapButton.setBackground(getResources().getDrawable(com.bip_public_android.R.drawable.right_rounded_rect_deselected));
                mapButton.setImageDrawable(getResources().getDrawable(com.bip_public_android.R.drawable.map_blue));
                listButton.setBackground(getResources().getDrawable(com.bip_public_android.R.drawable.left_rounded_rect_selected));
                listButton.setImageDrawable(getResources().getDrawable(com.bip_public_android.R.drawable.list_white));
                
                // hide web browser
                webview.setVisibility(View.GONE);
                webviewProgress.setVisibility(View.GONE);
                webview.clearView();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
	}

	private void toggleViewSteps(boolean stepClick) {
		//set viewSteps to true or false based on it the user clicked on step or not
		viewSteps = stepClick;
		if (viewSteps) {
			addStep.setText(com.bip_public_android.R.string.add_new_step);
		} else {
			addStep.setText(com.bip_public_android.R.string.add_new_label);
		}
		stepAdapter.notifyDataSetChanged();
	}
	
	private void setUpViews(){
		//Setting views
		projectTitleSwitcher = (ViewSwitcher) findViewById(com.bip_public_android.R.id.project_title_switcher);
		editIconSwitcher = (ViewSwitcher) findViewById(com.bip_public_android.R.id.edit_icon_switcher);
		projectTitleEditText = (EditText) projectTitleSwitcher.
				findViewById(com.bip_public_android.R.id.project_title_edit_view);
		projectDescriptionHolder = (LinearLayout) findViewById(com.bip_public_android.R.id.project_description_container);
		editProjectTitle = (ImageButton) findViewById(com.bip_public_android.R.id.edit_project_title_button);
		saveProjectTitle = (ImageButton) findViewById(com.bip_public_android.R.id.save_project_title_button);
		stepListView = (DragSortListView) findViewById(com.bip_public_android.R.id.project_steps_list);
		webview = (WebView) findViewById(com.bip_public_android.R.id.webview);
		webviewProgress = (ProgressBar) findViewById(com.bip_public_android.R.id.webview_progress);

		getSupportActionBar().setDisplayShowTitleEnabled(false); // hide build in progress title from toolbar

		stepButton = (RadioButton) findViewById(com.bip_public_android.R.id.stepToggleButton);
		labelButton = (RadioButton) findViewById(com.bip_public_android.R.id.labelToggleButton);

		//show correct table
		stepButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				toggleViewSteps(true);
			}
		});

		labelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleViewSteps(false);
			}
		});

		//Allowing longclick options for the steps
		registerForContextMenu(stepListView);

		//		projectTitleEditText.setImeActionLabel("Done", KeyEvent.KEYCODE_ENTER);
		projectTitleEditText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_DONE ||(event.getAction() == KeyEvent.ACTION_UP &&
						event.getKeyCode() == KeyEvent.KEYCODE_ENTER)){
					saveProjectTitle.performClick();
				}
				return false;
			}
		});
		editProjectTitle.setOnClickListener(new editProjectNameListener());		
		projectTitleSwitcher.setOnClickListener(new editProjectNameListener());

		saveProjectTitle.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				editMode = false;
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(projectTitleEditText.getWindowToken(), 0);

				if(projectTitleEditText.getText().toString().length()>0){
					//Assuring that we update with a new title
					if(!projectTitle.equals(projectTitleEditText.getText().toString())){
						projectTitle = projectTitleEditText.getText().toString();
						new updateProject().execute(projectTitle);
						// update title of project in textview
						TextView project_name = (TextView) findViewById(com.bip_public_android.R.id.project_title);
						project_name.setText(projectTitle);

						// set action bar title to project name
						getSupportActionBar().setTitle(projectTitle);
					}
					projectTitleSwitcher.setDisplayedChild(0);
					editIconSwitcher.setDisplayedChild(0);

				}else{
					Toast.makeText(ProjectDisplay.this, "Please add a Project Title", Toast.LENGTH_SHORT).show();
				}
			}		
		});

		addStep = (Button) findViewById(com.bip_public_android.R.id.project_screen_new_step);

		addStep.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(Method.isNetworkAvailable(getApplicationContext())){
					if (viewSteps){
						new AddStepTask().execute(PROJECT_STEPS_URL+"?auth_token="+auth_token);
					} else {
						new AddStepTask().execute(PROJECT_STEPS_URL+"?auth_token="+auth_token);
					}
				}else{
					Toast.makeText(getApplicationContext(), "No internet connection available. Check your connection settings.", Toast.LENGTH_LONG).show();
				}
			}
		}); 
	}

	@Override
	public void onResume(){
	    Log.d(TAG, "on resume");
		super.onResume();
		if(!preferences.contains("AuthToken")){
			Intent intent = new Intent(ProjectDisplay.this, LoginActivity.class);
			startActivityForResult(intent, 0);
			finish();
		}
		if(!alreadyLoaded && !mapMode){
		    Log.d(TAG, "loading project steps");
			loadProjectStepsFromAPI(PROJECT_STEPS_JSON_URL+"?auth_token=" + auth_token);
		}

	}

	/**
	 * Customized adapter to add steps to the DragSortListView:
	 * modified to handle steps and labels separately
	 */
	public class CustomizedStepAdapter extends BaseAdapter {
		/** The collections of sellable objects */
		private List<ParcelStep> allStepList;
		/** To inflate the view from an xml file */
		private LayoutInflater inflater;
		private List<ParcelStep> allLabelList;

		public CustomizedStepAdapter(Activity a, List<ParcelStep> steps, List<ParcelStep> labels) {
			allStepList = steps;
			inflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			allLabelList = labels;
		}

		@Override
		public int getCount() {
			if (viewSteps) {return allStepList.size();}
			return allLabelList.size();
		}

		@Override
		public ParcelStep getItem(int position) {
			// doesn't look like this is used for the Customized Step Adapter
			if (viewSteps) {
				return allStepList.get(position);
			}
			return allLabelList.get(position);
		}

		@Override
		public long getItemId(int position) {
			if (viewSteps) {
				return getStepId(position);
			} else {
				return getLabelId(position);
			}
		}

		public long getStepId(int position) {
			return allStepList.get(position).getId();
		}

		public long getLabelId(int position) {
			return allLabelList.get(position).getId();
		}

		@Override
		public boolean hasStableIds(){
			return true;
		}

		public void removedStep(){
			//just reload both lists to above horrible indexing errors
			loadProjectStepsFromAPI(PROJECT_STEPS_JSON_URL+"?auth_token=" + auth_token);
		}

		public void set(int position, ParcelStep value){
			if (viewSteps) {
				allStepList.set(position, value);
			} else {
				allLabelList.set(position, value);
			}

		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ParcelStep step;
			if (viewSteps) {
				step = allStepList.get(position);
			} else {
				step = allLabelList.get(position);
			}

			final StepHolder holder;	        
			if (convertView == null) {
				convertView = inflater.inflate(com.bip_public_android.R.layout.step_display, null);
				LayoutParams stepParams = new LayoutParams(LayoutParams.MATCH_PARENT, SQUARE_IMAGE_DIMENSION );
				convertView.setLayoutParams(stepParams);
				holder = new StepHolder(convertView);       
				convertView.setTag(holder);

			}
			//The convertView has already been set
			else {
				holder = (StepHolder) convertView.getTag();
			}

			//Ensuring we don't add views to another convertView's images
			holder.stepImages.removeAllViews();

			/*
			 * Getting step images from the website to add them to the list
			 * only if the thumbnails for the step haven't been previously
			 * added to the list containing all of the bitmaps of the steps
			 * in order to load the images more efficiently
			 */
			if (viewSteps) {
				if(step.getMultimediaList().size() > 0 && tempStepsThumbnails.get(position)==null){
					/** List of bitmaps used to hold a specific step's thumbnail's images*/
					final SparseArray<View> sortedThumbnails = new SparseArray<View>();

					//Steps list of all its media
					ArrayList<Multimedia> stepMedia = step.getMultimediaList();

					int totalImg = stepMedia.size();
					SparseArray<View> sortedImageView	= new SparseArray<View>(totalImg);

					// create multimedia objects for the first media of each step
					Multimedia multimedia = stepMedia.get(0);
					//Position of image to be added to the array
					int imagePos = multimedia.getPosition();

					//Getting the thumbnail url
					String path = multimedia.getSqThumbnailPath();
					View displayView;
					if(multimedia.isFromVideo()){
						LayoutInflater inflater = (LayoutInflater)ProjectDisplay.this.getSystemService
								(Context.LAYOUT_INFLATER_SERVICE);
						//If we have a video set to be the video preview layout
						displayView = inflater.inflate(com.bip_public_android.R.layout.video_preview, null);
						ImageView video_img = (ImageView)displayView.findViewById(com.bip_public_android.R.id.video_image_preview);
						video_img.setScaleType(ImageView.ScaleType.CENTER_CROP);
						video_img.setPadding(4, 4, 4, 4);
						video_img.setLayoutParams(new RelativeLayout.LayoutParams(SQUARE_IMAGE_DIMENSION, SQUARE_IMAGE_DIMENSION));
						new Method.GetImageFromURL(video_img, ProjectDisplay.this, null).execute(path);
					}else{
						displayView = new ImageView(ProjectDisplay.this);
						((ImageView) displayView).setScaleType(ImageView.ScaleType.CENTER_CROP);
						displayView.setPadding(4,4,4,4);
						displayView.setLayoutParams(new LayoutParams(SQUARE_IMAGE_DIMENSION, SQUARE_IMAGE_DIMENSION));
						new Method.GetImageFromURL((ImageView)displayView, ProjectDisplay.this, null).execute(path);
					}

					//Setting id of image as tag
					displayView.setTag(path);
					sortedImageView.put(imagePos, displayView);

					//Getting the view and adding it to the stepThumbnail's array
					sortedThumbnails.put(imagePos, displayView);

					//Adding the thumbnails to list containing all steps thumbnails
					tempStepsThumbnails.put(position, sortedThumbnails);

					for(int i = 0; i < sortedImageView.size(); i++){
						holder.stepImages.addView(sortedImageView.valueAt(i));
					}
				}
			/*
			 *If a step at a given position has images associated with it
			 *and the images have already been added to the sparse array,
			 *the bitmaps associated with it will be reused
			 */
				else if(tempStepsThumbnails.get(position, null)!=null){
					holder.stepImagesHolder.bringToFront();
					holder.stepImagesHolder.getParent().requestLayout();
					//Iterating over all of the previously stored in the thumbnails
					for(int i=0;i<tempStepsThumbnails.get(position).size(); i++){
						int key = tempStepsThumbnails.get(position).keyAt(i);
						if(tempStepsThumbnails.get(position).get(key)!=null){
							View thumbnail = tempStepsThumbnails.get(position).get(key);
							String path = (String)thumbnail.getTag();
							if(thumbnail.getParent()!=null){
								ViewGroup thumbParent = (ViewGroup)thumbnail.getParent();
								thumbParent.removeView(thumbnail);
							}

							if(step.getCurrentlyUploading().contains(path)){
								thumbnail.setAlpha(Constant.UPLOADING_ALPHA);
								thumbnail.setPadding(4, 4, 4, 4);
								if(thumbnail instanceof ImageView){
									((ImageView) thumbnail).setScaleType(ImageView.ScaleType.CENTER_CROP);
								}

								thumbnail.setLayoutParams(new LayoutParams(SQUARE_IMAGE_DIMENSION, SQUARE_IMAGE_DIMENSION));
							}else{
								thumbnail.setAlpha(Constant.UPLOADED_ALPHA);
							}
							holder.stepImages.addView(thumbnail);
						}
					}
				}
				holder.populateFrom(allStepList.get(position));
			} else {
				holder.populateFrom(allLabelList.get(position));

				View displayView;
				displayView = new ImageView(ProjectDisplay.this);
				((ImageView) displayView).setScaleType(ImageView.ScaleType.CENTER_CROP);
				displayView.setPadding(4, 4, 4, 4);
				displayView.setLayoutParams(new LayoutParams(SQUARE_IMAGE_DIMENSION, SQUARE_IMAGE_DIMENSION));
				String labelColor = allLabelList.get(position).getLabelColor();
				displayView.setBackgroundColor(Color.parseColor(labelColor));
				//Setting color as tag
				displayView.setTag(labelColor);

				//Getting the view and adding it to the labelThumbnail's array
				tempLabelThumbnails.put(position, displayView);
				holder.stepImages.addView(displayView);
			}
			return convertView;
		}

	}
	/**
	 * StepHolder class used to maintain the data for our list view
	 * in order to load items more efficiently
	 *
	 */
	static class StepHolder {
		//private ImageView stepHandles;
		private TextView stepTitleTextView;
		// private Button dragImageHere;
		private LinearLayout stepImages;
		// private LinearLayout stepImagesSwitcher;
		private HorizontalScrollView stepImagesHolder;

		public StepHolder(View convertView){
			//Setting up the views
			//stepHandles = (ImageView) convertView.findViewById(R.id.handles_step_display);
			stepImagesHolder = (HorizontalScrollView) convertView.findViewById(com.bip_public_android.R.id.step_image_scroll_view);
			stepImages = (LinearLayout) convertView.findViewById(com.bip_public_android.R.id.step_image_step_display);
			stepTitleTextView = (TextView) convertView.findViewById(com.bip_public_android.R.id.step_name_step_display);
			stepTitleTextView.setFocusable(false);
			stepImages.setFocusable(false);
			stepImagesHolder.setFocusable(false);

			//disable scrolling for stepImagesHolder
			stepImagesHolder.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return true;
				}
			});

		}

		private void populateFrom(ParcelStep step){

		    // prevent step names from overflowing in row
			if(step.getName().length() > 40){
				stepTitleTextView.setText(step.getName().substring(0, 40)+"..");
			}else{
				stepTitleTextView.setText(step.getName());
			}   
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {

			switch (requestCode) {
			/*
			 * When returning from viewing a step, we will 
			 * only update/load new changes if the images were changed
			 * also need to subtract one from the position since the
			 * overview was added as a header and is not included in the
			 * dataset
			 */
			case Constant.REQUEST_CODE_STEP:
				if(data.getStringExtra("projectDescription") != null){
					projectDescription = data.getStringExtra("projectDescription");
				}else{
					if(data.getBooleanExtra("deleted", false)){
						stepAdapter.removedStep(); // vcj: now refreshes so position doesn't matter
						return;
					}else if(data.getBooleanExtra("updatedDate", false)){
						//Reload the steps to assure that they are in the correct order.
						//TODO:  manually sort steps
						loadProjectStepsFromAPI(PROJECT_STEPS_JSON_URL+"?auth_token=" + auth_token);
						return;
					}

					int arrayPosition = currentlySelected.getArrayPosition();

					if(arrayPosition>=0){
						stepAdapter.set(arrayPosition, currentlySelected);
					}
					if(data.getBooleanExtra("changed_images", false)){
						if(tempStepsThumbnails.get(arrayPosition, null)!=null){
							SparseArray<View> newSparse = tempStepsThumbnails.get(arrayPosition);
							List<Integer> keysToRemove = new ArrayList<Integer>();
							for(int i=0 ; i<newSparse.size(); i++){
								int key = newSparse.keyAt(i);
								//Removing the imageview from thumbnails if they have been deleted
								if(currentlySelected.getExtraImages()!=null){
									//If the extra image has been removed from the selected step or the updated media list
									//doesn't contain the value remove it
									if(!currentlySelected.getExtraImages().contains(newSparse.get(key).getTag())
											&& !containsValue(currentlySelected.getMultimediaList(), (String)newSparse.get(key).getTag() )){
										keysToRemove.add(key);
									}
								}else{
									if(!containsValue(currentlySelected.getMultimediaList(), (String)newSparse.get(key).getTag())){
										keysToRemove.add(key);
									}
								}
							}
							/*
							 * Removing the images from the tempsteps thumbnails, while 
							 * keeping track of how many we have removed in order to correctly remove
							 * the right index
							 */
							if(keysToRemove.size()>0){
								for(int i: keysToRemove){
									tempStepsThumbnails.get(arrayPosition).remove(i);
								}
							}

							//If there are no images set the value of that position to null
							if(tempStepsThumbnails.get(arrayPosition).size()<1){
								tempStepsThumbnails.put(arrayPosition, null);
							}
						}
					}


					if(currentlySelected.getExtraImages()!=null){
						if(currentlySelected.getExtraImages().size()>0){
							SparseArray<View> updatedSparse;
							if(tempStepsThumbnails.get(arrayPosition, null)!=null){
								updatedSparse = tempStepsThumbnails.get(arrayPosition);
							}else{
								updatedSparse = new SparseArray<View>();
							}
							for(String path: currentlySelected.getExtraImages()){
								if(!Method.imageSparseContainsTag(updatedSparse, path)){
									Bitmap bmp;

									View uploadingView;

									if(path.endsWith("jpg") || path.endsWith("jpeg")||path.endsWith("png")||path.endsWith("gif")){
										bmp = PictureHorizontalLayout.resizeBitmap(BitmapFactory.decodeFile(path), 285);
										//Adding a simple image view for an image
										uploadingView = new ImageView(ProjectDisplay.this);
										((ImageView)uploadingView).setImageBitmap(bmp);
										((ImageView)uploadingView).setScaleType(ScaleType.FIT_XY);
									}else{
										bmp =  ThumbnailUtils.createVideoThumbnail(path,
												MediaStore.Images.Thumbnails.MICRO_KIND);
										LayoutInflater inflater = (LayoutInflater)this.getSystemService
												(Context.LAYOUT_INFLATER_SERVICE);
										//If we have a video set to be the video preview layout
										uploadingView = inflater.inflate(com.bip_public_android.R.layout.video_preview, null);
										ImageView video_img = (ImageView)uploadingView.findViewById(com.bip_public_android.R.id.video_image_preview);
										video_img.setScaleType(ImageView.ScaleType.FIT_XY);
										video_img.setImageBitmap(bmp);
									}

									//Setting the final params for the display view
									uploadingView.setPadding(5, 5, 5, 5);
									uploadingView.setLayoutParams(new LayoutParams(SQUARE_IMAGE_DIMENSION, SQUARE_IMAGE_DIMENSION));
									uploadingView.setAlpha(Constant.UPLOADING_ALPHA);
									uploadingView.setTag(path);

									updatedSparse.append(updatedSparse.size(), uploadingView);
								}	
							}
							tempStepsThumbnails.put(arrayPosition, updatedSparse);
						}
					}
					stepAdapter.notifyDataSetChanged();
					break;
				}
			}
		}
	}

	/**
	 * Checks the step's media to see if the multimedia contains a specific path
	 * @param stepMedia multimedia item to search for
	 * @param mediaPath multimedia thumbnail path
	 * @return true if the multimedia is in the project and false otherwise
	 */
	protected static boolean containsValue(ArrayList<Multimedia> stepMedia, String mediaPath) {
		for(Multimedia media: stepMedia){
			if(media.getSqThumbnailPath().equals(mediaPath)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Method used to remove a key/value mapping from a hashmap given a key that
	 * may or may not be stored inside the hashmap
	 * @param imagePaths hashmap to check
	 * @param value to check for in hashmap
	 * @return newly updated hashmap with the removed key mapping
	 */
	//	protected static <K, V> HashMap<K, V> removedKeyMapping(HashMap<K, V> map,K value){
	//		Set<Entry<K, V>> hashSet = map.entrySet();
	//		Iterator<Entry<K, V>> iter = hashSet.iterator();
	//		while(iter.hasNext()){
	//			Entry<K, V> entry = iter.next();
	//			if(entry.getKey().equals(value)){
	//				iter.remove();
	//				break;
	//			 }
	//		 }
	//		return map;
	//	}

	/**
	 *  Load project steps from build in progress website
	 * @param url website url including the users auth_token
	 */
	public void loadProjectStepsFromAPI(String url){
		if(Method.isNetworkAvailable(this)){
			GetProjectStepsTask getProjectStepsTask = new GetProjectStepsTask(ProjectDisplay.this);
			if(addedStep || addedLabel){
			    getProjectStepsTask.setMessageLoading("Loading step..");
			}else{
			    getProjectStepsTask.setMessageLoading("Loading project..");
			}
			
			getProjectStepsTask.execute(url);
		}else{
			Toast.makeText(getApplicationContext(), "No internet connection available!",  Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Fetching the steps for the current project
	 *
	 */
	public class GetProjectStepsTask extends UrlJsonAsyncTask{
		private final static String TAG = "GetProjectStepsTask";
		private ProgressDialog dialog;

		public GetProjectStepsTask(Context context) {
			super(context);
		}

		@Override
		protected void onPreExecute(){
			dialog = new ProgressDialog(ProjectDisplay.this);
		}

		@Override
		protected void onPostExecute(JSONObject json){
			//Boolean to check if the data was gathered successfully
			boolean successful = true;
			boolean isRemix;

			try{

				//Clears all the information added by the user when reloading
				tempStepsThumbnails.clear();

				//Getting the project's info
				JSONObject jsonProject = json.getJSONObject("data").getJSONObject("project");
				projectTitle = jsonProject.getString("title");
				projectDescription = jsonProject.getString("description");
				isRemix = jsonProject.getBoolean("remix?"); // gets whether the project is a remix of another one
				String getJsonImagePath;
				String getJsonVideoPath;
				if(isRemix){ // if it is a remixed project we need to get the remixed images
					getJsonImagePath = "remix_image_path";
					getJsonVideoPath = "remix_video_path";
				}else{ // we get the normal image and video paths
					getJsonImagePath = "image_path";
					getJsonVideoPath = "video";
				}

				//Getting all the steps from the json object
				JSONArray jsonSteps = json.getJSONObject("data").getJSONArray("steps");
				int length = jsonSteps.length();
				
				// add view modes to toolbar if the project has at least one step
				if(length > 0){
				    showActionBar();
				}
				
				//VCJ for the next branch
				List<ParcelStep> tempStepList = new ArrayList<ParcelStep>();
				List<ParcelStep> tempLabelList = new ArrayList<ParcelStep>();
				for(int i=0; i<length; i++){
					//Adding the step name to the string array in order to correctly display in list view
					JSONObject jsonStep = jsonSteps.getJSONObject(i);
					String name = jsonStep.getString("name");
					String publishedDate = jsonStep.getString("published_on_formatted");
					int id = jsonStep.getInt("id");
					int position = jsonStep.getInt("position");

					// label is a string: equal to "null" or "true"
					// Strange because jsonStep.get("label").getClass() claims it's a boolean
					String isLabel =(jsonStep.getString("label"));
					JSONArray jsonMediaArray = jsonStep.getJSONArray("images");

					if (!isLabel.equals("true")) {
						//List of multimedia to add to a step
						ArrayList<Multimedia> multList = new ArrayList<Multimedia>();

						if (jsonMediaArray != null) {
                            int len = jsonMediaArray.length();
                            for (int j=0;j<len;j++){
                                JSONObject mediaObj = jsonMediaArray.getJSONObject(j);
                                int imageId = mediaObj.getInt("id");
                                int imagePosition = mediaObj.getInt("position");
                                String previewUrl = mediaObj.getJSONObject(getJsonImagePath).
                                        getJSONObject("preview").getString("url");
                                String thumbUrl = mediaObj.getJSONObject(getJsonImagePath).
                                        getJSONObject("thumb").getString("url");
                                String imgUrl = mediaObj.getJSONObject(getJsonImagePath).getString("url");
                                String sqThumbUrl = mediaObj.getJSONObject(getJsonImagePath).
                                        getJSONObject("square_thumb").getString("url");

                                boolean hasVideo = true;
                                boolean isFromEmbeddedVideo = false;
                                String videoUrl = null;
                                //String videoWebm = null;
                                int videoRotation = 0; // default to 0

                                try{
                                    JSONObject videoObj = mediaObj.getJSONObject(getJsonVideoPath);

                                    //If the video has an embedded video path
                                    isFromEmbeddedVideo = videoObj.getString("embed_url").length() > 0;
                                    if(isFromEmbeddedVideo){
                                        videoUrl = videoObj.getString("embed_url");
                                    }else{//The video is not embedded
                                        if (isRemix) {
                                            videoRotation = videoObj.getJSONObject("video_path").getInt("rotation");
                                        } else {
                                            videoRotation = videoObj.getInt("rotation");
                                        }
                                        videoUrl = videoObj.getJSONObject("video_path").
                                                getString("url");
                                        //videoWebm = videoObj.getJSONObject("video_path").getJSONObject("webm").getString("url");
                                    }

                                }catch (JSONException e){
                                    //If we have exception, the image had no video associated to it
                                    hasVideo = false;
                                }

                                Multimedia stepMedia;
                                //If the image has a video associated with it
                                if(hasVideo){
                                    //If the video is embedded from youtube/vimeo
                                    if(isFromEmbeddedVideo){
                                        stepMedia = new Multimedia(imageId, imagePosition, imgUrl, previewUrl, thumbUrl, sqThumbUrl, videoUrl);
                                    }//The video is located on the amazon server
                                    else{
                                        stepMedia = new Multimedia(imageId, imagePosition, imgUrl, previewUrl, thumbUrl, sqThumbUrl, videoUrl, videoRotation);
                                    }
                                }else{
                                    //Create the video using only the image constructor
                                    stepMedia = new Multimedia(imageId, imagePosition, imgUrl, previewUrl, thumbUrl, sqThumbUrl);
                                }

                                //Adding media to the entire list of step's multimedea
                                multList.add(stepMedia);
                            }
                        }
						ParcelStep addStep = new ParcelStep(name, id, position, tempStepList.size(), publishedDate);
						//ParcelStep addStep = new ParcelStep(name, id, position,i, publishedDate);
						//adding all multimedia to the step
						addStep.setMultimediaList(multList);

						tempStepList.add(addStep);
					} else {
						ParcelStep addLabel = new ParcelStep(name, id, position, tempLabelList.size(), publishedDate);
						//label doesn't have an multimedia
						String labelColor = jsonStep.getString("label_color");
						addLabel.setLabelColor(labelColor);
						tempLabelList.add(addLabel);
					}

				}

				stepList = tempStepList;
				labelList = tempLabelList;

				stepAdapter = new CustomizedStepAdapter(ProjectDisplay.this, stepList, labelList);


				projectDescriptionHolder.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent;
						currentlySelected = null;
						intent = new Intent(ProjectDisplay.this, OverviewDisplay.class);
						intent.putExtra("projectTitle", projectTitle);
						intent.putExtra("projectDescription", projectDescription);
						startActivityForResult(intent, Constant.REQUEST_CODE_STEP);
					}
				});

				//vcj : moving description
				/*//Adding Project overview to the list view
				if(stepListView.getHeaderViewsCount()<1){
					stepListView.addHeaderView(overView);
				}*/


				stepListView.setAdapter(stepAdapter);
				// stepListView.setDropListener(onDrop);

//				DragSortController controller = new DragSortController(stepListView);
//
//				//Setting controller options
//				controller.setDragHandleId(R.id.handles_step_display);		
//				controller.setRemoveEnabled(false);
//				controller.setSortEnabled(true);
//				controller.setDragInitMode(DragSortController.ON_DRAG);
//
//				stepListView.setFloatViewManager(controller);
//				stepListView.setOnTouchListener(controller);
//				stepListView.setDragEnabled(true);
				stepListView.setClickable(true);

				//On item click for editing steps
				stepListView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id){
						Intent intent;
						Log.d(TAG, "Click listener: clicked : "+position);
						if (viewSteps) {
							currentlySelected = stepList.get(position);
							intent = new Intent(ProjectDisplay.this, StepDisplay.class);
						} else {
							// clicked a label
							currentlySelected = labelList.get(position);
							intent = new Intent(ProjectDisplay.this, LabelDisplay.class);
							intent.putExtra("labelColor", currentlySelected.getLabelColor());
							intent.putExtra("labelName", currentlySelected.getName());

						}
						intent.putExtra("fromProjects", true);
						intent.putExtra("projectID", projectID);
						startActivityForResult(intent, Constant.REQUEST_CODE_STEP);

					}
				});


			} catch(JSONException e){
				Log.e(TAG, "exception", e);
				//We didn't gather the data successfully and caught an exception
				successful = false;

			} finally{
				if(dialog.isShowing()){
					dialog.dismiss();
				}
				super.onPostExecute(json);
				if(successful){
					//vcj: need to modify this for adding labels
					if(addedStep){
						//TODO Fix the step list view clicking when adding a step
						Log.d(TAG, "total count : "+stepListView.getCount()+" steplist : "+stepAdapter.allStepList.size());
						if(stepListView.getCount() >= stepAdapter.allStepList.size()){
							int newlyAddedStepIndex =stepAdapter.allStepList.size()-1;
							stepListView.performItemClick(stepAdapter.getView(newlyAddedStepIndex, null, null)
									, newlyAddedStepIndex, stepAdapter.getItemId(newlyAddedStepIndex));
						}else{
							Toast.makeText(context, "Error creating step.", Toast.LENGTH_SHORT).show();
						}
						addedStep = false;
					} else if (addedLabel) {
						Log.d(TAG, "total count : "+stepListView.getCount()+" labellist : "+stepAdapter.allLabelList.size());
						if(stepListView.getCount() >= stepAdapter.allLabelList.size()){
							int newlyAddedStepIndex =stepAdapter.allLabelList.size()-1;
							stepListView.performItemClick(stepAdapter.getView(newlyAddedStepIndex, null, null)
									, newlyAddedStepIndex, stepAdapter.getItemId(newlyAddedStepIndex));
						}else{
							Toast.makeText(context, "Error creating step.", Toast.LENGTH_SHORT).show();
						}
						addedLabel = false;
					}
					alreadyLoaded = true;
					if(stepList.size()>0){ // if we have steps in our list
						lastStepId = stepList.get(stepList.size()-1).getId();
					}
				}else{
					Toast.makeText(getApplicationContext(), "Error loading steps.", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	/**
	 * Adding a new step to the current Project
	 *
	 */
	public class AddStepTask extends AsyncTask<Object, Void, HttpEntity>{

		private ProgressDialog dialog;

		@Override
		protected void onPreExecute(){
			dialog = new ProgressDialog(ProjectDisplay.this);
			String message = (viewSteps) ? "Adding step.." : "Adding label..";
			dialog.setMessage(message);
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected HttpEntity doInBackground(Object... params){

			DefaultHttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost((String) params[0]);
			JSONObject holder = new JSONObject();
			JSONObject stepObj = new JSONObject();
			try{
				// add placeholder attributes for the new step
				if (!viewSteps) {
					stepObj.put("label","true");
					String labelColor = "#00AEEF"; // set default color to blue
					stepObj.put("label_color", labelColor);
					stepObj.put("name", "New Label");
					stepObj.put("description", null);
				} else {
					stepObj.put("name", "New Step");
					stepObj.put("description", "");
				}
				stepObj.put("parent_id", lastStepId);
				stepObj.put("last", false);
				holder.put("step", stepObj);

				StringEntity se = new StringEntity(holder.toString());
				post.setEntity(se);
				post.setHeader("Content-Type", "application/json");
			} catch(Exception e){
				e.printStackTrace();
			}

			HttpResponse response = null;

			try{
				response = client.execute(post);

			} catch(HttpResponseException e){
				e.printStackTrace();
				Log.e("ClientProtocol", ""+e);
			} catch(IOException e){
				e.printStackTrace();
				Log.e("IO", "" + e);
			}

			HttpEntity result = null;

			if(response != null){
				result = response.getEntity();
			}

			return result;
		}


		protected void onPostExecute(HttpEntity result){
			if(result!=null){
				if(dialog.isShowing()){
					dialog.dismiss();
				}
				// reload projects
				if (viewSteps) {
					addedStep = true;
				} else {
					addedLabel = true;
				}
				loadProjectStepsFromAPI(PROJECT_STEPS_JSON_URL+"?auth_token=" + auth_token);                   
			}
		}
	}

	// Create Menu Options using Action Bar Sherlock
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
  
		menu.add(Menu.NONE, com.bip_public_android.R.id.refresh, Menu.NONE, "refresh")
		.setIcon(com.bip_public_android.R.drawable.ic_action_refresh)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);		

		SubMenu optionsMenu = menu.addSubMenu("Options Item");
		optionsMenu.add(Menu.NONE, com.bip_public_android.R.id.delete_project, Menu.NONE, "Delete Project")
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		optionsMenu.add(Menu.NONE, com.bip_public_android.R.id.logout, Menu.NONE, "Log Out")
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);        
		MenuItem optionsMenuItem = optionsMenu.getItem();

		optionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		optionsMenuItem.setIcon(com.bip_public_android.R.drawable.ic_action_overflow);

		return super.onCreateOptionsMenu(menu);
	}

	@Override    
	public boolean onOptionsItemSelected(MenuItem item){
		// Handle item selection
		switch(item.getItemId()){
		case com.bip_public_android.R.id.refresh:
			loadProjectStepsFromAPI(PROJECT_STEPS_JSON_URL+"?auth_token=" + auth_token);
			return true;
		case com.bip_public_android.R.id.logout:
			Method.Logout logout = new Method.Logout(this);
			logout.setMessageLoading("Logging out..");
			logout.execute(Constant.LOGOUT_URL+"?auth_token=" + auth_token);
			return true;
		case com.bip_public_android.R.id.delete_project:
			new AlertDialog.Builder(this)
			.setTitle("Delete Project")
			.setMessage("Are you sure you want to delete project " + projectTitle + "?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteProjectFromAPI(Constant.PROJECT_URL+projectID+"?auth_token=" + auth_token);
				}
			})
			.setNegativeButton("Cancel", null)
			.show();
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

	// run delete project task to delete project from build in progress website
	public void deleteProjectFromAPI(String url){
		new Method.DeleteProjectTask(this).execute(url);
		Intent intent = getIntent();
		setResult(RESULT_OK, intent);
		finish();
	}


	/**
	 * Creates the view for the overview in order to be added as an unmovable header
	 * in the drag sort list view.
	 */
/*	private View getOverView(){
		View overView = null;
		overView = getLayoutInflater().inflate(R.layout.project_overview, null);
		return overView;
	}*/


	@Override
	public void onBackPressed() {
		if(editMode && !backPressedFromActionBar){
			editMode = false;
			projectTitleSwitcher.setDisplayedChild(0);
			editIconSwitcher.setDisplayedChild(0);   
		}else{
			Intent i = getIntent();
			i.putExtra("newProjectCreated", newProjectCreated);
			setResult(RESULT_OK, i);
			finish();  
		}		
	}

	// Update name of project
	public class updateProject extends AsyncTask<Object, Void, HttpEntity>{
		String title; // new project title

		@Override
		protected HttpEntity doInBackground(Object... params){
			DefaultHttpClient client = new DefaultHttpClient();

			title = (String) params[0];
			String url= Constant.PROJECT_URL+projectID+"?auth_token=" + auth_token;
			HttpPut put = new HttpPut(url);
			JSONObject holder = new JSONObject();
			JSONObject projectObj = new JSONObject();

			try{
				projectObj.put("title", title);
				holder.put("project", projectObj);

				StringEntity se = new StringEntity(holder.toString());
				put.setEntity(se);
				put.setHeader("Content-Type", "application/json");
			} catch(Exception e){
				e.printStackTrace();
			}

			HttpResponse response = null;

			try{
				response = client.execute(put);
			} catch(Exception e){
				e.printStackTrace();
			}

			HttpEntity result = null;

			if(response != null){
				result = response.getEntity();
			}
			return result;
		}

		protected void onPostExecute(HttpEntity result){
			if(result !=null){
				// refresh project page                
				projectTitle = title;

				// hide keyboard
				InputMethodManager inputManager = 
						(InputMethodManager) getApplicationContext().
						getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(
						ProjectDisplay.this.getCurrentFocus().getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS); 
			}
		}     
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		final android.widget.AdapterView.AdapterContextMenuInfo info = 
				(android.widget.AdapterView.AdapterContextMenuInfo) item.getMenuInfo();



		switch (item.getItemId()) {
		case com.bip_public_android.R.id.edit_step_context_menu:
			Intent intent;
			currentlySelected = stepAdapter.getItem(info.position);
			Log.d(TAG, "step or label position: "+info.position+" viewSteps: "+viewSteps);
			if (viewSteps) {
				intent = new Intent(ProjectDisplay.this, StepDisplay.class);
				Log.d(TAG,"step display");
			} else {
				Log.d(TAG,"label name: "+currentlySelected.getName());
				intent = new Intent(ProjectDisplay.this, LabelDisplay.class);
				intent.putExtra("labelColor", currentlySelected.getLabelColor());
				intent.putExtra("labelName", currentlySelected.getName());
			}
			intent.putExtra("fromProjects", true);
			intent.putExtra("projectID", projectID);
			startActivityForResult(intent, Constant.REQUEST_CODE_STEP);


			break;
		case com.bip_public_android.R.id.delete_step_context_menu:
			final ParcelStep toDelete = stepAdapter.getItem(info.position);
			new AlertDialog.Builder(ProjectDisplay.this)
			.setTitle("Delete Step")
					.setMessage("Are you sure you want to delete step '"+toDelete.getName() +"'?")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							new Method.DeleteStepTask()
									.execute(Constant.PROJECT_URL + projectID + "/steps/" + toDelete.getPosition() + "?auth_token=" + auth_token+"&step_id="+toDelete.getId());
							loadProjectStepsFromAPI(PROJECT_STEPS_JSON_URL+"?auth_token=" + auth_token);
						}
					})
					.setNegativeButton( "No", null)
			.show();
			break;
		}
		return super.onContextItemSelected(item);
	}

	/*public void notifyStepAdapter(){
		stepAdapter.notifyDataSetChanged();
	}
*/
	/**
	 * onClickListener for editing the name of the project
	 * @author ttseng
	 *
	 */
	public class editProjectNameListener implements OnClickListener{
		public void onClick(View v){
			editMode = true;
			projectTitleSwitcher.setDisplayedChild(1);
			editIconSwitcher.setDisplayedChild(1);
			projectTitleEditText.setText(projectTitle);
			projectTitleEditText.requestFocus();
			((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
			.showSoftInput(projectTitleEditText, InputMethodManager.SHOW_FORCED);	        
		}
	}

/*	public static List<ParcelStep> getSteps(){
		return stepList;
	}*/
	
	public static int getId(){
		return projectID;
	}
	
	 /**
     * onCheckChanged - determines if radio buttons for switching between list anda process map views was selected
     * @author ttseng
     *
     */
}
