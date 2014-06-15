package bucci.dev.freestyle;

import android.content.IntentFilter;

class TimerIntentFilter extends IntentFilter {
    public static final String ACTION_TIMER_TICK = "timerTick";
    public static final String ACTION_TIMER_STOP = "timerStop";
    public static final String ACTION_TIMER_FINISH = "timerFinish";
    public static final String ACTION_PREPARATION_TIMER_TICK = "prepTimerTick";


    TimerIntentFilter() {
        addAction(ACTION_TIMER_TICK);
        addAction(ACTION_TIMER_STOP);
        addAction(ACTION_TIMER_FINISH);
        addAction(ACTION_PREPARATION_TIMER_TICK);
    }
}
