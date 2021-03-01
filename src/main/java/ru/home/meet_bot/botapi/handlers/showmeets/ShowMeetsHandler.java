package ru.home.meet_bot.botapi.handlers.showmeets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.home.meet_bot.MeetBot;
import ru.home.meet_bot.botapi.BotState;
import ru.home.meet_bot.botapi.InputMessageHandler;
import ru.home.meet_bot.botapi.handlers.askdesired.AskDesiredHandler;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.cache.UserDataCache;
import ru.home.meet_bot.service.MeetDataService;
import ru.home.meet_bot.service.ReplyMessagesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ShowMeetsHandler implements InputMessageHandler {
    private final UserDataCache userDataCache;
    private final ReplyMessagesService messagesService;
    private final AskDesiredHandler askDesiredHandler;
    private final MeetBot meetBot;
    private final MeetDataService meetDataService;

    public ShowMeetsHandler(UserDataCache userDataCache, ReplyMessagesService messagesService,
                            AskDesiredHandler askDesiredHandler, MeetDataService meetDataService,
                            @Lazy MeetBot meetBot) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.askDesiredHandler = askDesiredHandler;
        this.meetBot = meetBot;
        this.meetDataService = meetDataService;
    }

    @Override
    public SendMessage handle(Message message) {
        return processUsersInput(message);
    }

    @Override
    public BotState getHandleName() {
        return BotState.SHOW_ALL_MEETS;
    }

    private SendMessage processUsersInput(Message inputMsg) {
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        meetBot.sendText(chatId, messagesService.getReplyText("reply.lookMeet"));
        List<MeetData> allMeets = meetDataService.getAllMeets();
        /*allMeets.forEach(i -> meetBot.sendText(chatId, i.getCreator() != null ? i.toStringWithCreator() : i.toStringWithoutCreator()));*/
        if (allMeets.size() == 0) {
            meetBot.sendText(chatId, "Нет встреч");
        } else {
            allMeets.forEach(i -> sendMeetWithMarkup(chatId, i));
        }

        userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);

        return askDesiredHandler.handle(inputMsg);
    }

    private void sendMeetWithMarkup(Long chatId, MeetData meetData) {
        userDataCache.putMeetDataWithMsgId(meetBot.sendTextWithInlineMarkup(chatId, meetData.getCreator() != null ?
                meetData.toStringWithCreator() : meetData.toStringWithoutCreator(), getGoButtonsMarkup(meetData)), meetData);
    }

    private InlineKeyboardMarkup getGoButtonsMarkup(MeetData meetData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton buttonGo = new InlineKeyboardButton().setText("Иду! " + meetData.getGo());
        InlineKeyboardButton buttonNotGo = new InlineKeyboardButton().setText("Не иду " + meetData.getNotGo());
        buttonGo.setCallbackData("buttonGo");
        buttonNotGo.setCallbackData("buttonNotGo");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(buttonGo);
        keyboardButtonsRow1.add(buttonNotGo);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
}
