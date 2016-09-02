package com.bip_public_android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
* This class will be the first screen to show an animation to transition to the
* Menu Screen page.
*
*/
public class SplashScreen extends Activity {
    private static final int SPLASH_DISPLAY_TIME = 500;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.bip_public_android.R.layout.activity_splash_screen);
        
        // Get the view and start the animation
        ImageView logo_image = (ImageView) findViewById(com.bip_public_android.R.id.logo);
        Animation logo_anim = AnimationUtils.loadAnimation(this, com.bip_public_android.R.anim.fade_in);
        logo_image.startAnimation(logo_anim);

        // This is the last animation so need to make the transition
        // by calling on Transition end
        logo_anim.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            // A Handler to make the fade in and fade out effect for better transition
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(SplashScreen.this, MenuScreen.class);
                        startActivity(intent);
                        finish();
                        
                        // transition from splash to main menu
                        overridePendingTransition(com.bip_public_android.R.anim.fade_in, com.bip_public_android.R.anim.fade_out);
                    }
                }, SPLASH_DISPLAY_TIME);
            }
        });
    }
    /* Since Animation is costly, we need to clear the Animation if
     * the user decide to stop or pause it*/
    @Override
    protected void onPause() {
        super.onPause();
        
        ImageView logo_image = (ImageView) findViewById(com.bip_public_android.R.id.logo);
        logo_image.clearAnimation();
    }
    
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.activity_splash_screen, menu);
//        return true;
//    }
}
