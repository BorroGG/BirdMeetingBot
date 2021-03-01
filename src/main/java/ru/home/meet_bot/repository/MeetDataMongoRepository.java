package ru.home.meet_bot.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.home.meet_bot.model.MeetData;

import java.util.List;

@Repository
public interface MeetDataMongoRepository extends MongoRepository<MeetData, String> {
    List<MeetData> getAllByChatId(long chatId);
}
