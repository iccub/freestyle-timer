package bucci.dev.freestyle;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static bucci.dev.freestyle.TimerActivity.MSG_PAUSE_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_START_TIMER;
import static bucci.dev.freestyle.TimerActivity.MSG_STOP_TIMER;

public class TimerService extends Service {
    MusicPlayer musicPlayer;

    public static boolean hasTimerFinished = false;

    private static final String TAG = "BCC|TimerService";
    private class IncomingHandler extends Handler {
        CountDownTimer countDownTimer = null;

        {
            musicPlayer = new MusicPlayer(TimerService.this);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_START_TIMER:
                    musicPlayer.play(R.raw.prowo);

                    Log.i(TAG, "Timer started, miliseconds to finish: " + msg.obj);
                    countDownTimer = new CountDownTimer((Long) msg.obj, 500) {
                        @Override
                        public void onTick(long millsUntilFinished) {
                            sendBroadcastToTimerActivity(TimerIntentFilter.ACTION_TIMER_TICK, millsUntilFinished);

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

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "IBinder onBind()");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        musicPlayer.stop();

        return super.onUnbind(intent);
    }

}
