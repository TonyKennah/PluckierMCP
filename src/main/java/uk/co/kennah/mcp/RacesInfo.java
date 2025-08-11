package uk.co.kennah.mcp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.cloud.storage.StorageException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class RacesInfo {

    @Autowired
    private GCSReader gcsReader;

    private JsonArray getCachedRaceData() {
        JsonElement jsonElement = gcsReader.readFileFromGCSAsJson();
        if (jsonElement == null || !jsonElement.isJsonArray()) {
            // This case will be handled by the calling methods if they receive null.
            return null;
        }
        return jsonElement.getAsJsonArray();
    }

    @Tool(name = "getMeetings", description = "Retrieve all unique meeting place names from the race data.")
    public String getMeetings() {
        try {
            JsonArray races = getCachedRaceData();
            if (races == null) return "Error: Race data is not in the expected format.";
            Set<String> meetings = new HashSet<>();
            for (JsonElement raceElement : races) {
                JsonObject raceObject = raceElement.getAsJsonObject();
                if (raceObject.has("place")) {
                    meetings.add(raceObject.get("place").getAsString());
                }
            }

            if (meetings.isEmpty()) {
                return "No meetings found in the data.";
            }

            return "List of available meetings: " + meetings.stream().collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "An error occurred while fetching meetings: " + e.getMessage();
        }
    }

    @Tool(name = "getTopRated", description = "Get the top rated horse for a particular race, identified by its time and place. This is the highest single rating from any past race.")
    public String getTopRated(String time, String place) {
        JsonArray races = getCachedRaceData();
        if (races == null) return "Error: Race data is not in the expected format.";
        for (JsonElement raceElement : races) {
            JsonObject raceObject = raceElement.getAsJsonObject();
            if (raceObject.get("place").getAsString().equalsIgnoreCase(place)
                    && raceObject.get("time").getAsString().equals(time)) {

                JsonArray horses = raceObject.getAsJsonArray("horses");
                String topRatedHorseName = "N/A";
                double maxRating = -1.0;

                for (JsonElement horseElement : horses) {
                    JsonObject horseObject = horseElement.getAsJsonObject();
                    JsonArray pastForms = horseObject.getAsJsonArray("past_form");
                    if (pastForms != null) {
                        for (JsonElement formElement : pastForms) {
                            JsonObject formObject = formElement.getAsJsonObject();
                            if (formObject.has("rating")) {
                                double rating = formObject.get("rating").getAsDouble();
                                if (rating > maxRating) {
                                    maxRating = rating;
                                    topRatedHorseName = horseObject.get("name").getAsString();
                                }
                            }
                        }
                    }
                }
                if (maxRating == -1.0) {
                    return "No rated horses found for the race at " + place + " at " + time;
                }
                return "Top Rated for the " + time + " at " + place + " is: " + topRatedHorseName + " with a rating of " + maxRating;
            }
        }
        return "Could not find the race at " + place + " at " + time;
    }

    @Tool(name = "getBestAverageRated", description = "Get the horse with the best average rating for a particular race, identified by its time and place.")
    public String getBestAverageRated(String time, String place) {
        JsonArray races = getCachedRaceData();
        if (races == null) return "Error: Race data is not in the expected format.";
        for (JsonElement raceElement : races) {
            JsonObject raceObject = raceElement.getAsJsonObject();
            if (raceObject.get("place").getAsString().equalsIgnoreCase(place)
                    && raceObject.get("time").getAsString().equals(time)) {

                JsonArray horses = raceObject.getAsJsonArray("horses");
                String bestHorseName = "N/A";
                double maxAverageRating = -1.0;

                for (JsonElement horseElement : horses) {
                    JsonObject horseObject = horseElement.getAsJsonObject();
                    JsonArray pastForms = horseObject.getAsJsonArray("past_form");
                    double totalRating = 0;
                    int ratingCount = 0;

                    if (pastForms != null) {
                        for (JsonElement formElement : pastForms) {
                            JsonObject formObject = formElement.getAsJsonObject();
                            if (formObject.has("rating")) {
                                totalRating += formObject.get("rating").getAsDouble();
                                ratingCount++;
                            }
                        }
                    }

                    if (ratingCount > 0) {
                        double averageRating = totalRating / ratingCount;
                        if (averageRating > maxAverageRating) {
                            maxAverageRating = averageRating;
                            bestHorseName = horseObject.get("name").getAsString();
                        }
                    }
                }

                if (maxAverageRating == -1.0) {
                    return "No horses with an average rating found for the race at " + place + " at " + time;
                }
                return "Horse with best average rating for the " + time + " at " + place + " is: " + bestHorseName
                        + " with an average rating of " + String.format("%.2f", maxAverageRating);
            }
        }
        return "Could not find the race at " + place + " at " + time;
    }

    @Tool(name = "getBestMostRecentRated", description = "Get the horse with the highest rating from its most recent race, for a particular race identified by its time and place.")
    public String getBestMostRecentRated(String time, String place) {
        JsonArray races = getCachedRaceData();
        if (races == null) return "Error: Race data is not in the expected format.";
        for (JsonElement raceElement : races) {
            JsonObject raceObject = raceElement.getAsJsonObject();
            if (raceObject.get("place").getAsString().equalsIgnoreCase(place)
                    && raceObject.get("time").getAsString().equals(time)) {

                JsonArray horses = raceObject.getAsJsonArray("horses");
                String bestHorseName = "N/A";
                double maxRecentRating = -1.0;

                for (JsonElement horseElement : horses) {
                    JsonObject horseObject = horseElement.getAsJsonObject();
                    JsonArray pastForms = horseObject.getAsJsonArray("past_form");

                    if (pastForms != null && !pastForms.isEmpty()) {
                        JsonObject mostRecentForm = null;
                        LocalDate latestDate = null;

                        for (JsonElement formElement : pastForms) {
                            JsonObject formObject = formElement.getAsJsonObject();
                            if (formObject.has("date")) {
                                LocalDate date = LocalDate.parse(formObject.get("date").getAsString());
                                if (latestDate == null || date.isAfter(latestDate)) {
                                    latestDate = date;
                                    mostRecentForm = formObject;
                                }
                            }
                        }

                        if (mostRecentForm != null && mostRecentForm.has("rating")) {
                            double rating = mostRecentForm.get("rating").getAsDouble();
                            if (rating > maxRecentRating) {
                                maxRecentRating = rating;
                                bestHorseName = horseObject.get("name").getAsString();
                            }
                        }
                    }
                }

                if (maxRecentRating == -1.0) {
                    return "No horses with a recent rating found for the race at " + place + " at " + time;
                }
                return "Horse with best most recent rating for the " + time + " at " + place + " is: " + bestHorseName
                        + " with a rating of " + maxRecentRating;
            }
        }
        return "Could not find the race at " + place + " at " + time;
    }

    @Tool(name = "getAllRunners", description = "Get all the runners for a particular race, identified by its time and place.")
    public String getAllRunners(String time, String place) {
        JsonArray races = getCachedRaceData();
        if (races == null) return "Error: Race data is not in the expected format.";
        for (JsonElement raceElement : races) {
            JsonObject raceObject = raceElement.getAsJsonObject();
            if (raceObject.get("place").getAsString().equalsIgnoreCase(place)
                    && raceObject.get("time").getAsString().equals(time)) {

                JsonArray horses = raceObject.getAsJsonArray("horses");
                List<String> runnerNames = new ArrayList<>();
                for (JsonElement horseElement : horses) {
                    runnerNames.add(horseElement.getAsJsonObject().get("name").getAsString());
                }

                if (runnerNames.isEmpty()) {
                    return "No runners found for the race at " + place + " at " + time;
                }
                return "Runners for the " + time + " at " + place + ": " + String.join(", ", runnerNames);
            }
        }
        return "Could not find the race at " + place + " at " + time;
    }

    @Tool(name = "getAllTimes", description = "Get all the race times for a given meeting place.")
    public String getAllTimes(String place) {
        JsonArray races = getCachedRaceData();
        if (races == null) return "Error: Race data is not in the expected format.";
        List<String> raceTimes = new ArrayList<>();
        for (JsonElement raceElement : races) {
            JsonObject raceObject = raceElement.getAsJsonObject();
            if (raceObject.get("place").getAsString().equalsIgnoreCase(place)) {
                raceTimes.add(raceObject.get("time").getAsString());
            }
        }

        if (raceTimes.isEmpty()) {
            return "No race times found for meeting at " + place;
        }

        raceTimes.sort(Comparator.naturalOrder());
        return "Race times for " + place + ": " + String.join(", ", raceTimes);
    }

    @Tool(name = "getRawRaceData", description = "Reads the raw race data file from the configured Google Cloud Storage bucket.")
    public String getRawRaceData() {
        try {
            // Pretty-print the JSON for agent consumption
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(gcsReader.readFileFromGCSAsJson());
        } catch (StorageException e) {
            return "Error reading from GCS: " + e.getMessage();
        }
    }
}
