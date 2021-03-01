package ru.home.meet_bot.cache;

import org.springframework.stereotype.Component;
import ru.home.meet_bot.botapi.BotState;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.model.UserData;
import ru.home.meet_bot.service.UserDataService;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserDataCache implements DataCache {
    private final Map<Integer, BotState> usersBotStates = new HashMap<>();
    private final Map<Integer, MeetData> usersProfileData = new HashMap<>();
    private final Map<Long,UserData> userDataMap = new HashMap<>();
    private final Map<Integer, MeetData> msgIdMeetDataMap = new HashMap<>();
    private final UserDataService userDataService;

    public UserDataCache(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    public MeetData getMeetDataByMsgId(int msgId) {
        return msgIdMeetDataMap.get(msgId);
    }

    public void putMeetDataWithMsgId(int msgId, MeetData meetData) {
        msgIdMeetDataMap.put(msgId, meetData);
    }

    public UserData getUserData(long chatId) {
        UserData userData = userDataMap.get(chatId);
        if(userData == null) {
            userData = new UserData();
            userData.setChatId(chatId);
            userData.setSubscribed(false);
            saveUserData(chatId, userData);
            userDataService.saveUser(userData);
        }
        return userData;
    }

    public void saveUserData(long chatId, UserData userData) {
        userDataMap.put(chatId, userData);
    }

    @Override
    public void setUsersCurrentBotState(int userId, BotState botState) {
        usersBotStates.put(userId, botState);
    }

    @Override
    public BotState getUsersCurrentBotState(int userId) {
        BotState botState = usersBotStates.get(userId);
        if(botState == null) {
            botState = BotState.ASK_DESIRED;
        }
        return botState;
    }

    @Override
    public MeetData getUserProfileData(int userId) {
        MeetData meetData = usersProfileData.get(userId);
        if (meetData == null) {
            meetData = new MeetData();
        }
        return meetData;
    }

    @Override
    public void saveUserProfileData(int userId, MeetData meetData) {
        usersProfileData.put(userId, meetData);
    }

    @Override
    public void clear() {
        usersProfileData.clear();
    }
}
