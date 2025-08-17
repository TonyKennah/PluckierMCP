[![Build & Test Status](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml/badge.svg)](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml)



# Pluckier MCP Server

This is a Spring Boot application that provides information about horse races.  It uses Spring AI to expose tool functions for an AI agents and has REST endpoints to retrieve race information.  It pulls daily racing data in JSON format stored in a GCS bucket.

## Technology Stack

Simple enough code but it's all about the information, right?

*   Spring Framework
*   Java 17
*   Spring Boot 3.x
*   Spring AI
*   Google Cloud Storage
*   Maven

## Prerequisites

*   Java Development Kit (JDK) 17 or later.
*   Apache Maven.
*   ⚠️ Access to the specific GCP Cloud storage location, with access credentials stored in the environment:  GOOGLE_APPLICATION_CREDENTIALS

## Building & Running the Project

You can build the project using the Maven wrapper:

```sh
./mvn clean install
```

## Running the Application

Generally you run the server via an AI agent such as Gemini Cli, Claude Desktop, or ChatGPT.  Via a Java command, example for Gemini (settings.json file).
```
{
    "mcpServers": {
        "pluckier": {
            "command": "java",
            "args": [
	            "-Dspring.ai.mcp.server.stdio=true",
                "-jar",
                "<PATH_TO>target\\mcp-server-0.0.1-SNAPSHOT.jar"
            ]
        }
    }
}
```    

To run the application manually, use the Spring Boot Maven plugin:

```sh
./mvn spring-boot:run
```

Or a java command:

```sh
./java -jar target/mcp-server-0.0.1-SNAPSHOT.jar
```

The server will start on `http://localhost:8080` which shows links to the log viewer and exposes the parameterised REST endpoints.

<img width="1236" height="1046" alt="image" src="https://github.com/user-attachments/assets/6d1f222c-7a53-46d5-afd7-0a4319352dc2" />


### Live Log Viewer

The application provides a real-time log viewer to monitor server activity. This is particularly useful for observing the AI agent's behavior and the results of the tool function calls as they happen.

1.  Ensure the application is running.
2.  Open your web browser and navigate to:
    ```
    http://localhost:8080/logs.html
    ```
3.  The page will automatically connect to the server's WebSocket endpoint and display log messages as they are generated.

<img width="1803" height="605" alt="image" src="https://github.com/user-attachments/assets/6180d2db-c894-4e1b-ab51-65b0b7b58d9a" />

---

### Spring AI Tools

<img width="474" height="503" alt="image" src="https://github.com/user-attachments/assets/9e31b882-2438-403c-a313-6b957dea07dc" />

Example:

<img width="1203" height="340" alt="image" src="https://github.com/user-attachments/assets/555aab96-ba9e-4b0a-8ca7-d764909a09e1" />


