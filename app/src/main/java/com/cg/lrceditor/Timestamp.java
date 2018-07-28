package com.cg.lrceditor;

import java.util.concurrent.TimeUnit;

// UNUSED CLASS

public class Timestamp {

    private int minutes, seconds, milliseconds;

    public Timestamp(String time) {
        time = time.trim();
        if (time.matches("^(\\d\\d:\\d\\d[.|:]\\d\\d)")) {
            this.minutes = Integer.parseInt(time.substring(0, 2));
            this.seconds = Integer.parseInt(time.substring(3, 5));
            this.milliseconds = Integer.parseInt(time.substring(6, 8)) * 10;
        } else {
            throw new IllegalArgumentException("Bad constructor format for the timestamp");
        }
    }

    public Timestamp(int minutes, int seconds, int milliseconds) {
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds * 10;
    }

    public Timestamp(int milliseconds) {
        this.minutes = (int) TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        this.seconds = (int) (TimeUnit.MINUTES.toSeconds(this.minutes) - TimeUnit.MILLISECONDS.toSeconds(milliseconds));
        this.milliseconds = (int) (TimeUnit.MINUTES.toMillis(this.minutes) +
                                   TimeUnit.SECONDS.toMillis(this.seconds) -
                                   milliseconds);
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getMilliseconds() {
        return milliseconds / 10;
    }

    public void setMilliseconds(int milliseconds) {
        this.milliseconds = milliseconds * 10;
    }

    public void incrementBy100milli() {
        milliseconds = milliseconds + 100;
        if (milliseconds >= 1000) {
            seconds++;
            if (seconds >= 60) {
                minutes++;
                seconds = 0;
            }
            milliseconds = (((milliseconds / 10) % 10) * 10);
        }
    }

    public void decrementBy100milli() {
        milliseconds = milliseconds - 100;
        if (milliseconds < 0) {
            seconds--;
            milliseconds = 1000 + milliseconds;
            if (seconds < 0) {
                minutes--;
                seconds = 59;
                if (minutes < 0) {
                    minutes = seconds = milliseconds = 0;
                }
                seconds = 0;
            }
        }
    }

    public void incrementBy10Sec() {
        seconds += 10;
        if (seconds > 60) {
            minutes++;
            seconds %= 60;
        }
    }

    public void decrementBy10Sec() {
        seconds += 10;
        if (seconds > 60) {
            minutes++;
            seconds %= 60;
        }
    }

    public int timeToMilliseconds() {
        return (int) (TimeUnit.MINUTES.toMillis(this.minutes) + TimeUnit.SECONDS.toMillis(this.seconds) + this.milliseconds);
    }

    @Override
    public String toString() {
        return Integer.toString(getMinutes()) + ":" +
                Integer.toString(getSeconds()) + "." +
                Integer.toString(getMilliseconds());
    }
}
