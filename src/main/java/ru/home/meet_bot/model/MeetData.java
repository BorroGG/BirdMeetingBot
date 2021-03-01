package ru.home.meet_bot.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "MeetData")
public class MeetData implements Serializable {
    @Id
    String id;
    String creator;
    String description;
    LocalDate date;
    String time;
    String place;
    long chatId;
    int go;
    int notGo;
    Map<Long, Boolean> isVotedGo;
    Map<Long, Boolean> isVotedNotGo;

    public MeetData() {
        this.isVotedGo = new HashMap<>();
        this.isVotedNotGo = new HashMap<>();
    }

    public void setIsVotedGoBool(long chatId, boolean bool) {
        isVotedGo.put(chatId, bool);
    }

    public void setIsVotedNotGoBool(long chatId, boolean bool) {
        isVotedNotGo.put(chatId, bool);
    }

    public String toStringWithCreator() {
        return "Описание встречи: " + description + '\n' +
                "Дата и время встречи: " + date + " " + time + '\n' +
                "Место встречи: " + place + '\n' +
                "Создатель встречи: " + creator + '\n';
    }

    public String toStringWithoutCreator() {
        return "Описание встречи: " + description + '\n' +
                "Дата и время встречи: " + date + " " + time + '\n' +
                "Место встречи: " + place + '\n';
    }

    public String toStringWitCreatorWithNewMessage() {
        return "Появилась новая встреча! " + '\n' +
                "Описание встречи: " + description + '\n' +
                "Дата и время встречи: " + date + " " + time +'\n' +
                "Место встречи: " + place + '\n'+
                "Создатель встречи: " + creator + '\n';
    }

    public String toStringWithoutCreatorWithNewMessage() {
        return "Появилась новая встреча! " + '\n' +
                "Описание встречи: " + description + '\n' +
                "Дата и время встречи: " + date + " " + time + '\n' +
                "Место встречи: " + place + '\n';
    }

}
