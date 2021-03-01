package ru.home.meet_bot.botapi.handlers.fillingmeet;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.home.meet_bot.MeetBot;
import ru.home.meet_bot.botapi.BotState;
import ru.home.meet_bot.botapi.InputMessageHandler;
import ru.home.meet_bot.botapi.handlers.askdesired.AskDesiredHandler;
import ru.home.meet_bot.cache.UserDataCache;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.model.UserData;
import ru.home.meet_bot.service.MeetDataService;
import ru.home.meet_bot.service.ReplyMessagesService;
import ru.home.meet_bot.service.UserDataService;
import ru.home.meet_bot.utils.Calendar;
import ru.home.meet_bot.utils.StaticMethods;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class FillingMeetHandler implements InputMessageHandler {
    private final UserDataCache userDataCache;
    private final ReplyMessagesService messagesService;
    private final AskDesiredHandler askDesiredHandler;
    private final MeetBot meetBot;
    private final MeetDataService meetDataService;
    private final UserDataService userDataService;

    public FillingMeetHandler(UserDataCache userDataCache, ReplyMessagesService messagesService,
                              AskDesiredHandler askDesiredHandler, MeetDataService meetDataService,
                              UserDataService userDataService, @Lazy MeetBot meetBot) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.askDesiredHandler = askDesiredHandler;
        this.meetBot = meetBot;
        this.meetDataService = meetDataService;
        this.userDataService = userDataService;
    }

    @Override
    public SendMessage handle(Message message) {
        if (userDataCache.getUsersCurrentBotState(message.getFrom().getId()).equals(BotState.FILLING_MEET)) {
            userDataCache.setUsersCurrentBotState(message.getFrom().getId(), BotState.GET_DESCRIPTION);
        }
        return processUsersInput(message);
    }

    @Override
    public BotState getHandleName() {
        return BotState.FILLING_MEET;
    }

    private SendMessage processUsersInput(Message inputMsg) {
        String usersAnswer = inputMsg.getText();
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        MeetData meetData = userDataCache.getUserProfileData(userId);
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        SendMessage replyToUser = null;

        if (botState.equals(BotState.GET_DESCRIPTION)) {
            replyToUser = messagesService.getReplayMessage(chatId, "reply.description");
            userDataCache.setUsersCurrentBotState(userId, BotState.CHOOSE_DATE);
        }

         if (botState.equals(BotState.CHOOSE_DATE)) {
            meetData.setDescription(usersAnswer);
            replyToUser = messagesService.getReplayMessage(chatId, "reply.setDate");
            replyToUser.setReplyMarkup(getDateButtonsMarkup());
            userDataCache.setUsersCurrentBotState(userId, BotState.CHOOSE_TIME);
        }

        if (botState.equals(BotState.CHOOSE_TIME)) {
            replyToUser = messagesService.getReplayMessage(chatId, "reply.setDate");
            replyToUser.setReplyMarkup(getDateButtonsMarkup());
            userDataCache.setUsersCurrentBotState(userId, BotState.CHOOSE_DATE);
        }

        if (botState.equals(BotState.CHOOSE_PLACE)) {
            meetData.setTime(usersAnswer);
            replyToUser = messagesService.getReplayMessage(chatId, "reply.setPlace");
            userDataCache.setUsersCurrentBotState(userId, BotState.ADD_LINK);
        }

       if (botState.equals(BotState.ADD_LINK)) {
            if (Objects.isNull(meetData.getPlace())) {
                meetData.setPlace(usersAnswer);
            }

            replyToUser = createMessageWithKeyboard(chatId, messagesService.getReplyText("reply.addLink"), getLinkButtonsMarkup());
            userDataCache.setUsersCurrentBotState(userId, BotState.SHOW_CREATED_MEET);
        }

        if (botState.equals(BotState.SHOW_CREATED_MEET)) {
            if (usersAnswer.equals("Да")) {

                meetData.setCreator("@" + inputMsg.getFrom().getUserName());
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_CHANGE_MEET);
                meetBot.sendText(chatId, messagesService.getReplyText("reply.showCreatedMeet"));

                replyToUser = createMessageWithKeyboard(chatId, meetData.toStringWithCreator(), getCreatedMeetButtonsMarkup());
            } else if (usersAnswer.equals("Нет")) {

                meetData.setCreator(null);
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_CHANGE_MEET);
                meetBot.sendText(chatId, messagesService.getReplyText("reply.showCreatedMeet"));

                replyToUser = createMessageWithKeyboard(chatId, meetData.toStringWithoutCreator(), getCreatedMeetButtonsMarkup());
            } else {
                replyToUser = messagesService.getReplayMessage(chatId, "reply.addLink");
                userDataCache.setUsersCurrentBotState(userId, BotState.ADD_LINK);
                meetBot.sendText(chatId, messagesService.getReplyText("reply.incorrectValue"));
                handle(inputMsg);
            }
        }

         if (botState.equals(BotState.ASK_CHANGE_MEET)) {
            if (usersAnswer.equals("Подтвердить встречу")) {
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
                meetData.setChatId(chatId);
                meetData.setGo(0);
                meetData.setNotGo(0);

                meetDataService.saveMeet(meetData);
                List<UserData> userDataList = userDataService.getAllUsers();
                userDataList.stream()
                        .filter(UserData::isSubscribed)
                        .filter(i -> i.getChatId() != chatId)
                        .forEach(i -> meetBot.sendText(i.getChatId(), getTextMessage(meetData)));

                userDataCache.clear();
                replyToUser = askDesiredHandler.handle(inputMsg);
            } else if (usersAnswer.equals("Изменить встречу")) {
                userDataCache.clear();
                replyToUser = messagesService.getReplayMessage(chatId, "reply.description");
                userDataCache.setUsersCurrentBotState(userId, BotState.CHOOSE_DATE);
            } else if (usersAnswer.equals("Отменить встречу")) {
                userDataCache.clear();
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
                replyToUser = askDesiredHandler.handle(inputMsg);
            } else {
                replyToUser = messagesService.getReplayMessage(chatId, "reply.incorrectValue");
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_CHANGE_MEET);
            }
        } else {
            userDataCache.saveUserProfileData(userId, meetData);
        }

        return replyToUser;
    }

    private String getTextMessage(MeetData meetData) {
        return meetData.getCreator() != null ?
                meetData.toStringWitCreatorWithNewMessage() : meetData.toStringWithoutCreatorWithNewMessage();
    }

    private InlineKeyboardMarkup getDateButtonsMarkup() {
        Calendar calendar = new Calendar(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        return calendar.getCalendar();
    }

    private SendMessage createMessageWithKeyboard(long chatId, String textMessage, ReplyKeyboardMarkup replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.setText(textMessage);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        return sendMessage;

    }

    private ReplyKeyboardMarkup getCreatedMeetButtonsMarkup() {
        return StaticMethods.getButtonsMarkup("Подтвердить встречу", "Изменить встречу", "Отменить встречу");
    }

    private ReplyKeyboardMarkup getLinkButtonsMarkup() {
        return StaticMethods.getButtonsMarkup("Да", "Нет");
    }
}
