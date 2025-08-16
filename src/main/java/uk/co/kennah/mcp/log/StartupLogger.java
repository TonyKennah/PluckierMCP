package uk.co.kennah.mcp.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartupLogger.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // This log will now be cached by the WebSocketLogAppender and sent
        // as soon as a client connects to the log viewer.
        logger.info("""

						--------------------------------------------------------------------------------
						The Pluckier MCP server has started successfully and is waiting for commands.
						Congrats!
						--------------------------------------------------------------------------------
						""");
    }
}