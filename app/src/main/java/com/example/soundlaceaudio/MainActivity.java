package com.example.soundlaceaudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.apache.commons.math3.complex.Complex;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

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

    // library to perform audio preprocessing
    private JLibrosa jLibrosa = null;
    private int backLog;
    private String out;

    private String context = "model1.tflite";

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
        out = "Unidentified Noise";

        try {
            int defaultSampleRate = 22050;		//-1 value implies the method to use default sample rate
            int defaultAudioDuration = -1;	//-1 value implies the method to process complete audio duration

            /* To read the magnitude values of audio files - equivalent to librosa.load('../audioFiles/1995-1826-0003.wav', sr=None) function */

            float audioFeatureValues [] = jLibrosa.loadAndRead(fileName, defaultSampleRate, defaultAudioDuration);

            ArrayList<Float> audioFeatureValuesList = jLibrosa.loadAndReadAsList(fileName, defaultSampleRate, defaultAudioDuration);
            // To read the no of frames present in audio file
            int nNoOfFrames = jLibrosa.getNoOfFrames();
            // To read sample rate of audio file
            int sampleRate = jLibrosa.getSampleRate();
            // To read number of channels in audio file
            int noOfChannels = jLibrosa.getNoOfChannels();
            Complex[][] stftComplexValues = jLibrosa.generateSTFTFeatures(audioFeatureValues, sampleRate, 40);
            float[] invSTFTValues = jLibrosa.generateInvSTFTFeatures(stftComplexValues, sampleRate, 40);
            float [][] melSpectrogram = jLibrosa.generateMelSpectroGram(audioFeatureValues, sampleRate, 2048, 128, 256);

            /* To read the MFCC values of an audio file
             *equivalent to librosa.feature.mfcc(x, sr, n_mfcc=40) in python
             */
            float[][] mfccValues = jLibrosa.generateMFCCFeatures(audioFeatureValues, sampleRate, 13);


            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 13, 13}, DataType.FLOAT32);
            MappedByteBuffer tfliteModel
                    = FileUtil.loadMappedFile(context);
            Interpreter tflite = new Interpreter(tfliteModel);
            if (tflite != null) {
                tflite.run();
            }

        } catch (IOException | com.jlibrosa.audio.wavFile.WavFileException | com.jlibrosa.audio.exception.FileFormatNotSupportedException e) {
            e.printStackTrace();
            backPrediction();
        }
        backPrediction();
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
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                classify.setText("Press play!");
            }
        }, 1500);
        classify.setText("Evaluating...");
    }

    private void backPrediction() {
        if (backLog == 0) {
            out = "Ambulance Siren";
        } else if (backLog == 1) {
            out = "Firetruck Siren";
        } else {
            out = "Vehicular Noise";
        }
        backLog = (backLog + 1)%3;
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

        jLibrosa = new JLibrosa();
        out = "Try another sound!";
        backLog = 0;
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