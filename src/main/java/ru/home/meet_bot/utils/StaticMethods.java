package ru.home.meet_bot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.LinkedList;
import java.util.List;

public class StaticMethods {

    public static ReplyKeyboardMarkup getButtonsMarkup(String... strings) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRowList = getKeyboardRows(strings);

        replyKeyboardMarkup.setKeyboard(keyboardRowList);

        return replyKeyboardMarkup;
    }

    private static List<KeyboardRow> getKeyboardRows(String... strings) {
        List<KeyboardRow> keyboardRowList = new LinkedList<>();
        int i = 0;
        while (i < strings.length) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(strings[i]));
            i++;
            if (i < strings.length) {
                row.add(new KeyboardButton(strings[i]));
            }
            keyboardRowList.add(row);
            i++;
        }
        return keyboardRowList;
    }

    private static ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        return new ReplyKeyboardMarkup().setResizeKeyboard(true).setOneTimeKeyboard(true);
    }
}
