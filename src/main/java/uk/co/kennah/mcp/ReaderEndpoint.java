package uk.co.kennah.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.cloud.storage.StorageException;

@RestController
public class ReaderEndpoint {

    @Autowired
    private GCSReader gcsReader;

    @GetMapping("/info")
    public String getInfo() {
        try {
            return gcsReader.readFileFromGCS();
        } catch (StorageException e) {
            return "Error reading from GCS: " + e.getMessage();
        }
    }
}