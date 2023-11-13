package com.jkkc.serialtest;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.List;

public class PlayMedia extends AsyncTask<Void, Void, Void> {
    private static final String LOG_TAG = PlayMedia.class.getSimpleName();

    Context context;
    private MediaPlayer mediaPlayer;
    List<Integer> soundIDs;
    int idx =1;

    public PlayMedia(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }
    public PlayMedia(final Context context, List<Integer> soundIDs) {
        this.context = context;
        this.soundIDs=soundIDs;
        mediaPlayer = MediaPlayer.create(context,soundIDs.get(0));
        setNextMediaForMediaPlayer(mediaPlayer);
    }

    public void setNextMediaForMediaPlayer(MediaPlayer player){
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if(soundIDs.size() > idx){
                    mp.release();
                    mp = MediaPlayer.create(context,soundIDs.get(idx));
                    setNextMediaForMediaPlayer(mp);
                    mp.start();
                    idx += 1;
                }
            }
        });
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "", e);
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "", e);
        }

        return null;
    }
}
