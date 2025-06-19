package ru.dimaskama.voicemessages.api;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Class to dispatch simple events
 * @param <T> callback type
 */
public class Event<T> {

    private final Function<T[], T> invokerFactory;
    private T invoker;
    private T[] callbacks;

    @SuppressWarnings("unchecked")
    public Event(Class<T> type, Function<T[], T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        callbacks = (T[]) Array.newInstance(type, 0);
        invoker = invokerFactory.apply(callbacks);
    }

    public void register(T callback) {
        callbacks = Arrays.copyOf(callbacks, callbacks.length + 1);
        callbacks[callbacks.length - 1] = callback;
        invoker = invokerFactory.apply(callbacks);
    }

    public T invoker() {
        return invoker;
    }

}
