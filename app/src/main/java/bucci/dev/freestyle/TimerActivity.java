package bucci.dev.freestyle;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TimerActivity extends ActionBarActivity {
    private static final String TAG = "BCC|TimerActivity";

    public static final String TIME_LEFT = "TIME_LEFT";
    public static final String START_PAUSE_STATE = "startpause";
    public static final String SHOW_EXTRA_ROUND_BUTTON = "showExtraRoundButton";

    public static final String SHARED_PREFS = "prefs";
    public static final String SAVED_SONG_PATH = "SAVED_SONG_PATH";

    public static final int REQ_CODE_CHOOSE_SONG = 10;

    public static final int MSG_START_TIMER = 0;
    public static final int MSG_PAUSE_TIMER = 1;
    public static final int MSG_STOP_TIMER = 2;
    public static final int MSG_START_PREPARATION_TIMER = 3;
    public static final int MSG_START_EXTRA_ROUND_TIMER = 4;

    public static final long BATTLE_TIME = 3 * 60 * 1000;
    //    public static final long ROUTINE_TIME = 2 * 60 * 1000;
    public static final long ROUTINE_TIME = 5 * 1000;

    private static final long PRACTICE_TIME = (long) (1.5 * 60 * 1000);

    public static final long PREPARATION_TIME = 5 * 1000;


    private TextView timerTextView;
    private ImageView startPauseButton;
    private TextView musicTextView;

    private SharedPreferences settings;

    private SharedPreferences.Editor editor;

    private long startTime = 0;
    long timeLeft = 0;
    long preparationTimeLeft = 0;

    private TimerType timerType;

    static boolean mBound = false;

    private Messenger mService;

    Intent timerServiceIntent;
    static private int notificationId = 5;

    private static boolean isTimerActive = false;

    NotificationManager mNotificationManager;
    private boolean isExtraButtonShown = false;

//    MusicPlayer musicPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_timer);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        startPauseButton = (ImageView) findViewById(R.id.start_pause_button);
        startPauseButton.setTag("Start");
        timerTextView = (TextView) findViewById(R.id.timer_text);

        Typeface digital_font = Typeface.createFromAsset(getAssets(),
                "fonts/digital_clock_font.ttf");
        timerTextView.setTypeface(digital_font);

        musicTextView = (TextView) findViewById(R.id.music);

        settings = getSharedPreferences(SHARED_PREFS, 0);
        String savedSongPath = settings.getString(SAVED_SONG_PATH, "");

        initTimer();

        if (!savedSongPath.equals("")) {
            if (isSongLongEnough(savedSongPath)) {
                setSongName(savedSongPath);
            } else {
                editor = settings.edit();
                editor.remove(SAVED_SONG_PATH);
                editor.commit();
            }
        }

        if (getIntent().getStringExtra(START_PAUSE_STATE) != null) {
            if (getIntent().getStringExtra(START_PAUSE_STATE).equals("Start")) {
                startPauseButton.setTag("Start");
                startPauseButton.setImageResource(R.drawable.play_button);
            } else {
                startPauseButton.setTag("Pause");
                startPauseButton.setImageResource(R.drawable.pause_button);
            }
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

            }
            if (savedInstanceState != null && savedInstanceState.getString(START_PAUSE_STATE) != null) {
                if (savedInstanceState.getString(START_PAUSE_STATE).equals("Start")) {
                    startPauseButton.setTag("Start");
                    startPauseButton.setImageResource(R.drawable.play_button);
                } else {
                    startPauseButton.setTag("Pause");
                    startPauseButton.setImageResource(R.drawable.pause_button);
                }
//                    startPauseButton.setText(savedInstanceState.getString(START_PAUSE_STATE));
            }
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(SHOW_EXTRA_ROUND_BUTTON)) {
                showExtraRoundButton();
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

        Log.d(TAG, "onStart() hasTimerFinished: " + TimerService.hasTimerFinished);

        if (TimerService.hasTimerFinished) {
            setTimer(0);
//            startPauseButton.setText("Start");
            startPauseButton.setTag("Start");
            startPauseButton.setImageResource(R.drawable.play_button);
            timeLeft = startTime;

            showExtraRoundButton();
        }

    }


    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop timeleft: " + timeLeft);

        if (isTimerActive || timeLeft > 0)
            NotificationCreator.createTimerRunningNotification(getApplicationContext(), (String) startPauseButton.getTag(), timeLeft, timerType, isExtraButtonShown);


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


        ImageView startPauseButton = (ImageView) findViewById(R.id.start_pause_button);

        outState.putLong(TIME_LEFT, timeLeft);
        outState.putString(START_PAUSE_STATE, (String) startPauseButton.getTag());

        if (isExtraButtonShown)
            outState.putBoolean(SHOW_EXTRA_ROUND_BUTTON, true);

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

                startPauseButton.setTag("Start");
                startPauseButton.setImageResource(R.drawable.play_button);

                showExtraRoundButton();


