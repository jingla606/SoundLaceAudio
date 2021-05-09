package com.example.soundlaceaudio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
//import android.support.v4.*;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.*;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    private Button rec = null;
    private Button play = null;
    private TextView classify = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};


    private boolean mStartRecording;
    private boolean mStartPlaying;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }


    public void onRecordClick(View v) {
        onRecord(mStartRecording);
        if (mStartRecording) {
            rec.setText("Stop recording");
        } else {
            rec.setText("Start recording");
        }
        mStartRecording = !mStartRecording;
    }

    public void onPlayClick(View v) {
        onPlay(mStartPlaying);
        if (mStartPlaying) {
            play.setText("Stop playing");
        } else {
            play.setText("Start playing");
        }
        mStartPlaying = !mStartPlaying;
    }

    private void eval() {
        String out = "Unidentified Noise";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/usr/sbin/python3", "pyBridge.py", "");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            out = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        classify.setText(out);
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        eval();

        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        classify.setText("Try another sound!");
        player.release();
        player = null;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioChannels(1);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(48000);
        recorder.setOutputFile(fileName);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);
        // Record to the external cache directory for visibility
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/sample.wav";
        Log.d(fileName, "AUDIO SAMPLE FILENAME");
        mStartRecording = true;
        mStartPlaying = true;
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        rec = (Button)findViewById(R.id.Record);
        play = (Button)findViewById(R.id.Play);
        classify = (TextView) findViewById(R.id.textClassify);
        rec.setText("Start Recording");
        play.setText("Start Playing");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }
}