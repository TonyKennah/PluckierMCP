[![Build & Test Status](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml/badge.svg)](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml)



# MCP Server

![Example](gemini.jpg "Gemini using pluckier")

This is a Spring Boot application that provides information about horse races and interacts with Google Cloud Storage. It uses Spring AI to expose tool functions for an AI agent and exposes a REST endpoint `/info` to retrieve race information from a JSON file stored in a GCS bucket.

## Technology Stack

*   Java 17
*   Spring Boot 3.5.4
*   Spring AI
*   Google Cloud Storage
*   Maven

## Prerequisites

*   Java Development Kit (JDK) 17 or later.
*   Apache Maven.
*   Access to Google Cloud Storage. You must be authenticated, for example by running `gcloud auth application-default login`.

## Configuration

The application is configured to read race data from a JSON file located in a Google Cloud Storage bucket.

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

## Usage

### REST API

The application provides several REST endpoints to test the data retrieval logic that is exposed to the AI agent.

*   **GET /info**

    Retrieves the entire content of the raw `sample_races.json` file.

    **Example using cURL:**
    ```sh
    curl http://localhost:8080/info
    ```

*   **GET /meetings**

    Retrieves all unique meeting place names.

    **Example using cURL:**
    ```sh
    curl http://localhost:8080/meetings
    ```

*   **GET /all-times?place={place}**

    Retrieves all race times for a given meeting place.

    **Example using cURL:**
    ```sh
    curl "http://localhost:8080/all-times?place=Ascot"
    ```

*   **GET /all-runners?time={time}&place={place}**

    Retrieves all runners for a specific race.

    **Example using cURL:**
    ```sh
    curl "http://localhost:8080/all-runners?time=13:30&place=Ascot"
    ```

*   **GET /top-rated?time={time}&place={place}**

    Retrieves the horse with the highest single rating from any past race for a specific race.

    **Example using cURL:**
    ```sh
    curl "http://localhost:8080/top-rated?time=14:05&place=Ascot"
    ```

*   **GET /best-average-rated?time={time}&place={place}**

    Retrieves the horse with the best average rating for a specific race.

    **Example using cURL:**
    ```sh
    curl "http://localhost:8080/best-average-rated?time=13:30&place=Ascot"
    ```

*   **GET /best-most-recent-rated?time={time}&place={place}**

    Retrieves the horse with the highest rating from its most recent race.

    **Example using cURL:**
    ```sh
    curl "http://localhost:8080/best-most-recent-rated?time=13:30&place=Ascot"
    ```

### Spring AI Tools

The `RacesInfo` class is annotated with `@Tool` and provides functions that can be used by a Spring AI-powered agent:
*   `getMeetings()`: Retrieves all unique meeting place names from the race data.
*   `getTopRated(String time, String place)`: Get the top rated horse for a particular race, identified by its time and place. This is the highest single rating from any past race.
*   `getBestAverageRated(String time, String place)`: Get the horse with the best average rating for a particular race, identified by its time and place.
*   `getBestMostRecentRated(String time, String place)`: Get the horse with the highest rating from its most recent race, for a particular race identified by its time and place.
*   `getAllRunners(String time, String place)`: Get all the runners for a particular race, identified by its time and place.
*   `getAllTimes(String place)`: Get all the race times for a given meeting place.
*   `getRawRaceData()`: Reads the raw race data file from the configured Google Cloud Storage bucket.
