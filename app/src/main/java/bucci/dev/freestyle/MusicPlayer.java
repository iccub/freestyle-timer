package bucci.dev.freestyle;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * Created by bucci on 16.04.14.
 */
public class MusicPlayer {
    private static final String TAG = "BCC|MusicPlayer";

    private MediaPlayer player;
    boolean isPaused = false;
    Context context;

    MusicPlayer(Context context) {
        this.context = context;
    }

    public void play(int resourceId) {
        Log.d(TAG, "isPaused: " + isPaused);
        if (!isPaused)
            player = MediaPlayer.create(context, resourceId);
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


    public boolean isPaused() {
        return isPaused;
    }
}
