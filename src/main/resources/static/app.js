const MAX_RECORDING_SECONDS = 60;

let mediaRecorder = null;
let audioChunks = [];
let audioContext = null;
let analyser = null;
let visualizerAnimationId = null;
let timerIntervalId = null;
let recordingStartTime = null;

const startButton = document.getElementById('startButton');
const stopButton = document.getElementById('stopButton');
const statusText = document.getElementById('status');
const resultText = document.getElementById('result');
const errorBanner = document.getElementById('errorBanner');
const recordingMeta = document.getElementById('recordingMeta');
const timerLabel = document.getElementById('timer');
const visualizerCanvas = document.getElementById('visualizer');
const canvasCtx = visualizerCanvas.getContext('2d');
const playbackContainer = document.getElementById('playbackContainer');
const playbackAudio = document.getElementById('playbackAudio');
const copyButton = document.getElementById('copyButton');

function showError(message) {
    errorBanner.textContent = message;
    errorBanner.hidden = false;
}

function clearError() {
    errorBanner.hidden = true;
    errorBanner.textContent = '';
}

function setRecordingUI(isRecording) {
    recordingMeta.hidden = !isRecording;
    visualizerCanvas.hidden = !isRecording;
    startButton.disabled = isRecording;
    stopButton.disabled = !isRecording;
}

function startTimer() {
    recordingStartTime = Date.now();
    timerLabel.textContent = '0:00';

    timerIntervalId = setInterval(() => {
        const elapsedSeconds = Math.floor((Date.now() - recordingStartTime) / 1000);
        const minutes = Math.floor(elapsedSeconds / 60);
        const seconds = elapsedSeconds % 60;
        timerLabel.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;

        if (elapsedSeconds >= MAX_RECORDING_SECONDS) {
            statusText.textContent = 'Maximum recording length reached — stopping automatically.';
            stopButton.click();
        }
    }, 250);
}

function stopTimer() {
    if (timerIntervalId) {
        clearInterval(timerIntervalId);
        timerIntervalId = null;
    }
}

function startVisualizer(stream) {
    audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const source = audioContext.createMediaStreamSource(stream);

    analyser = audioContext.createAnalyser();
    analyser.fftSize = 256;
    source.connect(analyser);

    const bufferLength = analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    function draw() {
        visualizerAnimationId = requestAnimationFrame(draw);
        analyser.getByteFrequencyData(dataArray);

        const width = visualizerCanvas.width;
        const height = visualizerCanvas.height;
        canvasCtx.clearRect(0, 0, width, height);

        const barCount = 32;
        const step = Math.floor(bufferLength / barCount);
        const barWidth = width / barCount;

        for (let i = 0; i < barCount; i++) {
            const value = dataArray[i * step];
            const barHeight = (value / 255) * height;
            canvasCtx.fillStyle = `hsl(${215 + (value / 255) * 20}, 90%, ${45 + (value / 255) * 20}%)`;
            canvasCtx.fillRect(i * barWidth + 1, height - barHeight, barWidth - 2, barHeight);
        }
    }

    draw();
}

function stopVisualizer() {
    if (visualizerAnimationId) {
        cancelAnimationFrame(visualizerAnimationId);
        visualizerAnimationId = null;
    }
	
    if (audioContext) {
        audioContext.close();
        audioContext = null;
    }
    canvasCtx.clearRect(0, 0, visualizerCanvas.width, visualizerCanvas.height);
}

startButton.addEventListener('click', async () => {
    audioChunks = [];
    clearError();
    resultText.textContent = '';
    playbackContainer.hidden = true;
    copyButton.hidden = true;

    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const mimeType = getSupportedMimeType();

        if (!mimeType) {
            showError('Your browser does not support any audio recording format we can use.');
            statusText.textContent = 'Ready to record.';
            stream.getTracks().forEach(track => track.stop());
            return;
        }

        mediaRecorder = new MediaRecorder(stream, { mimeType });

        mediaRecorder.addEventListener('dataavailable', (event) => {
            if (event.data && event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });

        mediaRecorder.addEventListener('stop', async () => {
            const audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType });
            stream.getTracks().forEach(track => track.stop());
            stopVisualizer();
            stopTimer();

            playbackAudio.src = URL.createObjectURL(audioBlob);
            playbackContainer.hidden = false;

            setRecordingUI(false);
            startButton.disabled = true;
            statusText.textContent = 'Uploading audio and transcribing…';

            try {
                const transcription = await uploadAudio(audioBlob, mediaRecorder.mimeType);
                resultText.textContent = transcription;
                statusText.textContent = 'Transcription complete.';
                copyButton.hidden = false;
                copyButton.classList.remove('copied');
                copyButton.textContent = 'Copy';
            } catch (error) {
                console.error('Transcription error:', error);
                showError('Something went wrong while transcribing your recording. Please try again.');
                statusText.textContent = 'Ready to record.';
                resultText.textContent = 'Your transcription will appear here.';
            }

            startButton.disabled = false;
        });

        mediaRecorder.start();
        setRecordingUI(true);
        startVisualizer(stream);
        startTimer();
        statusText.textContent = 'Recording — click Stop when you\'re done.';

    } catch (error) {
        console.error('Microphone access error:', error);
        showError('Microphone access was denied or unavailable. Please check your browser permissions and try again.');
        statusText.textContent = 'Ready to record.';
        setRecordingUI(false);
    }
});

stopButton.addEventListener('click', () => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
        mediaRecorder.stop();
        statusText.textContent = 'Stopping recording…';
        stopButton.disabled = true;
    }
});

copyButton.addEventListener('click', async () => {
    try {
        await navigator.clipboard.writeText(resultText.textContent);
        copyButton.textContent = 'Copied!';
        copyButton.classList.add('copied');
        setTimeout(() => {
            copyButton.textContent = 'Copy';
            copyButton.classList.remove('copied');
        }, 1500);
    } catch (error) {
        console.error('Clipboard copy failed:', error);
        showError('Could not copy to clipboard — your browser may not support this.');
    }
});

async function uploadAudio(audioBlob, mimeType) {
    const formData = new FormData();
    const extension = mimeType.includes('mp4') ? 'mp4' : 'webm';
    formData.append('audio', audioBlob, `recording.${extension}`);

    const response = await fetch('/api/transcribe', {
        method: 'POST',
        body: formData
    });

    if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody.error || `Server returned ${response.status}`);
    }

    const data = await response.json();

    if (!data.transcription) {
        throw new Error('No transcription returned by the server.');
    }
    return data.transcription;
}

function getSupportedMimeType() {
    const types = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];
    for (const type of types) {
        if (MediaRecorder.isTypeSupported(type)) {
            return type;
        }
    }
    return null;
}