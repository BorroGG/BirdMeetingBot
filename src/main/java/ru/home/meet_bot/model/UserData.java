package ru.home.meet_bot.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "Users")
public class UserData implements Serializable {
    @Id
    long chatId;
    boolean isSubscribed;
    List<MeetData> meetDataList;
}
