package bucci.dev.freestyle;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

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

    public void play(String songPath) {
        Log.i(TAG, "Playing.. song path: " + songPath);
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


    public boolean isPaused() {
        return isPaused;
    }
}
