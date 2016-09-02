package com.example.helperClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bip_public_android.LoginActivity;
import com.bip_public_android.ProjectDisplay;
import com.bip_public_android.ProjectDisplay.CustomizedStepAdapter;
import com.bip_public_android.StepDisplay;
import com.savagelook.android.UrlJsonAsyncTask;

public class Method {
	
    private final static String TAG = "Method";
	
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	/**
	 * <code> UrlJsonAsyncTask <code> used to log the user out of the app. It will tell the bip server
	 * that the user is logging out and then delete the user's information stored on the phone.
	 * (Username, auth_token, and user_id)
	 * @author jan
	 *
	 */
	public static class Logout extends UrlJsonAsyncTask{
		Context context;
		SharedPreferences preferences;

		public Logout(Context context){
			super(context);
			this.context = context;
			preferences = context.getSharedPreferences("CurrentUser", Context.MODE_PRIVATE);            
		}

		@Override
		protected JSONObject doInBackground(String... urls){
			DefaultHttpClient client = new DefaultHttpClient();
			HttpDelete delete = new HttpDelete(urls[0]);
			String response;
			JSONObject json = new JSONObject();
			try{
				delete.setHeader("Content-Type", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				response = client.execute(delete, responseHandler);
				json = new JSONObject(response);
			} catch(HttpResponseException e){
				e.printStackTrace();
				Log.e("ClientProtocol", "" + e);
			} catch(IOException e){
				e.printStackTrace();
				Log.e("IO", ""+e);

			} catch(JSONException e){
				e.printStackTrace();
				Log.e("JSON", ""+e);
			}
			return json;
		}

		@Override
		protected void onPostExecute(JSONObject json){
			SharedPreferences.Editor editor = preferences.edit();

			//Even if the the user gets an error they will be successfully logged out
			if(preferences.contains("AuthToken")){
				editor.remove("AuthToken");
			}
			if(preferences.contains("Username")){
				editor.remove("Username");
			}
			if(preferences.contains("user_id")){
				editor.remove("user_id");
			}
			editor.commit();

			//Launch welcome activity to allow user to log back in
			Intent intent = new Intent(context, LoginActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			((Activity) context).startActivityForResult(intent, 0);
			((Activity) context).finish();
			Toast.makeText(context, "Logged out!", Toast.LENGTH_SHORT).show();

			super.onPostExecute(json);

		}
	}
	
	/**
	 * Gets a bitmap associated to the image from the specified url.
	 * Pass the url as a string in the execute method.
	 * @author jan
	 *
	 */
	public static class GetBitmapFromURL extends AsyncTask<String, Void, Bitmap>{

		private SparseArray<Bitmap> toSaveArray;
		private int positionInArray;
		private GridView gridToUpdate;
		
		/**
		 * Public constructor for the class.
		 * @param array - the array containing the bitmaps for images
		 * @param position - position of the bitmap in the array
		 * @param updatedView - gridview of images from the array
		 */
		public GetBitmapFromURL (SparseArray<Bitmap> array, int position, GridView updatedView){
			this.toSaveArray = array;
			this.positionInArray = position;
			this.gridToUpdate = updatedView;
		}

		@Override
		protected Bitmap doInBackground(String... url) {
			Bitmap bm = null;
			try{
				HttpGet httpRequest = new HttpGet();
				httpRequest.setURI(new URI(url[0]));
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = httpclient.execute(httpRequest);
				if(response != null){
					HttpEntity entity = response.getEntity();
					BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
					bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
				}
				httpRequest.abort();
			} catch (IOException e){
				Log.d(Constant.TAG, "" + e.getMessage());
			} catch (URISyntaxException e) {
				Log.d(Constant.TAG, "" + e.getMessage());
			}
			return bm;
		}

		@Override
		protected void onPostExecute(Bitmap result){
			//Save the bitmap to the array
			toSaveArray.put(positionInArray, result);
			gridToUpdate.invalidateViews();
		}
	}




	/**
	 * <code> public static class </code>
	 * <p>
	 * Class used to load and set the view of an ImageView from a given
	 * URL
	 */
	public static class GetImageFromURL extends AsyncTask<String, Void, Bitmap>{
		private final String TAG = "GetImageFromUrl";
		private ImageView imv;
		private ProgressBar pb;

		private boolean running = true;

		public GetImageFromURL(ImageView imageView, Context context, ProgressBar mediaProgress) {
			this.imv = imageView;
			if(mediaProgress != null){
				this.pb = mediaProgress;
				this.pb.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected Bitmap doInBackground(String... arg0) {
            Bitmap bm = null;
		    if(running){
	            try{
	                HttpGet httpRequest = new HttpGet();
	                httpRequest.setURI(new URI(arg0[0]));
	                HttpClient httpclient = new DefaultHttpClient();
	                HttpResponse response = httpclient.execute(httpRequest);
	                if(response != null){
	                    HttpEntity entity = response.getEntity();
	                    BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
	                    bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
	                }
	                httpRequest.abort();
	            } catch (IOException e){
	                Log.d(TAG, ""+e.getMessage());
	            } catch (URISyntaxException e) {
	                Log.d(TAG, ""+e.getMessage());
	            }
		    }
		    return bm;
		}

		@Override
		protected void onPostExecute(Bitmap result){
			if(imv!=null){
				if (imv.getParent() instanceof RelativeLayout){
					imv.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
				}else if(imv.getParent() instanceof LinearLayout){
					imv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
				}else if(imv.getParent() instanceof FrameLayout){
					imv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
				}
				// reveal play icon if the image belongs to a video
				if(( imv.getParent()) != null){
					if(com.bip_public_android.R.id.video_image_container == ((ViewGroup) imv.getParent()).getId()){
						ViewGroup container = (ViewGroup) imv.getParent();
						ImageView videoIcon = (ImageView) container.findViewById(com.bip_public_android.R.id.video_icon);
						if(videoIcon != null){
							int videoIconDimension = (int) Math.round(imv.getHeight()*0.5);
							RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(videoIconDimension, videoIconDimension);
							layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
							videoIcon.setLayoutParams(layoutParams);
							videoIcon.setVisibility(View.VISIBLE);
						}
					}
				}

				if(pb!=null){
					pb.setVisibility(View.GONE);
				}
				Animation animationFadeIn = AnimationUtils.loadAnimation(this.imv.getContext(), com.bip_public_android.R.anim.fade_in);
				this.imv.startAnimation(animationFadeIn);
				imv.setImageBitmap(result);
			}
		}

		@Override
		protected void onCancelled(){
		    Log.d(TAG, "CANCELLED LOADING IMAGES");
		    running = false;
		}
	}

	public static class DeleteStepTask extends AsyncTask<String, Void, HttpEntity>{
		@Override
		protected HttpEntity doInBackground(String... urls){
			DefaultHttpClient client = new DefaultHttpClient();
			HttpDelete delete = new HttpDelete(urls[0]);

			delete.setHeader("Content-Type", "application/json");

			HttpResponse response = null;

			try{
				response = client.execute(delete);
			} catch(HttpResponseException e){
				e.printStackTrace();
				Log.e("ClientProtocol", ""+e);
			} catch(IOException e){
				e.printStackTrace();
				Log.e("IO", ""+e);
			}

			HttpEntity result = null;

			if(response != null){
				result = response.getEntity();
			}

			return result;
		}

		protected void onPostExecute(HttpEntity result){

/*			if(result!=null){
				// go back to the project screen
			}*/
		}
	}
	/**
	 * Delete Project. Pass the url of the project to delete as a string in the first parameter
	 * of the execute method.
	 */
	public static class DeleteProjectTask extends AsyncTask<String, Void, HttpEntity>{

		private Context context;

		public DeleteProjectTask(Context context){
			this.context = context;
		}

		@Override
		protected HttpEntity doInBackground(String... urls){
			DefaultHttpClient client = new DefaultHttpClient();
			HttpDelete delete = new HttpDelete(urls[0]);

			delete.setHeader("Content-Type", "application/json");

			HttpResponse response = null;

			try{
				response = client.execute(delete);
			} catch(HttpResponseException e){
				e.printStackTrace();
				Log.e("ClientProtocol", ""+e);
			} catch(IOException e){
				e.printStackTrace();
				Log.e("IO", ""+e);
			}

			HttpEntity result = null;

			if(response != null){
				result = response.getEntity();
			}
			return result;
		}
		@Override
		protected void onPostExecute(HttpEntity result){

			if(result!=null){
				// go back to the project screen
				Toast.makeText(context, "Project Deleted!", Toast.LENGTH_LONG).show();
			}
		}
	}
	/**
	 * <code> extends AsyncTask<Object, Void, HttpEntity> </code>
	 * <p>
	 * Class used to update the step's information without affecting the step's date
	 * Takes in context for the constructor to display a message whenever the
	 * step's date has been updated.
	 * <p>
	 * Executes a background thread for updating step params are, in order, (stepURL, auth_token, stepName
	 * stepDescription, isLast)
	 * @param params object.. containing the information for the newly updated step
	 *
	 */
	public static class UpdateStepNameTask extends AsyncTask<Object, Void, HttpEntity>{

		private Context context;

		public UpdateStepNameTask(Context context){
			this.context = context;
		}

		/**
		 * OnPreExecute: show a toast message that it's saving the step
		 */
		@Override
		protected void onPreExecute(){
			Toast.makeText(context , "Updating Step..", Toast.LENGTH_SHORT).show();
		}

		/**
		 * Background thread for updating step params are, in order, (stepURL, auth_token, stepName
		 * stepDescription, isLast)
		 * @param params object.. containing the information for the newly updated step
		 */
		@Override
		protected HttpEntity doInBackground(Object... params){

			DefaultHttpClient client = new DefaultHttpClient();

			String stepURL = (String) params[0];
			String auth_token = (String) params[1];
			String stepName = (String) params[2];
			String stepDescription = (String) params[3];
			String url= stepURL+"?auth_token=" + auth_token;

			HttpPut put = new HttpPut(url);
			JSONObject holder = new JSONObject();
			JSONObject stepObj = new JSONObject();

			try{
				stepObj.put("name", stepName);
				stepObj.put("description", stepDescription);
				holder.put("step", stepObj);

				StringEntity se = new StringEntity(holder.toString());
				put.setEntity(se);
				put.setHeader("Content-Type", "application/json");

			} catch(Exception e){
				Log.d(Constant.TAG, ""+e.getMessage());
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
				Toast.makeText(context , "Updated Step!", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(context, "Error updating step", Toast.LENGTH_SHORT).show();
			}
		}
	}


	public static class UpdateLabelNameTask extends AsyncTask<Object, Void, HttpEntity>{

		private Context context;

		public UpdateLabelNameTask(Context context){
			this.context = context;
		}

		/**
		 * OnPreExecute: show a toast message that it's saving the step
		 */
		@Override
		protected void onPreExecute(){
			Toast.makeText(context , "Updating Label..", Toast.LENGTH_SHORT).show();
		}

		/**
		 * Background thread for updating step params are, in order, (stepURL, auth_token, stepName
		 * stepDescription, labelColor)
		 * @param params object.. containing the information for the newly updated step
		 */
		@Override
		protected HttpEntity doInBackground(Object... params){

			DefaultHttpClient client = new DefaultHttpClient();

			String stepURL = (String) params[0];
			String auth_token = (String) params[1];
			String stepName = (String) params[2];
			String labelColor = (String) params[3];
			String url= stepURL+"?auth_token=" + auth_token;

			HttpPut put = new HttpPut(url);
			JSONObject holder = new JSONObject();
			JSONObject stepObj = new JSONObject();

			try{
				stepObj.put("name", stepName);
				stepObj.put("label_color", labelColor);
				Log.d(TAG, "change label color to: "+labelColor);
				holder.put("step", stepObj);

				StringEntity se = new StringEntity(holder.toString());
				put.setEntity(se);
				put.setHeader("Content-Type", "application/json");

			} catch(Exception e){
				Log.d(Constant.TAG, ""+e.getMessage());
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
				Toast.makeText(context , "Updated Label!", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(context, "Error updating label", Toast.LENGTH_SHORT).show();
			}
		}
	}





	/**
	 * <code> extends AsyncTask<Object, Void, HttpEntity> </code>
	 * <p>
	 * Class used to update only the date of a certain step while leaving all other fields
	 * untouched. Takes in context for the constructor to display a message whenever the
	 * step's date has been updated.
	 * <p>
	 * Executes a background thread for updating step params are, in order, (stepURL, auth_token, stepPublishedOnDate
	 * hour, minute)
	 * @param params object.. containing the information for the newly updated step
	 *
	 */

	/**
	 * <code> public static class extends AsyncTask<Object, Void, HttpEntitiy></code>
	 * <p>
	 * Class used to upload images to bip-android-test website.
	 * @param params [ stepID, filepath, auth_token, projectID, userID]
	 *
	 */




	public static class UploadImage extends AsyncTask<Object, Void, HttpEntity>{

		private Context context;
		private CustomizedStepAdapter stepAdapter;
		private ParcelStep uploadStep;
		private String filepath;
		private ImageView image;
		private int position;

		public UploadImage(Context context,ParcelStep step,
				CustomizedStepAdapter stepAdapt, ImageView image, int position){
			this.context = context;
			this.stepAdapter = stepAdapt;
			this.uploadStep = step;
			this.image = image;
			this.position = position; // stepPosition
		}

		@Override
		protected void onPreExecute() {
			// Increasing the number of images that the step is uploading
			if(uploadStep.incTotalUploading() < 2){
				if(stepAdapter!=null){
					stepAdapter.notifyDataSetChanged();
				}
			}
		}

		@Override
		protected HttpEntity doInBackground(Object... params){
			DefaultHttpClient client = new DefaultHttpClient();
			String stepID = (String)params[0];
			filepath = (String)params[1];
			String auth_token = (String)params[2];
			String projectID = (String)params[3];
			Integer userID = (Integer)params[4];

			String filename = Uri.parse(filepath).getLastPathSegment();

			String url = Constant.IMAGE_URL+"?auth_token=" + auth_token;
			Log.d(TAG, "UPLOADIMAGE URL = " + url);

			HttpPost post = new HttpPost(url);
			HttpContext localContext = new BasicHttpContext();
			MultipartEntity imageMPentity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

			try{

				imageMPentity.addPart("project_id", new StringBody(""+projectID));
				imageMPentity.addPart("step_id", new StringBody(stepID));
				imageMPentity.addPart("content_type", new StringBody("image/jpeg"));
				imageMPentity.addPart("filename", new StringBody(filename));
				imageMPentity.addPart("image_path", new FileBody(new File(filepath)));
				imageMPentity.addPart("user_id", new StringBody(""+userID));
				post.setEntity(imageMPentity);

			} catch(Exception e){
				Log.e(StepDisplay.class.getName(), e.getLocalizedMessage(), e);
			}
			HttpResponse response = null;

			try {
				response = client.execute(post, localContext);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			HttpEntity result = null;

			if(response != null){
				result = response.getEntity();
			}


			//moved click listener back to Step Display so it's added in a UI thread and not a worker thread
			StepDisplay.setImageOnClickListener(image, context, filepath, position);

			return result;
		}

		/**
		 * Display notification allowing the user to see how many more files they have
		 * to upload and reducing them whenever they are successfully uploaded.
		 */
		protected void onPostExecute(HttpEntity result){
			// Start Asynctask to add images to imageSources and mediaRes
			String fn = "";
			for (int i = filepath.length()-1; i >= 0; i--){
				String character = filepath.substring(i,i+1);
				if(!character.equals("/")){
					fn = character + fn;
				} else {
					break;
				}
			}
			Log.d(TAG, "calling UpdateImages async task");
			// technically, you're not finished until this finishes,
			// so the color shouldn't change until the post Execute of this function
			new UpdateImages(context, image).execute(fn, filepath);

			// Display notification
			if(result !=null){
				NotificationManager mNotificationManager =
						(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Constant.currentlyUploading.decrementAndGet();
				Constant.totalUploadedImages.incrementAndGet();
				if(Constant.currentlyUploading.get() > 0){
				    String files_count;
				    if (Constant.currentlyUploading.get() == 1) {
				        files_count = "file";
				    } else {
				        files_count = "files";
				    }
					Builder uploadBuilder = new NotificationCompat.Builder(context)
						.setSmallIcon(com.bip_public_android.R.drawable.logo)
						.setContentTitle("Uploading to Build in Progress")
						.setContentText(Constant.currentlyUploading.get()+ " " + files_count +  " uploading");
					Notification uploadNotif = uploadBuilder.build();
					mNotificationManager.notify(Constant.NOTIFICATION_UPLOADING_ID, uploadNotif);
				} else {
				    String uploaded_count;
				    if(Constant.totalUploadedImages.get() > 1){
				        uploaded_count = "files";
				    } else {
				        uploaded_count = "file";
				    }
					Builder uploadedBuilder = new NotificationCompat.Builder(context)
						.setSmallIcon(com.bip_public_android.R.drawable.logo)
						.setContentTitle("Upload complete")
						.setTicker("Upload Complete")
						.setContentText(Constant.totalUploadedImages.get()+" " + uploaded_count + " uploaded to Build in Progress")
						.setAutoCancel(true);
					Notification uploadNotif = uploadedBuilder.build();
					mNotificationManager.notify(Constant.NOTIFICATION_UPLOADING_ID, uploadNotif);
					Constant.totalUploadedImages.getAndSet(0);
				}

				uploadStep.decTotalUploading();
				image.setAlpha(Constant.UPLOADED_ALPHA); // Change opacity after upload

				//Removing one from the step's image upload counter
				if(stepAdapter!=null){
					stepAdapter.notifyDataSetChanged();
				}

				//Removing the path from the list of currently uploading images
				uploadStep.removeCurrentlyUploading(filepath);
			}

		}

	}

	/**
	 * Add images to imageSources and mediaRes given filename.
	 * @author eunice
	 *
	 */
	public static class UpdateImages extends AsyncTask<String, Void, Void> {

		private Context context;
		//private ImageView image;

		public UpdateImages(Context context, ImageView image) {
			this.context = context;
			//this.image = image;
		}

		@Override
		protected Void doInBackground(String... params) {
			/*
			 * Get url and id from filename by parsing json file
			 */
			String filename = params[0];
			String filepath = params[1];
			Log.d(TAG, "VCJ local file path: "+filepath);


			// Read json file
			DefaultHttpClient defaultClient = new DefaultHttpClient();
			String url = Constant.STEP_URL + ProjectDisplay.getId() + "/steps.json";
			Log.d(TAG, "url = " + url);
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
				Log.d(TAG, "VCJ filename: "+filename+" contains filename: "+ json.contains(filename)+" project id: "+ProjectDisplay.getId());
				Log.d(TAG, "VCJ json: "+json);
				// Find start and end indices of url and id
				if(json.contains(filename)){
					int startUrl = json.indexOf(filename)-1; // index of the / between mediaID and filename
					int endUrl = json.indexOf(filename); // index of first character of filename

					int startId = json.indexOf(filename)-1;
					int endId = startId;

					// Get start index of url
					while(!json.substring(startUrl, startUrl+1).equals("\"")){
						startUrl--;
					}
					startUrl++; // start index of url

					// Get end index of url
					while(!json.substring(endUrl, endUrl+1).equals("\"")){
						endUrl++;
					}

					// Get start index of id
					while(!json.substring(startId-1, startId).equals("/")) {
						startId--;
					}

					String newSrc = json.substring(startUrl, endUrl);
					Log.d(TAG, "newUrl = " + newSrc);
					int newID = Integer.parseInt(json.substring(startId, endId));
					Log.d(TAG, "newID = " + newID);

					int newPosition = ProjectDisplay.currentlySelected.getMultimediaList().size();
					String previewPath;
					String thumbnailPath;
					String sqThumbnailPath;
					String videoURL;
					int videoRotation = 0;
					Multimedia multimedia;

					if(filename.endsWith("jpg") || filename.endsWith("jpeg")
							|| filename.endsWith("png") || filename.endsWith("gif")) {
						String image_path = "https://buildinprogresstest.s3.amazonaws.com/image/image_path/";
						previewPath = image_path + newID + "/preview_" + filename;
						thumbnailPath = image_path + newID + "/thumb_" + filename;
						sqThumbnailPath = image_path + newID + "/square_thumb_" + filename;
						multimedia = new Multimedia(newID, newPosition, newSrc, previewPath, thumbnailPath, sqThumbnailPath);
					} else {
						String video_path = "https://buildinprogresstest.s3.amazonaws.com/video/video_path/";
						previewPath = video_path + newID + "/preview_" + filename;
						thumbnailPath = video_path + newID + "/thumb_" + filename;
						sqThumbnailPath = video_path + newID + "/square_thumb_" + filename;
						Log.d(TAG, "VCJ uploaded video videoPath: " + video_path + newID + "/" + filename);
						// need the webm path and video rotation
						//int extension = filename.indexOf(".");
						//String name = filename.substring(0, extension);
						//String webmPath = video_path+newID+"/"+name+".webm"; // not sure if this is correct
						// since filepath has the local video path
						multimedia = new Multimedia(newID, newPosition, newSrc, previewPath, thumbnailPath, sqThumbnailPath, filepath, videoRotation);
					}

					//Multimedia multimedia = new Multimedia(newID, newPosition, newSrc, previewPath, thumbnailPath, sqThumbnailPath);
					//ProjectDisplay.currentlySelected.getMultimediaList().add(multimedia);

					if(!(filename.endsWith("jpg") || filename.endsWith("jpeg") || filename.endsWith("png") || filename.endsWith("gif"))) {
						// vcj: it's a video
						videoURL = multimedia.getVideoPath();
						Log.d(TAG, "VCJ videoURL check on upload :"+videoURL);
						//videoRotation = multimedia.getVideoRotation();
					}

					// Get image from website with url
					//URL newUrl;
					try {
						Log.d(TAG, "getting image from website with url!!!!");
						// Get image from url
						//newUrl = new URL(newSrc);
						//HttpGet httpRequest= new HttpGet(newUrl.toURI());
						//HttpClient httpclient = new DefaultHttpClient();
						//HttpResponse response = httpclient.execute(httpRequest);
						//HttpEntity entity = response.getEntity();
						//BufferedHttpEntity b_entity = new BufferedHttpEntity(entity);
						//InputStream input = b_entity.getContent();

						// Create bitmap from image
						//Bitmap bitmap = BitmapFactory.decodeStream(input);
						//Resources resources = context.getResources();
						//TaggedBitmapDrawable drawable = new TaggedBitmapDrawable(resources, bitmap, newSrc, newID, filename);
						//MediaPreview.addMedia(new ImageVideoResource(videoURL, videoRotation, drawable), newSrc);

						//image.setAlpha(Constant.UPLOADED_ALPHA); // Change opacity

					} catch (Exception e) {
						e.printStackTrace();
					}

					// Notify that images are uploaded and ready to use



				} else {
					Log.d(TAG, "FILE NOT FOUND");
					return null;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;

		}

	}



	/**
	 * <code> public static class extends AsyncTask<Object, Void, HttpEntitiy></code>
	 * <p>
	 * Class used to upload images to bip-android-test website.
	 * @param String.. [ stepID, filepath, auth_token, projectID, userid]
	 *
	 */
	public static class UploadVideo extends AsyncTask<Object, Void, HttpEntity>{

		private Context context;
		private CustomizedStepAdapter stepAdapter;
		private ParcelStep uploadStep;
		private String filepath;
		//private ImageView video_img;

		public UploadVideo(Context context,ParcelStep step, CustomizedStepAdapter stepAdapt, ImageView video_img){
			this.context = context;
			this.stepAdapter = stepAdapt;
			this.uploadStep = step;
			//this.video_img = video_img;
		}

		@Override
		protected void onPreExecute() {
			Log.d(TAG, "VCJ: uploading video");
			//Increasing the number of images that the step is uploading
			if(uploadStep.incTotalUploading()<2){
				if(stepAdapter!=null){
					stepAdapter.notifyDataSetChanged();
				}
			}
		}

		@Override
		protected HttpEntity doInBackground(Object... params){
			DefaultHttpClient client = new DefaultHttpClient();
			String stepID = (String)params[0];
			filepath = (String)params[1];
			String auth_token = (String)params[2];
			String projectID = (String)params[3];
			Integer userID = (Integer)params[4];

			String filename = Uri.parse(filepath).getLastPathSegment();
			String videoType = filename.substring(filename.lastIndexOf("."), filename.length());

			String url = Constant.VIDEO_URL+"?auth_token=" + auth_token;

			HttpPost post = new HttpPost(url);
			HttpContext localContext = new BasicHttpContext();
			MultipartEntity imageMPentity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

			try{

				imageMPentity.addPart("project_id", new StringBody(""+projectID));
				imageMPentity.addPart("step_id", new StringBody(stepID));
				imageMPentity.addPart("content_type", new StringBody("video/"+videoType));
				imageMPentity.addPart("filename", new StringBody(filename));
				imageMPentity.addPart("video_path", new FileBody(new File(filepath))); 
				imageMPentity.addPart("user_id", new StringBody(""+userID));
				post.setEntity(imageMPentity);                

			} catch(Exception e){
				Log.e(StepDisplay.class.getName(), e.getLocalizedMessage(), e);
			}
			HttpResponse response = null;

			try {
				response = client.execute(post, localContext);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
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
				NotificationManager mNotificationManager =
						(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Constant.currentlyUploading.decrementAndGet();
				Constant.totalUploadedImages.incrementAndGet();
                String files_count;
                if(Constant.currentlyUploading.get() == 1){
                    files_count = "file";
                }else{
                    files_count = "files";
                }
				if (Constant.currentlyUploading.get() > 0){
					Builder uploadBuilder = new NotificationCompat.Builder(context)
					.setSmallIcon(com.bip_public_android.R.drawable.logo)
					.setContentTitle("Uploading to Build in Progress")
					.setContentText(Constant.currentlyUploading.get()+" " + files_count + " uploading");
					Notification uploadNotif = uploadBuilder.build();
					mNotificationManager.notify(Constant.NOTIFICATION_UPLOADING_ID, uploadNotif);
				} else {				    
					Builder uploadedBuilder = new NotificationCompat.Builder(context)
					.setSmallIcon(com.bip_public_android.R.drawable.logo)
					.setContentTitle("Upload complete")
					.setTicker("Upload Complete")
					.setContentText(Constant.totalUploadedImages.get() + " " + files_count + " uploaded to Build in Progress")
					.setAutoCancel(true);
					Notification uploadNotif = uploadedBuilder.build();
					mNotificationManager.notify(Constant.NOTIFICATION_UPLOADING_ID, uploadNotif);	
					Constant.totalUploadedImages.getAndSet(0);    	

				}

				//Removing one from the step's image upload counter
				uploadStep.decTotalUploading();


				//vcj TODO: why don't you use subpath, filepath.lastindexof("/") to the end?
				String fn = ""; // get the filename
				for (int i = filepath.length()-1; i >= 0; i--){
					String character = filepath.substring(i,i+1);
					if(!character.equals("/")){
						fn = character + fn;
					} else {
						break;
					}
				}

				//VCJ TODO: video has no onclick listener which is why you can't click it immediately after uploading
				// need to add callingActivity to intent

				//new UpdateImages(context, video_img).execute(fn, filepath);
				// Change opacity of images after it is uploaded
				//video_img.setAlpha(Constant.UPLOADED_ALPHA);
				
				if(stepAdapter!=null){
					stepAdapter.notifyDataSetChanged();
				}

				//Removing the path from the list of currently uploading images
				uploadStep.removeCurrentlyUploading(filepath);


			}
		}     
	}

	/**
	 * Adding a new step to the current Project
	 *
	 */
	public static class AddStepTask extends AsyncTask<String, Void, HttpEntity>{

		private ProgressDialog dialog;
		private Context context;
		private int lastStepId;

		public AddStepTask(Context context, int lastStepId){
			this.context = context;
			this.lastStepId = lastStepId;
		}

		@Override
		protected void onPreExecute(){
			dialog = new ProgressDialog(context);
			dialog.setMessage("Adding step..");
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected HttpEntity doInBackground(String... urls){

			DefaultHttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(urls[0]);
			String stepname = urls[1];
			JSONObject holder = new JSONObject();
			JSONObject stepObj = new JSONObject();
			try{
				// add placeholder attributes for the new step
				stepObj.put("name", stepname);
				stepObj.put("parent_id", lastStepId);
				stepObj.put("last", false);
				stepObj.put("description", "");
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
			}
		}
	}
	
	/**
	 * Get the string path from a uri
	 * @param contentUri uri for the media
	 * @param context activity calling the method
	 * @return the string path for the media
	 */
	public static String getRealPathFromURI(Uri contentUri, Context context) {
		String[] proj = { MediaStore.Images.Media.DATA };
		CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
		Cursor cursor = loader.loadInBackground();
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}
	

	public static boolean imageSparseContainsTag(SparseArray<View> updatedSparse, String path) {
		for(int i =0; i<updatedSparse.size(); i++){
			int key = updatedSparse.keyAt(i);
			View image = updatedSparse.get(key);
			if((image.getTag()).equals(path)){
				return true;
			}
		}
		return false;
	}
	
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
			int reqWidth, int reqHeight) {
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);
		
		// Calculate in SampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}
	
	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
    }

    return inSampleSize;
}


	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

	/**
	 * Generate a value suitable for use in {@link #setId(int)}.
	 * This value will not collide with ID values generated at build time by aapt for R.id.
	 *
	 * @return a generated ID value
	 */
	public static int generateViewId() {
		for (;;) {
			final int result = sNextGeneratedId.get();
			// aapt-generated IDs have the high byte nonzero; clamp to the range under that.
			int newValue = result + 1;
			if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
			if (sNextGeneratedId.compareAndSet(result, newValue)) {
				return result;
			}
		}
	}
	
}


