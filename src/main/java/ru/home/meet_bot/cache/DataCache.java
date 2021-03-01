package ru.home.meet_bot.cache;

import ru.home.meet_bot.botapi.BotState;
import ru.home.meet_bot.model.MeetData;

public interface DataCache {

    void setUsersCurrentBotState(int userId, BotState botState);

    BotState getUsersCurrentBotState(int userId);

    MeetData getUserProfileData(int userId);

    void saveUserProfileData(int userId, MeetData meetData);

    void clear();
}
