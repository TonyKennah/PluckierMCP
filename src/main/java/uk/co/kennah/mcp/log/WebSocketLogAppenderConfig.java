package uk.co.kennah.mcp.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
@Profile("!test")
public class WebSocketLogAppenderConfig {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketLogAppenderConfig(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        WebSocketLogAppender appender = (WebSocketLogAppender) rootLogger.getAppender("WEBSOCKET");
        if (appender != null) {
            appender.setMessagingTemplate(messagingTemplate);
        }
    }
}