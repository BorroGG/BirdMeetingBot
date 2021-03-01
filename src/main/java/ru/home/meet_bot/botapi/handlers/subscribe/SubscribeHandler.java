package ru.home.meet_bot.botapi.handlers.subscribe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.home.meet_bot.MeetBot;
import ru.home.meet_bot.botapi.BotState;
import ru.home.meet_bot.botapi.InputMessageHandler;
import ru.home.meet_bot.botapi.handlers.askdesired.AskDesiredHandler;
import ru.home.meet_bot.cache.UserDataCache;
import ru.home.meet_bot.model.UserData;
import ru.home.meet_bot.service.ReplyMessagesService;
import ru.home.meet_bot.service.UserDataService;

@Slf4j
@Component
public class SubscribeHandler implements InputMessageHandler {
    private final UserDataCache userDataCache;
    private final ReplyMessagesService messagesService;
    private final AskDesiredHandler askDesiredHandler;
    private final MeetBot meetBot;
    private final UserDataService userDataService;

    public SubscribeHandler(UserDataCache userDataCache, ReplyMessagesService messagesService,
                            AskDesiredHandler askDesiredHandler,@Lazy MeetBot meetBot,
                            UserDataService userDataService) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.askDesiredHandler = askDesiredHandler;
        this.meetBot = meetBot;
        this.userDataService = userDataService;
    }

    @Override
    public SendMessage handle(Message inputMsg) {
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        UserData userData = userDataCache.getUserData(chatId);
        userData.setSubscribed(!userData.isSubscribed());
        if (userData.isSubscribed()) {
            meetBot.sendText(chatId, messagesService.getReplyText("reply.subscribeOn"));
        } else {
            meetBot.sendText(chatId, messagesService.getReplyText("reply.subscribeOff"));
        }
        userDataService.saveUser(userData);
        userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
        return askDesiredHandler.handle(inputMsg);
    }

    @Override
    public BotState getHandleName() {
        return BotState.SUBSCRIBE;
    }
}
