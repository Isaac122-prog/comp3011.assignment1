package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TranscriptionConcurrencyLoadTest {
    private static final int CONCURRENT_REQUESTS = 250;
    private static final String BOUNDARY = "----AssignmentLoadTestBoundary";

    @LocalServerPort
    private int port;

    @MockitoBean
    private TranscriptionService transcriptionService;

    @Test
    void handlesTwoHundredFiftyConcurrentRequestsWithoutFailure() throws Exception {
        given(transcriptionService.transcribe(any())).willReturn("stubbed transcription");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        URI uri = URI.create("http://localhost:" + port + "/api/transcribe");
        byte[] multipartBody = buildMultipartBody();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            tasks.add(() -> {
                startLatch.await();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                        .POST(BodyPublishers.ofByteArray(multipartBody))
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode();
            });
        }

        startLatch.countDown();
        List<Future<Integer>> futures = executor.invokeAll(tasks, 60, TimeUnit.SECONDS);
        executor.shutdown();

        int successCount = 0;
        for (Future<Integer> future : futures) {
            assertTrue(!future.isCancelled(), "Request timed out or was cancelled");
            if (future.get() == 200) {
                successCount++;
            }
        }

        assertEquals(CONCURRENT_REQUESTS, successCount,
                "All " + CONCURRENT_REQUESTS + " concurrent requests should succeed");
    }

    private byte[] buildMultipartBody() {
        String header = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"audio\"; filename=\"recording.webm\"\r\n"
                + "Content-Type: audio/webm\r\n\r\n";
        String footer = "\r\n--" + BOUNDARY + "--\r\n";

        byte[] fakeAudioBytes = "fake-audio-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[headerBytes.length + fakeAudioBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(fakeAudioBytes, 0, result, headerBytes.length, fakeAudioBytes.length);
        System.arraycopy(footerBytes, 0, result,
                headerBytes.length + fakeAudioBytes.length, footerBytes.length);
        return result;
    }
}