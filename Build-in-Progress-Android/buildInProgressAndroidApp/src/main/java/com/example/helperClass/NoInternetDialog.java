package com.example.helperClass;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.bip_public_android.MenuScreen;


public class NoInternetDialog {
    /**
     * Function to display simple Alert Dialog
     * @param context - application context
     * @param title - alert dialog title
     * @param message - alert message
     * @param status - success/failure (used to set icon). Pass null if you don't want icon
     */
    
    public static void showAlertDialog(final Context context, String title, String message, 
            Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        
        // Setting Dialog Title
        alertDialog.setTitle(title);
        
        // Setting Dialog Message
        alertDialog.setMessage(message);
        
        if(status != null) {
            // Setting alert dialog icon
            alertDialog.setIcon((status) ? com.bip_public_android.R.drawable.success : com.bip_public_android.R.drawable.fail);
        }
        
        // Setting OK Button
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
            	if(which == Dialog.BUTTON_POSITIVE){
            		Intent intent = new Intent(context, MenuScreen.class);
            		context.startActivity(intent);
            		((Activity)context).finish();
            		dialog.dismiss();
            	}
            }
        });
        
        // Showing Alert Message
        alertDialog.show();
    }
}

