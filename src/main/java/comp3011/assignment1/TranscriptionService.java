package comp3011.assignment1;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TranscriptionService {
    private final RestClient restClient;
    private final String apiKey;
    private final StatsService statsService;

    public TranscriptionService(
            RestClient.Builder restClientBuilder,
            @Value("${OPENAI_API_KEY:}") String apiKey, 
            StatsService statsService) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com")
                .build();
        this.apiKey = apiKey;
        this.statsService = statsService;
    }

    public String transcribe(MultipartFile audio) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set.");
        }

        ByteArrayResource audioResource = new ByteArrayResource(audio.getBytes()) {
            @Override
            public String getFilename() {
                String filename = audio.getOriginalFilename();
                return (filename != null && !filename.isBlank())
                        ? filename
                        : "recording.webm";
            }
        };

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", audioResource);
        formData.add("model", "gpt-4o-mini-transcribe");

        OpenAITranscriptionResponse response = restClient.post()
                .uri("/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(OpenAITranscriptionResponse.class);

        if (response == null || response.text() == null) {
            throw new IllegalStateException("OpenAI returned no transcription.");
        }

        if (response.usage() != null) {
            statsService.recordUsage(response.usage().inputTokens(),response.usage().outputTokens());
        }
        return response.text();
    }

    public record OpenAITranscriptionResponse(String text, Usage usage) {}
    public record Usage(long inputTokens, long outputTokens, long totalTokens) {}
}