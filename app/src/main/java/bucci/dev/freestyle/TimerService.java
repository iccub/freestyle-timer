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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static bucci.dev.freestyle.TimerActivity.MSG_PAUSE_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_EXTRA_ROUND_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_PREPARATION_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_STOP_TIMER;
import static bucci.dev.freestyle.TimerActivity.PREPARATION_TIME;
import static bucci.dev.freestyle.TimerActivity.SAVED_SONG_PATH;
import static bucci.dev.freestyle.TimerActivity.SHARED_PREFS;

public class TimerService extends Service {
    private MusicPlayer musicPlayer;
    private MediaPlayer beepPlayer;

    //Duplicated values from TimerType enum(Because serializable objects cannot be put in SharedPreferences)
    private final static int BATTLE_TIME_VALUE = 0;
    private final static int QUALIFICATION_TIME_VALUE = 1;
    private final static int ROUTINE_TIME_VALUE = 2;

    public static boolean hasTimerFinished = false;

    private static final String TAG = "BCC|TimerService";
    private SharedPreferences sharedPrefs;

    private class IncomingHandler extends Handler {
        CountDownTimer countDownTimer = null;

        {
            musicPlayer = new MusicPlayer(TimerService.this);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_START_TIMER:
                    /*SharedPreferences sharedPrefs = getSharedPreferences(SHARED_PREFS, 0);
                    musicPlayer.play(sharedPrefs.getString(SAVED_SONG_PATH, ""));

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

                        @Override
                        public void onFinish() {
                            Log.i(TAG, "Timer onFinish()");
                            musicPlayer.stop();

                            hasTimerFinished = true;

                            sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_FINISH, 0);
                        }
                    }.start();*/

                    startTimerFromBackground((Long) msg.obj);

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

                case MSG_START_PREPARATION_TIMER:
                    Log.i(TAG, "Preparation timer started");
                    hasTimerFinished = false;

                    if (beepPlayer != null) {
                        beepPlayer.release();
                        beepPlayer = null;

                    }

                    countDownTimer = new CountDownTimer(PREPARATION_TIME, 500) {
                        @Override
                        public void onTick(long millsUntilFinished) {
                            sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_PREPARATION_TIMER_TICK, millsUntilFinished);

                        }

                        @Override
                        public void onFinish() {
                            Log.i(TAG, "Timer onFinish()");
                            startTimerFromBackground(getStartTime());

//                            sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_PREPARATION_TIMER_FINISH, 0);
                        }
                    }.start();

                    break;

                case MSG_START_EXTRA_ROUND_TIMER:
                    Log.i(TAG, "Extra round preparation timer started");
                    hasTimerFinished = false;
                    countDownTimer = new CountDownTimer(PREPARATION_TIME, 500) {
                        @Override
                        public void onTick(long millsUntilFinished) {
                            sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_PREPARATION_TIMER_TICK, millsUntilFinished);

                        }

                        @Override
                        public void onFinish() {
                            Log.i(TAG, "Timer onFinish()");
                            startTimerFromBackground(10 * 1000);
                        }
                    }.start();

                break;


                default:
                    super.handleMessage(msg);
            }
            super.handleMessage(msg);
        }

        private void startTimerFromBackground(long time) {
            SharedPreferences settings = getSharedPreferences(SHARED_PREFS, 0);
            musicPlayer.play(settings.getString(SAVED_SONG_PATH, ""));


            Log.i(TAG, "Timer started, miliseconds to finish: " + time);
            countDownTimer = new CountDownTimer((Long) time, 500) {
                @Override
                public void onTick(long millsUntilFinished) {
                    if (isIntervalReached(millsUntilFinished)) {
                        playBeep();
                    } else if(isTimerFinishing(millsUntilFinished))
                        playFinishBeep();
                    sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_TICK, millsUntilFinished);

                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "Timer onFinish()");
                    musicPlayer.stop();

                    hasTimerFinished = true;

                    sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_FINISH, 0);
                }
            }.start();
        }

        //Playing finish horn sound a second before onFinish() for smoother experience
        private boolean isTimerFinishing(long millsUntilFinished) {
            return millsUntilFinished < 1000;
        }

        private boolean isIntervalReached(long millsUntilFinished) {
            return millsUntilFinished % (30 * 1000) < 500;
        }

        private void sendBroadcastToTimerActivity(String action, long timeLeft) {
            Intent intent = new Intent(action);
            if (action.equals(TimerIntentFilter.ACTION_TIMER_TICK) || action.equals(TimerIntentFilter.ACTION_PREPARATION_TIMER_TICK))
                intent.putExtra(TimerActivity.TIME_LEFT, timeLeft);
            LocalBroadcastManager.getInstance(TimerService.this).sendBroadcast(intent);

        }
    }

    private long getStartTime() {
        switch (sharedPrefs.getInt(StartActivity.TIMER_TYPE, -1)) {
            case BATTLE_TIME_VALUE:
                return TimerActivity.BATTLE_TIME + 100;
            case QUALIFICATION_TIME_VALUE:
                return TimerActivity.ROUTINE_TIME + 100;
            case ROUTINE_TIME_VALUE:
                return TimerActivity.ROUTINE_TIME + 100;
            default:
                Log.e(TAG, "getStartTime(), Error in getting start time");
                return 0;
        }

    }

    private void playBeep() {
        if (beepPlayer == null) {
            switch (sharedPrefs.getInt(StartActivity.TIMER_TYPE, -1)) {
                case BATTLE_TIME_VALUE:
                    beepPlayer = MediaPlayer.create(TimerService.this, R.raw.airhorn);
                    break;
                case ROUTINE_TIME_VALUE:
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
        Log.d(TAG, "IBinder onBind()");
        sharedPrefs = getSharedPreferences(SHARED_PREFS, 0);

        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "IBinder onUnbind()");
        musicPlayer.stop();

        return super.onUnbind(intent);
    }

}
