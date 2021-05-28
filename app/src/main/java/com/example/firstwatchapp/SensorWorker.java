package com.example.firstwatchapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class SensorWorker extends Worker implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private int count = 0;
    private final int MAX_DATA = 100;
    private float[][] sensorData = new float[3][MAX_DATA];
    private boolean isSensorActive = false;
    private long startTime;
    private long endTime;
    private UploadTask task;
    private Sensor accel;

    public SensorWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);

        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);

    }

    @Override
    public Result doWork(){
        Log.d("SensorWorler", "SensorWorker start");
        isSensorActive = true;
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        //sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isSensorActive) {
            if(count == 0){
                startTime = System.currentTimeMillis();
            }else if(count == MAX_DATA-1){
                endTime = System.currentTimeMillis();
            }

            if(count < MAX_DATA){
                sensorData[0][count] = event.values[0];
                sensorData[1][count] = event.values[1];
                sensorData[2][count] = event.values[2];
            }else if(count == MAX_DATA){

                //データの送信
                postData();

                isSensorActive = false;
                sensorManager.unregisterListener(this);


            }
            count++;
        }

    }

    private void postData(){

        HashMap<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("elapsedTime" , (endTime - startTime));

        ArrayList<Object> array = new ArrayList<>();

        for(int i = 0; i < MAX_DATA; i++){
            HashMap<String, Object> acceleration = new HashMap<>();
            acceleration.put("id", i);
            acceleration.put("x", sensorData[0][i]);
            acceleration.put("y", sensorData[1][i]);
            acceleration.put("z", sensorData[2][i]);
            array.add(acceleration);
        }
        jsonMap.put("acceleration" , array);


        if (jsonMap.size() > 0) {
            JSONObject responseJsonObject = new JSONObject(jsonMap);
            String jsonText = responseJsonObject.toString();
            task = new UploadTask();
            task.setListener(createListener());
            task.execute(jsonText);
        }
    }
    private UploadTask.Listener createListener() {
        return new UploadTask.Listener() {
            @Override
            public void onSuccess(String result) {
                Log.d("SensorWorker", result);
            }
        };
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}

