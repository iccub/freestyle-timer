package bucci.dev.freestyle;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class MusicPlayer {
    private static final String TAG = "BCC|MusicPlayer";

    private MediaPlayer player;
    private boolean isPaused = false;

    public void play(String songPath) {
        Log.i(TAG, "Preparing music player.. song path: " + songPath);
        if (!isPaused) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                player.setDataSource(songPath);
                player.prepare();
            } catch (IOException e) {
                Log.e(TAG, "ERROR: " + e.toString());
                e.printStackTrace();
            }
        }
        player.start();
    }

    public void stop() throws IllegalStateException {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        isPaused = false;
    }

    public void pause() {
        if (player != null) {
            player.pause();
            isPaused = true;
        }
    }

}
