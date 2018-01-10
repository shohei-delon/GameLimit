package com.delon.user.gamelimit;

import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView timerTextView;
    private Button startButton;

    private SharedPreferences timeRecordPref;
    private Date nowDate;
    private SoundPool soundPool;

    private MyTimer myTimer;

    private long limit;
    private long nowDate2;
    private long oldDate2;
    private long dayDiff;
    private String hour;
    private String minute;
    private String second;
    private int buttonCounter;
    private int startSoundId;
    private int stopSoundId;
    private int finishSoundId;
    private boolean soundTorF = false;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this,"ca-app-pub-9589073381069791~1815924468");

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        mAdView = (AdView) findViewById(R.id.ad_view);

        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);

        timerTextView = (TextView)findViewById(R.id.timer_text);
        startButton = (Button)findViewById(R.id.start_button);

        if (Build.VERSION.SDK_INT > 21 ) {
            soundTorF = true;

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA).build())
                    .setMaxStreams(3)
                    .build();

            startSoundId = soundPool.load(this,R.raw.btn01,0);
            stopSoundId = soundPool.load(this,R.raw.btn02,0);
            finishSoundId = soundPool.load(this, R.raw.btn07,0);

        }

        timeRecordPref = getSharedPreferences("timer",MODE_PRIVATE);

        nowDate = new Date();
        nowDate2 = nowDate.getTime();

        if (timeRecordPref.contains("date")) {
            oldDate2 = timeRecordPref.getLong("date", 0);

            dayDiff = (oldDate2 - nowDate2) / (24 * 60 * 60 * 1000);

            SharedPreferences.Editor editor = timeRecordPref.edit();
            editor.putLong("date", nowDate2);
            editor.apply();
        }

        if (dayDiff > 0){
            //もし日付がたっていたら
            long lim = dayDiff * 7200000;
            if (timeRecordPref.contains("hour")){
                //もしストップウォッチをスタートしていて日付がたっていたら
                loadLimit();
                limit = Integer.parseInt(hour) * 60 * 60 *1000 + Integer.parseInt(minute) * 60 *1000 + Integer.parseInt(second) * 1000 + lim;
                if (limit > 28800000){
                    limit = 28800000;
                }
                myTimer = new MyTimer(limit, 1000);
            }
        }else if (dayDiff <= 0&&timeRecordPref.contains("hour")){
            //もしストップウォッチをスタートしていて日付がたっていなかったら
            loadLimit();
            myTimer = new MyTimer(Integer.parseInt(hour) * 60 * 60 *1000 + Integer.parseInt(minute) * 60 *1000 + Integer.parseInt(second) * 1000, 1000);
        }else {
            //もしストップウォッチをスタートしていなかったら
            myTimer = new MyTimer(7200000, 1000);
        }
        startButton.setOnClickListener(this);

    }


    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.start_button:
                buttonCounter++;
                if (buttonCounter % 2 == 1) {
                    startButton.setText("stop");

                    if (timeRecordPref.contains("hour")) {
                        loadLimit();
                        myTimer = new MyTimer(Integer.parseInt(hour) * 60 * 60 *1000 + Integer.parseInt(minute) * 60 *1000 + Integer.parseInt(second) * 1000, 1000);
                    }

                    if (soundTorF){
                        //Apiレベルが高かったら
                        soundPool.play(startSoundId,1.0F, 1.0F, 0, 0, 1.0F);
                    }

                    myTimer.start();
                }else {
                    startButton.setText("start");

                    if (soundTorF){
                        soundPool.play(stopSoundId,1.0F, 1.0F, 0, 0, 1.0F);
                    }

                    saveLimit();
                    myTimer.cancel();
                }
                break;
        }
    }

    private void loadLimit(){
        hour = timeRecordPref.getString("hour", "00");
        minute = timeRecordPref.getString("minute", "00");
        second = timeRecordPref.getString("second", "00");
    }

    private void saveLimit(){
        //止めた時の時間を取得する
        String stopTime = timerTextView.getText().toString();
        //:←こいつを区切れ目としてstoptimerの値を保存する
        String[] stopTime2 = stopTime.split(":", 0);

        SharedPreferences.Editor editor = timeRecordPref.edit();
        editor.putBoolean("first", false);
        editor.putString("hour", stopTime2[0]);
        editor.putString("minute", stopTime2[1]);
        editor.putString("second", stopTime2[2]);
        editor.apply();
    }

    private class MyTimer extends CountDownTimer{

        MyTimer(long millisInFuture, long countDownInterval){
            super(millisInFuture, countDownInterval);
            timerTextView.setText(String.format("%02d", millisInFuture / 60 / 60 / 1000) + ":"
                    + String.format("%02d",millisInFuture / 60 / 1000 % 60) + ":"
                    + String.format("%02d", millisInFuture / 1000 % 60));

        }

        @Override
        public void onFinish(){
            //TODO カウントダウン完了後
            if (soundTorF){
                soundPool.play(finishSoundId,1.0F, 1.0F, 0, -1, 1.0F);
            }
        }

        @Override
        public void onTick(long millisUntilFinished){
            //インターバルごとに呼ばれる
            timerTextView.setText(String.format("%02d",millisUntilFinished / 1000 / 60 / 60) + ":"//●●時間
                    + String.format("%02d",millisUntilFinished / 1000 / 60 % 60) + ":"//●●分
                    + String.format("%02d",millisUntilFinished / 1000 % 60));//●●秒
        }

    }

    @Override
    public void onPause(){
        saveLimit();

        SharedPreferences.Editor editor = timeRecordPref.edit();
        editor.putLong("date",nowDate2);
        editor.apply();

        soundPool.release();

        super.onPause();
    }

}
