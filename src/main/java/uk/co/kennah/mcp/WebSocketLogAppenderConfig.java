package uk.co.kennah.mcp;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import uk.co.kennah.mcp.WebSocketLogAppender;

@Component
public class WebSocketLogAppenderConfig implements ApplicationListener<ContextRefreshedEvent> {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketLogAppenderConfig(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        WebSocketLogAppender.setMessagingTemplate(messagingTemplate);
    }
}