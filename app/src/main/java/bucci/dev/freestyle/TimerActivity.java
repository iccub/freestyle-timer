package bucci.dev.freestyle;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TimerActivity extends ActionBarActivity {
    private static final String TAG = "BCC|TimerActivity";

    public static final String TIME_LEFT = "TIME_LEFT";
    public static final String START_PAUSE_STATE = "startpause";

    public static final int MSG_START_TIMER = 0;
    public static final int MSG_PAUSE_TIMER = 1;
    public static final int MSG_STOP_TIMER = 2;

    private static final long BATTLE_TIME = 3 * 60 * 1000;
    private static final long ROUTINE_TIME = 2 * 60 * 1000;
    private static final long PRACTICE_TIME = (long) (1.5 * 60 * 1000);

    public static final int NOTIFICATION_TIMER_RUNNING = 5;
    public static final int NOTIFICATION_TIMER_FINISHED = 6;


    private TextView timerTextView;
    private Button startPauseButton;

    private long startTime = 0;
    long timeLeft = 0;
    private char timerType;

    static boolean mBound = false;

    private Messenger mService;

    Intent timerServiceIntent;
    static private int notificationId = 5;

    private boolean isTimerActive = false;

    NotificationManager mNotificationManager;

//    MusicPlayer musicPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_timer);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        startPauseButton = (Button) findViewById(R.id.start_pause_button);
        timerTextView = (TextView) findViewById(R.id.timer_text);
        initTimer();

//        musicPlayer = new MusicPlayer(getApplicationContext());

//        if (savedInstanceState != null)
//            Log.i(TAG, "bundle: TIME_LEFT: " + formatLongToTimerText(savedInstanceState.getLong(TIME_LEFT)) +
//                    "\nstartPauseButton: " + savedInstanceState.getString(START_PAUSE_STATE));

        if (getIntent().getStringExtra(START_PAUSE_STATE) != null) {
            startPauseButton.setText(getIntent().getStringExtra(START_PAUSE_STATE));
            setTimer(getIntent().getLongExtra(TIME_LEFT, 0));
            timeLeft = getIntent().getLongExtra(TIME_LEFT, 0);
            getIntent().removeExtra(START_PAUSE_STATE);
        } else {
            if (savedInstanceState == null || savedInstanceState.getLong(TIME_LEFT) == 0) {
                Log.d(TAG, "Timer set to start time");
                setTimer(startTime);
            } else {
                long savedTimeLeft = savedInstanceState.getLong(TIME_LEFT);
                if (savedTimeLeft > 0) {
                    timeLeft = savedTimeLeft;
                    setTimer(savedTimeLeft);
                }
                if (savedInstanceState.getString(START_PAUSE_STATE) != null) {
                    startPauseButton.setText(savedInstanceState.getString(START_PAUSE_STATE));
                }
            }
        }


        //Poprawki po refaktoryzacji register teraz w onResume()
//        LocalBroadcastManager.getInstance(this).registerReceiver(mMsgReceiver, new TimerIntentFilter());

    }


    @Override
    protected void onStart() {
        super.onStart();

        timerServiceIntent = new Intent(this, TimerService.class);
        boolean isServiceBinded = getApplicationContext().bindService(timerServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "bindService(): " + isServiceBinded);

        if (TimerService.hasTimerFinished) {
            setTimer(0);
            startPauseButton.setText("Start");
            timeLeft = startTime;
        }

    }


    @Override
    protected void onStop() {
        super.onStop();

        if (isTimerActive || timeLeft > 0)
            //createNotification();
            NotificationCreator.createTimerRunningNotification(getApplicationContext(), (String) startPauseButton.getText(), timeLeft, timerType);

        if (isFinishing()) {
            mNotificationManager.cancel(notificationId);
            if (mBound) {
                Log.i(TAG, "unbindService()");
                getApplicationContext().unbindService(mConnection);

                //Pauzowanie to tak naprawde jego anulowanie
                // aby wznowic timer trzeba go utworzyc od nowa z odliczonym czasem
                // wiec pauza bez wznowienia = anulowanie
                sendMessageToService(MSG_STOP_TIMER);

                mBound = false;
            }
        }

        //Poprawka po refaktoryzacji, unregister w onPause()
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMsgReceiver);
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMsgReceiver, new TimerIntentFilter());
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);


        Log.i(TAG, "onResume()");

        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMsgReceiver);

