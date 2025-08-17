package uk.co.kennah.mcp.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.kennah.mcp.aitools.RacesInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class demonstrates pure unit testing for the RacesInfoController.
 * It uses Mockito to test the controller in isolation, without loading the Spring context.
 * This makes the tests very fast and focused on the controller's logic.
 */
@ExtendWith(MockitoExtension.class)
class RacesInfoControllerTest {

    @Mock
    private RacesInfo racesInfo; // A mock of the service dependency

    @InjectMocks
    private RacesInfoController controller; // The controller instance we are testing

    @Test
    void getMeetingsShouldCallServiceAndReturnItsResponse() {
        // Arrange: Define what the mock service should return
        String expectedResponse = "List of available meetings: Ascot, York";
        when(racesInfo.getMeetings()).thenReturn(expectedResponse);

        // Act: Call the controller method
        String actualResponse = controller.getMeetings();

        // Assert: Check that the response is what we expected
        assertThat(actualResponse).isEqualTo(expectedResponse);
        // Optional: Verify that the service method was indeed called
        verify(racesInfo).getMeetings();
    }

    @Test
    void getNapOfTheDayShouldReturnBestBet() {
        // Arrange
        String expectedResponse = "The nap of the day is GoodHorse in the 14:05 at Ascot, with a recent average rating of 100.00.";
        when(racesInfo.getNapOfTheDay()).thenReturn(expectedResponse);

        // Act
        String actualResponse = controller.getNapOfTheDay();

        // Assert
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }
}

