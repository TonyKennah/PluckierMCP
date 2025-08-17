package uk.co.kennah.mcp.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import uk.co.kennah.mcp.gcp.GCSReader;

public class Util {
    // Local record for temporary data holding
    private record HorseAverageRating(String name, double average) {
    }

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

    public static String findHorseByAverageRating(String time, String place, GCSReader gcsReader,
            Optional<Integer> limit, boolean findMax, String description, String failureMessage) {
        return Util.findRace(time, place, gcsReader)
                .map(race -> {
                    Stream<HorseAverageRating> ratingsStream = StreamSupport
                            .stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(JsonElement::getAsJsonObject)
                            .map(horse -> new HorseAverageRating(
                                    horse.get("name").getAsString(),
                                    calculateAverageRating(horse, limit)))
                            .filter(h -> h.average() >= 0);

                    Optional<HorseAverageRating> result;
                    if (findMax) {
                        result = ratingsStream.max(Comparator.comparingDouble(HorseAverageRating::average));
                    } else {
                        result = ratingsStream.min(Comparator.comparingDouble(HorseAverageRating::average));
                    }

                    return result
                            .map(horse -> description + " for the " + time + " at " + place + " is: " + horse.name()
                                    + " with an average rating of " + String.format("%.2f", horse.average()))
                            .orElse(failureMessage + " for the race at " + place + " at " + time);
                })
                .orElse("Could not find the race at " + place + " at " + time);
    }

    public static String findNap(GCSReader gcsReader, Predicate<JsonObject> raceFilter, String successMessage,
            String failureMessage) {
        JsonArray races = getCachedRaceData(gcsReader);
        if (races == null) {
            return "Error: Race data is not available or in the expected format.";
        }

        // Local record for holding candidate horses
        record NapCandidate(String horseName, String time, String place, double averageRating) {
        }

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

    public static String findRaceWinPercentages(String time, String place, GCSReader gcsReader) {
        // Local record for temporary data holding
        record HorseRating(String name, int rating) {}

        return Util.findRace(time, place, gcsReader)
                .map(race -> {
                    java.util.List<HorseRating> horseRatings = StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(JsonElement::getAsJsonObject)
                            .map(horse -> {
                                String horseName = horse.get("name").getAsString();
                                if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                    return new HorseRating(horseName, 0);
                                }
                                return new HorseRating(horseName, Util.getMaxRating(horse).orElse(0));
                            })
                            .collect(Collectors.toList());

                    long totalRatingPool = horseRatings.stream().mapToLong(HorseRating::rating).sum();

                    if (totalRatingPool == 0) {
                        return "No rating data available to calculate win percentages for the race at " + place + " at " + time;
                    }

                    return "Win percentages for the " + time + " at " + place + ": " + horseRatings.stream()
                            .sorted(Comparator.comparing(HorseRating::rating).reversed())
                            .map(hr -> String.format("%s: %.2f%%", hr.name(), (hr.rating() / (double) totalRatingPool) * 100))
                            .collect(Collectors.joining(", "));
                })
                .orElse("Could not find the race at " + place + " at " + time);
    }

    public static String findBestMostRecentRatedHorse(String time, String place, GCSReader gcsReader) {
        // Local record for temporary data holding
        record HorseRecentRating(String name, int rating) {}
        return Util.findRace(time, place, gcsReader)
                .map(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .map(horse -> {
                            if (!horse.has("past") || !horse.get("past").isJsonArray()) {
                                return Optional.<HorseRecentRating>empty();
                            }
                            return Util.getMostRecentForm(horse)
                                .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                                .map(form -> new HorseRecentRating(horse.get("name").getAsString(), form.get("name").getAsInt()));
                        })
                        .flatMap(Optional::stream) // Filter out horses with no recent rated form
                        .max(Comparator.comparingInt(HorseRecentRating::rating))
                        .map(top -> "Horse with best most recent rating for the " + time + " at " + place + " is: " + top.name()
                                + " with a rating of " + top.rating())
                        .orElse("No horses with a recent rating found for the race at " + place + " at " + time))
                .orElse("Could not find the race at " + place + " at " + time);
    }


