package ru.home.meet_bot.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.home.meet_bot.model.UserData;


@Repository
public interface UserDataMongoRepository extends MongoRepository<UserData,String> {
}
