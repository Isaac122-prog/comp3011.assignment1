package comp3011.assignment1;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TranscriptionController {

    @PostMapping(
        value = "/transcribe",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> transcribe(
            @RequestParam("audio") MultipartFile audio) {

        if (audio == null || audio.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "No audio file was received."
                    ));
        }

        System.out.println(
                "Received audio: " + audio.getOriginalFilename()
        );

        System.out.println(
                "Audio size: " + audio.getSize() + " bytes"
        );

        System.out.println(
                "Audio type: " + audio.getContentType()
        );

        String transcription =
                "Audio received successfully. Transcription service is not connected yet.";

        return ResponseEntity.ok(
                Map.of("transcription", transcription)
        );
    }
}
