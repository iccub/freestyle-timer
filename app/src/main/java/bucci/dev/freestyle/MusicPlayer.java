/*
 Copyright Michal Buczek, 2014
 All rights reserved.

This file is part of Freestyle Timer.

    Freestyle Timer is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Freestyle Timer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Freestyle Timer; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


package bucci.dev.freestyle;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;


public class MusicPlayer {
    private static final boolean DEBUG = false;
    private static final String TAG = "BCC|MusicPlayer";

    private MediaPlayer player;
    private boolean isPaused = false;

    public void play(String songPath) {
        if (DEBUG) Log.i(TAG, "Preparing music player.. song path: " + songPath);
        if (!isPaused) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                player.setDataSource(songPath);
                player.prepare();
            } catch (IOException e) {
                if (DEBUG) Log.e(TAG, "ERROR: " + e.toString());
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
