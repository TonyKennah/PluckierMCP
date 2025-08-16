package uk.co.kennah.mcp.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;
    // Non-static fields for instance-based safety in Spring
    private final Queue<byte[]> eventCache = new ConcurrentLinkedQueue<>();
    private volatile SimpMessagingTemplate messagingTemplate;
    private volatile boolean clientSubscribed = false;

    /**
     * Allows the Spring-managed initializer to provide the template once the
     * application context is ready.
     * @param template The SimpMessagingTemplate bean.
     */
    public void setMessagingTemplate(SimpMessagingTemplate template) {
        this.messagingTemplate = template;
        // Don't flush here; wait for a client to subscribe.
    }

    /**
     * Called by a listener when a client subscribes to the log topic. This
     * triggers the cache to be flushed and enables direct logging.
     */
    public void onClientSubscribed() {
        this.clientSubscribed = true;
        flushCache();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted() || this.encoder == null) {
            return;
        }

        byte[] encodedEvent = this.encoder.encode(eventObject);
        // Cache events until both the template is ready AND a client has subscribed.
        if (clientSubscribed && this.messagingTemplate != null) {
            // If we are live, ensure the cache is flushed first to maintain order
            if (!eventCache.isEmpty()) {
                flushCache();
            }
            sendMessage(encodedEvent);
        } else {
            eventCache.add(encodedEvent);
        }
    }

    private void flushCache() {
        byte[] eventBytes;
        while ((eventBytes = eventCache.poll()) != null) {
            sendMessage(eventBytes);
       }
    }

    private void sendMessage(byte[] encodedEvent) {
        if (this.messagingTemplate != null) {
            // The receiving client (logs.html) expects a string.
            this.messagingTemplate.convertAndSend("/topic/logs", new String(encodedEvent, StandardCharsets.UTF_8));
        }
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
