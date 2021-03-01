package ru.home.meet_bot.botapi;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BotStateContext {
    private final Map<BotState, InputMessageHandler> messageHandlers = new HashMap<>();

    public BotStateContext(List<InputMessageHandler> messageHandlers) {
        messageHandlers.forEach(handler -> this.messageHandlers.put(handler.getHandleName(), handler));
    }

    public SendMessage processInputMessage(BotState currentState, Message message) {
        InputMessageHandler currentMessageHandler = findMessageHandler(currentState);
        return currentMessageHandler.handle(message);
    }

    private InputMessageHandler findMessageHandler(BotState currentState) {
        if (isFillingProfileState(currentState)) {
            return messageHandlers.get(BotState.FILLING_MEET);
        } else if (isShowMyMeets(currentState)) {
            return messageHandlers.get(BotState.SHOW_USER_MEETS);
        }
        return messageHandlers.get(currentState);
    }

    private boolean isShowMyMeets(BotState currentState) {
       switch (currentState) {
           case SHOW_USER_MEETS:
           case DELETE_MEET:
           case GO_BACK:
           case CHOOSE_ACT:
           case ASK_NOTIFY:
           case ASK_CAUSE:
           case SEND_NOTIFY:
               return true;
           default:
               return false;
       }
    }

    private boolean isFillingProfileState(BotState currentState) {
        switch (currentState) {
            case CHOOSE_DATE:
            case CHOOSE_TIME:
            case CHOOSE_PLACE:
            case GET_DESCRIPTION:
            case ADD_LINK:
            case SHOW_CREATED_MEET:
            case FILLING_MEET:
            case ASK_CHANGE_MEET:
                return true;
            default:
                return false;
        }
    }
}
