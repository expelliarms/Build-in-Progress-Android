package com.bip_public_android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.helperClass.Constant;
import com.example.helperClass.Method;
import com.savagelook.android.UrlJsonAsyncTask;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity for displaying labels (Similar in functionality to StepDisplay)
 *
 * Created by victo_000 on 8/18/2015.
 */
public class LabelDisplay extends SherlockFragmentActivity {

    private final static String TAG = "LabelDisplay";

    /** Stores the 4 available hex colors for labels and maps them to the color name
     *<ul>
     *     <li>"#00AEEF" = BLUELABELCOLOR;</li>
     *     <li>"#EF4436" = REDLABELCOLOR;</li>
     *     <li>"#78B920" = GREENLABELCOLOR;</li>
     *     <li>"#9e9e9e" = GREYLABELCOLOR;</li>
     *</ul>
     * **/
    private Map<String, String> hexToColorName= new HashMap<String, String>();
    /** Stores the 4 available color names for labels and maps them to their hex values
     *<ul>
     *     <li>BLUELABELCOLOR = "#00AEEF";</li>
     *     <li>REDLABELCOLOR = "#EF4436";</li>
     *     <li>GREENLABELCOLOR = "#78B920";</li>
     *     <li>GREYLABELCOLOR = "#9e9e9e";</li>
     *</ul>
     * **/
    private Map<String, String> colorNameToHex= new HashMap<String, String>();

    private RadioGroup colorGroup;

    private String stepURL;
    private int projectID;
    private int stepID;
    private String stepURLjson;

    private String auth_token;

    private ViewSwitcher labelNameSwitcher;
    private EditText labelNameEditText;
    private boolean editMode = false;
    private boolean editTextClicked = false;
    private boolean changed = false;

    private boolean backPressedFromActionBar = false;

    private String labelColor;
    private TextView labelName;
    private String labelNameString;

    private Menu editMenu;

