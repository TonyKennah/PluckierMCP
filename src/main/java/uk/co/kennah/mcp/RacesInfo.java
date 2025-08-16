package uk.co.kennah.mcp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(RacesInfo.class);

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


    @Tool(name = "get_best_ever_rated", description = "Get the best rated horse for a particular race, identified by its time and place. This is the highest single rating from any past race.")
    public String getBestEverRated(String time, String place) {
        logger.info("AI tool call for best ever rated horse in the {} at {}", time, place);
        // Local record for temporary data holding
        record HorseRating(String name, int rating) {}

        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .filter(horse -> horse.has("past") && horse.get("past").isJsonArray())
                        .flatMap(horse -> StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                .map(JsonElement::getAsJsonObject)
                                .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                                .map(form -> new HorseRating(horse.get("name").getAsString(), form.get("name").getAsInt())))
                        .max(Comparator.comparingInt(HorseRating::rating))
                        .map(top -> "Top Rated for the " + time + " at " + place + " is: " + top.name() + " with a rating of " + top.rating())
                        .orElse("No rated horses found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "get_top_rated", description = "Get the horse with the best average rating over last 3 runs for a particular race, identified by its time and place.")
    public String getTopRated(String time, String place) {
        logger.info("AI tool call for top rated (last 3 runs) horse in the {} at {}", time, place);
        // Local record for temporary data holding
        record HorseAverageRating(String name, int average) {}

        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .map(horse -> {
                            if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                return new HorseAverageRating(horse.get("name").getAsString(), -1);
                            }
                            IntSummaryStatistics stats = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .limit(3)
                                    .filter(form -> form.has("name"))
                                    .mapToInt(form -> form.get("name").getAsInt())
                                    .summaryStatistics();
                            return new HorseAverageRating(horse.get("name").getAsString(), stats.getCount() > 0 ? (int) stats.getAverage() : -1);
                        })
                        .filter(h -> h.average() >= 0)
                        .max(Comparator.comparingDouble(HorseAverageRating::average))
                        .map(top -> "Horse with best last 3 run average rating for the " + time + " at " + place + " is: " + top.name()
                                + " with an average rating of " + String.format("%.2f", top.average()))
                        .orElse("No horses with a recent average rating found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "get_bottom_rated", description = "Get the horse with the worst average rating over last 3 runs (the fiddle) for a particular race, identified by its time and place.")
    public String getBottomRated(String time, String place) {
        logger.info("AI tool call for bottom rated (last 3 runs) horse in the {} at {}", time, place);
        // Local record for temporary data holding
        record HorseAverageRating(String name, int average) {}

        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .map(horse -> {
                            if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                return new HorseAverageRating(horse.get("name").getAsString(), -1);
                            }
                            IntSummaryStatistics stats = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .limit(3)
                                    .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                                    .mapToInt(form -> form.get("name").getAsInt())
                                    .summaryStatistics();
                            return new HorseAverageRating(horse.get("name").getAsString(), stats.getCount() > 0 ? (int)stats.getAverage() : -1);
                        })
                        .filter(h -> h.average() >= 0)
                        .min(Comparator.comparingDouble(HorseAverageRating::average))
                        .map(top -> "Horse with worst last 3 run average rating for the " + time + " at " + place + " is: " + top.name()
                                + " with an average rating of " + String.format("%.2f", top.average()))
                        .orElse("No horses with a recent average rating found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "get_best_average_rated", description = "Get the horse with the best average rating for a particular race, identified by its time and place.")
    public String getBestAverageRated(String time, String place) {
        logger.info("AI tool call for best average rated horse in the {} at {}", time, place);
        // Local record for temporary data holding
        record HorseAverageRating(String name, double average) {}

        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .map(horse -> {
                            if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                return new HorseAverageRating(horse.get("name").getAsString(), -1);
                            }
                            IntSummaryStatistics stats = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
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

    @Tool(name = "get_best_most_recent_rated", description = "Get the horse with the highest rating from its most recent race, for a particular race identified by its time and place.")
    public String getBestMostRecentRated(String time, String place) {
        logger.info("AI tool call for best most recent rated horse in the {} at {}", time, place);
        // Local record for temporary data holding
        record HorseRecentRating(String name, int rating) {}
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


        return findRace(time, place)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .map(horse -> {
                            if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                return Optional.<HorseRecentRating>empty();
                            }
                            Optional<JsonObject> mostRecentForm = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .filter(form -> form.has("date") && form.get("date").isJsonPrimitive())
                                    .max(Comparator.comparing(form -> LocalDate.parse(form.get("date").getAsString(), formatter)));
                            
                                    return mostRecentForm.filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                                    .map(form -> new HorseRecentRating(horse.get("name").getAsString(), form.get("name").getAsInt()));
                        })
                        .flatMap(Optional::stream) // Filter out horses with no recent rated form
                        .max(Comparator.comparingInt(HorseRecentRating::rating))
                        .map(top -> "Horse with best most recent rating for the " + time + " at " + place + " is: " + top.name()
                                + " with a rating of " + top.rating())
                        .orElse("No horses with a recent rating found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }

    @Tool(name = "get_all_runners", description = "Get all the runners for a particular race, identified by its time and place.")
    public String getAllRunners(String time, String place) {
        logger.info("AI tool call for all runners in the {} at {}", time, place);
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

    @Tool(name = "get_past_run_dates", description = "Get all the past race dates for a given horse name.")
    public String getPastRunDates(String horseName) {
        logger.info("AI tool call for past run dates for horse: {}", horseName);
        JsonArray races = getCachedRaceData();
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Find the first occurrence of the horse, as its past data should be consistent.
        Optional<JsonObject> horseOptional = StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("horses") && race.get("horses").isJsonArray())
                .flatMap(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false))
                .map(JsonElement::getAsJsonObject)
                .filter(horse -> horse.has("name") && horse.get("name").getAsString().equalsIgnoreCase(horseName))
                .findFirst();

        if (horseOptional.isEmpty()) {
            return "Could not find a horse named: " + horseName;
        }

        JsonObject horse = horseOptional.get();
        if (!horse.has("past") || !horse.get("past").isJsonArray() || horse.getAsJsonArray("past").isEmpty()) {
            return "No past race data found for horse: " + horseName;
        }

        String dates = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(pastRace -> pastRace.has("date"))
                .map(pastRace -> pastRace.get("date").getAsString())
                .distinct()
                .sorted(Comparator.comparing((String dateStr) -> LocalDate.parse(dateStr, formatter)).reversed())
                .collect(Collectors.joining(", "));

        return "Past race dates for " + horseName + ": " + dates;
    }

    @Tool(name = "get_all_times", description = "Get all the race times for a given meeting place.")
    public String getAllTimes(String place) {
        logger.info("AI tool call for all race times at {}", place);
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

    @Tool(name = "get_meetings", description = "Retrieve all unique meeting place names from the race data.")
    public String getMeetings() {
        logger.info("AI tool call for all meeting places");
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

    @Tool(name = "find_horse_race", description = "Finds the race time and meeting for a given horse name.")
    public String findHorseRace(String horseName) {
        logger.info("AI tool call to find race for horse: {}", horseName);
        JsonArray races = getCachedRaceData();
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }

        String result = StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("horses") && race.get("horses").isJsonArray()) // defensive check
                .filter(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .anyMatch(horse -> horse.has("name") && horse.get("name").getAsString().equalsIgnoreCase(horseName)))
                .map(race -> race.get("time").getAsString() + " at " + race.get("place").getAsString())
                .collect(Collectors.joining(", "));

        if (result.isEmpty()) {
            return "Could not find any races for horse: " + horseName;
        }

        return horseName + " is running in: " + result;
    }

    @Tool(name = "get_next_race", description = "Reports the next race time and meeting based on the current system time.")
    public String getNextRace() {
        logger.info("AI tool call for the next race");
        JsonArray races = getCachedRaceData();
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }

        LocalTime now = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Optional<JsonObject> nextRaceOptional = StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("time") && race.has("place"))
                .filter(race -> {
                    try {
                        return LocalTime.parse(race.get("time").getAsString(), timeFormatter).isAfter(now);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .min(Comparator.comparing(race -> LocalTime.parse(race.get("time").getAsString(), timeFormatter)));

        return nextRaceOptional
                .map(race -> "The next race is at " + race.get("time").getAsString() + " at " + race.get("place").getAsString() + ".")
                .orElse("There are no more races scheduled for today.");
    }

    @Tool(name = "get_horse_form", description = "Get the recent form (past race dates and ratings) for a specific horse in a particular race.")
    public String getHorseForm(String time, String place, String horseName) {
        logger.info("AI tool call for form for horse {} in the {} at {}", horseName, time, place);
        Optional<JsonObject> raceOptional = findRace(time, place);
        if (raceOptional.isEmpty()) {
            return "Could not find the race at " + place + " at " + time;
        }

        Optional<JsonObject> horseOptional = StreamSupport.stream(raceOptional.get().getAsJsonArray("horses").spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(h -> h.has("name") && h.get("name").getAsString().equalsIgnoreCase(horseName))
                .findFirst();

        if (horseOptional.isEmpty()) {
            return "Could not find horse " + horseName + " in the " + time + " at " + place;
        }

        JsonObject horse = horseOptional.get();
        if (!horse.has("past") || !horse.get("past").isJsonArray() || horse.getAsJsonArray("past").isEmpty()) {
            return "No past race data found for horse: " + horseName;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formDetails = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(form -> form.has("date") && form.has("name"))
                .sorted(Comparator.comparing((JsonObject form) -> LocalDate.parse(form.get("date").getAsString(), formatter)).reversed())
                .map(form -> "Date: " + form.get("date").getAsString() + ", Rating: " + form.get("name").getAsInt())
                .collect(Collectors.joining("; "));

        if (formDetails.isEmpty()) {
            return "No valid past performance data found for " + horseName;
        }

        return "Form for " + horseName + ": " + formDetails;
    }

    @Tool(name = "get_nap_of_the_day", description = "Find the best bet of the day across all races, based on the highest average rating over the last 3 runs.")
    public String getNapOfTheDay() {
        logger.info("AI tool call for Nap of the Day");
        JsonArray races = getCachedRaceData();
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }

        // Local record for holding candidate horses
        record NapCandidate(String horseName, String time, String place, double averageRating) {}

        Optional<NapCandidate> bestBet = StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("horses") && race.get("horses").isJsonArray())
                .flatMap(race -> {
                    String time = race.get("time").getAsString();
                    String place = race.get("place").getAsString();
                    return StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(JsonElement::getAsJsonObject)
                            .map(horse -> {
                                if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                    return new NapCandidate(horse.get("name").getAsString(), time, place, -1);
                                }
                                IntSummaryStatistics stats = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                        .map(JsonElement::getAsJsonObject)
                                        .limit(3)
                                        .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                                        .mapToInt(form -> form.get("name").getAsInt())
                                        .summaryStatistics();
                                double average = stats.getCount() > 0 ? stats.getAverage() : -1;
                                return new NapCandidate(horse.get("name").getAsString(), time, place, average);
                            });
                })
                .filter(candidate -> candidate.averageRating() >= 0)
                .max(Comparator.comparingDouble(NapCandidate::averageRating));

        return bestBet
                .map(nap -> String.format("The nap of the day is %s in the %s at %s, with a recent average rating of %.2f.",
                        nap.horseName(), nap.time(), nap.place(), nap.averageRating()))
                .orElse("Could not determine a nap of the day from the available data.");
    }

    @Tool(name = "get_handicap_nap_of_the_day", description = "Find the best bet of the day from handicap races only, based on the highest average rating over the last 3 runs.")
    public String getHandicapNapOfTheDay() {
        logger.info("AI tool call for Handicap Nap of the Day");
        JsonArray races = getCachedRaceData();
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }

        // Local record for holding candidate horses
        record NapCandidate(String horseName, String time, String place, double averageRating) {}

        Optional<NapCandidate> bestBet = StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("horses") && race.get("horses").isJsonArray())
                // Filter for Handicap races only. Detail contains handicap.
                .filter(race -> race.has("detail") && race.get("detail").getAsString().toLowerCase().contains("handicap"))
                .flatMap(race -> {
                    String time = race.get("time").getAsString();
                    String place = race.get("place").getAsString();
                    return StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(JsonElement::getAsJsonObject)
                            .map(horse -> {
                                if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                    return new NapCandidate(horse.get("name").getAsString(), time, place, -1);
                                }
                                IntSummaryStatistics stats = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                        .map(JsonElement::getAsJsonObject)
                                        .limit(3)
                                        .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                                        .mapToInt(form -> form.get("name").getAsInt())
                                        .summaryStatistics();
                                double average = stats.getCount() > 0 ? stats.getAverage() : -1;
                                return new NapCandidate(horse.get("name").getAsString(), time, place, average);
                            });
                })
                .filter(candidate -> candidate.averageRating() >= 0)
                .max(Comparator.comparingDouble(NapCandidate::averageRating));

        return bestBet
                .map(nap -> String.format("The handicap nap of the day is %s in the %s at %s, with a recent average rating of %.2f.",
                        nap.horseName(), nap.time(), nap.place(), nap.averageRating()))
                .orElse("Could not determine a nap of the day from today's handicap races.");
    }
    
}
