package ru.home.meet_bot.botapi.handlers.askdesired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.home.meet_bot.botapi.BotState;
import ru.home.meet_bot.botapi.InputMessageHandler;
import ru.home.meet_bot.cache.UserDataCache;
import ru.home.meet_bot.service.ReplyMessagesService;
import ru.home.meet_bot.service.UserDataService;
import ru.home.meet_bot.utils.StaticMethods;


@Slf4j
@Component
public class AskDesiredHandler implements InputMessageHandler {
    private final UserDataCache userDataCache;
    private final ReplyMessagesService messagesService;
    private final UserDataService userDataService;

    public AskDesiredHandler(UserDataCache userDataCache, ReplyMessagesService messagesService,
                             UserDataService userDataService) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.userDataService = userDataService;
    }

    @Override
    public SendMessage handle(Message message) {
        return processUsersInput(message);
    }

    @Override
    public BotState getHandleName() {
        return BotState.ASK_DESIRED;
    }

    private SendMessage processUsersInput(Message inputMsg) {
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        userDataCache.saveUserData(chatId, userDataCache.getUserData(chatId));
        userDataService.saveUser(userDataCache.getUserData(chatId));

        SendMessage replayToUser = messagesService.getReplayMessage(chatId, "reply.generalQuestion");
        userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
        replayToUser.setReplyMarkup(getInlineMessageButtons());

        return replayToUser;
    }

    private ReplyKeyboardMarkup getInlineMessageButtons() {
        return StaticMethods.getButtonsMarkup("Создать новую встречу",
                "Посмотреть текущие встречи", "Мои встречи", "Подписаться/Отписаться");
    }
}
