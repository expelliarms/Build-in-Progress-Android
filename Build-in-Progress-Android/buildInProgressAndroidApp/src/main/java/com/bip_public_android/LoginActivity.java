package com.bip_public_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.example.helperClass.Constant;
import com.savagelook.android.UrlJsonAsyncTask;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoginActivity extends SherlockActivity {
    
    private SharedPreferences preferences;
    private String username;
    private String password;
    private String auth_token;
    private Boolean from_share; // if coming from share activity
    
    private EditText usernameField;
    private EditText passwordField;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(com.bip_public_android.R.layout.activity_login);
        
        Intent intent = getIntent();
        from_share = intent.getBooleanExtra("from_share", false);
        
        usernameField = (EditText) findViewById(com.bip_public_android.R.id.username);
        passwordField = (EditText) findViewById(com.bip_public_android.R.id.password);
        
        preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
        
        usernameField.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				String result = s.toString().replaceAll(" ", "");
			    if (!s.toString().equals(result)) {
			         usernameField.setText(result);
			         usernameField.setSelection(result.length());
			         // alert the user
			    }
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int end,
					int count) {
			}
        	
        });

        findViewById(com.bip_public_android.R.id.registerButton).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	//TODO register activity adding
                        Intent intent = new Intent(LoginActivity.this,
                                Register.class);
                        startActivityForResult(intent, 0);
                    }
                });
        
        // Automatically start login after user presses 'enter' after entering
        // their password
        passwordField.setOnKeyListener(new OnKeyListener() {
            
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction()==KeyEvent.ACTION_DOWN){
                    switch(keyCode){
                    case KeyEvent.KEYCODE_ENTER:
                        login(findViewById(com.bip_public_android.R.id.loginButton));
                        return true;
                    default:
                        break;
                    }
                }
                return false;
            }
        });
        
        
    }
    
    // Login called when the user presses the "login" button of the sign in screen
    public void login(View button){
        username = usernameField.getText().toString();
        password = passwordField.getText().toString();
        
        if(username.length()==0 || password.length()==0){
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_LONG).show();
            return;
        }else{
            LoginTask loginTask = new LoginTask(LoginActivity.this);
            loginTask.setMessageLoading("Logging in..");
            loginTask.execute(Constant.LOGIN_API_ENDPOINT_URL);            
        }
        
    }
    
    // Login to Build in Progress via UrlJsonAsyncTask
    public class LoginTask extends UrlJsonAsyncTask{
        public LoginTask(Context context){
            super(context);
        }
        
        @Override
        protected JSONObject doInBackground(String... urls){
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(urls[0]);
            JSONObject holder = new JSONObject();
            JSONObject userObj = new JSONObject();
            String response = null;
            JSONObject json = new JSONObject();
            
            try{
                try{
                    // set up returned values in case something goes wrong
                    json.put("success", false);
                    json.put("info", "Something went wrong.  Please retry!");
                    // add the user's username and password to the params
                    userObj.put("username", username);
                    userObj.put("password", password);
                    holder.put("user", userObj);
                    StringEntity se = new StringEntity(holder.toString());
                    post.setEntity(se);
                    
                    // setup the request headers
                    post.setHeader("Accept", "application/json");
                    post.setHeader("Content-Type", "application/json");
                    
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    response = client.execute(post, responseHandler);
                    json = new JSONObject(response);
                } catch (HttpResponseException e){
                    e.printStackTrace();
                    Log.e("ClientProtocol", ""+e);
                    json.put("info", "Login invalid.  Please retry!");
                } catch(IOException e){
                    e.printStackTrace();
                    Log.e("IO", "" + e);
                }
            } catch(JSONException e){
                e.printStackTrace();
                Log.e("JSON", ""+e);
            }
            return json;
        }
        
        @Override
        protected void onPostExecute(JSONObject json){
            try{
                if(json.getBoolean("success")){
                    SharedPreferences.Editor editor = preferences.edit();
                    
                    // saved the returned auth_token into the SharedPreferences
                    auth_token = json.getJSONObject("data").getString("auth_token");
                    editor.putString("AuthToken", auth_token);
                    editor.putString("Username", username.toLowerCase());
                    editor.commit();
                    
                    // launch the UserProjectsActivity and close this one
                    // Intent intent = new Intent(getApplicationContext(), MenuScreen.class);
                    if(from_share){
                        Intent returnIntent = getIntent();
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    }else{
                        Intent intent = new Intent(getApplicationContext(), AllProjectDisplay.class);
                        intent.putExtra("Username", username.toLowerCase());
                        intent.putExtra("auth_token", auth_token);
                        startActivity(intent);                        
                    }
                    finish();
                }
                Toast.makeText(context, json.getString("info"), Toast.LENGTH_SHORT ).show();
            } catch (Exception e){
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            } finally{
                super.onPostExecute(json);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; then adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(com.bip_public_android.R.menu.welcome, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        finish();
    }

}
