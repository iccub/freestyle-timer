package bucci.dev.freestyle;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;

import static bucci.dev.freestyle.TimerActivity.MSG_PAUSE_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_STOP_TIMER;
import static bucci.dev.freestyle.TimerActivity.SAVED_SONG_PATH;
import static bucci.dev.freestyle.TimerActivity.SHARED_PREFS;

public class TimerService extends Service {
    private MusicPlayer musicPlayer;
    private MediaPlayer beepPlayer;


    public static boolean hasTimerFinished = false;

    private static final String TAG = "BCC|TimerService";
    private char timerType;

    private class IncomingHandler extends Handler {
        CountDownTimer countDownTimer = null;

        {
            musicPlayer = new MusicPlayer(TimerService.this);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_START_TIMER:
                    SharedPreferences settings = getSharedPreferences(SHARED_PREFS, 0);
                    musicPlayer.play(settings.getString(SAVED_SONG_PATH, ""));

                    Log.i(TAG, "Timer started, miliseconds to finish: " + msg.obj);
                    countDownTimer = new CountDownTimer((Long) msg.obj, 500) {
                        @Override
                        public void onTick(long millsUntilFinished) {
                            if (isIntervalReached(millsUntilFinished)) {
                                playBeep();
                            } else if(isTimerFinishing(millsUntilFinished))
                                playFinishBeep();
                            sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_TICK, millsUntilFinished);

                        }

                        //Playing finish horn sound second before onFinish() for smoother experience
                        private boolean isTimerFinishing(long millsUntilFinished) {
                            return millsUntilFinished < 1000;
                        }

                        private boolean isIntervalReached(long millsUntilFinished) {
                            return millsUntilFinished % (30 * 1000) < 500;
                        }

                        @Override
                        public void onFinish() {
                            Log.i(TAG, "Timer onFinish()");

                            musicPlayer.stop();

//                            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//                            mNotificationManager.cancel(TimerActivity.NOTIFICATION_TIMER_RUNNING);

                            hasTimerFinished = true;

                            sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_FINISH, 0);
                        }
                    }.start();

                    break;

                case MSG_PAUSE_TIMER:
                    musicPlayer.pause();
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        Log.i(TAG, "Timer paused");

                        hasTimerFinished = false;
                    }

                    break;

                case MSG_STOP_TIMER:
                    musicPlayer.stop();
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        Log.i(TAG, "Timer stopped");

                        hasTimerFinished = false;
                    }
                    sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_STOP, 0);

                    break;

                case 3:
                    Log.i(TAG, "wchodze w trojke");


                default:
                    super.handleMessage(msg);
            }
            super.handleMessage(msg);
        }

        private void sendBroadcastToTimerActivity(String action, long timeleft) {
            Intent intent = new Intent(action);
            if (action.equals(TimerIntentFilter.ACTION_TIMER_TICK))
                intent.putExtra(TimerActivity.TIME_LEFT, timeleft);
            LocalBroadcastManager.getInstance(TimerService.this).sendBroadcast(intent);

        }
    }

    private void playBeep() {
        if (beepPlayer == null) {
            switch (timerType) {
                case StartActivity.TYPE_BATTLE:
                    beepPlayer = MediaPlayer.create(TimerService.this, R.raw.airhorn);
                    break;
                case StartActivity.TYPE_ROUTINE:
                    beepPlayer = MediaPlayer.create(TimerService.this, R.raw.beep2);
                    break;
            }

        }

        beepPlayer.start();
    }

    private void playFinishBeep() {
        beepPlayer = MediaPlayer.create(TimerService.this, R.raw.airhorn_finish);
        beepPlayer.start();
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "IBinder onBind() type: ");
        timerType = intent.getCharExtra(StartActivity.TIMER_TYPE, 'E');

        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        musicPlayer.stop();

        return super.onUnbind(intent);
    }

}
