package com.bip_public_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.example.helperClass.Constant;
import com.example.helperClass.Method;
import com.example.helperClass.Method.DeleteProjectTask;
import com.example.helperClass.Method.Logout;
import com.example.helperClass.Project;
import com.savagelook.android.UrlJsonAsyncTask;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class display all of the projects currently active
 */

public class AllProjectDisplay extends SherlockActivity {
	
	private final static String TAG = "AllProjectDisplay";
	
	/**User's private preferences associated with the app */
    private SharedPreferences preferences;
	//Information stored in the preferences
	private String auth_token;
    private String username;
    
    private String USER_PROJECTS_JSON_URL;
    
    /**
     * Boolean keeping track of whether the projects have already been previously loaded */
    private boolean alreadyLoaded = false;
    
    private ProjectsAdapter projectsAdapter;

	/** Projects gathered from website */
	private ArrayList<Project> projectList = new ArrayList<Project>();
	/** The underline grid view of the model */
	private GridView imagegrid;
	
	/** Images holding the project's to display them quicker */
	private SparseArray<Bitmap> projectImages = new SparseArray<Bitmap>();
	private Boolean[] projectImagesLoading; // used to store whether or not an image is already being loaded (so it doesn't try to run another async task)
	
	private LinearLayout noInternetLayout;
	
