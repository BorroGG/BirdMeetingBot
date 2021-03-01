package ru.home.meet_bot.botapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.home.meet_bot.MeetBot;
//import ru.home.meet_bot.botapi.handlers.fillingmeet.FillingMeetHandler;
import ru.home.meet_bot.botapi.handlers.showmeets.ShowMeetsHandler;
import ru.home.meet_bot.botapi.handlers.showmeets.ShowUserMeetsHandler;
import ru.home.meet_bot.cache.UserDataCache;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.service.MeetDataService;
import ru.home.meet_bot.utils.Calendar;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TelegramFacade {
    private final BotStateContext botStateContext;
    private final UserDataCache userDataCache;
    private final MeetDataService meetDataService;
    private final MeetBot meetBot;
    private final ShowMeetsHandler showMeetsHandler;
    private final ShowUserMeetsHandler showUserMeetsHandler;
    //private FillingMeetHandler fillingMeetHandler;

    public TelegramFacade(BotStateContext botStateContext, UserDataCache userDataCache,
            /*FillingMeetHandler fillingMeetHandler,*/
                          @Lazy MeetBot meetBot, MeetDataService meetDataService,
                          ShowMeetsHandler showMeetsHandler, ShowUserMeetsHandler showUserMeetsHandler) {
        this.botStateContext = botStateContext;
        this.userDataCache = userDataCache;
        this.meetBot = meetBot;
        //this.fillingMeetHandler = fillingMeetHandler;
        this.meetDataService = meetDataService;
        this.showMeetsHandler = showMeetsHandler;
        this.showUserMeetsHandler = showUserMeetsHandler;
    }

    public BotApiMethod<?> handleUpdate(Update update) {
        SendMessage replayMessage = null;

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            log.info("New callbackQuery from User: {}, userId: {}, with data: {}", update.getCallbackQuery().getFrom().getUserName(),
                    callbackQuery.getFrom().getId(), update.getCallbackQuery().getData());
            return processCallbackQuery(callbackQuery);
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            log.info("New message from User: {}, userId: {}, chatId: {}, with text: {}", message.getFrom().getUserName(),
                    message.getFrom().getId(), message.getChatId(), message.getText());
            replayMessage = handleInputMessage(message);
        }

        return replayMessage;
    }

    private BotApiMethod<?> processCallbackQuery(CallbackQuery buttonQuery) {
        String callback = buttonQuery.getData();
        long chatId = buttonQuery.getMessage().getChatId();
        int userId = buttonQuery.getFrom().getId();
        int msgId = buttonQuery.getMessage().getMessageId();
        BotState botState = userDataCache.getUsersCurrentBotState(userId);
        String idQuery = buttonQuery.getId();

        log.info(userDataCache.getUsersCurrentBotState(userId).toString());

        if (botState.equals(BotState.ASK_DESIRED) || botState.equals(BotState.CHOOSE_ACT)) {
            MeetData meetData = userDataCache.getMeetDataByMsgId(msgId);
            boolean hasChange = false;
            if (callback.equals("buttonGo")) {

                if (Objects.isNull(meetData.getIsVotedGo())
                        || Objects.isNull(meetData.getIsVotedGo().get(chatId))
                        || !meetData.getIsVotedGo().get(chatId)) {

                    meetData.setGo(meetData.getGo() + 1);

                    if (Objects.isNull(meetData.getIsVotedNotGo())
                            || Objects.isNull(meetData.getIsVotedNotGo().get(chatId))) {

                        meetBot.sendAnswerCallbackQuery(new AnswerCallbackQuery().setCallbackQueryId(idQuery).setText("Вы проголосовали"));
                        meetData.setIsVotedNotGoBool(chatId, false);

                    } else if (meetData.getIsVotedNotGo().get(chatId)) {

                        meetBot.sendAnswerCallbackQuery(new AnswerCallbackQuery().setCallbackQueryId(idQuery).setText("Вы изменили свой выбор"));
                        meetData.setNotGo(meetData.getNotGo() - 1);
                    }

                    meetData.setIsVotedGoBool(chatId, true);
                    meetData.setIsVotedNotGoBool(chatId, false);
                    hasChange = true;
                } else {
                    meetBot.sendAnswerCallbackQuery(new AnswerCallbackQuery().setCallbackQueryId(idQuery).setText("Вы уже проголосовали"));
                }
            } else if (callback.equals("buttonNotGo")) {

                if (Objects.isNull(meetData.getIsVotedNotGo())
                        || Objects.isNull(meetData.getIsVotedNotGo().get(chatId))
                        || !meetData.getIsVotedNotGo().get(chatId)) {

                    meetData.setNotGo(meetData.getNotGo() + 1);

                    if (Objects.isNull(meetData.getIsVotedGo())
                            || Objects.isNull(meetData.getIsVotedGo().get(chatId))) {

                        meetBot.sendAnswerCallbackQuery(new AnswerCallbackQuery().setCallbackQueryId(idQuery).setText("Вы проголосовали"));
                        meetData.setIsVotedGoBool(chatId, false);

                    } else if (meetData.getIsVotedGo().get(chatId)) {

                        meetBot.sendAnswerCallbackQuery(new AnswerCallbackQuery().setCallbackQueryId(idQuery).setText("Вы изменили свой выбор"));
                        meetData.setGo(meetData.getGo() - 1);
                    }

                    meetData.setIsVotedNotGoBool(chatId, true);
                    meetData.setIsVotedGoBool(chatId, false);
                    hasChange = true;
                } else {
                    meetBot.sendAnswerCallbackQuery(new AnswerCallbackQuery().setCallbackQueryId(idQuery).setText("Вы уже проголосовали"));
                }
            }
            meetDataService.saveMeet(meetData);
            if (hasChange) {
                DeleteMessage delMsg = new DeleteMessage();
                delMsg.setChatId(chatId);
                delMsg.setMessageId(msgId);
                meetBot.deleteMessage(delMsg);
            }

            if (botState.equals(BotState.ASK_DESIRED)) {
                showMeetsHandler.handle(buttonQuery.getMessage());
            } else {
                showUserMeetsHandler.handle(buttonQuery.getMessage());
            }

            return new SendMessage(chatId, " ");
        } else {
            if (Pattern.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$", callback) && !callback.isEmpty()) {

                DeleteMessage delMsg = new DeleteMessage();
                delMsg.setChatId(chatId);
                delMsg.setMessageId(msgId);
                meetBot.deleteMessage(delMsg);

                LocalDate date = LocalDate.parse(callback);
                InlineKeyboardMarkup cal = new Calendar(date.getYear(), date.getMonthValue()).getCalendar();
                SendMessage message = new SendMessage(chatId, "Выбери дату встречи: ");
                message.setReplyMarkup(cal);

                return message;
            } else if (!callback.equals(" ")) {

                DeleteMessage delMsg = new DeleteMessage();
                delMsg.setChatId(chatId);
                delMsg.setMessageId(msgId);
                meetBot.deleteMessage(delMsg);

                MeetData meetData = userDataCache.getUserProfileData(userId);
                meetData.setDate(LocalDate.parse(callback.substring(1)));

                userDataCache.saveUserProfileData(userId, meetData);
                userDataCache.setUsersCurrentBotState(userId, BotState.CHOOSE_PLACE);

                meetBot.sendText(chatId, "Вы выбрали: " + LocalDate.parse(callback.substring(1)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)));
                return new SendMessage(chatId, "Введите время встречи:");
            } else {
                return new SendMessage(chatId, callback);
            }
        }
        //return new SendMessage(chatId, callback);
    }

    private SendMessage handleInputMessage(Message message) {
        String inputMsg = message.getText();
        int userId = message.getFrom().getId();
        //long chatId = message.getChatId();
        BotState botState;
        SendMessage replayMessage;
        if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ASK_DESIRED)) {
            switch (inputMsg) {
                case "/start":
                    botState = BotState.ASK_DESIRED;
                    break;
                case "Создать новую встречу":
                    botState = BotState.GET_DESCRIPTION;
                    break;
                case "Посмотреть текущие встречи":
                    botState = BotState.SHOW_ALL_MEETS;
                    break;
                case "Мои встречи":
                    botState = BotState.SHOW_USER_MEETS;
                    break;
                case "Подписаться/Отписаться":
                    botState = BotState.SUBSCRIBE;
                    break;
                default:
                    botState = userDataCache.getUsersCurrentBotState(userId);
                    break;
            }
        } else {
            botState = userDataCache.getUsersCurrentBotState(userId);
        }
        userDataCache.setUsersCurrentBotState(userId, botState);

        replayMessage = botStateContext.processInputMessage(botState, message);

        return replayMessage;
    }
}
