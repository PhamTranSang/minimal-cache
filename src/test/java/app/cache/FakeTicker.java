package app.cache;

import java.time.Duration;

final class FakeTicker implements Ticker {

    private long nanos;

    @Override
    public long read() {
        return nanos;
    }

    void advance(final Duration duration) {
        nanos += duration.toNanos();
    }
}