//                startPauseButton.setText("Start");
            } else if (action.equals(TimerIntentFilter.ACTION_PREPARATION_TIMER_TICK)) {
                long l = intent.getLongExtra(TIME_LEFT, 0);
//                timeLeft = l;
                preparationTimeLeft = l;
                setPrepareTimer(l);
//                setTimer(l);

            } else if (action.equals(TimerIntentFilter.ACTION_PREPARATION_TIMER_FINISH)) {
//                sendMessageToService(MSG_START_TIMER);

                if (!isTimerActive)
                    isTimerActive = true;

//                ImageView startPauseButton = (ImageView) findViewById(R.id.start_pause_button);
//                startPauseButton.setTag("Pause");
//                startPauseButton.setImageResource(R.drawable.pause_button);
            }


        }
    };

    private void showExtraRoundButton() {
        ImageView button = (ImageView) findViewById(R.id.extra_round_button);
        if (button.getVisibility() != View.VISIBLE)
            button.setVisibility(View.VISIBLE);

        isExtraButtonShown = true;
    }

    private void hideExtraRoundButton() {
        ImageView button = (ImageView) findViewById(R.id.extra_round_button);
        if (button.getVisibility() != View.GONE)
            button.setVisibility(View.GONE);

        isExtraButtonShown = false;
    }

    private void setPrepareTimer(long time) {
        timerTextView.setText(formatLongToShortTimerText(time));

    }


    private void setTimer(long time) {
        timerTextView.setText(formatLongToTimerText(time));
    }


    private void initTimer() {
        Log.d(TAG, "initTimer()");
        Intent intent = getIntent();

//        timerType = intent.getCharExtra(StartActivity.TIMER_TYPE, 'E');
//        timerType = 'B';


        timerType = (TimerType) intent.getSerializableExtra(StartActivity.TIMER_TYPE);

        addTimerTypeToPrefs(timerType);

        switch (timerType) {
            case BATTLE:
                startTime = BATTLE_TIME;
                break;
            case QUALIFICATION:
                startTime = ROUTINE_TIME;
                break;
            case ROUTINE:
//                startTime = PRACTICE_TIME;
                startTime = 5000;
                break;

        }

        startTime += 100;
    }

    private void addTimerTypeToPrefs(TimerType timerType) {
        Log.d(TAG, "addTimerTypeToPrefs: " + timerType.getValue());
        settings = getSharedPreferences(SHARED_PREFS, 0);
        int timerTypeValue = settings.getInt(StartActivity.TIMER_TYPE, -1);

        if (timerTypeValue != -1 || timerTypeValue != timerType.getValue()) {
            Log.d(TAG, "adding timer type value: " + timerType.getValue());
            editor = settings.edit();
            editor.putInt(StartActivity.TIMER_TYPE, timerType.getValue());
            editor.commit();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_CHOOSE_SONG && resultCode == Activity.RESULT_OK) {
            if ((data != null) && (data.getData() != null)) {
                Uri songUri = data.getData();

                String songPath = getImagePath(songUri);

                if (isSongLongEnough(songPath)) {
                    settings = getSharedPreferences(SHARED_PREFS, 0);

                    editor = settings.edit();
                    editor.putString(SAVED_SONG_PATH, songPath);
                    editor.commit();

                    setSongName(songPath);
                } else {
                    makeChooseLongerSongToast();
                    chooseSong();
                }

            }
        }
    }


    //MediaPlayer.setdatasource with Uri from ACTION_GET_CONTENT is not working
    // it's workaround using real path
    public String getImagePath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Audio.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }


    private void makeChooseLongerSongToast() {

        Toast.makeText(getApplicationContext(), "Choose song longer than " + formatLongToTimerText(startTime), Toast.LENGTH_LONG).show();
    }

    private boolean isSongLongEnough(String songPath) {
        MediaMetadataRetriever songRetriever = new MediaMetadataRetriever();
//        songRetriever.setDataSource(getApplicationContext(), songUri);
        songRetriever.setDataSource(songPath);
        String durationMetadata = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long songDuration = Long.parseLong(durationMetadata);

        switch (timerType) {
            case BATTLE:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + BATTLE_TIME);
                return songDuration >= BATTLE_TIME;
            case QUALIFICATION:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + ROUTINE_TIME);
                return songDuration >= ROUTINE_TIME;
            case ROUTINE:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + PRACTICE_TIME);
                return songDuration >= PRACTICE_TIME;
        }


        return false;

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
        switch (item.getItemId()) {
            case R.id.choose_song:
                if (!isTimerActive) {
                    chooseSong();
                } else {
                    Toast.makeText(getApplicationContext(), "Stop timer to switch a song", Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void onButtonClick(View view) {
        hideExtraRoundButton();

        if (mBound) {
            if (view.getId() == R.id.start_pause_button) {
                ImageView startPauseButton = (ImageView) view;
                Log.i(TAG, "tag: " + view.getTag());
                if (startPauseButton.getTag().equals("Start")) {
                    Log.d(TAG, "Start clicked");
                    Log.i(TAG, "Start time: " + startTime + " timeleft: " + timeLeft);
                    if (timeLeft == 0 || timeLeft == startTime) {
                        Log.d(TAG, "Wlaczam  timer prepare");
                        sendMessageToService(MSG_START_PREPARATION_TIMER);
                    } else
                        sendMessageToService(MSG_START_TIMER);

                    if (!isTimerActive)
                        isTimerActive = true;

                    view.setTag("Pause");
                    ((ImageView) view).setImageResource(R.drawable.pause_button);
                } else {
                    Log.i(TAG, "Pause clicked");

                    sendMessageToService(MSG_PAUSE_TIMER);
                    view.setTag("Start");
                    ((ImageView) view).setImageResource(R.drawable.play_button);
                }
            } else if (view.getId() == R.id.stop_reset_button) {
                Log.d(TAG, "Stop clicked");
                isTimerActive = false;

                if (startPauseButton.getTag().equals("Pause")) {
                    startPauseButton.setTag("Start");
                    startPauseButton.setImageResource(R.drawable.play_button);
                }
                sendMessageToService(MSG_STOP_TIMER);
            } else if (view.getId() == R.id.extra_round_button) {
                Log.d(TAG, "Extra round clicked");

                sendMessageToService(MSG_START_EXTRA_ROUND_TIMER);

                if (!isTimerActive)
                    isTimerActive = true;

                startPauseButton.setTag("Pause");
                startPauseButton.setImageResource(R.drawable.pause_button);
            }

        } else
            Log.w(TAG, "onButtonClick() mBound false");
    }

    private String formatLongToShortTimerText(long time) {
        int seconds = (int) (time / 1000);
        seconds = seconds % 60;
        return String.format("%02d", seconds);
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

    public void chooseSong() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/mpeg");
        startActivityForResult(intent, REQ_CODE_CHOOSE_SONG);
    }

    public void setSongName(String songPath) {
        MediaMetadataRetriever songRetriever = new MediaMetadataRetriever();
        songRetriever.setDataSource(songPath);

        String songTitle = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);


        StringBuffer buf = new StringBuffer();
        buf.append(artist);
        buf.append(" - ");
        buf.append(songTitle);

        if (buf.length() > 32)
            buf.replace(30, 31, "..");

        musicTextView.setText(buf.toString());
    }


}
