package bucci.dev.freestyle;

import android.content.IntentFilter;

public class TimerIntentFilter extends IntentFilter {
    public static final String ACTION_TIMER_TICK = "timerTick";
    public static final String ACTION_TIMER_STOP = "timerStop";
    public static final String ACTION_TIMER_FINISH = "timerFinish";

    TimerIntentFilter() {
        addAction(ACTION_TIMER_TICK);
        addAction(ACTION_TIMER_STOP);
        addAction(ACTION_TIMER_FINISH);
    }
}
