package com.bip_public_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.example.helperClass.Constant;
import com.example.helperClass.Method;
import com.example.helperClass.Method.Logout;

/**
 * Menu class for the application. Allows the user to take pictures
 * or view their projects.
 *
 */
public class MenuScreen extends SherlockActivity {
//	private final String TAG = "MenuScreen";
    private String username;
    private String auth_token;
    private SharedPreferences preferences;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.bip_public_android.R.layout.activity_menu_screen);
		
		preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
		
		auth_token = preferences.getString("AuthToken", "");
		
        Typeface robotoThin=Typeface.createFromAsset(getAssets(),"fonts/Roboto-Light.ttf");

        TextView mainprompt=(TextView)findViewById(com.bip_public_android.R.id.mainprompt);
        mainprompt.setTypeface(robotoThin);        

        Button picBtn = (Button) findViewById(com.bip_public_android.R.id.camera_menu_screen);
        picBtn.setTypeface(robotoThin); 
        
        //Listener for the camera button starts the custom camera activity
        picBtn.setOnClickListener(new OnClickListener(){           
            
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(v.getContext(), CustomCamera.class);
                cameraIntent.putExtra("menu", true);
                startActivity(cameraIntent);  
            }   
        });

        Button vidBtn = (Button) findViewById(com.bip_public_android.R.id.gallery_menu_screen);
        //View projects listener starts the projects activity
        vidBtn.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent projectIntent = new Intent(v.getContext(), AllProjectDisplay.class);
                projectIntent.putExtra("Username", username);
                projectIntent.putExtra("auth_token", auth_token);
                startActivity(projectIntent);
                
            }
      		
        });
	}
	
    @Override
    public void onResume(){
        super.onResume();
        if(!preferences.contains("AuthToken")){
        	Intent intent = new Intent(MenuScreen.this, LoginActivity.class);
            startActivityForResult(intent, 0);
            finish();
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      // Save UI state changes to the savedInstanceState.
      // This bundle will be passed to onCreate if the process is
      // killed and restarted.
      
      // etc.
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
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
        case com.bip_public_android.R.id.logout:
            Method.Logout logout = new Logout(this);
            logout.setMessageLoading("Logging out..");
            logout.execute(Constant.LOGOUT_URL+"?auth_token=" + auth_token);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
