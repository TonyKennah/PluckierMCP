package uk.co.kennah.mcp.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.co.kennah.mcp.aitools.RacesInfo;

@RestController
public class RacesInfoController {

    private static final Logger logger = LoggerFactory.getLogger(RacesInfoController.class);

    @Autowired
    private RacesInfo racesInfo;

    @GetMapping("/meetings") //works
    public String getMeetings() {
        logger.info("REST request received for all the meetings");
        return racesInfo.getMeetings();
    }

    @GetMapping("/get-odds") //works
    public String getOdds(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request received for all the odds");
        return racesInfo.getOdds(time, place);
    }

    @GetMapping("/top-rated")
    public String getTopRated(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for top rated horse in the {} at {}", time, place);
        return racesInfo.getTopRated(time, place);
    }

    @GetMapping("/bottom-rated")
    public String getBottomRated(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for bottom rated horse in the {} at {}", time, place);
        return racesInfo.getBottomRated(time, place);
    }

    @GetMapping("/best-ever-rated")
    public String getBestEverRated(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for best ever rated horse in the {} at {}", time, place);
        return racesInfo.getBestEverRated(time, place);
    }

    @GetMapping("/best-average-rated")
    public String getBestAverageRated(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for best average rated horse in the {} at {}", time, place);
        return racesInfo.getBestAverageRated(time, place);
    }

    @GetMapping("/best-most-recent-rated")
    public String getBestMostRecentRated(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for best most recent rated horse in the {} at {}", time, place);
        return racesInfo.getBestMostRecentRated(time, place);
    }

    @GetMapping("/race-win-percentages-from-best-ever")
    public String getRaceWinPercentagesFromBestEver(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for race win percentages for best ever performance in the {} at {}", time, place);
        return racesInfo.getRaceWinPercentagesFromBestEver(time, place);
    }

    @GetMapping("/race-win-percentages-from-last-three")
    public String getRaceWinPercentagesFromLastThree(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for race win percentages for last three runs in the {} at {}", time, place);
        return racesInfo.getRaceWinPercentagesFromLastThree(time, place);
    }

    @GetMapping("/race-win-percentages-from-last-one")
    public String getRaceWinPercentagesFromLastOne(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for race win percentages for last run in the {} at {}", time, place);
        return racesInfo.getRaceWinPercentagesFromLastOne(time, place);
    }

    @GetMapping("/race-win-percentages-from-all")
    public String getRaceWinPercentagesFromAll(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for race win percentages for all past runs in the {} at {}", time, place);
        return racesInfo.getRaceWinPercentagesFromAll(time, place);
    }

    @GetMapping("/all-runners") //works
    public String getAllRunners(@RequestParam String time, @RequestParam String place) {
        logger.info("REST request for all runners in the {} at {}", time, place);
        return racesInfo.getAllRunners(time, place);
    }

    @GetMapping("/all-times") //works
    public String getAllTimes(@RequestParam String place) {
        logger.info("REST request for all times at {}", place);
        return racesInfo.getAllTimes(place);
    }

    @GetMapping("/find-horse-race")
    public String findHorseRace(@RequestParam String horseName) {
        logger.info("REST request to find race for horse: {}", horseName);
        return racesInfo.findHorseRace(horseName);
    }

    @GetMapping("/past-run-dates")
    public String getPastRunDates(@RequestParam String horseName) {
        logger.info("REST request for past run dates for horse: {}", horseName);
        return racesInfo.getPastRunDates(horseName);
    }

    @GetMapping("/next-race")
    public String getNextRace() {
        logger.info("REST request for the next race");
        return racesInfo.getNextRace();
    }

    @GetMapping("/horse-form")
    public String getHorseForm(@RequestParam String time, @RequestParam String place, @RequestParam String horseName) {
        logger.info("REST request for form for horse {} in the {} at {}", horseName, time, place);
        return racesInfo.getHorseForm(time, place, horseName);
    }

    @GetMapping("/nap-of-the-day")
    public String getNapOfTheDay() {
        logger.info("REST request received for Nap of the Day");
        return racesInfo.getNapOfTheDay();
    }

    @GetMapping("/nap-of-the-day-handicap")
    public String getHandicapNapOfTheDay() {
        logger.info("REST request received for Nap of the Day handicap races only");
        return racesInfo.getHandicapNapOfTheDay();
    }

    @GetMapping("/nap-of-the-day-uk-handicap")
    public String getUkHandicapNapOfTheDay() {
        logger.info("REST request received for Nap of the Day UK handicap races only");
        return racesInfo.getUkHandicapNapOfTheDay();
    }
}