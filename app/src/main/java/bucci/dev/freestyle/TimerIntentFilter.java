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
