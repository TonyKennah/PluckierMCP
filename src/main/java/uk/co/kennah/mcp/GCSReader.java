package uk.co.kennah.mcp;

import java.beans.JavaBean;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.gson.*;

@Component
public class GCSReader {

    @Value("${gcs.bucket.name}")
    private String bucket;

    @Value("${gcs.file.name}")
    private String file;

    @Autowired
    private Environment env;

    @Autowired
    private Storage storage;

    public String readFileFromGCS() {
        try {
            System.out.println(bucket);
            System.out.println(file);
            System.out.println(env.getProperty("gcs.bucket.name"));
            System.out.println(env.getProperty("gcs.file.name"));

            BlobId blobId = BlobId.of(env.getProperty("gcs.bucket.name"), env.getProperty("gcs.file.name"));
            Blob blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                return "Error: File not found in bucket ";
            }
            byte[] content = blob.getContent();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement je = JsonParser.parseString(new String(content, StandardCharsets.UTF_8));
            String prettyJsonString = gson.toJson(je);
            return prettyJsonString;
        } catch (StorageException e) {
            // Consider adding logging here to see the full stack trace
            return "Error reading from GCS: " + e.getMessage();
        }
    }

}
