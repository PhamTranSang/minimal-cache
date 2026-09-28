package app.cache;

@FunctionalInterface
public interface Ticker {

    long read();

    static Ticker systemTicker() {
        return System::nanoTime;
    }
}
