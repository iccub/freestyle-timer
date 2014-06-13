package bucci.dev.freestyle;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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

    public static final String DIGITAL_CLOCK_FONT = "fonts/digital_clock_font.ttf";
    public static final String MP3_MIME_TYPE = "audio/mpeg";

    public static final String TIME_LEFT_PARAM = "TIME_LEFT_PARAM";
    public static final String SHOW_EXTRA_ROUND_BUTTON_PARAM = "SHOW_EXTRA_ROUND_BUTTON_PARAM";
    public static final String SAVED_SONG_PATH_PARAM = "SAVED_SONG_PATH_PARAM";
    public static final String START_PAUSE_STATE_PARAM = "START_PAUSE_STATE_PARAM";

    public static final String SHARED_PREFS_PARAM = "prefs";


    public static final int REQ_CODE_CHOOSE_SONG = 10;
    public static final int MSG_START_TIMER = 0;
    public static final int MSG_PAUSE_TIMER = 1;
    public static final int MSG_STOP_TIMER = 2;
    public static final int MSG_START_PREPARATION_TIMER = 3;

    public static final int MSG_START_EXTRA_ROUND_TIMER = 4;
    public static final long BATTLE_DURATION = 3 * 60 * 1000;
    public static final long QUALIFICATION_DURATION = (long) (1.5 * 60 * 1000);
    //raczej nie
    public static final long ROUTINE_DURATION = 5 * 1000;
    public static final long PREPARATION_DURATION = 5 * 1000;
    public static final long EXTRA_TIME_DURATION = 60 * 1000;

    public static final int DELAY_FOR_BEEP = 100;

    public static final String PLAY_BUTTON_START_STATE = "Start";
    public static final String PLAY_BUTTON_PAUSE_STATE = "Pause";

    public static final int TIMER_TYPE_ERROR_VALUE = -1;
    public static final String SONG_PATH_EMPTY_VALUE = "";


    public static final String LONG_TIME_FORMAT = "%d:%02d"; // m:ss time format
    public static final String TIMER_SHORT_FORMAT = "%02d";  // ss time format


    private TextView timerTextView;
    private ImageView playButton;
    private TextView musicTextView;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    private long startTime = 0;
    private long timeLeft = 0;

    private TimerType timerType;

    private static boolean serviceBound = false;
    private boolean isExtraButtonShown = false;
    private static boolean isTimerActive = false;

    private Messenger service;
    private NotificationManager notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_timer);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        settings = getSharedPreferences(SHARED_PREFS_PARAM, MODE_PRIVATE);

        initButtons();
        initTimer();
        setLastUsedSong();
        manageRecreatingActivity(savedInstanceState);

    }

    private void initButtons() {
        playButton = (ImageView) findViewById(R.id.start_pause_button);
        playButton.setTag(PLAY_BUTTON_START_STATE);

        timerTextView = (TextView) findViewById(R.id.timer_text);
        Typeface digital_font = Typeface.createFromAsset(getAssets(), DIGITAL_CLOCK_FONT);
        timerTextView.setTypeface(digital_font);

        musicTextView = (TextView) findViewById(R.id.music);
    }

    private void initTimer() {
        Log.d(TAG, "initTimer()");
        Intent intent = getIntent();

        timerType = (TimerType) intent.getSerializableExtra(StartActivity.TIMER_TYPE);
        addTimerTypeToSharedPrefs(timerType);

        switch (timerType) {
            case BATTLE:
                startTime = BATTLE_DURATION;
                break;
            case QUALIFICATION:
                startTime = QUALIFICATION_DURATION;
                break;
            case ROUTINE:
//                startTime = ROUTINE_DURATION;
                startTime = 5000;
                break;

        }

        //Small delay for airHorn/beep
        startTime += DELAY_FOR_BEEP;
    }

    private void addTimerTypeToSharedPrefs(TimerType timerType) {
        Log.d(TAG, "addTimerTypeToSharedPrefs: " + timerType.getValue());
        int timerTypeValue = settings.getInt(StartActivity.TIMER_TYPE, TIMER_TYPE_ERROR_VALUE);

        if (timerTypeValue != TIMER_TYPE_ERROR_VALUE || timerTypeValue != timerType.getValue()) {
            Log.d(TAG, "adding timer type value: " + timerType.getValue());
            editor = settings.edit();
            editor.putInt(StartActivity.TIMER_TYPE, timerType.getValue());
            editor.commit();
        }

    }

    private void setLastUsedSong() {
        String savedSongPath = settings.getString(SAVED_SONG_PATH_PARAM, SONG_PATH_EMPTY_VALUE);
        if (!savedSongPath.equals(SONG_PATH_EMPTY_VALUE)) {
            if (isSongLongEnough(savedSongPath)) {
                setSongName(savedSongPath);
            } else {
                editor = settings.edit();
                editor.remove(SAVED_SONG_PATH_PARAM);
                editor.commit();
            }
        }
    }

    private boolean isSongLongEnough(String songPath) {
        MediaMetadataRetriever songRetriever = new MediaMetadataRetriever();
        songRetriever.setDataSource(songPath);
        String durationMetadata = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long songDuration = Long.parseLong(durationMetadata);

        switch (timerType) {
            case BATTLE:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + BATTLE_DURATION);
                return songDuration >= BATTLE_DURATION;
            case QUALIFICATION:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + QUALIFICATION_DURATION);
                return songDuration >= QUALIFICATION_DURATION;
            case ROUTINE:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + ROUTINE_DURATION);
                return songDuration >= ROUTINE_DURATION;
        }

        return false;
    }

    public void setSongName(String songPath) {
        MediaMetadataRetriever songRetriever = new MediaMetadataRetriever();
        songRetriever.setDataSource(songPath);

        String songTitle = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);


        StringBuilder buf = new StringBuilder();
        buf.append(artist);
        buf.append(" - ");
        buf.append(songTitle);

        if (buf.length() > 32)
            buf.replace(30, 31, "..");

        musicTextView.setText(buf.toString());
    }

    private void manageRecreatingActivity(Bundle savedInstanceState) {
        if (isTimerResumedFromNotification()) {
            Log.d(TAG, "Timer resumed from notification");
            if (getIntent().getStringExtra(START_PAUSE_STATE_PARAM).equals(PLAY_BUTTON_START_STATE))
                setPlayButtonState(PLAY_BUTTON_START_STATE);
            else
                setPlayButtonState(PLAY_BUTTON_PAUSE_STATE);

            setTimer(getIntent().getLongExtra(TIME_LEFT_PARAM, 0));
            timeLeft = getIntent().getLongExtra(TIME_LEFT_PARAM, 0);
            getIntent().removeExtra(START_PAUSE_STATE_PARAM);
        }

        if (savedInstanceState == null || savedInstanceState.getLong(TIME_LEFT_PARAM) == 0) {
            Log.d(TAG, "Timer set to start time");
            setTimer(startTime);
        } else {
            Log.d(TAG, "Timer set to savedTime");
            long savedTimeLeft = savedInstanceState.getLong(TIME_LEFT_PARAM);
            if (savedTimeLeft > 0) {
                timeLeft = savedTimeLeft;
                setTimer(savedTimeLeft);
            }
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getString(START_PAUSE_STATE_PARAM) != null) {
                if (savedInstanceState.getString(START_PAUSE_STATE_PARAM).equals(PLAY_BUTTON_START_STATE))
                    setPlayButtonState(PLAY_BUTTON_START_STATE);
                else
                    setPlayButtonState(PLAY_BUTTON_PAUSE_STATE);
            }

            if (savedInstanceState.getBoolean(SHOW_EXTRA_ROUND_BUTTON_PARAM))
                showExtraRoundButton();
        }
    }

    private boolean isTimerResumedFromNotification() {
        return getIntent().getStringExtra(START_PAUSE_STATE_PARAM) != null;
    }

    private void setTimer(long time) {
        timerTextView.setText(formatLongToTimerText(time));
    }

    String formatLongToTimerText(long l) {
        int seconds = (int) (l / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format(LONG_TIME_FORMAT, minutes, seconds);
    }

    private void showExtraRoundButton() {
        ImageView extraRoundButton = (ImageView) findViewById(R.id.extra_round_button);
        if (extraRoundButton.getVisibility() != View.VISIBLE)
            extraRoundButton.setVisibility(View.VISIBLE);

        isExtraButtonShown = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent timerServiceIntent = new Intent(this, TimerService.class);

        boolean isServiceBinded = getApplicationContext().bindService(timerServiceIntent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onStart(), isServiceBinded: " + isServiceBinded);

        if (TimerService.hasTimerFinished) {
            setTimer(0);
            playButton.setTag(PLAY_BUTTON_START_STATE);
            playButton.setImageResource(R.drawable.play_button);
            timeLeft = startTime;

            showExtraRoundButton();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop timeLeft: " + timeLeft);

        if (isTimerActive || isTimerResumed())
            NotificationCreator.createTimerRunningNotification(getApplicationContext(), (String) playButton.getTag(), timeLeft, timerType, isExtraButtonShown);

        if (isFinishing()) {
            notificationManager.cancel(NotificationCreator.NOTIFICATION_TIMER_RUNNING);
            if (serviceBound) {
                Log.i(TAG, "onStop() isFinishing, unbinding service..");
                getApplicationContext().unbindService(connection);

                sendMessageToService(MSG_STOP_TIMER);
                serviceBound = false;
            }
        }
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
                Log.i(TAG, "startTime: " + startTime);
                msg.obj = startTime;
                break;
        }

        try {
            service.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessage RemoteException, e: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isTimerResumed() {
        return timeLeft > 0;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMsgReceiver, new TimerIntentFilter());
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NotificationCreator.NOTIFICATION_TIMER_RUNNING);

        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMsgReceiver);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(TIME_LEFT_PARAM, timeLeft);
        outState.putString(START_PAUSE_STATE_PARAM, (String) playButton.getTag());

        if (isExtraButtonShown)
            outState.putBoolean(SHOW_EXTRA_ROUND_BUTTON_PARAM, true);

        Log.i(TAG, "onSaveInstanceState(): " + outState.toString());
        super.onSaveInstanceState(outState);
    }

    private BroadcastReceiver mMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(TimerIntentFilter.ACTION_TIMER_TICK)) {
                long timeLeftFromService = intent.getLongExtra(TIME_LEFT_PARAM, 0);
                timeLeft = timeLeftFromService;
                setTimer(timeLeftFromService);

            } else if (action.equals(TimerIntentFilter.ACTION_TIMER_STOP)) {
                timeLeft = 0;
                setTimer(startTime);

            } else if (action.equals(TimerIntentFilter.ACTION_TIMER_FINISH)) {
                timeLeft = 0;
                playButton.setTag(PLAY_BUTTON_START_STATE);
                playButton.setImageResource(R.drawable.play_button);
                showExtraRoundButton();

            } else if (action.equals(TimerIntentFilter.ACTION_PREPARATION_TIMER_TICK)) {
                long timeLeftFromService = intent.getLongExtra(TIME_LEFT_PARAM, 0);
                setPrepareTimer(timeLeftFromService);

            }

        }
    };

    private void setPrepareTimer(long time) {
        timerTextView.setText(formatLongToShortTimerText(time));
    }

    private String formatLongToShortTimerText(long time) {
        int seconds = (int) (time / 1000);
        seconds = seconds % 60;
        return String.format(TIMER_SHORT_FORMAT, seconds);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_CHOOSE_SONG && resultCode == Activity.RESULT_OK) {
            if ((data != null) && (data.getData() != null)) {
                Uri songUri = data.getData();
                String songPath = Utils.getImagePathFromUri(getApplicationContext(), songUri);

                if (isSongLongEnough(songPath)) {
                    editor = settings.edit();
                    editor.putString(SAVED_SONG_PATH_PARAM, songPath);
                    editor.commit();

                    setSongName(songPath);
                } else {
                    makeChooseLongerSongToast();
                    chooseSong();
                }
            }
        }
    }

    private void makeChooseLongerSongToast() {
        Toast.makeText(getApplicationContext(), getString(R.string.choose_song_longer_text) + formatLongToTimerText(startTime), Toast.LENGTH_LONG).show();
    }

    public void chooseSong() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType(MP3_MIME_TYPE);
        startActivityForResult(intent, REQ_CODE_CHOOSE_SONG);
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
                    Toast.makeText(getApplicationContext(), getString(R.string.stop_timer_to_switch_song_text), Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void onButtonClick(View view) {
        //Whatever button we click, extraRound button should hide
        hideExtraRoundButton();

        if (serviceBound) {
            switch (view.getId()) {
                case R.id.start_pause_button:
                    if (playButton.getTag().equals(PLAY_BUTTON_START_STATE)) {
                        Log.i(TAG, "Start clicked, start time: " + startTime + " timeLeft: " + timeLeft);
                        if (timeLeft == 0 || timeLeft == startTime) {
                            Log.d(TAG, "Starting prepare timer");
                            sendMessageToService(MSG_START_PREPARATION_TIMER);
                        } else
                            sendMessageToService(MSG_START_TIMER);

                        if (!isTimerActive)
                            isTimerActive = true;

                        setPlayButtonState(PLAY_BUTTON_PAUSE_STATE);
                    } else {
                        Log.i(TAG, "Pause clicked");
                        sendMessageToService(MSG_PAUSE_TIMER);
                        setPlayButtonState(PLAY_BUTTON_START_STATE);
                    }
                    break;

                case R.id.stop_reset_button:
                    Log.d(TAG, "Stop clicked");
                    isTimerActive = false;

                    if (playButton.getTag().equals(PLAY_BUTTON_PAUSE_STATE)) {
                        setPlayButtonState(PLAY_BUTTON_START_STATE);
                    }
                    sendMessageToService(MSG_STOP_TIMER);
                    break;

                case R.id.extra_round_button:
                    Log.i(TAG, "Extra round clicked");
                    sendMessageToService(MSG_START_EXTRA_ROUND_TIMER);

                    if (!isTimerActive)
                        isTimerActive = true;

                    setPlayButtonState((PLAY_BUTTON_PAUSE_STATE));
                    break;
            }

        } else
            Log.w(TAG, "onButtonClick() serviceBound false");
    }

    private void setPlayButtonState(String state) {
        playButton.setTag(state);
        if (state.equals(PLAY_BUTTON_START_STATE))
            playButton.setImageResource(R.drawable.play_button);
        else
            playButton.setImageResource(R.drawable.pause_button);
    }

    private void hideExtraRoundButton() {
        ImageView button = (ImageView) findViewById(R.id.extra_round_button);
        if (button.getVisibility() != View.GONE)
            button.setVisibility(View.GONE);

        isExtraButtonShown = false;
    }


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            TimerActivity.this.service = new Messenger(service);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected()");
            service = null;
            serviceBound = false;
        }
    };

}
