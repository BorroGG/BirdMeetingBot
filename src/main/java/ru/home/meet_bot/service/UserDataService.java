package ru.home.meet_bot.service;

import org.springframework.stereotype.Service;
import ru.home.meet_bot.model.UserData;
import ru.home.meet_bot.repository.UserDataMongoRepository;

import java.util.List;

@Service
public class UserDataService {
    private final UserDataMongoRepository userDataMongoRepository;

    public UserDataService(UserDataMongoRepository userDataMongoRepository) {
        this.userDataMongoRepository = userDataMongoRepository;
    }

    public void saveUser(UserData userData) {
        userDataMongoRepository.save(userData);
    }

    public List<UserData> getAllUsers() {
        return userDataMongoRepository.findAll();
    }
}