    public static String findBestEverRatedHorse(String time, String place, GCSReader gcsReader) {
        // Local record for temporary data holding
        record HorseRating(String name, int rating) {}

        return Util.findRace(time, place, gcsReader)
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

    public static String getFormDetails(JsonObject horse) {
        return  StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(form -> form.has("date") && form.has("name"))
                .sorted(Comparator
                        .comparing((JsonObject form) -> LocalDate.parse(form.get("date").getAsString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .reversed())
                .map(form -> "Date: " + form.get("date").getAsString() + ", Rating: " + form.get("name").getAsInt())
                .collect(Collectors.joining("; "));
    }

    public static String getTimes(JsonArray races, String place) {
        return StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.get("place").getAsString().equalsIgnoreCase(place))
                .map(race -> race.get("time").getAsString())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    public static Optional<JsonObject> getRaceOptional(JsonArray races) {
        return StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("time") && race.has("place"))
                .filter(race -> {
                    try {
                        return LocalTime.parse(race.get("time").getAsString(), DateTimeFormatter.ofPattern("HH:mm")).isAfter(LocalTime.now());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .min(Comparator.comparing(
                        race -> LocalTime.parse(race.get("time").getAsString(), DateTimeFormatter.ofPattern("HH:mm"))));

    }

    public static Optional<JsonObject> getMostRecentForm(JsonObject horse){
        return StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                    .map(JsonElement::getAsJsonObject)
                                    .filter(form -> form.has("date") && form.get("date").isJsonPrimitive())
                                    .max(Comparator.comparing(form -> LocalDate.parse(form.get("date").getAsString(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    public static OptionalInt getMaxRating(JsonObject horse){
        return StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                                        .map(JsonElement::getAsJsonObject)
                                        .filter(form -> form.has("name") && form.get("name").isJsonPrimitive())
                                        .mapToInt(form -> form.get("name").getAsInt())
                                        .max();
    }

    public static Optional<JsonObject> getSimpleHorseOptional(Optional<JsonObject> raceOptional, String horseName){
        return StreamSupport.stream(raceOptional.get().getAsJsonArray("horses").spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(h -> h.has("name") && h.get("name").getAsString().equalsIgnoreCase(horseName))
                .findFirst();
    }

    public static String getRunners(JsonObject race){
        return StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                            .map(horse -> horse.getAsJsonObject().get("name").getAsString())
                            .collect(Collectors.joining(", "));
    }

    public static Set<String> getMeetings(JsonArray races){
        return StreamSupport.stream(races.spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .filter(race -> race.has("place"))
                    .map(race -> race.get("place").getAsString())
                    .collect(Collectors.toSet());
    }

    public static String getResult(JsonArray races, String horseName){
        return StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("horses") && race.get("horses").isJsonArray()) // defensive check
                .filter(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .anyMatch(horse -> horse.has("name") && horse.get("name").getAsString().equalsIgnoreCase(horseName)))
                .map(race -> race.get("time").getAsString() + " at " + race.get("place").getAsString())
                .collect(Collectors.joining(", "));
    }

    public static Optional<JsonObject> getHorseOptional(JsonArray races, String horseName) {
        return StreamSupport.stream(races.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(race -> race.has("horses") && race.get("horses").isJsonArray())
                .flatMap(race -> StreamSupport.stream(race.getAsJsonArray("horses").spliterator(), false))
                .map(JsonElement::getAsJsonObject)
                .filter(horse -> horse.has("name") && horse.get("name").getAsString().equalsIgnoreCase(horseName))
                .findFirst();
    }

    public static String getDates(JsonObject horse) {
        return StreamSupport.stream(horse.getAsJsonArray("past").spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(pastRace -> pastRace.has("date"))
                .map(pastRace -> pastRace.get("date").getAsString())
                .distinct()
                .sorted(Comparator
                        .comparing(
                                (String dateStr) -> LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .reversed())
                .collect(Collectors.joining(", "));
    }

    public static String findAllRunners(String time, String place, GCSReader gcsReader) {
        return Util.findRace(time, place, gcsReader)
                .map(race -> {
                    String runners = Util.getRunners(race);
                    return runners.isEmpty() ? "No runners found for the race at " + place + " at " + time
                            : "Runners for the " + time + " at " + place + ": " + runners;
                })
                .orElse("Could not find the race at " + place + " at " + time);
    }

}
