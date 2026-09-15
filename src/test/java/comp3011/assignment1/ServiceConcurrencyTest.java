package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;


class ServiceConcurrencyTest {
    @Test
    void concurrentUsageRecordingProducesExactTotals() throws InterruptedException {
        StatsService statsService = new StatsService();

        int threadCount = 250;
        long inputPerCall = 10;
        long outputPerCall = 4;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    statsService.recordUsage(inputPerCall, outputPerCall);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(true, completed, "All threads should complete within timeout");
        assertEquals(threadCount * inputPerCall, statsService.getInputTokens());
        assertEquals(threadCount * outputPerCall, statsService.getOutputTokens());
    }
}