	AsyncTask fetchImageAsyncTask; 
	private ArrayList fetchImagesTasks;
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.bip_public_android.R.layout.activity_all_project_display);
		
		noInternetLayout = (LinearLayout) findViewById(com.bip_public_android.R.id.no_internet_display);

		if(!Method.isNetworkAvailable(this)){
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View noInternetView = inflater.inflate(com.bip_public_android.R.layout.no_internet_view, null);
			noInternetLayout.addView(noInternetView);
			Toast.makeText(this, "No internet connection available. Check your connection settings", Toast.LENGTH_LONG).show();
		}
		
		preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
        auth_token = preferences.getString("AuthToken", "");
        username = preferences.getString("Username", null);
        
        USER_PROJECTS_JSON_URL = Constant.USER_PROFILE_URL+"/"+username+".json?auth_token="+auth_token;
        
		setUpViews();
		setUpAdapter();
	}
	
	private void setUpViews(){

		getSupportActionBar().setDisplayShowTitleEnabled(false); // hide build in progress title from toolbar

		imagegrid = (GridView) findViewById(com.bip_public_android.R.id.gridview_all_project_display);
		
		//Allow users to long click items in the grid
		registerForContextMenu(imagegrid);

		Button addProject = (Button) findViewById(com.bip_public_android.R.id.add_project_all_project_display);

		addProject.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (Method.isNetworkAvailable(getApplicationContext())) {
					new AddProjectTask().execute(Constant.NEW_PROJECT_URL + "?auth_token=" + auth_token);
				} else {
					Toast.makeText(getApplicationContext(), "No internet connection available. Check your connection settings", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	private void setUpAdapter(){
		projectsAdapter = new ProjectsAdapter(projectList);
		imagegrid.setAdapter(projectsAdapter);
        imagegrid.setClickable(true);
        imagegrid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String projectTitle = projectList.get(position).getName();
				int projectID = projectList.get(position).getId();

				// cancel GetProjectsTask
				Log.d(TAG, "number of fetch images tasks running: " + fetchImagesTasks.size());
				for (int i = 0; i < fetchImagesTasks.size(); i++) {
					AsyncTask currentTask = (AsyncTask) fetchImagesTasks.get(i);
					if (currentTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
						Log.d(TAG, "cancelling task " + i);
						currentTask.cancel(true);
					}
				}

				Intent intent = new Intent(
						AllProjectDisplay.this, ProjectDisplay.class);
				intent.putExtra("projectID", projectID);
				intent.putExtra("projectTitle", projectTitle);
				startActivityForResult(intent, Constant.REQUEST_CODE_PROJECT);
			}
		});
	}

	@Override
    public void onResume(){
        super.onResume();
        //Getting username to load their projects
        if(preferences.contains("AuthToken")){
        	if(preferences.contains("Username")){
        		if(!alreadyLoaded){
		        	username = preferences.getString("Username", null);
		        	//Log.d(TAG, "running loadUserProjectsfromApi from onResume");
		        	loadUserProjectsFromAPI(USER_PROJECTS_JSON_URL);
        		}
        	}
        } else{
            Intent intent = new Intent(AllProjectDisplay.this, LoginActivity.class);
            startActivityForResult(intent, 0);
        }
	}

	private void checkUsername (String username){
		if ( !username.equals(this.username) ){
			//TODO: handle usernames not being equal. May be users are logged in incorrectly
		}
	}
	
	private void storeUserId (int id){
		Editor editor = preferences.edit();
        editor.putInt("UserID", id);
        editor.commit();
	}
	
	// Load user's projects from build in progress website 
    public void loadUserProjectsFromAPI(String url){
    	if(Method.isNetworkAvailable(getApplicationContext())){
    		if(noInternetLayout.getChildCount()>0){
    			noInternetLayout.removeAllViews();
    		}
    		fetchImagesTasks = new ArrayList();
    		Log.d(TAG, "starting getProjectsTask from loadUserProjectsFromAPI");
    		GetProjectsTask getProjectsTask = new GetProjectsTask(AllProjectDisplay.this);
	        getProjectsTask.setMessageLoading("Loading projects..");
	        getProjectsTask.execute(url);
    	}else{
    		//TODO probably message of something here
    	}
    }

	/**
	 * The adapter to handle displaying projects
	 */
	public class ProjectsAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private ArrayList<Project> projects;

		public ProjectsAdapter(ArrayList<Project> project) {
			this.projects = project;
			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public ProjectsAdapter() {
			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public void removedProject(int position){
			projects.remove(position);
		}
		
		public void addProject(Project project){
			projects.add(project);
		}
		
		@Override
		public int getCount() {
			return projects.size();
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
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			Project project = projects.get(position);
			
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(com.bip_public_android.R.layout.project_display, null);
				holder.projectDescription = (TextView) convertView
						.findViewById(com.bip_public_android.R.id.project_description_all_project_display);
				holder.projectImage = (ImageView) convertView
						.findViewById(com.bip_public_android.R.id.project_avatar_all_project_display);
				convertView.setTag(holder);
			}
			// picture is already there
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
            //Setting project names
            holder.projectDescription.setText(project.getName());

			//Can't figure out how to get null object from null json value, just check length	
			if (project.getImagePath()!=null){

				holder.projectImage.setVisibility(View.VISIBLE);

				if(projectImages.get(position)!= null){ // the image has already been loaded
				    holder.projectImage.setImageBitmap(projectImages.get(position));
				}else{
				    if(projectImagesLoading[position] == null){
	                    //get image and set the projectImagesLoading value to true
	                    fetchImageAsyncTask = new Method.GetBitmapFromURL(projectImages, position, imagegrid).execute(project.getImagePath());
	                    Log.d(TAG, "adding fetchImagesAsyncTask for project " + project.getName());
	                    fetchImagesTasks.add(fetchImageAsyncTask);
	                    projectImagesLoading[position] = true;
	                }
				}				
			}else{
				holder.projectImage.setVisibility(View.INVISIBLE);
			}
			
			if(project.getBuilt()){
				ImageView built = (ImageView) convertView.findViewById(com.bip_public_android.R.id.project_built);
				built.setVisibility(View.VISIBLE);
			}else{
				ImageView built = (ImageView) convertView.findViewById(com.bip_public_android.R.id.project_built);
				built.setVisibility(View.GONE);
			}
			if(project.isCollaborative()){
				ImageView collaborative = (ImageView) convertView.findViewById(com.bip_public_android.R.id.project_collaborative);
				collaborative.setVisibility(View.VISIBLE);
			}else{
				ImageView collaborative = (ImageView) convertView.findViewById(com.bip_public_android.R.id.project_collaborative);
				collaborative.setVisibility(View.GONE);
			}

			return convertView;

		}
		/**
		 * The view holder to store the check box and imageView 
		 */
		private class ViewHolder {
			private TextView projectDescription;
			private ImageView projectImage;
		}

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK){
			switch(requestCode){

			// returned from editing a project	
			case Constant.REQUEST_CODE_PROJECT:
			    if(data.getBooleanExtra("newStepCreated", true)){
                    // refresh the list of projects to add the new project
			        Log.d(TAG, "running loadUserProjectsfromApi from onActivityResult");
			        loadUserProjectsFromAPI(USER_PROJECTS_JSON_URL);
			        return;
			    }
			}
		}
	}
	
	// fetch projects from the user's profile page
    public class GetProjectsTask extends UrlJsonAsyncTask{
    	
    	private ProgressDialog dialog;
    	
        public GetProjectsTask(Context context){
            super(context);
        }
        
        @Override
        protected void onPreExecute(){
            dialog = new ProgressDialog(AllProjectDisplay.this);
            dialog.setMessage("Loading projects..");
            dialog.show();
        }
        
		@Override
        protected void onPostExecute(JSONObject json){
		    Log.d(TAG, "in onPostExecute of GetProjectsTask");
			projectList.clear();
            
			try{
            	//Refresh the projects with
            	
                JSONObject userInfo = json.getJSONObject("data").getJSONObject("user");
                String username = userInfo.getString("username");
                
                //Assure that the username stored on the phone is equal to the username from the json
                checkUsername(username);
                
                //Get the user id and store it on the preferences
                int userid = userInfo.getInt("id");
                
                storeUserId(userid);
                
                JSONArray jsonProjects = json.getJSONObject("data").getJSONArray("projects");
                int length = jsonProjects.length();
                Log.d(TAG, "number of projects: " + length);
                // initialize arraylist of fetchimagestasks to keep track of asynctasks
                fetchImagesTasks = new ArrayList();
                projectImagesLoading = new Boolean[length];                
                
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
                    projectList.add(tempProj);
                }  
                Log.d(TAG, "DONE with onPostExecute of GetProjectsTask");
                
            } catch(JSONException e){
            	Log.d(TAG, ""+e.getClass().toString());
            	Log.d(TAG, ""+e.getMessage());
            } finally{
                super.onPostExecute(json);
            	alreadyLoaded = true;
            	Log.d(TAG, "set already loaded to true");
                //Refresh the adapters views
                projectsAdapter.notifyDataSetChanged();
            	if(dialog != null && dialog.isShowing()){
            	    try{
            	        dialog.dismiss();
            	    }catch(Exception ex){
            	        ex.printStackTrace();
            	    }
            		
            	}
            }
        }
		
    }
    
    // Add a new user project
    public class AddProjectTask extends AsyncTask<String, Void, JSONObject>{
        
        private ProgressDialog dialog;
        
        @Override
        protected void onPreExecute(){
            // cancel GetProjectsTask
            Log.d(TAG, "number of fetch images tasks running: " + fetchImagesTasks.size());
            for(int i=0; i< fetchImagesTasks.size(); i++){
                AsyncTask currentTask = (AsyncTask) fetchImagesTasks.get(i);
                if(currentTask.getStatus().equals(AsyncTask.Status.RUNNING)){
                    //Log.d(TAG, "cancelling task " + i);
                    currentTask.cancel(true);
                }
            }     
            
            dialog = new ProgressDialog(AllProjectDisplay.this);
            dialog.setMessage("Creating project..");
            dialog.show();
            dialog.setCanceledOnTouchOutside(false);
        }
        
        @Override
        protected JSONObject doInBackground(String... urls){
            DefaultHttpClient client = new DefaultHttpClient();
            JSONObject json = new JSONObject();
            String response = null;
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            HttpGet get = new HttpGet(urls[0]);    
            get.setHeader("Accept", "application/json");
            get.setHeader("Content-Type", "application/json");
                    
            try{
                response = client.execute(get, responseHandler);
                json = new JSONObject(response);
            } catch(HttpResponseException e){
                e.printStackTrace();
                Log.e("ClientProtocol", ""+e);
            } catch(IOException e){
                e.printStackTrace();
                Log.e("IO", "" + e);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return json;
        }
                

        protected void onPostExecute(JSONObject json){
            if(json != null){
                Log.d(TAG, "jsonResponse: " + json);
                
                if(dialog != null && dialog.isShowing()){
                    try{
                        dialog.dismiss();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
                
                // open up the new project
                int newProjectID;
                String newProjectTitle;
                try {
                    newProjectID = json.getInt("id");
                    newProjectTitle = json.getString("title");
                    
                    projectList.clear();
                    
                    // open up new project
                    Intent intent = new Intent(
                            AllProjectDisplay.this, ProjectDisplay.class);                
                    intent.putExtra("projectID", newProjectID); 
                    intent.putExtra("projectTitle", newProjectTitle);
                    intent.putExtra("newProjectCreated", true);
                    startActivityForResult(intent, Constant.REQUEST_CODE_PROJECT);    
                    
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
              
    }   
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(Menu.NONE, com.bip_public_android.R.id.refresh, Menu.NONE, "refresh")
            .setIcon(com.bip_public_android.R.drawable.ic_action_refresh)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        SubMenu optionsMenu = menu.addSubMenu("Options Item");
        optionsMenu.add(Menu.NONE, com.bip_public_android.R.id.logout, Menu.NONE, "Log Out").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        
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
            //Log.d(TAG, "running loadUserProjectsfromApi from refresh");
            loadUserProjectsFromAPI(USER_PROJECTS_JSON_URL);
            return true;
        case com.bip_public_android.R.id.logout:
            Logout logout = new Logout(this);
            logout.setMessageLoading("Logging out..");
            logout.execute(Constant.LOGOUT_URL+"?auth_token=" + auth_token);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
			MenuInflater inflater = getMenuInflater();
		    inflater.inflate(com.bip_public_android.R.menu.project_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	String projectTitle = projectList.get(info.position).getName();
        final int projectID = projectList.get(info.position).getId();
        
	    switch (item.getItemId()) {
	    case com.bip_public_android.R.id.edit_project_context_menu:
            Intent intent = new Intent(
            		AllProjectDisplay.this, ProjectDisplay.class);
            intent.putExtra("projectID", projectID); 
            intent.putExtra("projectTitle", projectTitle);
            intent.putExtra("newProjectCreated", false);
            startActivityForResult(intent, Constant.REQUEST_CODE_PROJECT);
	    	break;
	    case com.bip_public_android.R.id.delete_project_context_menu:
	    	new AlertDialog.Builder(this)
	            .setTitle("Delete Project")
	            .setMessage("Are you sure you want to delete project " + projectTitle + "?")
	            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                
	                @Override
	                public void onClick(DialogInterface dialog, int which) {
	                    new DeleteProjectTask(AllProjectDisplay.this)
	                    	.execute(Constant.PROJECT_URL+projectID+"?auth_token=" + auth_token);
	                    projectsAdapter.removedProject(info.position);
	                    projectsAdapter.notifyDataSetChanged();
	                }
	            })
	            .setNegativeButton("Cancel", null)
	            .show();
	        
	    	break;
	    }
	    return super.onContextItemSelected(item);
	}
	
	public static class ProjectImageView extends ImageView{

		public ProjectImageView(Context context) {
			super(context);
			Resources r = getResources();
			float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
			LayoutParams imgParam = new LayoutParams(LayoutParams.MATCH_PARENT, (int)px);
			this.setLayoutParams(imgParam);
			this.setVisibility(View.INVISIBLE);
			this.setMaxHeight((int) px);
			this.setScaleType(ScaleType.CENTER_CROP);
			this.setAdjustViewBounds(true);
		}
		
	}
}
