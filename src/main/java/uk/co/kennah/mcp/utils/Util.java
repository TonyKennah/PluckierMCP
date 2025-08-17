package uk.co.kennah.mcp.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import uk.co.kennah.mcp.gcp.GCSReader;

public class Util {
    // Local record for temporary data holding
    private record HorseAverageRating(String name, double average) {}

    public static String formatTime(String time) {
        // Assuming time is in HH:mm format
        return time.replace(":", "");
    }

    public static JsonArray getCachedRaceData(GCSReader gcsReader) {
        JsonElement jsonElement = gcsReader.readFileFromGCSAsJson();
        if (jsonElement == null || !jsonElement.isJsonArray()) {
            // This case will be handled by the calling methods if they receive null.
            return null;
        }
        return jsonElement.getAsJsonArray();
    }

    public static Optional<JsonObject> findRace(String time, String place, GCSReader gcsReader) {
        JsonArray races = getCachedRaceData(gcsReader);
        if (races == null) {
            return Optional.empty();
        }
        return StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.get("place").getAsString().equalsIgnoreCase(place)
                        && race.get("time").getAsString().equals(time))
                .findFirst();
    }

    public static double calculateAverageRating(JsonObject horse, Optional<Integer> limit) {
        if (!horse.has("past") || !horse.get("past").isJsonArray()) {
            return -1;
        }
        var pastStream = StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                .map(JsonElement::getAsJsonObject);

        var limitedStream = limit.map(pastStream::limit).orElse(pastStream);

        IntSummaryStatistics stats = limitedStream
                .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                .mapToInt(form -> form.get("name").getAsInt())
                .summaryStatistics();
        
        return stats.getCount() > 0 ? stats.getAverage() : -1;
    }

    public static String findHorseByAverageRating(String time, String place, GCSReader gcsReader, Optional<Integer> limit, boolean findMax, String description, String failureMessage) {
        return Util.findRace(time, place, gcsReader)
                .map(race -> {
                    Stream<HorseAverageRating> ratingsStream = StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(JsonElement::getAsJsonObject)
                            .map(horse -> new HorseAverageRating(
                                    horse.get("name").getAsString(),
                                    calculateAverageRating(horse, limit)
                            ))
                            .filter(h -> h.average() >= 0);

                    Optional<HorseAverageRating> result;
                    if (findMax) {
                        result = ratingsStream.max(Comparator.comparingDouble(HorseAverageRating::average));
                    } else {
                        result = ratingsStream.min(Comparator.comparingDouble(HorseAverageRating::average));
                    }

                    return result.map(horse -> description + " for the " + time + " at " + place + " is: " + horse.name()
                                    + " with an average rating of " + String.format("%.2f", horse.average()))
                            .orElse(failureMessage + " for the race at " + place + " at " + time);
                })
                .orElse("Could not find the race at " + place + " at " + time);
    }

    public static String findNap(GCSReader gcsReader, Predicate<JsonObject> raceFilter, String successMessage, String failureMessage) {
        JsonArray races = getCachedRaceData(gcsReader);
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }

        // Local record for holding candidate horses
        record NapCandidate(String horseName, String time, String place, double averageRating) {}

        Optional<NapCandidate> bestBet = StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("horses") && race.get("horses").isJsonArray())
                .filter(raceFilter) // Apply the specific filter
                .flatMap(race -> {
                    String time = race.get("time").getAsString();
                    String place = race.get("place").getAsString();
                    return StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(JsonElement::getAsJsonObject)
                            .map(horse -> {
                                double average = Util.calculateAverageRating(horse, Optional.of(3));
                                return new NapCandidate(horse.get("name").getAsString(), time, place, average);
                            });
                })
                .filter(candidate -> candidate.averageRating() >= 0)
                .max(Comparator.comparingDouble(NapCandidate::averageRating));

        return bestBet
                .map(nap -> String.format(successMessage,
                        nap.horseName(), nap.time(), nap.place(), nap.averageRating()))
                .orElse(failureMessage);
    }
}
