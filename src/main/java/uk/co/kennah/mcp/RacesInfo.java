package uk.co.kennah.mcp;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    private Optional<JsonObject> findRace(String time, String place) {
        JsonArray races = getCachedRaceData();
        if (races == null) {
            return Optional.empty();
        }
        return StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.get("place").getAsString().equalsIgnoreCase(place)
                        && race.get("time").getAsString().equals(time))
                .findFirst();
    }

    @Tool(name = "getMeetings", description = "Retrieve all unique meeting place names from the race data.")
    public String getMeetings() {
        try {
            JsonArray races = getCachedRaceData();
            if (races == null) return "Error: Race data is not in the expected format.";
            Set<String> meetings = StreamSupport.stream(races.spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .filter(race -> race.has("place"))
                    .map(race -> race.get("place").getAsString())
                    .collect(Collectors.toSet());

            if (meetings.isEmpty()) {
                return "No meetings found in the data.";
            }

            return "List of available meetings: " + meetings.stream().sorted().collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "An error occurred while fetching meetings: " + e.getMessage();
        }
    }

    @Tool(name = "getBestEverRated", description = "Get the best rated horse for a particular race, identified by its time and place. This is the highest single rating from any past race.")
    public String getBestEverRated(String time, String place) {
        // Local record for temporary data holding
        record HorseRating(String name, double rating) {}

        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .flatMap(horse -> StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                .map(JsonElement::getAsJsonObject)
                                .filter(form -> form.has("name"))
                                .map(form -> new HorseRating(horse.get("name").getAsString(), form.get("name").getAsInt())))
                        .max(Comparator.comparingDouble(HorseRating::rating))
                        .map(top -> "Top Rated for the " + time + " at " + place + " is: " + top.name() + " with a rating of " + top.rating())
                        .orElse("No rated horses found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "getTopRated", description = "Get the horse with the best average rating over last 3 runs for a particular race, identified by its time and place.")
    public String getTopRated(String time, String place) {
        // Local record for temporary data holding
        record HorseAverageRating(String name, double average) {}

        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .map(horse -> {
                            IntSummaryStatistics stats = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .limit(3)
                                    .filter(form -> form.has("name"))
                                    .mapToInt(form -> form.get("name").getAsInt())
                                    .summaryStatistics();
                            return new HorseAverageRating(horse.get("name").getAsString(), stats.getCount() > 0 ? stats.getAverage() : -1);
                        })
                        .filter(h -> h.average() >= 0)
                        .max(Comparator.comparingDouble(HorseAverageRating::average))
                        .map(top -> "Horse with best last 3 run average rating for the " + time + " at " + place + " is: " + top.name()
                                + " with an average rating of " + String.format("%.2f", top.average()))
                        .orElse("No horses with a recent average rating found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "getBestAverageRated", description = "Get the horse with the best average rating for a particular race, identified by its time and place.")
    public String getBestAverageRated(String time, String place) {
        // Local record for temporary data holding
        record HorseAverageRating(String name, double average) {}

        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .map(horse -> {
                            IntSummaryStatistics stats = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .filter(form -> form.has("name"))
                                    .mapToInt(form -> form.get("name").getAsInt())
                                    .summaryStatistics();
                            return new HorseAverageRating(horse.get("name").getAsString(), stats.getCount() > 0 ? stats.getAverage() : -1);
                        })
                        .filter(h -> h.average() >= 0)
                        .max(Comparator.comparingDouble(HorseAverageRating::average))
                        .map(top -> "Horse with best average rating for the " + time + " at " + place + " is: " + top.name()
                                + " with an average rating of " + String.format("%.2f", top.average()))
                        .orElse("No horses with an average rating found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "getBestMostRecentRated", description = "Get the horse with the highest rating from its most recent race, for a particular race identified by its time and place.")
    public String getBestMostRecentRated(String time, String place) {
        // Local record for temporary data holding
        record HorseRecentRating(String name, int rating) {}
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .filter(horse -> horse.has("past"))
                        .map(horse -> {
                            Optional<JsonObject> mostRecentForm = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .filter(form -> form.has("date"))
                                    .max(Comparator.comparing(form -> LocalDate.parse(form.get("date").getAsString(), formatter)));
                            
                                    System.out.println("-----------------------------" + place + " " + time + " " + mostRecentForm.toString());
                           
                                    return mostRecentForm.filter(form -> form.has("name"))
                                    .map(form -> new HorseRecentRating(horse.get("name").getAsString(), form.get("name").getAsInt()));
                        })
                        .flatMap(Optional::stream) // Filter out horses with no recent rated form
                        .max(Comparator.comparingInt(HorseRecentRating::rating))
                        .map(top -> "Horse with best most recent rating for the " + time + " at " + place + " is: " + top.name()
                                + " with a rating of " + top.rating())
                        .orElse("No horses with a recent rating found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "getAllRunners", description = "Get all the runners for a particular race, identified by its time and place.")
    public String getAllRunners(String time, String place) {
        return findRace(time, place)
                .map(race -> {
                    String runners = StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(horse -> horse.getAsJsonObject().get("name").getAsString())
                            .collect(Collectors.joining(", "));
                    return runners.isEmpty() ? "No runners found for the race at " + place + " at " + time
                            : "Runners for the " + time + " at " + place + ": " + runners;
                })
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "getAllTimes", description = "Get all the race times for a given meeting place.")
    public String getAllTimes(String place) {
        JsonArray races = getCachedRaceData();
        if (races == null) return "Error: Race data is not in the expected format.";

        String times = StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.get("place").getAsString().equalsIgnoreCase(place))
                .map(race -> race.get("time").getAsString())
                .sorted()
                .collect(Collectors.joining(", "));

        return times.isEmpty()
                ? "No race times found for meeting at " + place
                : "Race times for " + place + ": " + times;
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
