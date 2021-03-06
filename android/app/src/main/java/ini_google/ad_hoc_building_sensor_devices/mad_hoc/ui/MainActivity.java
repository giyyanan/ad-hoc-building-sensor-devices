package ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.util.HashMap;

import ini_google.ad_hoc_building_sensor_devices.R;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final Activity activity = this;
    private static String androidID;
    private Button scanButton, startButton;
    private TextView textView, connectedList;
    private EditText urlTextView;
    private String instanceID;
    private static String device_hash;
    private int triggerPoint = 0;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference defaultConfig;
    private DatabaseReference applicationInfo;
    private JSONObject configFromFireBase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Fetch Android ID after all the components have been created
        androidID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

        textView = (TextView) findViewById(R.id.orTextView);
        //connectedList = (TextView) findViewById(R.id.connectedList);
        urlTextView = (EditText) findViewById(R.id.urlTextView);
        scanButton = (Button) findViewById(R.id.scanButton);

        //startButton = (Button) findViewById(R.id.startButton);

        instanceID = "";

        urlTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                instanceID = s.toString();
                textView.setText(instanceID);
                if(!instanceID.isEmpty()){
                    downloadDefaultConfig();
                }
            }
        });


        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Propagate Main Activity to IntentIntegrator
                IntentIntegrator intentIntegrator = new IntentIntegrator(activity);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                intentIntegrator.setPrompt("Scan");
                intentIntegrator.setCameraId(0);
                intentIntegrator.setBeepEnabled(false);
                intentIntegrator.setBarcodeImageEnabled(false);
                intentIntegrator.initiateScan();
            }
        });
    }

    // QR code
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (result != null) {
            if (result.getContents() == null) {
                Log.d("Main Activity Scan", "Cancel Scan ====================================");
                Toast.makeText(this, "Canceled", Toast.LENGTH_LONG).show();
            } else {
                Log.d("Main Activity Scan", "Scanned ====================================");
                //Toast.makeText(this, "Scanned " + result.getContents(), Toast.LENGTH_LONG).show();
                // start google search activity immediately
                System.out.println("result: " + result.getContents());

                instanceID = result.getContents();
                System.out.println("URL: " + instanceID);
                urlTextView.setText(instanceID);
                if(!instanceID.isEmpty()) {
                    downloadDefaultConfig();
                }
            }
        } else {
            //empty
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private void downloadDefaultConfig() {
        //reference for the application
        applicationInfo = database.getReference("/app_ids");
        //fetch reference of Instance
        DatabaseReference instanceInfo = applicationInfo.child(instanceID);
        instanceInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    //fetch application Id from firebase
                    JSONObject appInfo = new JSONObject((HashMap) dataSnapshot.getValue());
                    String appID = appInfo.get("app_id").toString();
                    applicationInfo = database.getReference("/apps/".concat(appID));
                    defaultConfig = applicationInfo.child("default_config");
                    defaultConfig.addListenerForSingleValueEvent(new ValueEventListener() {

                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try {
                                //default config for the app from firebase
                                configFromFireBase = new JSONObject((HashMap) dataSnapshot.getValue());
                                //switch
                                Intent intent = new Intent(activity, ListActivity.class);
                                intent.putExtra("sensorConfig", configFromFireBase.toString());
                                intent.putExtra("instanceID", instanceID);
                                startActivity(intent);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}