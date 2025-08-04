package uk.co.kennah.mcp;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.StorageException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import com.google.gson.*;


@RestController
public class Something {

    @Autowired
    private Storage storage;


    @GetMapping("/info")
    public String getInfo() {


                try {
            BlobId blobId = BlobId.of("pluckier.appspot.com", "sample_races.json");
            Blob blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                return "Error: File not found in bucket ";
            }
            byte[] content = blob.getContent();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(new String(content, StandardCharsets.UTF_8));
            String prettyJsonString = gson.toJson(je);
            return prettyJsonString;
        } catch (StorageException e) {
            // Consider adding logging here to see the full stack trace
            return "Error reading from GCS: " + e.getMessage();
        }

    }
}