package com.dragontwister.yondekiku.managers;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.dragontwister.yondekiku.interfaces.RecognitionCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContinuousRecognitionManager implements RecognitionListener {
    private final RecognitionCallback callback;
    private final String[] activationWords;
    private final String[] deactivationWords;
    private final boolean shouldMute;

    private final AudioManager audioManager;

    private final SpeechRecognizer speech;
    private final Intent recognizerIntent;

    private List<String> matches;

    public ContinuousRecognitionManager(Context context, String[] activationWords, String[] deactivationWords, boolean shouldMute, RecognitionCallback callback){
        this.callback = callback;
        this.activationWords = activationWords;
        this.deactivationWords = deactivationWords;
        this.shouldMute = shouldMute;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        speech = SpeechRecognizer.createSpeechRecognizer(context);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        matches = new ArrayList<>();
    }

    public void startRecognition(){
        if (audioManager.isBluetoothA2dpOn()) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        } else{
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
        }

        speech.setRecognitionListener(this);
        speech.startListening(recognizerIntent);
    }

    public void stopRecognition() {
        speech.stopListening();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
    }

    public void cancelRecognition() {
        speech.cancel();
    }

    public void destroyRecognizer() {
        muteRecognition(false);
        speech.destroy();
    }

    private void muteRecognition(boolean mute){
        int flag = mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE;
        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, flag, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, flag, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, flag, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, flag, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, flag, 0);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        muteRecognition(shouldMute);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {
        callback.onRmsChanged(rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY :{
                cancelRecognition();
            }
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT : {
                destroyRecognizer();
            }
            case SpeechRecognizer.ERROR_NO_MATCH:{ }
        }

        startRecognition();
    }

    @Override
    public void onResults(Bundle results) {
        List<String> hold = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        StringBuilder text = new StringBuilder();
        for(int i=0; i<hold.size(); i++){
            text.append(hold.get(i));
        }

        matches.add(text.toString());
        callback.onResults(matches);
        matches.clear();

        startRecognition();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
