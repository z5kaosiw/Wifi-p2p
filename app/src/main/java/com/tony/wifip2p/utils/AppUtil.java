package com.tony.wifip2p.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AppUtil {

    public static  boolean validateMicAvailability(){
        Boolean available = true;
        AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_DEFAULT, 44100);
        try{
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED ){
                Log.e("TAG","recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED");
                available = false;

            }

            recorder.startRecording();
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING){
                Log.e("TAG","recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING");
                recorder.stop();
                available = false;

            }
            recorder.stop();
        }catch (Exception e){

             Log.e("TAG",e.toString());

        }finally{
            recorder.release();
            recorder = null;
        }

        return available;
    }
}
