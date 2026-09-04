package comp3011.assignment1;

import java.util.Map;

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

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> transcribe(
            @RequestParam("audio") MultipartFile audio) {

        if (audio == null || audio.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audio file was received."));
        }

        try {
            String transcription = transcriptionService.transcribe(audio);
            return ResponseEntity.ok(Map.of("transcription", transcription));
        } catch (Exception e) {
            System.err.println("Transcription failed: " + e.getClass().getSimpleName());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to transcribe audio."));
        }
    }
}