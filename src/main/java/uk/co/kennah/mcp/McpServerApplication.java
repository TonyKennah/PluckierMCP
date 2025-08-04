package uk.co.kennah.mcp;

import com.google.cloud.spring.autoconfigure.storage.GcpStorageAutoConfiguration;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

	@Bean
	public String applicationName() {
		return "MCP Server Application";
	}

	@Bean
	public String applicationVersion() {
		return "1.0.0";
	}

	@Bean
	public String applicationDescription() {
		return "This is a sample application for managing MCP functionalities.";
	}

	@Bean
	public List<ToolCallback> toolCallbacks(RacesInfo info) {
		return List.of(ToolCallbacks.from(info));
	}

}
