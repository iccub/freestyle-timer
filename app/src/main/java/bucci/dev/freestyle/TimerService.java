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

import static bucci.dev.freestyle.TimerActivity.DELAY_FOR_BEEP;
import static bucci.dev.freestyle.TimerActivity.MSG_PAUSE_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_EXTRA_ROUND_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_PREPARATION_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_STOP_TIMER;
import static bucci.dev.freestyle.TimerActivity.PREPARATION_DURATION;
import static bucci.dev.freestyle.TimerActivity.SAVED_SONG_PATH_PARAM;
import static bucci.dev.freestyle.TimerActivity.SHARED_PREFS_PARAM;
import static bucci.dev.freestyle.TimerActivity.TIMER_TYPE_ERROR_VALUE;

public class TimerService extends Service {
    private static final String TAG = "BCC|TimerService";

    //Duplicated values from TimerType enum(Because serializable objects cannot be put in SharedPreferences)
    private final static int BATTLE_TIME_VALUE = 0;
    private final static int QUALIFICATION_TIME_VALUE = 1;
    private final static int ROUTINE_TIME_VALUE = 2;
    public static final int COUNT_DOWN_INTERVAL = 500;

    public static boolean hasTimerFinished = false;

    private SharedPreferences sharedPrefs;

    private MusicPlayer musicPlayer;
    private MediaPlayer beepPlayer;

    private class IncomingHandler extends Handler {
        CountDownTimer countDownTimer = null;

        {
            musicPlayer = new MusicPlayer();
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_START_TIMER:
                    startTimerFromBackground((Long) msg.obj);
                    break;

                case MSG_PAUSE_TIMER:
                    musicPlayer.pause();
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        Log.d(TAG, "Timer paused");

                        hasTimerFinished = false;
                    }

                    break;

                case MSG_STOP_TIMER:
                    musicPlayer.stop();
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        Log.d(TAG, "Timer stopped");

                        hasTimerFinished = false;
                    }
                    sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_STOP, 0);

                    break;

                case MSG_START_PREPARATION_TIMER:
                    Log.i(TAG, "Preparation timer started");
                    startPreparationTimer(false);
                    break;

                case MSG_START_EXTRA_ROUND_TIMER:
                    Log.i(TAG, "Extra round preparation timer started");
                    startPreparationTimer(true);
                    break;

                default:
                    super.handleMessage(msg);
            }
            super.handleMessage(msg);
        }

        private void startPreparationTimer(final boolean isExtraRound) {
            hasTimerFinished = false;

            if (beepPlayer != null) {
                beepPlayer.release();
                beepPlayer = null;
            }

            countDownTimer = new CountDownTimer(PREPARATION_DURATION, COUNT_DOWN_INTERVAL) {
                @Override
                public void onTick(long millsUntilFinished) {
                    sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_PREPARATION_TIMER_TICK, millsUntilFinished);
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Preparation timer onFinish()");
                    if (isExtraRound)
                        startTimerFromBackground(TimerActivity.EXTRA_TIME_DURATION);
                    else
                        startTimerFromBackground(getStartTime());

                }
            }.start();
        }

        private void startTimerFromBackground(long time) {
            SharedPreferences settings = getSharedPreferences(SHARED_PREFS_PARAM, 0);
            musicPlayer.play(settings.getString(SAVED_SONG_PATH_PARAM, ""));


            Log.i(TAG, "Timer started, milliseconds to finish: " + time);
            countDownTimer = new CountDownTimer(time, COUNT_DOWN_INTERVAL) {
                @Override
                public void onTick(long millsUntilFinished) {
                    if (getTimerTypeValue() != ROUTINE_TIME_VALUE) {
                        if (isIntervalReached(millsUntilFinished)) {
                            playBeep();
                        } else if (isTimerFinishing(millsUntilFinished))
                            playFinishBeep();
                    }
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
                intent.putExtra(TimerActivity.TIME_LEFT_PARAM, timeLeft);
            LocalBroadcastManager.getInstance(TimerService.this).sendBroadcast(intent);

        }
    }

    private long getStartTime() {
        switch (getTimerTypeValue()) {
            case BATTLE_TIME_VALUE:
                return TimerActivity.BATTLE_DURATION + DELAY_FOR_BEEP;
            case QUALIFICATION_TIME_VALUE:
                return TimerActivity.QUALIFICATION_DURATION + DELAY_FOR_BEEP;
            case ROUTINE_TIME_VALUE:
                return TimerActivity.routine_duration + DELAY_FOR_BEEP;
            default:
                Log.e(TAG, "getStartTime(), Error in getting start time");
                return 0;
        }

    }

    private int getTimerTypeValue() {
        return sharedPrefs.getInt(StartActivity.TIMER_TYPE, TIMER_TYPE_ERROR_VALUE);
    }

    private void playBeep() {
        if (beepPlayer == null) {
            switch (getTimerTypeValue()) {
                case BATTLE_TIME_VALUE:
                    beepPlayer = MediaPlayer.create(TimerService.this, R.raw.airhorn);
                    break;
                case QUALIFICATION_TIME_VALUE:
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

    final Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "IBinder onBind()");
        sharedPrefs = getSharedPreferences(SHARED_PREFS_PARAM, MODE_PRIVATE);

        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "IBinder onUnbind()");
        musicPlayer.stop();

        return super.onUnbind(intent);
    }

}
