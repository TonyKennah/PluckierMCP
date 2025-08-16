package uk.co.kennah.mcp;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@EnableCaching
public class McpServerApplication {

	private static final Logger logger = LoggerFactory.getLogger(McpServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

	/**
	 * Customizes the handling of dropped errors in the reactive stream pipeline.
	 * This is particularly useful for providing a clear log message when an AI client
	 * disconnects before the server can send back a response from a tool call.
	 */
	@PostConstruct
	public void init() {
		Hooks.onErrorDropped(e -> {
			if (e instanceof RuntimeException && e.getMessage() != null && e.getMessage().contains("Failed to enqueue message")) {
				logger.warn("""

						--------------------------------------------------------------------------------
						AI tool call completed, but the result could not be sent back to the client.
						This usually means the client (e.g., the Gemini CLI) was closed or disconnected
						before the server's response was ready. This is not a server error.
						Original error was: {}
						--------------------------------------------------------------------------------
						""", e.toString());
			} else {
				// Default behavior for other dropped errors
				logger.error("A non-fatal error was dropped by the reactive framework:", e);
			}
		});
	}

	@Bean
	public String applicationName() {
		return "Pluckier MCP Server Application";
	}

	@Bean
	public String applicationVersion() {
		return "1.0.0";
	}

	@Bean
	public String applicationDescription() {
		return "An application supplying horse racing information.";
	}

	@Bean
	public List<ToolCallback> toolCallbacks(RacesInfo info) {
		return List.of(ToolCallbacks.from(info));
	}

}
