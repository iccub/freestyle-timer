package bucci.dev.freestyle;

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
