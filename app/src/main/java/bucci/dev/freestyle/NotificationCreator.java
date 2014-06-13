package bucci.dev.freestyle;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class NotificationCreator {
    public static final int NOTIFICATION_TIMER_RUNNING = 5;

    public static void createTimerRunningNotification(Context context, String startPauseButtonState, long timeLeft, TimerType timerType, boolean extraButtonVisibleState) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.bft_icon)
                        .setContentTitle(context.getString(R.string.notification_timer_running_text))
                        .setAutoCancel(true)
                        .setOngoing(true);

        Intent resultIntent = new Intent(context, TimerActivity.class);
        resultIntent.putExtra(StartActivity.TIMER_TYPE, timerType);
        resultIntent.putExtra(TimerActivity.START_PAUSE_STATE_PARAM, startPauseButtonState);
        resultIntent.putExtra(TimerActivity.TIME_LEFT_PARAM, timeLeft);
        if (extraButtonVisibleState)
            resultIntent.putExtra(TimerActivity.SHOW_EXTRA_ROUND_BUTTON_PARAM, true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(TimerActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_TIMER_RUNNING, builder.build());
    }

}
