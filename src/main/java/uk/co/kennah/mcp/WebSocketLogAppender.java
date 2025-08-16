package uk.co.kennah.mcp;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;
    private static final Queue<byte[]> eventCache = new ConcurrentLinkedQueue<>();
    private static volatile SimpMessagingTemplate messagingTemplate;

    /**
     * This static method allows the Spring-managed config to provide the template
     * once the application context is ready.
     * @param template The SimpMessagingTemplate bean.
     */
    public static void setMessagingTemplate(SimpMessagingTemplate template) {
        messagingTemplate = template;
        flushCache();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted() || this.encoder == null) {
            return;
        }

        byte[] encodedEvent = this.encoder.encode(eventObject);
        if (messagingTemplate == null) {
            // Cache events until the messaging template is available.
            eventCache.add(encodedEvent);
        } else {
            // Ensure cache is flushed before sending new event to maintain order.
            if (!eventCache.isEmpty()) {
                flushCache();
            }
            sendMessage(encodedEvent);
        }
    }

    private static void flushCache() {
        byte[] eventBytes;
        while ((eventBytes = eventCache.poll()) != null) {
            sendMessage(eventBytes);
       }
    }

    private static void sendMessage(byte[] encodedEvent) {
        if (messagingTemplate != null) {
            // The receiving client (logs.html) expects a string.
            messagingTemplate.convertAndSend("/topic/logs", new String(encodedEvent, StandardCharsets.UTF_8));
        }
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}

