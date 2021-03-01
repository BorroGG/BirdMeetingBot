package ru.home.meet_bot.appconfig;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import ru.home.meet_bot.MeetBot;
import ru.home.meet_bot.botapi.TelegramFacade;
import ru.home.meet_bot.model.MeetData;
import ru.home.meet_bot.service.MeetDataService;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "telegrambot")
public class BotConfig {
    private String webHookPath;
    private String botUserName;
    private String botToken;

    @Bean
    public MeetBot meetBot(TelegramFacade telegramFacade, MeetDataService meetDataService) {
        DefaultBotOptions options = ApiContext.getInstance(DefaultBotOptions.class);

        MeetBot meetBot = new MeetBot(options, telegramFacade);
        meetBot.setBotUserName(botUserName);
        meetBot.setBotToken(botToken);
        meetBot.setWebHookPath(webHookPath);

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(() -> {
            List<MeetData> meetDataList = meetDataService.getAllMeets();
            meetDataList.stream()
                    .filter(i -> i.getDate().isBefore(LocalDate.now().minusDays(3)))
                    .forEach(i -> meetDataService.deleteMeetById(i.getId()));
        }, 24, 24, TimeUnit.HOURS);

        return meetBot;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

}
