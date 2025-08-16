package uk.co.kennah.mcp.log;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class LogSubscriptionListener implements ApplicationListener<SessionSubscribeEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public LogSubscriptionListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        String destination = (String) event.getMessage().getHeaders().get("simpDestination");

        if ("/topic/logs".equals(destination)) {
            // A client has just subscribed. First, tell the appender to flush any cached startup logs.
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            WebSocketLogAppender appender = (WebSocketLogAppender) root.getAppender("WEBSOCKET");
            if (appender != null) {
                appender.onClientSubscribed();
            }

            // Now, send a dedicated "welcome" message directly to the newly connected client.
            String welcomeMessage = String.format("%s --- Connected to log stream ---", LocalTime.now().format(TIME_FORMATTER));
            messagingTemplate.convertAndSend("/topic/logs", welcomeMessage);
        }
    }
}