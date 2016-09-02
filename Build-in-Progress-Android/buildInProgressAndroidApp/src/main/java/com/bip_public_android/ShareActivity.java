package com.bip_public_android;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.example.helperClass.Constant;
import com.example.helperClass.Method;
import com.example.helperClass.Multimedia;
import com.example.helperClass.NewStepDialog;
import com.example.helperClass.ParcelStep;
import com.example.helperClass.PictureHorizontalLayout;
import com.example.helperClass.Project;
import com.savagelook.android.UrlJsonAsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShareActivity extends SherlockActivity {

	private static final String TAG = "SharedActivity";

	private SharedPreferences preferences;

	private List<Project> projects = new ArrayList<Project>();
	private List<ParcelStep> steps = new ArrayList<ParcelStep>();

	//Adapters for the spinners
	private ArrayAdapter<Project> projectsAdapter;
	private ArrayAdapter<ParcelStep> stepsAdapter;

	/** Information passed to the activity */
	private Uri imageToUpload;
	private ArrayList<Uri> imagesToUpload;
	
	private Project selectedProject;
	private int lastStepId;

	private String auth_token;
	private String username;
	private int userID;
	
	/** Measure of ten DP for the current device */
	private int tenDP;
	
	//Views
	private Spinner projectSpinner;
	private Spinner stepSpinner;
	private LinearLayout imageHolder;

	@Override
	protected void onCreate(Bundle savedBundle){
		super.onCreate(savedBundle);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(com.bip_public_android.R.layout.activity_share);

		//Getting user's credentials to upload images
		preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
		auth_token = preferences.getString("AuthToken", "");
		userID = preferences.getInt("user_id", -1);
		tenDP = dpToPx(10);

		setUpViews();
		setUpAdapters();

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			if (type.startsWith("image/")) {
				handleSendImage(intent); // Handle single image being sent
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
			if (type.startsWith("image/")) {
				handleSendMultipleImages(intent); // Handle multiple images being sent
			}
		} else {
			// Handle other intents, such as being started from the home screen
		}
	}


	private void setUpViews() {
	    
		projectSpinner = (Spinner) findViewById(com.bip_public_android.R.id.project_select);
		projectSpinner.setOnItemSelectedListener(new ProjectSelectListener());
		stepSpinner = (Spinner) findViewById(com.bip_public_android.R.id.step_select);
		stepSpinner.setOnItemSelectedListener(new StepSelectListener());

		HorizontalScrollView scroll = (HorizontalScrollView)findViewById(com.bip_public_android.R.id.share_scroll_view);
		imageHolder = (LinearLayout) scroll.findViewById(com.bip_public_android.R.id.share_images_holder);

		Button okButton = (Button) findViewById(com.bip_public_android.R.id.share_ok);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//If the user has selected an item
				if(stepSpinner.getSelectedItem() != null){
					ParcelStep selectedStep = (ParcelStep)stepSpinner.getSelectedItem();
					int stepId = selectedStep.getId();

					int projectID = ((Project)projectSpinner.getSelectedItem()).getId();
					//We only have the one image to upload
					if(imageToUpload != null){
						String localpath = Method.getRealPathFromURI(imageToUpload, ShareActivity.this);
						new Method.UploadImage(ShareActivity.this, selectedStep, null, null, 0) // need to change last 2 args later
						.execute(""+stepId, localpath, auth_token, ""+projectID, userID); 
					}else{//we have multiple
						for( Uri imageUri : imagesToUpload ){
							String localpath = Method.getRealPathFromURI(imageUri, ShareActivity.this);
							new Method.UploadImage(ShareActivity.this, selectedStep, null, null, 0) // need to change last 2 args later
							.execute(""+stepId, localpath, auth_token, ""+projectID, userID);
						}
					}

					//Finish the activity after successfully uploading
					ShareActivity.this.finish();
				}else{
					Toast.makeText(getApplicationContext(), "Select a step.", Toast.LENGTH_SHORT).show();
				}
			}
		});

		Button cancelButton = (Button) findViewById(com.bip_public_android.R.id.share_cancel);
		cancelButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				ShareActivity.this.finish();
			}

		});
	}

	private void setUpAdapters(){
		projectsAdapter = new ProjectAdapter(ShareActivity.this, android.R.layout.simple_spinner_dropdown_item, projects);
		projectSpinner.setAdapter(projectsAdapter);
		stepsAdapter = new StepsAdapter(ShareActivity.this, com.bip_public_android.R.layout.activity_share, steps);
		stepSpinner.setAdapter(stepsAdapter);
	}

	private void handleSendImage(Intent intent) {
		Log.d(TAG, "Got one image to upload");
		imageToUpload = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (imageToUpload != null) {

			Bitmap bitmap = null;
			try {
				bitmap = PictureHorizontalLayout.resizeBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageToUpload), 285);

			} catch (FileNotFoundException e) {
				Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ImageView child = new ImageView(getApplicationContext());
			child.setImageBitmap(bitmap);
			if (imageHolder!=null) {
				imageHolder.addView(child);
			}else{
				Log.d(TAG, "FML");
			}
		}
	}

	private void handleSendMultipleImages(Intent intent) {
		imagesToUpload = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
		if (imagesToUpload != null) {
			for(Uri imageUri : imagesToUpload){
				Bitmap bitmap = null;
				try {
					bitmap = PictureHorizontalLayout.resizeBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri), 285);

				} catch (FileNotFoundException e) {
					Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				ImageView child = new ImageView(getApplicationContext());
				child.setImageBitmap(bitmap);
				if (imageHolder!=null) {
					imageHolder.addView(child);
				}else{
					Log.d(TAG, "FML");
				}
			}
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		//Getting username to load their projects
		if(preferences.contains("AuthToken")){
			if(preferences.contains("Username")){
				username = preferences.getString("Username", null);
				loadUserProjectsFromAPI(Constant.USER_PROFILE_URL+"/"+username+".json");
			}
		} else{
			Intent intent = new Intent(ShareActivity.this, LoginActivity.class);
			intent.putExtra("from_share", true);
			startActivityForResult(intent, 0);
		}
	}

	private void loadUserProjectsFromAPI(String userURL){
		GetProjectsTask getProjectsTask = new GetProjectsTask(ShareActivity.this);
		getProjectsTask.setMessageLoading("Loading projects..");
		getProjectsTask.execute(userURL);
	}


	// fetch projects from the user's profile page
	public class GetProjectsTask extends UrlJsonAsyncTask {

		private ProgressDialog dialog;

		public GetProjectsTask(Context context){
			super(context);
		}

		@Override
		protected void onPreExecute(){
			dialog = new ProgressDialog(ShareActivity.this);
			dialog.setMessage("Loading projects..");
			dialog.show();
			dialog.setCanceledOnTouchOutside(false);
		}

		@Override
		protected void onPostExecute(JSONObject json){
			projects.clear();
			try{
				//Adding all of the projects to the list
				JSONArray jsonProjects = json.getJSONObject("data").getJSONArray("projects");
				int length = jsonProjects.length();
				
				JSONObject userInfo = json.getJSONObject("data").getJSONObject("user");
				//Updating the user's id in order to correctly upload images/videos
				int userid = userInfo.getInt("id");
				userID = userid;
				
				Editor editor = preferences.edit();
				editor.putInt("user_id", userid);
				editor.commit();

				for(int i=0; i<length;i++){
					JSONObject projObj = jsonProjects.getJSONObject(i);
					String title = projObj.getString("title");
					int id = projObj.getInt("id");
					boolean built = projObj.getBoolean("built");
					boolean collaborative = projObj.getBoolean("collaborative?");
					String image_path = null;
					if(projObj.getJSONObject("image_path").length()>0)
						image_path = projObj.getJSONObject("image_path").getString("preview");
					Project tempProj = new Project(title, image_path, id, built);
					tempProj.setCollaborative(collaborative);
					projects.add(tempProj);
				}               

			} catch(JSONException e){
				Log.d(TAG, ""+e.getClass().toString());
				Log.d(TAG, ""+e.getMessage());
			} finally{
				super.onPostExecute(json);
				if(dialog.isShowing()){
					dialog.dismiss();
				}
				//Refresh the projects dropdown
				projectsAdapter.notifyDataSetChanged();
			}
		}
	}

	private class ProjectAdapter extends ArrayAdapter<Project>{

		public ProjectAdapter(Context context, int resource,
				List<Project> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent){
			final Holder holder;
			if(convertView == null){
				holder = new Holder();
				convertView = new TextView(ShareActivity.this);
				holder.projectName = (TextView) convertView;
				convertView.setTag(holder);
			}else{
				holder = (Holder) convertView.getTag();
			}
			holder.projectName.setPadding(tenDP, tenDP, tenDP, tenDP);
			holder.projectName.setTextColor(Color.BLACK);
			holder.projectName.setText(getItem(position).getName());

			return convertView;

		}

		@Override
		public View getView (int position, View convertView, ViewGroup parent){
			final Holder holder;
			if(convertView == null){
				holder = new Holder();
				convertView = new TextView(ShareActivity.this);
				holder.projectName = (TextView) convertView;
				convertView.setTag(holder);
			}else{
				holder = (Holder) convertView.getTag();
			}
			holder.projectName.setTextColor(Color.BLACK);
			holder.projectName.setText(getItem(position).getName());

			return convertView;
		}

		private class Holder{
			private TextView projectName;
		}

	}

	private class StepsAdapter extends ArrayAdapter<ParcelStep>{

		public StepsAdapter(Context context, int resource,
				List<ParcelStep> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent){
			final Holder holder;
			if(convertView == null){
				holder = new Holder();
				convertView = new TextView(ShareActivity.this);
				holder.projectName = (TextView) convertView;
				convertView.setTag(holder);
			}else{
				holder = (Holder) convertView.getTag();
			}
			//Special case to display the dummy step so users can add step
			if(getItem(position).getId() < 0){
				holder.projectName.setBackgroundResource(com.bip_public_android.R.drawable.custom_button_blue);
			}else{
				//remove the blue background if it has it
				holder.projectName.setBackgroundResource(0);
			}
			holder.projectName.setPadding(tenDP, tenDP, tenDP, tenDP);
			holder.projectName.setTextColor(Color.BLACK);
			holder.projectName.setText(getItem(position).getName());

			return convertView;

		}

		@Override
		public View getView (int position, View convertView, ViewGroup parent){
			final Holder holder;
			if(convertView == null){
				holder = new Holder();
				convertView = new TextView(ShareActivity.this);
				holder.projectName = (TextView) convertView;
				convertView.setTag(holder);
			}else{
				holder = (Holder) convertView.getTag();
			}
			holder.projectName.setTextColor(Color.BLACK);
			holder.projectName.setText(getItem(position).getName());

			return convertView;
		}

		private class Holder{
			private TextView projectName;
		}

	}

	/**
	 *  Load project steps from build in progress website
	 * @param url
	 */
	public void loadProjectStepsFromAPI(String url){
		if(Method.isNetworkAvailable(this)){
			GetProjectStepsTask getProjectStepsTask = new GetProjectStepsTask(ShareActivity.this);
			getProjectStepsTask.setMessageLoading("Loading steps..");
			getProjectStepsTask.execute(url);
		}else{
			Toast.makeText(getApplicationContext(), "No internet connection available. Refresh later.",  Toast.LENGTH_LONG).show();
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
			dialog = new ProgressDialog(ShareActivity.this);
			dialog.setMessage("Loading steps..");
			dialog.show();
			dialog.setCanceledOnTouchOutside(false);
		}

		@Override
		protected void onPostExecute(JSONObject json){
			steps.clear();
			//Boolean to check if the data was gathered successfully
			boolean successful = true;
			boolean isRemix;

			try{

				//Getting the project's info
				JSONObject jsonProject = json.getJSONObject("data").getJSONObject("project");
				isRemix = jsonProject.getBoolean("remix?"); // gets whether the project is a remix of another one

				//Getting all the steps from the json object
				JSONArray jsonSteps = json.getJSONObject("data").getJSONArray("steps");
				int length = jsonSteps.length();
				for(int i=0; i<length; i++){
					String name = (jsonSteps.getJSONObject(i).getString("name"));

					String getJsonImagePath;
					String getJsonVideoPath;
					if(isRemix){ // if it is a remixed project we need to get the remixed images
						getJsonImagePath = "remix_image_path";
						getJsonVideoPath = "remix_video_path";
					}else{ // we get the normal image and video paths
						getJsonImagePath = "image_path";
						getJsonVideoPath = "video";
					}

					//Adding the step name to the string array in order to correctly display in list view
					String publishedDate = jsonSteps.getJSONObject(i).getString("published_on_formatted");
					int id = (jsonSteps.getJSONObject(i).getInt("id"));
					int position = (jsonSteps.getJSONObject(i).getInt("position"));

					JSONArray jsonArray = jsonSteps.getJSONObject(i).getJSONArray("images");

					//List of multimedia to add to a step
					ArrayList<Multimedia> multList = new ArrayList<Multimedia>();

					if (jsonArray != null) {
						int len = jsonArray.length();
						for (int j=0;j<len;j++){
							JSONObject stepObj = jsonArray.getJSONObject(j);
							int imageId = stepObj.getInt("id");
							int imagePosition = stepObj.getInt("position");
							String previewUrl = stepObj.getJSONObject(getJsonImagePath).
									getJSONObject("preview").getString("url");
							String thumbUrl = stepObj.getJSONObject(getJsonImagePath).
									getJSONObject("thumb").getString("url");
							String imgUrl = stepObj.getJSONObject(getJsonImagePath).getString("url");
							String sqThumbUrl = stepObj.getJSONObject(getJsonImagePath).
									getJSONObject("square_thumb").getString("url");

							boolean hasVideo = true;
							boolean isFromEmbeddedVideo = false;
							String videoUrl = null;
							String videoWebm = null;
							int videoRotation = 0; // default to 0

							try{
								JSONObject videoObj = stepObj.getJSONObject(getJsonVideoPath);

								//If the video has an embedded video path
								isFromEmbeddedVideo = videoObj.getString("embed_url").length() > 0;
								if(isFromEmbeddedVideo){
									videoUrl = videoObj.getString("embed_url");
								}else{//The video is not embedded
								    videoRotation = videoObj.getInt("rotation");
									videoUrl = videoObj.getJSONObject("video_path").
											getString("url");
									//videoWebm = videoObj.getJSONObject("video_path").getJSONObject("webm").getString("url");
								}

							}catch (JSONException e){
								//If we have exception, the image had no video associated to it
								hasVideo = false;
							}

							Multimedia stepMedia = null;
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
					ParcelStep addStep = new ParcelStep(name, id, position,i, publishedDate);
					//adding all multimedia to the step
					addStep.setMultimediaList(multList);
					
					//Assure that the last step ID is updated when we load steps
					lastStepId = id;

					steps.add(addStep);
				}
				//Dummy step for users to click on and add step
				
				ParcelStep dummyStep = new ParcelStep("+Add New Step", -1, -1, -1, null);
				steps.add(dummyStep);


			} catch(JSONException e){
				Log.e(TAG, "exception", e);
				//We didn't gather the data successfully and caught an exception
				successful = false;

			} finally{
				super.onPostExecute(json);

				if(dialog.isShowing()){
					dialog.dismiss();
				}
				if(successful){

				}else{
					Toast.makeText(getApplicationContext(), "Error loading steps.", Toast.LENGTH_SHORT).show();
				}
				stepsAdapter.notifyDataSetChanged();
			}
		}
	}

	private class ProjectSelectListener implements AdapterView.OnItemSelectedListener{

		@Override
		public void onItemSelected(AdapterView<?> parent, View v, int position,
				long id) {
			selectedProject = (Project)parent.getItemAtPosition(position);
			String stepsUrl = Constant.PROJECT_URL+selectedProject.getId()+"/steps.json";
			loadProjectStepsFromAPI(stepsUrl);
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Freak out here

		}
	}

	private class StepSelectListener implements AdapterView.OnItemSelectedListener{

		@Override
		public void onItemSelected(AdapterView<?> parent, View v, int position,
				long id) {
			//User clicked the add new step button HANDLE ADDING NEW STEP
			if ( ((ParcelStep)parent.getItemAtPosition(position)).getId() < 0 ){
//				stepSpinner.setBackgroundResource(R.drawable.custom_button_blue);
				new NewStepDialog(ShareActivity.this, selectedProject.getId(), lastStepId, auth_token).show();
			}else{
//				stepSpinner.setBackgroundResource(0);
			}

		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Freak out here

		}

	}

	public int dpToPx(int dp) {
		DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
		int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));       
		return px;
	}
}
