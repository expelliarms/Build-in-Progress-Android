package com.bip_public_android;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
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

public class Register extends SherlockActivity {
    
    private final static String TAG = "RegisterActivity";
    
    private String email;
    private String username;
    private String password;
    private String passwordConfirmation;
    EditText passwordConfirmationField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.bip_public_android.R.layout.activity_register);
        // hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        
        getSharedPreferences("CurrentUser", MODE_PRIVATE);
        
        passwordConfirmationField  = (EditText) findViewById(com.bip_public_android.R.id.passwordConfirmation);
        
        // automatically start registration after user presses "enter" after entering password confirmation
        passwordConfirmationField.setOnKeyListener(new OnKeyListener() {
            
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction()==KeyEvent.ACTION_DOWN){
                    switch(keyCode){
                    case KeyEvent.KEYCODE_ENTER:
                        register(findViewById(com.bip_public_android.R.id.registerButton));
                        return true;
                    default:
                        break;
                    }
                }
                return false;
            }
        });
    }
    
    public void register(View button){
        registerNewAccount(button);
    }
    
    public void registerNewAccount(View button){
        Log.d(TAG, "registering new account");
        EditText emailField = (EditText) findViewById(com.bip_public_android.R.id.email);
        email = emailField.getText().toString();
        EditText usernameField = (EditText) findViewById(com.bip_public_android.R.id.username);
        username = usernameField.getText().toString();
        EditText passwordField = (EditText) findViewById(com.bip_public_android.R.id.password);
        password = passwordField.getText().toString();
        passwordConfirmation = passwordConfirmationField.getText().toString();
                
        if(email.length()==0 || username.length()==0 || password.length()==0 || passwordConfirmation.length()==0){
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_LONG).show();
            return;
        } else{
            if(!password.equals(passwordConfirmation)){
                Toast.makeText(this, "Your passwords do not match.  Please check again!", Toast.LENGTH_LONG).show();
                return;
            }
            else if(password.length()<8){
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_LONG).show();
                return;
            }else{
                RegisterTask registerTask = new RegisterTask(Register.this);
                registerTask.setMessageLoading("Registering new account..");
                registerTask.execute(Constant.REGISTER_URL);
            }
        }
    }
    
    private class RegisterTask extends UrlJsonAsyncTask{
        public RegisterTask(Context context){
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
                    json.put("info", "Something went wrong. Please retry!");
                    
                    userObj.put("username", username);
                    userObj.put("email", email);
                    userObj.put("password", password);
                    userObj.put("password_confirmation", passwordConfirmation);
                    holder.put("user", userObj);
                    StringEntity se = new StringEntity(holder.toString());
                    post.setEntity(se);
                    
                    // setup the request headers
                    post.setHeader("Accept", "application/json");
                    post.setHeader("Content-Type", "application/json");
                    
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    response = client.execute(post, responseHandler);
                    json = new JSONObject(response);
                } catch(HttpResponseException e){
                    e.printStackTrace();
                    Log.e("ClientProtocol", "" + e);
                } catch(IOException e){
                    e.printStackTrace();
                    Log.e("IO", "" + e);
                } 
            } catch(JSONException e){
                e.printStackTrace();
                Log.e("JSON", "" + e);
            }
            
            return json;
        }
        
        @Override
        protected void onPostExecute(JSONObject json){
            Log.d(TAG, "in onPostExecute");
            Log.d(TAG, json.toString());

            try{         
                Toast.makeText(context, "Please confirm your acount via email", Toast.LENGTH_LONG).show();
                
                Log.d(TAG, "returning to login screen");
                // launch the welcome screen (login) and close this one
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();   
                
                // to add - this should check whether a success response was received from the website - otherwise, the user might have entered an existing username or email.
                if(Boolean.valueOf(json.getString("success"))){
                   
                }else{
                    Toast.makeText(context, "Username or email already exists!", Toast.LENGTH_LONG).show();
                }
            } catch(Exception e){
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            } finally{
                super.onPostExecute(json);
            }
        }
    }

}
