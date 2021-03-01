package ru.home.meet_bot.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.home.meet_bot.MeetBot;

@RestController
public class WebHookController {
    private final MeetBot meetBot;

    public WebHookController(MeetBot meetBot) {
        this.meetBot = meetBot;
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return meetBot.onWebhookUpdateReceived(update);
    }
}
