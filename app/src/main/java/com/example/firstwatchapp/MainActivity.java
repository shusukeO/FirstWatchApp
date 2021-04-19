package com.example.firstwatchapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends WearableActivity implements SensorEventListener, LocationListener {

    private TextView mTextView, textView;
    private  Button startButton;
    private String text = null;
    private SensorManager sensorManager;
    private Sensor sensor;
    int count = 0;
    private float x,y,z;

    private Sensor geoSensor;

    private File file;

    public static final int MAX_DATA = 100;
    private float[][] sensorData = new float[3][MAX_DATA];

    Handler handler= new Handler();

    private boolean isSensorActive = false;

    long startTime;
    long endTime;

    //Postクラス
    private UploadTask task;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        geoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        mTextView = (TextView) findViewById(R.id.data);
        textView = findViewById(R.id.text);
        startButton = findViewById(R.id.startButton);

        // Enables Always-on
        setAmbientEnabled();


        //データ保存ファイル作成
        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String datafile ="datafile.json";
        file = new File(path, datafile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        Sensor accel = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        //sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
    }

    // 解除するコードも入れる!
    @Override
    protected void onPause() {
        super.onPause();
        // Listenerを解除
        sensorManager.unregisterListener(this);
    }

    public void startFunc(View v){
        if(!isSensorActive){
            isSensorActive = true;
            startButton.setText("計測中");
            textView.setText("...");
        }
        Log.d("start button", "pushed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isSensorActive) {
            //x = (x * GAIN + event.values[0] * (1 - GAIN));
            //y = (y * GAIN + event.values[1] * (1 - GAIN));
            //z = (z * GAIN + event.values[2] * (1 - GAIN));
//            x = event.values[0];
//            y = event.values[1];
//            z = event.values[2];

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
                Log.d("saveFile", "データの保存開始！！ ");
                postData();


                //別スレッドで保存開始
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //ファイルに書き出し
                        saveFile();

                        //スレッド終了後に「保存されました」と表示
                     handler.post(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(text); // 画面に描画する処理
                                startButton.setText("計測開始");
                                isSensorActive = false;
                                count = 0;
                            }
                        });
                    }
                })).start();

            }
            count++;
//            mTextView.setText(String.format("X : %f\nY : %f\nZ : %f" , x, y, z));
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

    public void saveFile(){
        // 現在ストレージが書き込みできるかチェック
        if(isExternalStorageWritable()){

            String str = (endTime - startTime) + "ms(処理時間)" +"[";

            for(int i = 0; i < MAX_DATA; i++){
                str += "{\"id\":" + i + ",\"x\":" + sensorData[0][i] + ",\"y\":" + sensorData[1][i] + ",\"z\":" + sensorData[2][i] + "}";
                if(i != MAX_DATA -1){
                    str += ",";
                }
//                str += "x : " + sensorData[0][i] + ", y : " + sensorData[1][i] + ", z : " + sensorData[2][i] + "\n";
            }

            str += "]";

            try(FileOutputStream fileOutputStream =
                        new FileOutputStream(file, false);
                OutputStreamWriter outputStreamWriter =
                        new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                BufferedWriter bw =
                        new BufferedWriter(outputStreamWriter);
            ) {

                bw.write(str);
                bw.flush();
                text = "保存されました";
            } catch (Exception e) {
                text = "error: FileOutputStream";
                e.printStackTrace();
            }
//            textView.setText(text);
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    @Override
    protected void onDestroy() {
        task.setListener(null);
        super.onDestroy();
    }

    private UploadTask.Listener createListener() {
        return new UploadTask.Listener() {
            @Override
            public void onSuccess(String result) {
                textView.setText(result);
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
