package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TranscriptionController.class)
class TranscriptionLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranscriptionService transcriptionService;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger controllerLogger;

    @BeforeEach
    void attachLogCapture() {
        controllerLogger = (Logger) LoggerFactory.getLogger(TranscriptionController.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        controllerLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogCapture() {
        controllerLogger.detachAppender(logAppender);
    }

    @Test
    void logsErrorWhenTranscriptionServiceFails() throws Exception {
        given(transcriptionService.transcribe(any()))
                .willThrow(new IOException("simulated upstream failure"));

        MockMultipartFile audio = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", "fake-audio-bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(audio))
                .andExpect(status().isInternalServerError());

        boolean errorLogged = logAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains("Transcription failed"));

        assertTrue(errorLogged, "Expected an ERROR-level log entry for the failed transcription");
    }

    @Test
    void logsInfoWhenTranscriptionSucceeds() throws Exception {
        given(transcriptionService.transcribe(any())).willReturn("hello world");

        MockMultipartFile audio = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", "fake-audio-bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(audio))
                .andExpect(status().isOk());

        boolean successLogged = logAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.INFO
                        && event.getFormattedMessage().contains("Transcription succeeded"));

        assertTrue(successLogged, "Expected an INFO-level log entry for the successful transcription");
    }
}