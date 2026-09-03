let mediaRecorder = null;
let audioChunks = [];

const startButton = document.getElementById('startButton');
const stopButton = document.getElementById('stopButton');
const statusText = document.getElementById('status');
const resultText = document.getElementById('result');

startButton.addEventListener('click', async () => {
    audioChunks = [];

    try {
        const stream = await navigator.mediaDevices.getUserMedia({
            audio: true
        });

        const mimeType = getSupportedMimeType();

        if (!mimeType) {
            statusText.innerText =
                'Error: No supported audio recording format found.';

            stream.getTracks().forEach(track => track.stop());
            return;
        }

        mediaRecorder = new MediaRecorder(stream, {
            mimeType: mimeType
        });

        mediaRecorder.addEventListener('dataavailable', (event) => {
            if (event.data && event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });

        mediaRecorder.addEventListener('stop', async () => {
            const audioBlob = new Blob(audioChunks, {
                type: mediaRecorder.mimeType
            });
            stream.getTracks().forEach(track => track.stop());

            statusText.innerText =
                'Uploading audio and transcribing...';

            startButton.disabled = true;
            stopButton.disabled = true;

            try {
                const transcription = await uploadAudio(
                    audioBlob,
                    mediaRecorder.mimeType
                );

                resultText.innerText = transcription;
                statusText.innerText = 'Transcription complete.';
            } catch (error) {
                console.error('Transcription error:', error);

                statusText.innerText =
                    'Error: Unable to transcribe the recording.';
                resultText.innerText = '';
            }

            startButton.disabled = false;
            stopButton.disabled = true;
        });

        mediaRecorder.start();
        statusText.innerText = 'Recording...';

        startButton.disabled = true;
        stopButton.disabled = false;

        resultText.innerText = '';

    } catch (error) {
        console.error('Microphone access error:', error);

        statusText.innerText =
            'Error: Microphone access was denied or unavailable.';
        startButton.disabled = false;
        stopButton.disabled = true;
    }
});

stopButton.addEventListener('click', () => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
        mediaRecorder.stop();

        statusText.innerText = 'Stopping recording...';
        stopButton.disabled = true;
    }
});

async function uploadAudio(audioBlob, mimeType) {
    const formData = new FormData();

    const extension = mimeType.includes('mp4') ? 'mp4' : 'webm';

    formData.append(
        'audio',
        audioBlob,
        `recording.${extension}`
    );

    const response = await fetch('/api/transcribe', {
        method: 'POST',
        body: formData
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(
            `Server returned ${response.status}: ${errorText}`
        );
    }

    const data = await response.json();

    if (!data.transcription) {
        throw new Error('No transcription returned by the server.');
    }
    return data.transcription;
}

function getSupportedMimeType() {
    const types = [
        'audio/webm;codecs=opus',
        'audio/webm',
        'audio/mp4'
    ];

    for (const type of types) {
        if (MediaRecorder.isTypeSupported(type)) {
            return type;
        }
    }
    return null;
}
