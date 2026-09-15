package comp3011.assignment1;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class TranscriptionController {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionController.class);
    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> transcribe(
            @RequestParam("audio") MultipartFile audio) {

        if (audio == null || audio.isEmpty()) {
            log.warn("Transcription request rejected: no audio file received.");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audio file was received."));
        }

        log.info("Transcription request received: filename={}, size={} bytes, contentType={}",
                audio.getOriginalFilename(), audio.getSize(), audio.getContentType());

        try {
            String transcription = transcriptionService.transcribe(audio);
            log.info("Transcription succeeded: {} characters returned.", transcription.length());
            return ResponseEntity.ok(Map.of("transcription", transcription));
        } catch (Exception e) {
            log.error("Transcription failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to transcribe audio."));
        }
    }
}