    private static boolean alreadyLoaded = false;
    private boolean fromProjects = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Method.isNetworkAvailable(this)) {
            // Displaying a nicer connection error dialog
            Toast.makeText(
                    this,
                    "No internet connection available. Check your connection settings",
                    Toast.LENGTH_LONG).show();
            Intent noInternet = new Intent(this, AllProjectDisplay.class);
            noInternet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(noInternet);
            finish();
        }
        setContentView(com.bip_public_android.R.layout.activity_label_display);

        // Getting preferences and auth_token
        SharedPreferences preferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
        auth_token = preferences.getString("AuthToken", "");

        Bundle extras = getIntent().getExtras();
        labelColor = extras.getString("labelColor");
        labelNameString = extras.getString("labelName");
        Log.d(TAG, "Name from projectScreen: "+labelNameString);
        projectID = extras.getInt("projectID", -1);
        fromProjects = extras.getBoolean("fromProjects", false);

        stepID = ProjectDisplay.currentlySelected.getId();
        colorGroup = (RadioGroup) findViewById(com.bip_public_android.R.id.label_color_group);
        labelName = (TextView) findViewById(com.bip_public_android.R.id.label_name);
        labelName.setText(labelNameString);

        labelNameSwitcher = (ViewSwitcher) findViewById(com.bip_public_android.R.id.label_name_switcher);
        labelNameSwitcher.setOnClickListener(new editLabelListener());

        labelNameEditText = (EditText) labelNameSwitcher
                .findViewById(com.bip_public_android.R.id.label_name_edit_view);
        labelNameEditText.setText(labelNameString);
        labelName.setOnClickListener(new editLabelListener());

        initializeColorMaps();

        // add back button to action bar
        getSupportActionBar().setDisplayShowTitleEnabled(false); // hide build in progress title from toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        int stepPosition = ProjectDisplay.currentlySelected.getPosition();
        Log.d(TAG, "position from projectscreen: "+stepPosition);
        stepURL = Constant.PROJECT_URL + projectID + "/steps/" + stepPosition;
        stepURLjson = stepURL + ".json";

        setSelected();

        Button deleteLabelButton = (Button) findViewById(com.bip_public_android.R.id.delete_label_button);
        // delete step when user clicks "delete step" button
        deleteLabelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (Method.isNetworkAvailable(getApplicationContext())) {
                    new AlertDialog.Builder(LabelDisplay.this)
                            .setTitle("Delete Step")
                            .setMessage(
                                    "Are you sure you want to delete label '"
                                            + ProjectDisplay.currentlySelected
                                            .getName() + "'?")
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            deleteLabelFromAPI(stepURL + "?auth_token=" + auth_token + "&step_id=" + stepID);
                                        }
                                    }).setNegativeButton("Cancel", null).show();
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "No internet connection available. Check your connection settings",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        //Changes the background color of the label's title bar so that the user can
        colorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                labelNameSwitcher.setBackgroundColor(Color.parseColor(getColorSelected()));
            }
        });
    }

    /**Function that creates the two dictionaries mapping hex values to color names (hexToColorName)
     *  and color names to their hex values (colorNameToHex)**/
    private void initializeColorMaps() {
        ArrayList<String> colorLabelStrings = new ArrayList<String>(4);
        colorLabelStrings.addAll(Arrays.asList("BLUELABELCOLOR", "REDLABELCOLOR", "GREENLABELCOLOR", "GREYLABELCOLOR"));

        ArrayList<String> colorHexStrings = new ArrayList<String>(4);
        colorHexStrings.addAll(Arrays.asList("#00AEEF", "#EF4436", "#78B920", "#9e9e9e"));

        for (int i =0; i<colorHexStrings.size();i++){
            hexToColorName.put(colorHexStrings.get(i), colorLabelStrings.get(i));
            colorNameToHex.put(colorLabelStrings.get(i), colorHexStrings.get(i));
        }
    }

    private void deleteLabelFromAPI(String url) {
        // use same method function as a step
        new Method.DeleteStepTask().execute(url);
        Intent intent = getIntent();
        intent.putExtra("deleted", true);
        intent.putExtra("updatedDate", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**Function that returns the hex color that the user selected**/
    private String getColorSelected() {
        Log.d(TAG, "color switcher : "+colorGroup.getCheckedRadioButtonId());
        switch (colorGroup.getCheckedRadioButtonId()) {
            case com.bip_public_android.R.id.label_color_blue:
                return colorNameToHex.get("BLUELABELCOLOR");
            case com.bip_public_android.R.id.label_color_red:
                return colorNameToHex.get("REDLABELCOLOR");
            case com.bip_public_android.R.id.label_color_green:
                return colorNameToHex.get("GREENLABELCOLOR");
            case com.bip_public_android.R.id.label_color_grey:
                return colorNameToHex.get("GREYLABELCOLOR");
        }
        return "";
    }

    private enum LabelColors {
        BLUELABELCOLOR,
        REDLABELCOLOR,
        GREENLABELCOLOR,
        GREYLABELCOLOR
    }

    private void setSelected() {
        Log.d(TAG, "label color: "+labelColor+" and color name: "+hexToColorName.get(labelColor));
        LabelColors currentColor = LabelColors.valueOf(hexToColorName.get(labelColor));
        //initially set the background color of the label title
        labelNameSwitcher.setBackgroundColor(Color.parseColor(labelColor));
        //xml files toggle the border based on whether the button is checked or not
        switch (currentColor) {
            case BLUELABELCOLOR:
                colorGroup.check(com.bip_public_android.R.id.label_color_blue);
                break;
            case REDLABELCOLOR:
                colorGroup.check(com.bip_public_android.R.id.label_color_red);
                break;
            case GREENLABELCOLOR:
                colorGroup.check(com.bip_public_android.R.id.label_color_green);
                break;
            case GREYLABELCOLOR:
                colorGroup.check(com.bip_public_android.R.id.label_color_grey);
                break;
        }
    }


    // toggle edit mode for step (when edit button clicked)
    public class editLabelListener implements View.OnClickListener {

        public void onClick(View v) {
            Log.d(TAG, "clicked view: " + v.getId());
            editMode = true;
            MenuItem editMenuItem = editMenu.findItem(com.bip_public_android.R.id.edit);
            editTextClicked = true;
            onOptionsItemSelected(editMenuItem);
            editLabel(v);
            editTextClicked = false;
        }
    }

    public void editLabel(View v) {
        //Log.d(TAG, "labelName: " + labelNameString);
        labelNameEditText.setText(labelNameString);
        labelNameSwitcher.setDisplayedChild(1);

        if (v != null) {
            // autofocus on whatever view was clicked
            if (v.getId() == com.bip_public_android.R.id.step_name) {
                //Log.d(TAG, "clicked on label name");
                labelNameEditText.requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(labelNameEditText,
                                InputMethodManager.SHOW_FORCED);
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle() == "edit") {
            editMode = false;
        }
        // Handle item selection
        switch (item.getItemId()) {
            case com.bip_public_android.R.id.refresh:
                loadStepFromAPI(stepURLjson + "?auth_token=" + auth_token);
                return true;
            case com.bip_public_android.R.id.edit:
                if (editMode) {
                    // edit action bar icon
                    item.setIcon(com.bip_public_android.R.drawable.ic_action_edit);
                    item.setTitle("edit");
                    editMode = true;
                    saveStep();
                } else {
                    editMode = true;
                    item.setIcon(com.bip_public_android.R.drawable.ic_action_accept);
                    item.setTitle("save");
                    if (!editTextClicked) {
                        editLabel(null);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(Menu.NONE, com.bip_public_android.R.id.refresh, Menu.NONE, "refresh")
                .setIcon(com.bip_public_android.R.drawable.ic_action_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(Menu.NONE, com.bip_public_android.R.id.edit, Menu.NONE, "edit")
                .setIcon(com.bip_public_android.R.drawable.ic_action_edit).setTitle("edit")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        this.editMenu = menu;
        return true;
    }

    @Override
    public void onBackPressed() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(labelNameEditText.getWindowToken(), 0);

        saveStep(); // in case they toggle the color and then press back
        // check if user is pressing back from edit
        // first back: removes keyboard and second back: executes this if
        if (!backPressedFromActionBar && editMode) {
            labelNameSwitcher.setDisplayedChild(0);
            // cycle edit icon
            MenuItem editMenuItem = editMenu.findItem(com.bip_public_android.R.id.edit);
            onOptionsItemSelected(editMenuItem);

        } else {
            Intent returnIntent = getIntent();
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }

    public void saveStep() {
        Log.w(TAG, "saving label");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(labelNameEditText.getWindowToken(), 0);
        boolean nameChanged = !ProjectDisplay.currentlySelected.getName().equals(labelNameEditText.getText().toString());
        boolean colorChanged = !ProjectDisplay.currentlySelected.getLabelColor().equals(getColorSelected());

        if (labelNameEditText.getText().toString().length() > 0) {
            /*
             * Only toggle stepNameSwitcher back to TextView if we're not on the
             * project overview step (or if only the check box for last step was
             * selected)
             */
            if (labelNameSwitcher.getCurrentView().getId() == labelNameEditText
                    .getId()) {
                // Assuring that the label (step) name has changed in order to update
                if (nameChanged||colorChanged) {
                    changed = true;
                    ProjectDisplay.currentlySelected.setName(labelNameEditText
                            .getText().toString());
                }
                labelNameEditText.setText(ProjectDisplay.currentlySelected.getName());
                labelNameSwitcher.setDisplayedChild(0);
            }

            if (colorChanged) {
                changed = true;
                ProjectDisplay.currentlySelected.setLabelColor(getColorSelected());
            }

            // Assuring we changed the label (step) before trying to update
            if (changed) {
                if (Method.isNetworkAvailable(getApplicationContext())) {
                    // update title of project in textview
                    labelName.setText(ProjectDisplay.currentlySelected.getName());

                    // set action bar title to project name
                    getSupportActionBar().setTitle(
                            ProjectDisplay.currentlySelected.getName());
                    changed = false;

                    //Log.d(TAG, " arguments : stepURL : "+stepURL+" auth_token : "+auth_token+" name : "+ProjectDisplay.currentlySelected.getName()+" description : "+stepDescriptionText);

                    new Method.UpdateLabelNameTask(LabelDisplay.this).execute(stepURL,
                            auth_token,
                            ProjectDisplay.currentlySelected.getName(),
                            getColorSelected());
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "No internet connection available. Please check connection settings",
                            Toast.LENGTH_SHORT).show();
                }
            }

        } else {
            labelNameSwitcher.setDisplayedChild(0);
            labelNameEditText.setText(ProjectDisplay.currentlySelected.getName());
            Toast.makeText(LabelDisplay.this, "Please add a label name",
                    Toast.LENGTH_LONG).show();
        }
    }

    // Load label from build in progress website
    public void loadStepFromAPI(String url) {
        if (Method.isNetworkAvailable(getApplicationContext())) {
            GetLabelTask getProjectLabelTask = new GetLabelTask(LabelDisplay.this);
            getProjectLabelTask.execute(url);
        } else {
            Toast.makeText(getApplicationContext(),
                    "No internet connection. Check connection settings",
                    Toast.LENGTH_LONG).show();
        }
    }

    // fetch label
    public class GetLabelTask extends UrlJsonAsyncTask {

        private ProgressDialog dialog;

        public GetLabelTask(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(LabelDisplay.this);
            dialog.setMessage("Loading label..");
            dialog.show();
            dialog.setCanceledOnTouchOutside(false);
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            try {
                //Log.d(TAG, "OnPostExecute: fetching label assets");
                JSONObject jsonStep = json.getJSONObject("data").getJSONObject(
                        "step");
                //Log.d(TAG,"JSON : " + jsonStep);

                String name = jsonStep.getString("name");
                String newLabelColor = jsonStep.getString("label_color");
                labelColor = newLabelColor;
                ProjectDisplay.currentlySelected.setLabelColor(newLabelColor);
                ProjectDisplay.currentlySelected.setName(name);
                // update the color on the app
                setSelected();
                // update the text
                labelNameString = name;
                labelNameEditText.setText(name);
                labelName.setText(name);

            } catch (Exception e) {
                //Log.d(TAG, "Try failed. json not found : " + e.getMessage());
            } finally {
                // can't call this code if there is no json return
                super.onPostExecute(json);
                // Sets the boolean to true so that we don't reload page again
                alreadyLoaded = true;
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                labelNameSwitcher.setDisplayedChild(0);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!alreadyLoaded || fromProjects) {
            Log.d(TAG, "loading label from api from onresume");
            loadStepFromAPI(stepURLjson+ "?auth_token=" + auth_token);
            fromProjects = false;
        }
    }

}
