package ru.home.meet_bot;

import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.home.meet_bot.botapi.TelegramFacade;

public class MeetBot extends TelegramWebhookBot {
    private String webHookPath;
    private String botUserName;
    private String botToken;

    private final TelegramFacade telegramFacade;

    public MeetBot(DefaultBotOptions botOptions, TelegramFacade telegramFacade) {
        super(botOptions);
        this.telegramFacade = telegramFacade;
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return telegramFacade.handleUpdate(update);
    }

    @Override
    public String getBotPath() {
        return webHookPath;
    }

    public void setWebHookPath(String webHookPath) {
        this.webHookPath = webHookPath;
    }

    public void setBotUserName(String botUserName) {
        this.botUserName = botUserName;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    @SneakyThrows
    public void sendText(long chatId, String text) {
        execute(new SendMessage(chatId, text).enableHtml(true));
    }

    @SneakyThrows
    public void deleteMessage(DeleteMessage delMsg)  {
        execute(delMsg);
    }

    @SneakyThrows
    public int sendTextWithInlineMarkup(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        return execute(new SendMessage(chatId, text).setReplyMarkup(inlineKeyboardMarkup).enableHtml(true)).getMessageId();
    }

    @SneakyThrows
    public void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        execute(answerCallbackQuery);
    }
}
