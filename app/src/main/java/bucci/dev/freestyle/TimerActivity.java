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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
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
    private static final boolean DEBUG = false;
    private static final String TAG = "BCC|TimerActivity";

    public static final String DIGITAL_CLOCK_FONT = "fonts/digital_clock_font.ttf";
    public static final String MP3_MIME_TYPE = "audio/mpeg";

    public static final String TIME_LEFT_PARAM = "TIME_LEFT_PARAM";
    public static final String SHOW_EXTRA_ROUND_BUTTON_PARAM = "SHOW_EXTRA_ROUND_BUTTON_PARAM";
    public static final String SAVED_SONG_PATH_PARAM = "SAVED_SONG_PATH_PARAM";
    public static final String START_PAUSE_STATE_PARAM = "START_PAUSE_STATE_PARAM";
    public static final String FIRST_TIME_USED_PARAM = "FIRST_TIME_USED_PARAM";

    public static final String SHARED_PREFS_PARAM = "prefs";


    public static final int REQ_CODE_CHOOSE_SONG = 10;
    public static final int MSG_START_TIMER = 0;
    public static final int MSG_PAUSE_TIMER = 1;
    public static final int MSG_STOP_TIMER = 2;
    public static final int MSG_START_PREPARATION_TIMER = 3;

    public static final int MSG_START_EXTRA_ROUND_TIMER = 4;
    public static final long BATTLE_DURATION = 3 * 60 * 1000;
    public static final long QUALIFICATION_DURATION = (long) (1.5 * 60 * 1000);
    //routine timer varies on song duration so it's not constant
    public static long routine_duration = 0;

    public static final long PREPARATION_DURATION = 10 * 1000;
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

        if (isFirstTimeUsed())
            createFirstTimeUsedDialog();

        initButtons();
        initTimer();
        setLastUsedSong();
        manageRecreatingActivity(savedInstanceState);

    }

    private void createFirstTimeUsedDialog() {
        if (DEBUG) Log.i(TAG, "App first time used, showing welcome screen");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getString(R.string.first_time_dialog_guide) + "\n\n" +
                getString(R.string.first_time_dialog_ideas_bugs_text) + "\n\n" +
                getString(R.string.have_fun_text))
                .setTitle(getString(R.string.first_time_dialog_title))
                .setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editor = settings.edit();
                        editor.putBoolean(FIRST_TIME_USED_PARAM, false);
                        editor.commit();
                    }
                });


        builder.create().show();

    }

    private boolean isFirstTimeUsed() {
        return settings.getBoolean(FIRST_TIME_USED_PARAM, true);
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
        if (DEBUG) Log.d(TAG, "initTimer()");
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
                if (timerTextView.getText().equals(getString(R.string.song_empty)))
                    startTime = 0;
                else {
                    long duration = MediaUtils.getSongDuration(settings.getString(SAVED_SONG_PATH_PARAM, SONG_PATH_EMPTY_VALUE));
                    routine_duration = duration;
                    startTime = duration;
                }

//                startTime = 5000;
                break;

        }

        //Small delay for airHorn/beep
        startTime += DELAY_FOR_BEEP;
    }

    private void addTimerTypeToSharedPrefs(TimerType timerType) {
        if (DEBUG) Log.d(TAG, "addTimerTypeToSharedPrefs: " + timerType.getValue());
        int timerTypeValue = settings.getInt(StartActivity.TIMER_TYPE, TIMER_TYPE_ERROR_VALUE);

        if (timerTypeValue != TIMER_TYPE_ERROR_VALUE || timerTypeValue != timerType.getValue()) {
            if (DEBUG) Log.d(TAG, "adding timer type value: " + timerType.getValue());
            editor = settings.edit();
            editor.putInt(StartActivity.TIMER_TYPE, timerType.getValue());
            editor.commit();
        }

    }

    private void setLastUsedSong() {
        String savedSongPath = settings.getString(SAVED_SONG_PATH_PARAM, SONG_PATH_EMPTY_VALUE);
        if (!savedSongPath.equals(SONG_PATH_EMPTY_VALUE)) {
            if (MediaUtils.isSongLongEnough(savedSongPath, timerType)) {
                musicTextView.setText(MediaUtils.getSongName(savedSongPath));
            } else {
                editor = settings.edit();
                editor.remove(SAVED_SONG_PATH_PARAM);
                editor.commit();
            }
        }
    }

    private void manageRecreatingActivity(Bundle savedInstanceState) {
        if (isTimerResumedFromNotification()) {
            if (DEBUG) Log.d(TAG, "Timer resumed from notification");
            if (getIntent().getStringExtra(START_PAUSE_STATE_PARAM).equals(PLAY_BUTTON_START_STATE))
                setPlayButtonState(PLAY_BUTTON_START_STATE);
            else
                setPlayButtonState(PLAY_BUTTON_PAUSE_STATE);

            setTimer(getIntent().getLongExtra(TIME_LEFT_PARAM, 0));
            timeLeft = getIntent().getLongExtra(TIME_LEFT_PARAM, 0);
            getIntent().removeExtra(START_PAUSE_STATE_PARAM);
        } else {
            if (savedInstanceState == null || savedInstanceState.getLong(TIME_LEFT_PARAM) == 0) {
                if (DEBUG) Log.d(TAG, "Timer set to start time");
                setTimer(startTime);
            } else {
                if (DEBUG) Log.d(TAG, "Timer set to savedTime");
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
        if (DEBUG) Log.d(TAG, "onStart(), isServiceBinded: " + isServiceBinded);

        if (TimerService.hasTimerFinished) {
            setTimer(0);
            playButton.setTag(PLAY_BUTTON_START_STATE);
            playButton.setImageResource(R.drawable.play_button);
            timeLeft = startTime;

            if (timerType == TimerType.BATTLE)
                showExtraRoundButton();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.d(TAG, "onStop timeLeft: " + timeLeft);

        if (isTimerActive || isTimerResumed())
            NotificationCreator.createTimerRunningNotification(getApplicationContext(), (String) playButton.getTag(), timeLeft, timerType, isExtraButtonShown);

        if (isFinishing()) {
            notificationManager.cancel(NotificationCreator.NOTIFICATION_TIMER_RUNNING);
            isTimerActive = false;
            if (serviceBound) {
                if (DEBUG) Log.i(TAG, "onStop() isFinishing, unbinding service..");
                getApplicationContext().unbindService(connection);

                sendMessageToService(MSG_STOP_TIMER);
                serviceBound = false;
            }
        }
    }

    private void sendMessageToService(int messageType) {
        if (DEBUG) Log.d(TAG, "sendMessageToService(" + messageType + ")");
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
                if (DEBUG) Log.i(TAG, "startTime: " + startTime);
                msg.obj = startTime;
                break;
        }

        try {
            service.send(msg);
        } catch (RemoteException e) {
            if (DEBUG) Log.e(TAG, "sendMessage RemoteException, e: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isTimerResumed() {
        return timeLeft > 0;
    }

    @Override
    protected void onResume() {
        if (DEBUG) Log.d(TAG, "onResume()");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMsgReceiver, new TimerIntentFilter());
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NotificationCreator.NOTIFICATION_TIMER_RUNNING);

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.d(TAG, "onPause()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMsgReceiver);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(TIME_LEFT_PARAM, timeLeft);
        outState.putString(START_PAUSE_STATE_PARAM, (String) playButton.getTag());

        if (isExtraButtonShown)
            outState.putBoolean(SHOW_EXTRA_ROUND_BUTTON_PARAM, true);

        if (DEBUG) Log.i(TAG, "onSaveInstanceState(): " + outState.toString());
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

                if (timerType == TimerType.BATTLE)
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

                String songPath = MediaUtils.getPath(getApplicationContext(), songUri);


                if (timerType != TimerType.ROUTINE) {
                    if (MediaUtils.isSongLongEnough(songPath, timerType)) {
                        saveSongPath(songPath);
                    } else {
                        makeChooseLongerSongToast();
                        chooseSong();
                    }
                } else {
                    saveSongPath(songPath);

                    long duration = MediaUtils.getSongDuration(settings.getString(SAVED_SONG_PATH_PARAM, SONG_PATH_EMPTY_VALUE));
                    routine_duration = duration;
                    startTime = duration;
                    timerTextView.setText(formatLongToTimerText(startTime));
                }
            }
        }
    }

    private void saveSongPath(String songPath) {
        editor = settings.edit();
        editor.putString(SAVED_SONG_PATH_PARAM, songPath);
        editor.commit();
        musicTextView.setText(MediaUtils.getSongName(songPath));
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
            case R.id.about_dialog:
                showAboutDialog();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        if (DEBUG) Log.i(TAG, "App first time used, showing welcome screen");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        try {
            builder.setMessage(getString(R.string.app_name) + " " + getVersionName() + "\n" +
                    getString(R.string.about_author) + "\n\n" +
                    getString(R.string.about_contact_info))
                    .setTitle(getString(R.string.about_title))
                    .setPositiveButton(getString(R.string.close_text), null);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        builder.create().show();

    }

    private String getVersionName() throws PackageManager.NameNotFoundException {
        return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    }

    public void onButtonClick(View view) {
        //Whatever button we click, extraRound button should hide\
        if (timerType == TimerType.BATTLE)
            hideExtraRoundButton();

        if (serviceBound) {
            switch (view.getId()) {
                case R.id.start_pause_button:
                    if (playButton.getTag().equals(PLAY_BUTTON_START_STATE)) {
                        if (DEBUG) Log.i(TAG, "Start clicked, start time: " + startTime + " timeLeft: " + timeLeft);
                        if (timeLeft == 0 || timeLeft == startTime) {
                            if (DEBUG) Log.d(TAG, "Starting prepare timer");
                            sendMessageToService(MSG_START_PREPARATION_TIMER);
                        } else
                            sendMessageToService(MSG_START_TIMER);

                        if (!isTimerActive)
                            isTimerActive = true;

                        setPlayButtonState(PLAY_BUTTON_PAUSE_STATE);
                    } else {
                        if (DEBUG) Log.i(TAG, "Pause clicked");
                        sendMessageToService(MSG_PAUSE_TIMER);
                        setPlayButtonState(PLAY_BUTTON_START_STATE);
                    }
                    break;

                case R.id.stop_reset_button:
                    if (DEBUG) Log.d(TAG, "Stop clicked");
                    isTimerActive = false;

                    if (playButton.getTag().equals(PLAY_BUTTON_PAUSE_STATE)) {
                        setPlayButtonState(PLAY_BUTTON_START_STATE);
                    }
                    sendMessageToService(MSG_STOP_TIMER);
                    break;

                case R.id.extra_round_button:
                    if (DEBUG) Log.i(TAG, "Extra round clicked");
                    sendMessageToService(MSG_START_EXTRA_ROUND_TIMER);

                    if (!isTimerActive)
                        isTimerActive = true;

                    setPlayButtonState((PLAY_BUTTON_PAUSE_STATE));
                    break;
            }

        } else
        if (DEBUG) Log.w(TAG, "onButtonClick() serviceBound false");
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
            if (DEBUG) Log.d(TAG, "onServiceConnected()");
            TimerActivity.this.service = new Messenger(service);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (DEBUG) Log.d(TAG, "onServiceDisconnected()");
            service = null;
            serviceBound = false;
        }
    };

}
