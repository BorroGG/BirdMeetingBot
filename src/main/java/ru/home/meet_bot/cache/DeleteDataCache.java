package ru.home.meet_bot.cache;

import org.springframework.stereotype.Component;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.service.MeetDataService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DeleteDataCache {
    private final Map<Long, List<MeetData>> userDeleteMap = new HashMap<>();
    private final MeetDataService meetDataService;

    public DeleteDataCache(MeetDataService meetDataService) {
        this.meetDataService = meetDataService;
    }

    public List<MeetData> getMeets(long chatId) {
        List<MeetData> meetDataList = meetDataService.getMeetsByChatId(chatId);
        if (Objects.isNull(userDeleteMap.get(chatId))) {
            userDeleteMap.put(chatId,meetDataList);
        }
        return meetDataList;
    }

    public void clear(long chatId) {
        userDeleteMap.get(chatId).clear();
    }
}
