package ru.dimaskama.voicemessages.logger;

public interface AbstractLogger {

    void info(String message);

    void info(String message, Exception e);

    void warn(String message);

    void warn(String message, Exception e);

    void error(String message);

    void error(String message, Exception e);

}
