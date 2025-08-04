package uk.co.kennah.mcp;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class RacesInfo {

    private final Storage storage;

    public RacesInfo(Storage storage) {
        this.storage = storage;
    }

    @Tool(name = "getAllMeetings", description = "Retrieve all of the meeting names.")
    public String getMeetings() {
        return "List of meetings: " + " Ascot, Chester, Epsom";
    }

    @Tool(name = "getTopRated", description = "Get the top rated horse for a particular race.")
    public String getTopRated(String time, String place) {
        return "Top Rated is: " + "RedRum";
    }

    @Tool(name = "readFileFromGCS", description = "Reads a file from a Google Cloud Storage bucket.")
    public String readFileFromGCS(String bucketName, String fileName) {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            Blob blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                return "Error: File not found in bucket '" + bucketName + "': " + fileName;
            }
            byte[] content = blob.getContent();
            return new String(content, StandardCharsets.UTF_8);
        } catch (StorageException e) {
            // Consider adding logging here to see the full stack trace
            return "Error reading from GCS: " + e.getMessage();
        }
    }

    
}
