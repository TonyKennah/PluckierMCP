package uk.co.kennah.mcp;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.gson.*;

@Component
public class GCSReader {

    private static final Logger logger = LoggerFactory.getLogger(GCSReader.class);
    
    @Value("${gcs.bucket.name}")
    private String bucket;

    @Value("${gcs.file.name}")
    private String file;

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
            return JsonParser.parseString(new String(content, StandardCharsets.UTF_8));
        } catch (StorageException e) {
            logger.error("Error reading from GCS", e);
            return JsonParser.parseString("{\"error\": \"Error reading from GCS: " + e.getMessage() + "\"}");
        }
    }

}
