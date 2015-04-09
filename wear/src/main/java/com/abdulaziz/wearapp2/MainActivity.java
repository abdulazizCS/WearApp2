package com.abdulaziz.wearapp2;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;

import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {


    private GoogleApiClient mGoogleApiClient;
    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private View mView;
    private TextView mTextValues;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private long mShakeTime = 0;
    private long mRotationTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        mSensorManager = (SensorManager) MainActivity.this.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.activity_main, null);

    }




    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

        mTextValues = (TextView) findViewById(R.id.text_values);
        mTextValues.setText(
                "x = " + Float.toString(event.values[0]) + "\n" +
                        "y = " + Float.toString(event.values[1]) + "\n" +
                        "z = " + Float.toString(event.values[2]) + "\n"
        );

        if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = FloatMath.sqrt(gX * gX + gY * gY + gZ * gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            if(gForce > SHAKE_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 100));
                onButtonClicked(mView);
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color
            if(Math.abs(event.values[0]) > ROTATION_THRESHOLD){
                //mView.setBackgroundColor(Color.rgb(100, 100, 0));

            } else if (Math.abs(event.values[1]) > ROTATION_THRESHOLD ){
               // mView.setBackgroundColor(Color.rgb(0, 100, 100));
                onButtonClicked(mView);

            }else if (Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                //mView.setBackgroundColor(Color.rgb(0, 100, 0));
                onButtonClicked(mView);

            }
            else {
                mView.setBackgroundColor(Color.BLACK);

            }
        }
    }



    public void onButtonClicked(View target) {
        if (mGoogleApiClient == null)
            return;

        final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                final List<Node> nodes = result.getNodes();
                if (nodes != null) {
                    for (int i=0; i<nodes.size(); i++) {
                        final Node node = nodes.get(i);

                        // You can just send a message
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "/MESSAGE", null);

                        // or you may want to also check for a result:
                         final PendingResult<MessageApi.SendMessageResult> pendingSendMessageResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "/MESSAGE", null);
                         pendingSendMessageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                 if (sendMessageResult.getStatus().getStatusCode()== WearableStatusCodes.SUCCESS) {
                                     TextView mTextView = (TextView) findViewById(R.id.text);

                                     mTextView.setText("Sending...");

                                  }
                              }
                         });
                    }
                }
            }
        });
    }
}
