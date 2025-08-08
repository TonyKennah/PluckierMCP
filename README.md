[![Build & Test Status](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml/badge.svg)](https://github.com/TonyKennah/PluckierMCP/actions/workflows/maven.yml)



# MCP Server

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

Currently, these values are hardcoded in `src/main/java/uk/co/kennah/mcp/RacesInfo.java`:
*   **Bucket Name:** `tony-kennah-mcp`
*   **File Name:** `races.json`

You will need to create this bucket and upload the corresponding `races.json` file for the application to function correctly.

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

The application provides a simple REST endpoint to get all race information.

*   **GET /info**

    Retrieves the entire content of the `races.json` file.

    **Example using cURL:**
    ```sh
    curl http://localhost:8080/info
    ```

### Spring AI Tools

The `RacesInfo` class is annotated with `@Tool` and provides functions that can be used by a Spring AI-powered agent:
*   `getMeetings(String date)`: Retrieves race meetings for a given date.
*   `getTopRated(String meeting)`: Retrieves the top-rated horse for a given meeting.