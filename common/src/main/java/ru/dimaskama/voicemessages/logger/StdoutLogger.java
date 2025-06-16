package ru.dimaskama.voicemessages.logger;

public class StdoutLogger implements AbstractLogger {

    @Override
    public void info(String message) {
        System.out.println(message);
    }

    @Override
    public void info(String message, Exception e) {
        System.out.println(message);
        e.printStackTrace(System.out);
    }

    @Override
    public void warn(String message) {
        System.out.println(message);
    }

    @Override
    public void warn(String message, Exception e) {
        System.out.println(message);
        e.printStackTrace(System.out);
    }

    @Override
    public void error(String message) {
        System.err.println(message);
    }

    @Override
    public void error(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace(System.err);
    }

}
