package comp3011.assignment1;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AdminController {

    private final Instant serverStart;
    private final ConfigurableApplicationContext applicationContext;
    private final StatsService statsService;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

    public AdminController(
            ConfigurableApplicationContext applicationContext,
            StatsService statsService) {
        this.applicationContext = applicationContext;
        this.statsService = statsService;
        this.serverStart = Instant.now();
    }

    @GetMapping("/admin/uptime")
    public ResponseEntity<Map<String, Object>> getServerUptime() {
        Instant now = Instant.now();
        double uptimeSeconds = Duration.between(serverStart, now).toNanos() / 1_000_000_000.0;

        return ResponseEntity.ok(Map.of(
                "utcServerStart", serverStart.toString(),
                "utcNow", now.toString(),
                "serverUptimeSeconds", uptimeSeconds));
    }

    @GetMapping("/global/stats")
    public ResponseEntity<Map<String, Long>> getGlobalStats() {
        return ResponseEntity.ok(Map.of(
                "inputTokens", statsService.getInputTokens(),
                "outputTokens", statsService.getOutputTokens()));
    }

    @PostMapping("/admin/shutdown")
    public ResponseEntity<Map<String, String>> shutdownServer() {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            return ResponseEntity.status(409)
                    .body(Map.of("message", "Graceful shutdown is already in progress."));
        }

        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            applicationContext.close();
        }, "graceful-shutdown");

        shutdownThread.start();
        return ResponseEntity.accepted()
                .body(Map.of("message", "Graceful shutdown requested."));
    }
}