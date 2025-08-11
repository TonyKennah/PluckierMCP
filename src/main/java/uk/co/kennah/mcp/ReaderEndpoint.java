package uk.co.kennah.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReaderEndpoint {

    @Autowired
    private RacesInfo racesInfo;

    @GetMapping("/info")
    public String getInfo() {
        return racesInfo.getRawRaceData();
    }

    @GetMapping("/meetings")
    public String getMeetings() {
        return racesInfo.getMeetings();
    }

    @GetMapping("/top-rated")
    public String getTopRated(@RequestParam String time, @RequestParam String place) {
        return racesInfo.getTopRated(time, place);
    }

    @GetMapping("/best-average-rated")
    public String getBestAverageRated(@RequestParam String time, @RequestParam String place) {
        return racesInfo.getBestAverageRated(time, place);
    }

    @GetMapping("/best-most-recent-rated")
    public String getBestMostRecentRated(@RequestParam String time, @RequestParam String place) {
        return racesInfo.getBestMostRecentRated(time, place);
    }

    @GetMapping("/all-runners")
    public String getAllRunners(@RequestParam String time, @RequestParam String place) {
        return racesInfo.getAllRunners(time, place);
    }

    @GetMapping("/all-times")
    public String getAllTimes(@RequestParam String place) {
        return racesInfo.getAllTimes(place);
    }
}