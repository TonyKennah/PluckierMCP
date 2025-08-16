package uk.co.kennah.mcp.log;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketLogAppenderConfig implements ApplicationListener<ContextRefreshedEvent> {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketLogAppenderConfig(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Get the root logger from the SLF4J factory, which is backed by Logback
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        // Find our appender by its name (as configured in logback-spring.xml)
        WebSocketLogAppender appender = (WebSocketLogAppender) root.getAppender("WEBSOCKET");

        // If the appender is found, set the template on the instance.
        // This is the crucial step that allows the appender to flush its cache.
        if (appender != null) {
            appender.setMessagingTemplate(messagingTemplate);
        }
    }
}