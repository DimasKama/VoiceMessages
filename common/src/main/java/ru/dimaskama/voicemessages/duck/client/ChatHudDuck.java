package ru.dimaskama.voicemessages.duck.client;

import ru.dimaskama.voicemessages.client.screen.widget.PlaybackPlayerWidget;

import java.util.List;

public interface ChatHudDuck {

    List<PlaybackPlayerWidget> voicemessages_getVisiblePlaybackPlayerWidgets();

}
