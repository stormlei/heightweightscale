package com.jkkc.serialtest;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class QpMediaPlayer {
    private HashMap<String, AssetFileDescriptor> audioIds;
    private MediaPlayer mediaPlayer;
    private String audioPath = "audio";
    private String ext = "wav";

    private Context context;

    public QpMediaPlayer(Context context) {
        this.context = context;
        audioIds = new HashMap<>();
        mediaPlayer = new MediaPlayer();
    }

    public void init() throws IOException {
        //加载整数部分音效
        int intPartCount = 249;
        for (int i = 0; i <= intPartCount; i++) {
            String key = "p"+i;
            audioIds.put(key, context.getAssets().openFd(audioPath+"/"+key+"."+ext));
        }

        //加载小数部分音效
        int decimalPartCount = 9;
        for (int i = 0; i <= decimalPartCount; i++) {
            String key = "d"+i;
            audioIds.put(key, context.getAssets().openFd(audioPath+"/"+key+"."+ext));
        }

        audioIds.put("cm", context.getAssets().openFd(audioPath+"/cm."+ext));
        audioIds.put("kg", context.getAssets().openFd(audioPath+"/kg."+ext));
        audioIds.put("weight", context.getAssets().openFd(audioPath+"/weight."+ext));
        audioIds.put("height", context.getAssets().openFd(audioPath+"/height."+ext));
    }

    private void play(String id) {
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        AssetFileDescriptor audio = audioIds.get(id);
        try{
            mediaPlayer.setDataSource(audio.getFileDescriptor(), audio.getStartOffset(), audio.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
        }catch (Exception e){
            Log.e("QpMediaPlayer", "play error: "+e.getMessage()+"}");
        }
    }

    public void playNumber(String type, Float number) throws InterruptedException {
        String input = number.toString();
        String[] splitValues = input.split("\\.");

        List<String> audioIndex = new ArrayList<String>();
        //先加入类型
        audioIndex.add(type);
        //暂停1秒,保证类型读完
        audioIndex.add("gap_1100");

        //加入整数部分
        String intPart = splitValues[0];
        audioIndex.add("p"+intPart);
        if(Integer.parseInt(intPart) < 100){
            //暂停800毫秒
            audioIndex.add("gap_800");
        } else {
            //暂停1000毫秒
            audioIndex.add("gap_1000");
        }


        //加入小数部分
        String decimalPart = splitValues[1];
        if(!TextUtils.isEmpty(decimalPart) && !"0".equals(decimalPart)){
            decimalPart = decimalPart.substring(0, 1);
            audioIndex.add("d"+decimalPart);
            //暂停600毫秒
            audioIndex.add("gap_800");
        }

        //加入单位
//        if("weight".equals(type)) {
//            audioIndex.add("kg");
//        } else if ("height".equals(type)){
//            audioIndex.add("cm");
//        }
//        audioIndex.add("gap_1000");

        //循环audioIndex, 调用soundPlayer.play(audioIndex)
        for (String it: audioIndex) {
            if(it.startsWith("gap_")){
                Thread.sleep(Long.parseLong(it.substring(4)));
            } else {
                play(it);
            }
        }
    }
}