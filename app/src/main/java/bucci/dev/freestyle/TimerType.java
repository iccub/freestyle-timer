package bucci.dev.freestyle;

/**
 * Created by bucci on 10.06.14.
 */
public enum TimerType {
    BATTLE(0),
    QUALIFICATION(1),
    ROUTINE(2);

    private int value;

    TimerType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
