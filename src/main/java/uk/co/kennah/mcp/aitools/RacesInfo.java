package uk.co.kennah.mcp.aitools;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import uk.co.kennah.mcp.gcp.GCSReader;
import uk.co.kennah.mcp.utils.Util;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class RacesInfo {

    private static final Logger logger = LoggerFactory.getLogger(RacesInfo.class);

    @Autowired
    private GCSReader gcsReader;

    private JsonArray getCachedRaceData() {
        return Util.getCachedRaceData(gcsReader);
    }

    @Tool(name = "get_nap_of_the_day", description = "Find the best bet of the day across all races, based on the highest average rating over the last 3 runs.")
    public String getNapOfTheDay() {
        logger.info("AI tool call for Nap of the Day");
        return Util.findNap(gcsReader,
                race -> true, // No filter, include all races
                "The nap of the day is %s in the %s at %s, with a recent average rating of %.2f.",
                "Could not determine a nap of the day from the available data.");
    }

    @Tool(name = "get_handicap_nap_of_the_day", description = "Find the best bet of the day from handicap races only, based on the highest average rating over the last 3 runs.")
    public String getHandicapNapOfTheDay() {
        logger.info("AI tool call for Handicap Nap of the Day");
        Predicate<JsonObject> handicapFilter = race -> race.has("detail")
                && race.get("detail").getAsString().toLowerCase().contains("handicap");

        return Util.findNap(gcsReader,
                handicapFilter,
                "The handicap nap of the day is %s in the %s at %s, with a recent average rating of %.2f.",
                "Could not determine a nap of the day from today's handicap races.");
    }

    @Tool(name = "get_uk_handicap_nap_of_the_day", description = "Find the best bet of the day from UK handicap races only, based on the highest average rating over the last 3 runs.")
    public String getUkHandicapNapOfTheDay() {
        logger.info("AI tool call for UK Handicap Nap of the Day");
        Predicate<JsonObject> ukHandicapFilter = race -> {
            boolean isHandicap = race.has("detail") && race.get("detail").getAsString().toLowerCase().contains("handicap");
            boolean isUk = race.has("country") && "UK".equalsIgnoreCase(race.get("country").getAsString());
            return isHandicap && isUk;
        };

        return Util.findNap(gcsReader,
                ukHandicapFilter,
                "The UK handicap nap of the day is %s in the %s at %s, with a recent average rating of %.2f.",
                "Could not determine a nap of the day from today's UK handicap races.");
    }

    @Tool(name = "get_top_rated", description = "Get the horse with the best average rating over last 3 runs for a particular race, identified by its time and place.")
    public String getTopRated(String time, String place) {
        logger.info("AI tool call for top rated (last 3 runs) horse in the {} at {}", time, place);
        return Util.findHorseByAverageRating(time, place, gcsReader, Optional.of(3), true,
                "Horse with best last 3 run average rating",
                "No horses with a recent average rating found");
    }

    @Tool(name = "get_bottom_rated", description = "Get the horse with the worst average rating over last 3 runs (the fiddle) for a particular race, identified by its time and place.")
    public String getBottomRated(String time, String place) {
        logger.info("AI tool call for bottom rated (last 3 runs) horse in the {} at {}", time, place);
        return Util.findHorseByAverageRating(time, place, gcsReader, Optional.of(3), false,
                "Horse with worst last 3 run average rating",
                "No horses with a recent average rating found");
    }

    @Tool(name = "get_best_average_rated", description = "Get the horse with the best average rating for a particular race, identified by its time and place.")
    public String getBestAverageRated(String time, String place) {
        logger.info("AI tool call for best average rated horse in the {} at {}", time, place);
        return Util.findHorseByAverageRating(time, place, gcsReader, Optional.empty(), true,
                "Horse with best average rating",
                "No horses with an average rating found");
    }

    @Tool(name = "get_best_ever_rated", description = "Get the best rated horse for a particular race, identified by its time and place. This is the highest single rating from any past race.")
    public String getBestEverRated(String time, String place) {
        logger.info("AI tool call for best ever rated horse in the {} at {}", time, place);
        return Util.findBestEverRatedHorse(time, place, gcsReader);
    }

    @Tool(name = "get_best_most_recent_rated", description = "Get the horse with the highest rating from its most recent race, for a particular race identified by its time and place.")
    public String getBestMostRecentRated(String time, String place) {
        logger.info("AI tool call for best most recent rated horse in the {} at {}", time, place);
        return Util.findBestMostRecentRatedHorse(time, place, gcsReader);
    }

    @Tool(name = "get_race_win_percentages_from_last_one", description = "Calculates the win percentage for each horse in a race based on their last run.")
    public String getRaceWinPercentagesFromLastOne(String time, String place) {
        logger.info("AI tool call for race win percentages from last run in the {} at {}", time, place);
        return Util.findRaceWinPercentagesFromLastOne(time, place, gcsReader);
    }

    @Tool(name = "get_race_win_percentages_from_last_three", description = "Calculates the win percentage for each horse in a race based on their average over the last three runs.")
    public String getRaceWinPercentagesFromLastThree(String time, String place) {
        logger.info("AI tool call for race win percentages from last three runs in the {} at {}", time, place);
        return Util.findRaceWinPercentagesFromLastThree(time, place, gcsReader);
    }

    @Tool(name = "get_race_win_percentages_from_best_ever", description = "Calculates the win percentage for each horse in a race based on their best-ever rating.")
    public String getRaceWinPercentagesFromBestEver(String time, String place) {
        logger.info("AI tool call for race win percentages from best ever performance in the {} at {}", time, place);
        return Util.findRaceWinPercentagesFromBestEver(time, place, gcsReader);
    }

    @Tool(name = "get_race_win_percentages_from_all", description = "Calculates the win percentage for each horse in a race based on all their past ratings.")
    public String getRaceWinPercentagesFromAll(String time, String place) {
        logger.info("AI tool call for race win percentages from all past performance in the {} at {}", time, place);
        return Util.findRaceWinPercentagesFromAll(time, place, gcsReader);
    }

    @Tool(name = "get_all_runners", description = "Get all the runners for a particular race, identified by its time and place.")
    public String getAllRunners(String time, String place) {
        logger.info("AI tool call for all runners in the {} at {}", time, place);
        return Util.findAllRunners(time, place, gcsReader);
    }

    @Tool(name = "get_all_runners_with_odds", description = "Get all the runners for a particular race with their current odds, identified by its time and place.")
    public String getAllRunnersWithOdds(String time, String place) {
        logger.info("AI tool call for all runners with odds in the {} at {}", time, place);
        return Util.findAllRunnersWithOdds(time, place, gcsReader);
    }

    @Tool(name = "get_past_run_dates", description = "Get all the past race dates for a given horse name.")
    public String getPastRunDates(String horseName) {
        logger.info("AI tool call for past run dates for horse: {}", horseName);
        JsonArray races = getCachedRaceData();
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }
        // Find the first occurrence of the horse, as its past data should be consistent.
        Optional<JsonObject> horseOptional = Util.getHorseOptional(races, horseName);
        if (horseOptional.isEmpty()) {
            return "Could not find a horse named: " + horseName;
        }
        JsonObject horse = horseOptional.get();
        if (!horse.has("past") || !horse.get("past").isJsonArray() || horse.getAsJsonArray("past").isEmpty()) {
            return "No past race data found for horse: " + horseName;
        }
        return "Past race dates for " + horseName + ": " + Util.getDates(horse);
    }

    @Tool(name = "get_all_times", description = "Get all the race times for a given meeting place.")
    public String getAllTimes(String place) {
        logger.info("AI tool call for all race times at {}", place);
        JsonArray races = getCachedRaceData();
        if (races == null) return "Error: Race data is not in the expected format.";
        String times = Util.getTimes(races, place);
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
            Set<String> meetings = Util.getMeetings(races);
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
        String result = Util.getResult(races, horseName);
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
        Optional<JsonObject> nextRaceOptional = Util.getRaceOptional(races);
        return nextRaceOptional
                .map(race -> "The next race is at " + race.get("time").getAsString() + " at " + race.get("place").getAsString() + ".")
                .orElse("There are no more races scheduled for today.");
    }

    @Tool(name = "get_horse_form", description = "Get the recent form (past race dates and ratings) for a specific horse in a particular race.")
    public String getHorseForm(String time, String place, String horseName) {
        logger.info("AI tool call for form for horse {} in the {} at {}", horseName, time, place);
        Optional<JsonObject> raceOptional = Util.findRace(time, place, gcsReader);
        if (raceOptional.isEmpty()) {
            return "Could not find the race at " + place + " at " + time;
        }
        Optional<JsonObject> horseOptional = Util.getSimpleHorseOptional(raceOptional, horseName);
        if (horseOptional.isEmpty()) {
            return "Could not find horse " + horseName + " in the " + time + " at " + place;
        }
        JsonObject horse = horseOptional.get();
        if (!horse.has("past") || !horse.get("past").isJsonArray() || horse.getAsJsonArray("past").isEmpty()) {
            return "No past race data found for horse: " + horseName;
        }
        String formDetails = Util.getFormDetails(horse);
        if (formDetails.isEmpty()) {
            return "No valid past performance data found for " + horseName;
        }
        return "Form for " + horseName + ": " + formDetails;
    }
}