//        if (isTimerActive || timeLeft > 0)
//            createNotification();

        super.onPause();
    }


    private void sendMessageToService(int messageType) {
        Log.d(TAG, "sendMessageToService(" + messageType + ")");
        Message msg = Message.obtain();
        msg.what = messageType;
        switch (messageType) {
            case MSG_START_TIMER:
                if (isTimerResumed())
                    msg.obj = timeLeft;
                else
                    msg.obj = startTime;
                break;
            case MSG_STOP_TIMER:
                Log.i(TAG, "STARTTIME: " + startTime);
                msg.obj = startTime;
                break;

            case 3:
                msg.obj = startTime;
                break;
        }

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException, e: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isTimerResumed() {
        return timeLeft > 0;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {

        Log.i(TAG, "savedInstance()");


        Button startPauseButton = (Button) findViewById(R.id.start_pause_button);

//        if (startPauseButton.getText().equals("Start")) {
//            Log.i(TAG, "Wchodze");
        outState.putLong(TIME_LEFT, timeLeft);
//        }
        outState.putString(START_PAUSE_STATE, (String) startPauseButton.getText());

        super.onSaveInstanceState(outState);
    }

    private BroadcastReceiver mMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(TimerIntentFilter.ACTION_TIMER_TICK)) {
                long l = intent.getLongExtra(TIME_LEFT, 0);
                timeLeft = l;
                setTimer(l);

            } else if (action.equals(TimerIntentFilter.ACTION_TIMER_STOP)) {
                timeLeft = 0;
                setTimer(startTime);
            } else if (action.equals(TimerIntentFilter.ACTION_TIMER_FINISH)) {
                timeLeft = 0;
                startPauseButton.setText("Start");
            }


        }
    };

    private void setTimer(long time) {
        timerTextView.setText(formatLongToTimerText(time));
    }


    private void initTimer() {
        Intent intent = getIntent();

        timerType = intent.getCharExtra(StartActivity.TIMER_TYPE, 'E');

        switch (timerType) {
            case StartActivity.TYPE_BATTLE:
                startTime = BATTLE_TIME;
                break;
            case StartActivity.TYPE_ROUTINE:
                startTime = ROUTINE_TIME;
                break;
            case StartActivity.TYPE_PRACTICE:
//                startTime = PRACTICE_TIME;
                startTime = 5000;
                break;
            case 'E':
                Log.e(TAG, "Timer type error");

        }

        startTime += 900;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.timer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void onButtonClick(View view) {
        if (mBound) {
            if (view.getId() == R.id.start_pause_button) {
                Button startPauseButton = (Button) view;
                if (startPauseButton.getText().equals("Start")) {

//                    musicPlayer.play(R.raw.short_sound_1);

                    Log.i(TAG, "Start clicked");
                    sendMessageToService(MSG_START_TIMER);

                    if (!isTimerActive)
                        isTimerActive = true;

                    ((Button) view).setText("Pause");
                } else {
                    Log.i(TAG, "Pause clicked");

//                    musicPlayer.pause();

                    sendMessageToService(MSG_PAUSE_TIMER);
                    ((Button) view).setText("Start");
                }
            } else {
                Log.d(TAG, "Stop clicked");

//                musicPlayer.stop();


                if (startPauseButton.getText().equals("Pause"))
                    startPauseButton.setText("Start");
                sendMessageToService(MSG_STOP_TIMER);
            }

        } else
            Log.w(TAG, "onButtonClick() mBound false");
    }

    private void createNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Timer is running")
//                        .setContentText("Hello World!")
                        .setAutoCancel(true)
                        .setOngoing(true);

        Intent resultIntent = new Intent(this, TimerActivity.class);
        resultIntent.putExtra(StartActivity.TIMER_TYPE, timerType);
        resultIntent.putExtra(START_PAUSE_STATE, (String) startPauseButton.getText());
        resultIntent.putExtra(TIME_LEFT, timeLeft);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(TimerActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationId, mBuilder.build());


    }


    String formatLongToTimerText(long l) {
        int seconds = (int) (l / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            mService = new Messenger(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected()");
            mService = null;
            mBound = false;
        }
    };

}
