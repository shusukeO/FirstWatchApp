package com.example.firstwatchapp;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import android.widget.Toast;

public class MainActivity extends WearableActivity {

    private PeriodicWorkRequest sensorWorkRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFunc();
            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopFunc();
            }
        });

    }

    private void startFunc(){
        startWorkManager();
        Toast.makeText(this , "バックグラウンド処理を開始しました", Toast.LENGTH_LONG).show();
        Log.d("Main Activity", "start button pushed");
    }

    private void stopFunc(){
        WorkManager.getInstance(this).cancelUniqueWork("postAccelerationData");
        Toast.makeText(this , "バックグラウンド処理を終了しました", Toast.LENGTH_LONG).show();
        Log.d("Main Activity", "stop button pushed");
    }

    private void startWorkManager(){

        //ネットワーク状態の制限
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //15分ごと（最短区間）のリクエストを作成
        sensorWorkRequest =
                new PeriodicWorkRequest.Builder(SensorWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        //同一リクエストは無効にしつつキューに入れる
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "postAccelerationData",
                ExistingPeriodicWorkPolicy.KEEP,
                sensorWorkRequest
        );
    }
}
