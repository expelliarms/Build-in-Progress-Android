package com.bip_public_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.helperClass.Constant;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

public class OverviewDisplay extends SherlockActivity {

	//	private static final String TAG = "OverviewDisplay";

	private SharedPreferences preferences;
	private String auth_token;

	private boolean editMode = false;
	private boolean editTextClicked = false;
	private boolean backPressedFromActionBar = false;

	private Menu editMenu;    
	private EditText stepDescriptionEditText;
	private TextView stepDescriptionTextView;
	private TextView stepDescriptionLabel;
	private ViewSwitcher stepDescriptionSwitcher;

	private String projectDescription;
	private int projectID;

	private final static String projectOverviewHelpText= "Type a description of your project here!";

	@Override
	protected void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		setContentView(com.bip_public_android.R.layout.activity_step_display);

		String projectTitle = getIntent().getStringExtra("projectTitle");
		projectDescription = getIntent().getStringExtra("projectDescription");
		projectID = getIntent().getIntExtra("projectID", -1);

		// add back button to action bar
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(projectTitle);

		//Getting preferences and auth_token
		preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
		auth_token = preferences.getString("AuthToken", "");

		setUpViews();

	}

	private void setUpViews(){
		//Accessing most of views
		LinearLayout stepImagesContainer = (LinearLayout) findViewById(com.bip_public_android.R.id.step_images_container);
		// TextView stepPublishedDate = (TextView) findViewById(R.id.step_date_published);
		TextView stepName = (TextView) findViewById(com.bip_public_android.R.id.step_name);
		Button deleteStepButton = (Button) findViewById(com.bip_public_android.R.id.delete_step_button);
		stepDescriptionLabel = (TextView) findViewById(com.bip_public_android.R.id.step_description_label);
		stepDescriptionSwitcher = (ViewSwitcher) findViewById(com.bip_public_android.R.id.step_description_switcher);
		stepDescriptionEditText = (EditText) stepDescriptionSwitcher.
				findViewById(com.bip_public_android.R.id.step_description_edit_view);
		stepDescriptionTextView = (TextView) stepDescriptionSwitcher.findViewById(com.bip_public_android.R.id.step_description);

		// hide title from toolbar
		getSupportActionBar().setDisplayShowTitleEnabled(false);


		if(!projectDescription.equals("null")){//assuring that the description isn't null
			stepDescriptionTextView.setText(projectDescription); //setting the already defined description to the project
		}

		//Set name at top of the activity
		stepName.setText("Project Description");

		// hide elements of step display for project overview
		stepDescriptionLabel.setVisibility(View.GONE);

		stepImagesContainer.setVisibility(View.GONE);
		deleteStepButton.setVisibility(View.GONE);
		stepDescriptionEditText.setHint(projectOverviewHelpText);
		stepDescriptionTextView.setHint(projectOverviewHelpText);


		// step description clicked - toggle to edit mode
		stepDescriptionSwitcher.setOnClickListener(new editStepModeListener());

		// prevent enters in the stepDescriptionEditText
		stepDescriptionEditText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return keyCode == KeyEvent.KEYCODE_ENTER;
			}
		});

		//Setting editor action when the user clicks enter, since we don't want them to be able to
		//have multiple paragraphs on their project description
		//TODO get this to work
		stepDescriptionEditText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_DONE ||(event.getAction() == KeyEvent.ACTION_UP &&
						event.getKeyCode() == KeyEvent.KEYCODE_ENTER)){
					saveDescription();
				}
				return false;
			}
		});
	}

	private void editStep(){
		if(!projectDescription.equals("null")){
			stepDescriptionEditText.setText(projectDescription);
		}
		stepDescriptionSwitcher.setDisplayedChild(1);

		// autofocus on the description of the project overview
		stepDescriptionEditText.requestFocus();
		((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
		.showSoftInput(stepDescriptionEditText, InputMethodManager.SHOW_FORCED);

	}

	// toggle edit mode for step (when edit button clicked
	public class editStepModeListener implements OnClickListener{

		public void onClick(View v){
			editMode = true;
			MenuItem editMenuItem = editMenu.findItem(com.bip_public_android.R.id.edit);
			editTextClicked = true;
			onOptionsItemSelected(editMenuItem);
			editStep();
			editTextClicked = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.add(Menu.NONE, com.bip_public_android.R.id.edit, Menu.NONE, "edit")
		.setIcon(com.bip_public_android.R.drawable.ic_action_edit)
		.setTitle("edit")
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		this.editMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){

		if(item.getTitle()=="edit"){
			editMode = false;
		}
		// Handle item selection
		switch(item.getItemId()){
		case com.bip_public_android.R.id.edit:
			if(editMode){
				// edit action bar icon
				item.setIcon(com.bip_public_android.R.drawable.ic_action_edit);
				item.setTitle("edit");

				editMode = false;
				saveDescription();

			}else{
				editMode = true;
				item.setIcon(com.bip_public_android.R.drawable.ic_action_accept);
				item.setTitle("save");
				if(editTextClicked==false){
					// user clicked on menu icon, not directly on editText
					editStep();
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

	private void saveDescription() {
		String tempDesc = stepDescriptionEditText.getEditableText().toString();
		if(tempDesc.length() > 0){
			projectDescription = tempDesc;
			//TODO handle saving project description here
			new updateProject().execute(tempDesc);
		}

		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);	 

		imm.hideSoftInputFromWindow(stepDescriptionEditText.getWindowToken(), 0);

		//Reverting back to unchangeable state
		stepDescriptionSwitcher.setDisplayedChild(0);
		stepDescriptionTextView.setText(tempDesc);
	}

	@Override
	public void onBackPressed(){

		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);   
		imm.hideSoftInputFromWindow(stepDescriptionEditText.getWindowToken(), 0);

		// check if user is pressing back from edit
		if(!backPressedFromActionBar && editMode){
			stepDescriptionSwitcher.setDisplayedChild(0);

			// cycle edit icon
			MenuItem editMenuItem = editMenu.findItem(com.bip_public_android.R.id.edit);
			onOptionsItemSelected(editMenuItem);

		}else{
			Intent returnIntent = getIntent();
			returnIntent.putExtra("projectDescription", projectDescription);
			setResult(RESULT_OK, returnIntent);
			finish();
		}
	}

	// Update name of project
	public class updateProject extends AsyncTask<String, Void, HttpEntity>{
		String description; // new project title

		@Override
		protected HttpEntity doInBackground(String... params){
			DefaultHttpClient client = new DefaultHttpClient();

			description = params[0];
			String url = Constant.PROJECT_URL+projectID+"?auth_token=" + auth_token;
			HttpPut put = new HttpPut(url);
			JSONObject holder = new JSONObject();
			JSONObject projectObj = new JSONObject();

			try{
				projectObj.put("description", description);
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

			}else{
				Toast.makeText(getApplicationContext(), "Error saving description", Toast.LENGTH_SHORT).show();
			}

		}     
	}
}
