package uk.co.kennah.mcp;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.kennah.mcp.gcp.GCSHorseReader;
import uk.co.kennah.mcp.gcp.GCSOddsReader;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.cache.type=none"
})
@AutoConfigureMockMvc
class McpServerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private GCSHorseReader gcsReader;

	@MockBean
	private GCSOddsReader gcsOddsReader;

	@Test
	void getMeetingsEndpointShouldReturnMeetingList() throws Exception {
		// Arrange: Define the mock data that the GCSReader should return.
		String mockJsonData = """
            [
                {"place": "Ascot", "time": "13:50", "horses": []},
                {"place": "York", "time": "14:00", "horses": []},
                {"place": "Ascot", "time": "14:25", "horses": []}
            ]
            """;
		when(gcsReader.readFileFromGCSAsJson()).thenReturn(JsonParser.parseString(mockJsonData));

		// Act & Assert: Perform a GET request to the /meetings endpoint and verify the response.
		mockMvc.perform(get("/meetings"))
				.andExpect(status().isOk())
				.andExpect(content().string("List of available meetings: Ascot, York"));
	}

	@Test
	void getNapOfTheDayShouldReturnBestBet() throws Exception {
		// Arrange: Define the mock data that the GCSReader should return.
		String mockJsonData = """
            [
              {"time": "14:05", "place": "Ascot", "detail": "(CLASS 4) (3yo+)", "horses": [
                  {"name": "GoodHorse", "past": [{"date": "02/01/2023", "name": 100}, {"date": "03/01/2023", "name": 100}, {"date": "01/01/2023", "name": 100}]},
                  {"name": "NoFormHorse", "past": []},
                  {"name": "BadHorse", "past": [{"date": "01/01/2023", "name": 50}, {"date": "02/01/2023", "name": 50}, {"date": "03/01/2023", "name": 50}]}
              ]},
              {"time": "15:00", "place": "York", "detail": "(CLASS 5) (4yo+)", "horses": [
                  {"name": "AverageHorse", "past": [{"date": "01/01/2023", "name": 75}, {"date": "02/01/2023", "name": 75}, {"date": "03/01/2023", "name": 75}]}
              ]}
            ]
            """;
		when(gcsReader.readFileFromGCSAsJson()).thenReturn(JsonParser.parseString(mockJsonData));

		// Act & Assert: Perform a GET request to the /nap-of-the-day endpoint and verify the response.
		mockMvc.perform(get("/nap-of-the-day"))
				.andExpect(status().isOk())
				.andExpect(content().string("The nap of the day is GoodHorse in the 14:05 at Ascot, with a recent average rating of 100.00."));
	}

}
