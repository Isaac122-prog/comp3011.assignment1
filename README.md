# comp3011.assignment1
COMP3011 Assignment 1

## Testing
Four regression test classes live in src/test/java/comp3011/assignment1/. Each one targets a specific requirement from the assignment spec rather than
being a generic test. The sections below explain what each proves and what would go undetected without it.

### TranscriptionControllerTest

Uses @WebMvcTest with a mocked TranscriptionService, so no real OpenAI call is made. Covers three paths through /api/transcribe:
1. A successful transcription returns 200 OK with the expected JSON body.
2. An empty/missing audio file returns 400 Bad Request rather than crashing or silently accepting bad input.
3. An upstream failure is caught and converted into a safe 500 response, rather than leaking a stack trace or internal error detail back to the client.

Without this test, a change to the controller's error handling logic could silently start returning the wrong status code, or start leaking exception details to the client, without anything catching it before submission.

### AdminControllerTest

Also @WebMvcTest -based, covering the three admin/statistics endpoints against the YAML spec:
1. /api/v1/admin/uptime returns all three required fields (utcServerStart, utcNow, serverUptimeSeconds).
2. /api/v1/global/stats correctly reflects whatever StatsService reports proving the controller and the stats bean are wired together correctly, not just that the endpoint returns JSON.
3. Calling /api/v1/admin/shutdown twice in a row returns 202 Accepted the first time and 409 Conflict the second. This is the one behaviour in the whole app that's easy to silently break (e.g. if the AtomicBoolean guard were ever replaced with a plain boolean), since it would still "work" under casual manual testing but fail the spec's double-shutdown requirement.

### ServiceConcurrencyTest

Fires 250 threads at StatsService.recordUsage() simultaneously and asserts the final totals . This test exists specifically catch a lost-update race condition: if StatsService's counters were ever changed from AtomicLong to plain long fields, this test would start failing intermittently (the final count would be *less* than expected, because concurrent += operations on a plain long can overwrite each other) — while casual single-threaded testing would never reveal the bug.

### ConcurrencyLoadTest

Boots the full application with @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT) and fires 250 genuinely simultaneous blocking
HTTP requests at /api/transcribe (with the transcription service stubbed, so no real OpenAI cost is incurred), asserting every single request completes successfully with no timeouts or dropped connections.

This is the assignment's core non-functional requirement — "handle >200 concurrent blocking HTTP requests" — verified directly rather than assumed from configuration alone. It exercises the real server.tomcat.threads.max setting in application.properties; if that value were ever reduced below what's needed, or a controller became accidentally stateful/blocking on a shared lock, this test would fail or time out where a single manual request would look completely fine.

### TranscriptionLoggingTest

Captures real log output via a Logback ListAppender attached directly to TranscriptionController's logger, and asserts that a failed transcription produces an ERROR-level log entry and a successful one produces an INFO-level entry. This ties the logging strategy described above to an actual regression test, if the log messages in the controller were ever removed or their level changed, this test would fail immediately rather than the gap only being noticed by a human reading the code.
