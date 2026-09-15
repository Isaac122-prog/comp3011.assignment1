package comp3011.assignment1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TranscriptionService transcriptionService;

    @Test
    void returnsTranscriptionOnSuccess() throws Exception {
        given(transcriptionService.transcribe(any())).willReturn("hello world");

        MockMultipartFile audio = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", "fake-audio-bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(audio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription").value("hello world"));
    }

    @Test
    void returnsBadRequestWhenNoAudioProvided() throws Exception {
        MockMultipartFile emptyAudio = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", new byte[0]);

        mockMvc.perform(multipart("/api/transcribe").file(emptyAudio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returnsInternalServerErrorWhenServiceFails() throws Exception {
        given(transcriptionService.transcribe(any()))
                .willThrow(new IOException("simulated upstream failure"));

        MockMultipartFile audio = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", "fake-audio-bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(audio))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unable to transcribe audio."));
    }
}