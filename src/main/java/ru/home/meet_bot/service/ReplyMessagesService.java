package ru.home.meet_bot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
public class ReplyMessagesService {

    private final LocaleMessageService localeMessageService;

    public ReplyMessagesService(LocaleMessageService localeMessageService) {
        this.localeMessageService = localeMessageService;
    }

    public SendMessage getReplayMessage(long chatId, String replayMessage) {
        return new SendMessage(chatId, localeMessageService.getMessage(replayMessage));
    }

    public SendMessage getReplayMessage(long chatId, String replayMessage, Object... args) {
        return new SendMessage(chatId, localeMessageService.getMessage(replayMessage, args));
    }

    public String getReplyText(String replyText) {
        return localeMessageService.getMessage(replyText);
    }
}
