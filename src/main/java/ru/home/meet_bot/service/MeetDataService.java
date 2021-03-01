package ru.home.meet_bot.service;

import org.springframework.stereotype.Service;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.repository.MeetDataMongoRepository;

import java.util.List;

@Service
public class MeetDataService {

    private final MeetDataMongoRepository meetDataMongoRepository;

    public MeetDataService(MeetDataMongoRepository meetDataMongoRepository) {
        this.meetDataMongoRepository = meetDataMongoRepository;
    }

    public List<MeetData> getAllMeets() {
        return meetDataMongoRepository.findAll();
    }

    public void saveMeet(MeetData meetData) {
        meetDataMongoRepository.save(meetData);
    }

    public void deleteMeetById(String meetId) {
        meetDataMongoRepository.deleteById(meetId);
    }

    public List<MeetData> getMeetsByChatId(long chatId) {
        return meetDataMongoRepository.getAllByChatId(chatId);
    }

}
