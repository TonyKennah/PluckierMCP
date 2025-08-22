package uk.co.kennah.mcp.gcp;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.gson.*;

@Component
public class GCSHorseReader {

    private static final Logger logger = LoggerFactory.getLogger(GCSHorseReader.class);

    @Value("${gcs.bucket.name}")
    private String bucket;

    @Value("${gcs.file.name}")
    private String file;

    @Value("${gcs.oddsfile.name}")
    private String oddsFile;

    @Autowired
    private Storage storage;

    @Cacheable("raceData")
    public JsonElement readFileFromGCSAsJson() {
        logger.info("Reading all of today's horse racing data to cache.");
        try {
            BlobId blobId = BlobId.of(bucket, file);
            Blob blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                logger.error("File '{}' not found in GCS bucket '{}'", file, bucket);
                return JsonParser.parseString("{\"error\": \"File not found in bucket '" + bucket + "'\"}");
            }
            byte[] content = blob.getContent();
            JsonElement races = JsonParser.parseString(new String(content, StandardCharsets.UTF_8));
            JsonElement odds = readOddsFileFromGCSAsJson();

            races = updateRacesWithNewOdds(races, odds);

            return races;
        } catch (StorageException e) {
            logger.error("Error reading from GCS", e);
            return JsonParser.parseString("{\"error\": \"Error reading from GCS: " + e.getMessage() + "\"}");
        }
    }

    private JsonElement readOddsFileFromGCSAsJson() {
        logger.info("Updating horse racing data with latest odds.");
        try {
            BlobId blobId = BlobId.of(bucket, oddsFile);
            Blob blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                logger.error("File '{}' not found in GCS bucket '{}'", oddsFile, bucket);
                return JsonParser.parseString("{\"error\": \"File not found in bucket '" + bucket + "'\"}");
            }
            byte[] content = blob.getContent();
            return JsonParser.parseString(new String(content, StandardCharsets.UTF_8));
        } catch (StorageException e) {
            logger.error("Error reading from GCS", e);
            return JsonParser.parseString("{\"error\": \"Error reading from GCS: " + e.getMessage() + "\"}");
        }
    }

    private JsonElement updateRacesWithNewOdds(JsonElement races, JsonElement odds) {
        // here we need to update the odds attribute on each horse in each of the races
        // the odds are listed in the JsonElement "odds"
        // Check if we have a valid array of races and a valid array for odds
        if (races.isJsonArray() && odds.isJsonArray()) {
            // Create a map of horse names to their odds for efficient lookup
            Map<String, JsonElement> oddsMap = new HashMap<>();
            for (JsonElement oddsElement : odds.getAsJsonArray()) {
                if (oddsElement.isJsonObject()) {
                    JsonObject oddsObject = oddsElement.getAsJsonObject();
                    if (oddsObject.has("name") && oddsObject.get("name").isJsonPrimitive()) {
                        String horseName = oddsObject.get("name").getAsString();
                        // If odds are missing, it's a non-runner (NR).
                        JsonElement oddsValue = oddsObject.has("odds")
                                ? oddsObject.get("odds")
                                : new JsonPrimitive("NR");
                        oddsMap.put(horseName, oddsValue);
                    }
                }
            }

            JsonArray racesArray = races.getAsJsonArray();
            // Iterate over each race in the array
            for (JsonElement raceElement : racesArray) {
                if (raceElement.isJsonObject()) {
                    JsonObject raceObject = raceElement.getAsJsonObject();

                    // Check if the race has a "horses" array
                    if (raceObject.has("horses") && raceObject.get("horses").isJsonArray()) {
                        JsonArray horsesArray = raceObject.getAsJsonArray("horses");
                        // Iterate over each horse in the race
                        for (JsonElement horseElement : horsesArray) {
                            if (horseElement.isJsonObject()) {
                                JsonObject horseObject = horseElement.getAsJsonObject();
                                // Get the horse's name to use as a key for the odds lookup
                                if (horseObject.has("name") && horseObject.get("name").isJsonPrimitive()) {
                                    String horseName = horseObject.get("name").getAsString();
                                    // Find the odds for this horse and add it to the horse object
                                    if (oddsMap.containsKey(horseName)) {
                                        horseObject.add("odds", oddsMap.get(horseName));
                                    } else if (oddsMap.containsKey(horseName.replace("'", ""))) {
                                        horseObject.add("odds", oddsMap.get(horseName.replace("'", "")));
                                    } else if (oddsMap.containsKey(horseName.toUpperCase())) {
                                        horseObject.add("odds", oddsMap.get(horseName.toUpperCase()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return races;
    }

}
