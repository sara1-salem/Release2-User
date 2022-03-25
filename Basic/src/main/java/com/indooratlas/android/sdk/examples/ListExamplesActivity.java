package com.indooratlas.android.sdk.examples;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.indooratlas.android.sdk.examples.geofence.GeofenceMapsOverlayActivity;

import java.util.ArrayList;


/**
 * Main entry into IndoorAtlas examples. Displays all example activities as list and opens selected
 * activity. Activities are populated from AndroidManifest metadata.
 */
public class ListExamplesActivity extends AppCompatActivity {
    private static final String TAG = "IAExample";
    private ImageView splashicon;
    private static int splashtimeout=1500;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    // private ExamplesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        splashicon = findViewById(R.id.imageView);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ListExamplesActivity.this,GeofenceMapsOverlayActivity.class);
                startActivity(intent);
                finish();
            }
        },splashtimeout);
        Animation myanime = AnimationUtils.loadAnimation(this,R.anim.mysplash);     //using animation in splash activity
        splashicon.startAnimation(myanime);


    }




    public void onClickA(View v)
    {
        Intent myintent = new Intent(ListExamplesActivity.this, GeofenceMapsOverlayActivity.class);
        // show map to USER -- DONE
        startActivity(myintent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isSdkConfigured()) {
            ensurePermissions();
        }
    }

    public static boolean checkLocationPermissions(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks that we have access to required information, if not ask for users permission.
     */
    private void ensurePermissions() {

        if (!checkLocationPermissions(this)) {
            // We don't have access to FINE_LOCATION (Required by Google Maps example)
            // IndoorAtlas SDK has minimum requirement of COARSE_LOCATION to enable WiFi scanning
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle(R.string.location_permission_request_title)
                        .setMessage(R.string.location_permission_request_rationale)
                        .setPositiveButton(R.string.permission_button_accept, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "request permissions");
                                ActivityCompat.requestPermissions(ListExamplesActivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
                            }
                        })
                        .setNegativeButton(R.string.permission_button_deny, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(ListExamplesActivity.this,
                                        R.string.location_permission_denied_message,
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                        .show();

            } else {

                // ask user for permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);

            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length == 0
                    || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.location_permission_denied_message,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    //    /**
//     * Adapter for example activities.
//     */
    class ExamplesAdapter extends BaseAdapter {

        final ArrayList<ExampleEntry> mExamples;

        ExamplesAdapter(Context context) {
            mExamples = listActivities(context);
        }


        @Override
        public int getCount() {
            return mExamples.size();
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
            ExampleEntry entry = mExamples.get(position);
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2,
                        parent, false);
            }
            TextView labelText = (TextView) convertView.findViewById(android.R.id.text1);
            TextView descriptionText = (TextView) convertView.findViewById(android.R.id.text2);
            labelText.setText(entry.mLabel);
            descriptionText.setText(entry.mDescription);
            return convertView;
        }
    }


    /**
     * Returns a list of activities that are part of this application, skipping
     * those from included libraries and *this* activity.
     */
    public ArrayList<ExampleEntry> listActivities(Context context) {

        ArrayList<ExampleEntry> result = new ArrayList<>();
        try {
            final String packageName = context.getPackageName();
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);

            for(ActivityInfo activityInfo : info.activities) {
                parseExample(activityInfo, result);
            }

            return result;

        } catch (Exception e) {
            throw new IllegalStateException("failed to get list of activities", e);
        }

    }

    private void parseExample(ActivityInfo info, ArrayList<ExampleEntry> list) {
        try {
            Class<?> cls = Class.forName(info.name);
            if (cls.isAnnotationPresent(SdkExample.class)) {
                SdkExample annotation = (SdkExample) cls.getAnnotation(SdkExample.class);
                list.add(new ExampleEntry(new ComponentName(info.packageName, info.name),
                        annotation.title() != -1
                                ? getString(annotation.title())
                                : info.loadLabel(getPackageManager()).toString(),
                        getString(annotation.description())));
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "failed to read example info for class: " + info.name, e);
        }

    }


    static class ExampleEntry implements Comparable<ExampleEntry> {

        final ComponentName mComponentName;

        final String mLabel;

        final String mDescription;

        ExampleEntry(ComponentName name, String label, String description) {
            mComponentName = name;
            mLabel = label;
            mDescription = description;
        }

        @Override
        public int compareTo(ExampleEntry another) {
            return mLabel.compareTo(another.mLabel);
        }
    }

    //this was changed
    private boolean isSdkConfigured() {
        return !"api-key-not-set".equals("20bafe68-db36-4b89-9397-4612217b49a1")
                && !"api-secret-not-set".equals("gZK/d9gq1MnORalhUJ+WLQ2shzX6h6TvjhwV8Z0K/QxkSO2NuwN+YTcwN/NNvabx47t+Z7MaIiap/j1ucMP8Mvw6ir5Pvo8cJ2RLhtJqVqxTELGMtCY1mxIcF/4rxQ==");
    }


}
