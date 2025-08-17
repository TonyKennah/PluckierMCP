[![Build & Test Status](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml/badge.svg)](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml)



# Pluckier MCP Server

![Example](gemini.jpg "Gemini using pluckier")

# Communication Viewer
<img width="1803" height="605" alt="image" src="https://github.com/user-attachments/assets/6180d2db-c894-4e1b-ab51-65b0b7b58d9a" />

# Pluckier MCP
This is a Spring Boot application that provides information about horse races and interacts with Google Cloud Storage. It uses Spring AI to expose tool functions for an AI agent and exposes a REST endpoint `/info` to retrieve race information from a JSON file stored in a GCS bucket.

## Technology Stack

Simple code but all about the information.

*   Spring Framework
*   Java 17
*   Spring Boot 3.x
*   Spring AI
*   Google Cloud Storage
*   Maven

## Prerequisites

*   Java Development Kit (JDK) 17 or later.
*   Apache Maven.
*   Access to Google Cloud Storage. You must be authenticated, for example by running `gcloud auth application-default login`.

## Configuration

The application is configured via `src/main/resources/application.properties`. You must provide the following properties for it to connect to Google Cloud Storage:

*   `gcs.bucket.name`: The name of your GCS bucket.
*   `gcs.file.name`: The name of the JSON file within the bucket.

Example `application.properties`:
```properties
gcs.bucket.name=your-gcs-bucket
gcs.file.name=your-race-data.json
```

## Building the Project

You can build the project using the Maven wrapper:

```sh
./mvnw clean install
```

## Running the Application

To run the application, use the Spring Boot Maven plugin:

```sh
./mvnw spring-boot:run
```

The server will start on `http://localhost:8080`.

How It All Connects
- The logs.html frontend connects to the server at the /ws endpoint.
- The frontend then subscribes to the /topic/logs channel.
On the server, the WebSocketLogAppender intercepts a log message.
- The appender sends that message to the /topic/logs destination.
- The message broker, configured by this class, receives the message and broadcasts it to all clients subscribed to /topic/logs.


## Usage

### Live Log Viewer

The application provides a real-time log viewer to monitor server activity. This is particularly useful for observing the AI agent's behavior and the results of the tool function calls as they happen.

1.  Ensure the application is running.
2.  Open your web browser and navigate to:
    ```
    http://localhost:8080/logs.html
    ```
3.  The page will automatically connect to the server's WebSocket endpoint and display log messages as they are generated.

---

### Spring AI Tools

The `RacesInfo` class is annotated with `@Tool` and provides functions that can be used by a Spring AI-powered agent:
*   `get_best_ever_rated(String time, String place)`: Get the best rated horse for a particular race, identified by its time and place. This is the highest single rating from any past race.
*   `get_top_rated(String time, String place)`: Get the horse with the best average rating over its last 3 runs for a particular race.
*   `get_bottom_rated(String time, String place)`: Get the horse with the worst average rating over its last 3 runs.
*   `get_best_average_rated(String time, String place)`: Get the horse with the best average rating across all its past runs for a particular race.
*   `get_best_most_recent_rated(String time, String place)`: Get the horse with the highest rating from its most recent race.
*   `get_race_win_percentages(String time, String place)`: Calculates the win percentage for each horse in a race based on their best-ever rating.
*   `get_all_runners(String time, String place)`: Get all the runners for a particular race.
*   `get_past_run_dates(String horseName)`: Get all the past race dates for a given horse.
*   `get_all_times(String place)`: Get all the race times for a given meeting place.
*   `get_meetings()`: Retrieve all unique meeting place names.
*   `find_horse_race(String horseName)`: Finds the race time and meeting for a given horse.
*   `get_next_race()`: Reports the next race time and meeting based on the current system time.
*   `get_horse_form(String time, String place, String horseName)`: Get the recent form (past race dates and ratings) for a specific horse in a particular race.
*   `get_nap_of_the_day()`: Find the best bet of the day across all races, based on the highest average rating over the last 3 runs.
*   `get_handicap_nap_of_the_day()`: Find the best bet of the day from handicap races only, based on the highest average rating over the last 3 runs.
