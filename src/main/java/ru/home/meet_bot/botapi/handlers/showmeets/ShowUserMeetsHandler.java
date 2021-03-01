package ru.home.meet_bot.botapi.handlers.showmeets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.home.meet_bot.MeetBot;
import ru.home.meet_bot.botapi.BotState;
import ru.home.meet_bot.botapi.InputMessageHandler;
import ru.home.meet_bot.botapi.handlers.askdesired.AskDesiredHandler;
import ru.home.meet_bot.cache.DeleteDataCache;
import ru.home.meet_bot.cache.UserDataCache;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.model.UserData;
import ru.home.meet_bot.service.MeetDataService;
import ru.home.meet_bot.service.ReplyMessagesService;
import ru.home.meet_bot.service.UserDataService;
import ru.home.meet_bot.utils.StaticMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ShowUserMeetsHandler implements InputMessageHandler {
    private final UserDataCache userDataCache;
    private final ReplyMessagesService messagesService;
    private final AskDesiredHandler askDesiredHandler;
    private final MeetBot meetBot;
    private final MeetDataService meetDataService;
    private final DeleteDataCache deleteDataCache;
    private final UserDataService userDataService;
    private MeetData meetDataToDelete;


    public ShowUserMeetsHandler(UserDataCache userDataCache, ReplyMessagesService messagesService,
                                AskDesiredHandler askDesiredHandler, @Lazy MeetBot meetBot,
                                MeetDataService meetDataService, DeleteDataCache deleteDataCache,
                                UserDataService userDataService) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.askDesiredHandler = askDesiredHandler;
        this.meetBot = meetBot;
        this.meetDataService = meetDataService;
        this.deleteDataCache = deleteDataCache;
        this.userDataService = userDataService;
    }

    @Override
    public SendMessage handle(Message inputMsg) {
        String message = inputMsg.getText();
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        BotState botState = userDataCache.getUsersCurrentBotState(userId);

       // log.info("message = " + message);

        if (botState.equals(BotState.SHOW_USER_MEETS) || inputMsg.hasReplyMarkup()) {
            int id = 1;
            meetBot.sendText(chatId, messagesService.getReplyText("reply.showUserMeets"));
            List<MeetData> userMeets = deleteDataCache.getMeets(chatId);
            if (userMeets.size() == 0) {
                meetBot.sendText(chatId, "Нет встреч");
            } else {
                for (MeetData meet : userMeets) {
                    //sendMeet(chatId, meet, id++);
                    sendMeetWithMarkup(chatId, meet, id++);
                }
            }
            userDataCache.setUsersCurrentBotState(userId, BotState.CHOOSE_ACT);
            if (id == 1) {
                return createMessageWithKeyboard(chatId, messagesService.getReplyText("reply.generalQuestion"), getMyMeetsDeleteMarkup());
            } else {
                return createMessageWithKeyboard(chatId, messagesService.getReplyText("reply.generalQuestion"), getMyMeetsMarkup());
            }
        } else if (botState.equals(BotState.CHOOSE_ACT) || message.equals("Вернуться")) {
            if (message.equals("Вернуться")) {
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
                deleteDataCache.clear(chatId);

                return askDesiredHandler.handle(inputMsg);
            }

            if (message.equals("Удалить встречу")) {
                userDataCache.setUsersCurrentBotState(userId, BotState.DELETE_MEET);

                return createMessageWithKeyboard(chatId, "Введите номер встречи для удаления", getMyMeetsDeleteMarkup());
            }
        } else if (botState.equals(BotState.ASK_NOTIFY)) {
            if (message.equals("Да")) {
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_CAUSE);
                return createMessageWithKeyboard(chatId, "Добавите причину отмены?", getYesNoButtonsMarkup());
            } else if (message.equals("Нет")) {
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
                return askDesiredHandler.handle(inputMsg);
            } else {
                meetBot.sendText(chatId, "Неверный ответ!");
                return createMessageWithKeyboard(chatId, "Отправить уведомление об отмене встречи?", getYesNoButtonsMarkup());
            }
        } else if (botState.equals(BotState.ASK_CAUSE)) {
            if (message.equals("Да")) {
                userDataCache.setUsersCurrentBotState(userId, BotState.SEND_NOTIFY);
                return new SendMessage(chatId, "Введите причину отмены: ");
            } else if (message.equals("Нет")) {

                List<UserData> userDataList = userDataService.getAllUsers();
                userDataList.stream()
                        .filter(UserData::isSubscribed)
                        .filter(i -> i.getChatId() != chatId)
                        .forEach(i -> meetBot.sendText(i.getChatId(), sendTextCancelMeet()));

                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
                return askDesiredHandler.handle(inputMsg);
            } else {
                meetBot.sendText(chatId, "Неверный ответ!");
                return createMessageWithKeyboard(chatId, "Добавите причину отмены?", getYesNoButtonsMarkup());
            }
        } else if (botState.equals(BotState.SEND_NOTIFY)) {

            List<UserData> userDataList = userDataService.getAllUsers();
            userDataList.stream()
                    .filter(UserData::isSubscribed)
                    .filter(i -> i.getChatId() != chatId)
                    .forEach(i -> sendTextCancelMeetWithCause(message, i));

            userDataCache.setUsersCurrentBotState(userId, BotState.ASK_DESIRED);
            return askDesiredHandler.handle(inputMsg);
        } else {
            try {
                int num = Integer.parseInt(message);
                meetDataToDelete = deleteDataCache.getMeets(chatId).get(num - 1);
                meetDataService.deleteMeetById(meetDataToDelete.getId());
                deleteDataCache.clear(chatId);
                meetBot.sendText(chatId, "Удаление прошло успешно!");
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_NOTIFY);
                return createMessageWithKeyboard(chatId, "Отправить уведомление об отмене встречи?", getYesNoButtonsMarkup());
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                userDataCache.setUsersCurrentBotState(userId, BotState.DELETE_MEET);
                meetBot.sendText(chatId, "Неверный номер!");
                return new SendMessage(chatId, "Введите номер встречи для удаления");
            }
        }
        return new SendMessage(chatId, messagesService.getReplyText("reply.generalQuestion"));
    }

    private void sendTextCancelMeetWithCause(String message, UserData i) {
        meetBot.sendText(i.getChatId(), meetDataToDelete.getCreator() != null ?
                "Встреча отменена:" + "\n" + meetDataToDelete.toStringWithCreator() + "\n" + "Причина: " + "\n" + message
                : "Встреча отменена:" + "\n" + meetDataToDelete.toStringWithoutCreator() + "\n" + "Причина: " + "\n" + message);
    }

    private String sendTextCancelMeet() {
        return meetDataToDelete.getCreator() != null ?
                "Встреча отменена:" + "\n" + meetDataToDelete.toStringWitCreatorWithNewMessage()
                : "Встреча отменена:" + "\n" + meetDataToDelete.toStringWithoutCreatorWithNewMessage();
    }

    @Override
    public BotState getHandleName() {
        return BotState.SHOW_USER_MEETS;
    }

    private void sendMeet(long chatId, MeetData meetData, int id) {
        meetBot.sendText(chatId, meetData.getCreator() != null ?
                "#" + id + "." + "\n" + meetData.toStringWithCreator() :
                "#" + id + "." + "\n" + meetData.toStringWithoutCreator());
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

    private ReplyKeyboardMarkup getMyMeetsMarkup() {
        return StaticMethods.getButtonsMarkup("Вернуться", "Удалить встречу");
    }

    private ReplyKeyboardMarkup getMyMeetsDeleteMarkup() {
        return StaticMethods.getButtonsMarkup("Вернуться");
    }

    private ReplyKeyboardMarkup getYesNoButtonsMarkup() {
        return StaticMethods.getButtonsMarkup("Да", "Нет");
    }

    private void sendMeetWithMarkup(Long chatId, MeetData meetData, int id) {
        userDataCache.putMeetDataWithMsgId(meetBot.sendTextWithInlineMarkup(chatId, meetData.getCreator() != null ?
                "#" + id + "." + "\n" + meetData.toStringWithCreator() :
                "#" + id + "." + "\n" + meetData.toStringWithoutCreator(), getGoButtonsMarkup(meetData)), meetData);
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
