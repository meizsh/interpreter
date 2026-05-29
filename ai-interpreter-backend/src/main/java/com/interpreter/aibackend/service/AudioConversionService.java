package com.interpreter.aibackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class AudioConversionService {
    private static final Logger logger = LoggerFactory.getLogger(AudioConversionService.class);

    @Value("${audio.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${audio.conversion.timeout-seconds:60}")
    private long timeoutSeconds;

    public String convertToAsrWavIfNeeded(String inputPath) throws Exception {
        String extension = getExtension(inputPath);
        if ("wav".equals(extension) || "pcm".equals(extension)) {
            return inputPath;
        }

        if (!isConvertible(extension)) {
            throw new IllegalArgumentException("Unsupported audio format: " + extension + ". Please upload WAV, MP3, or M4A.");
        }

        Path input = Path.of(inputPath);
        String outputPath = stripExtension(inputPath) + "_asr.wav";
        List<String> command = List.of(
                ffmpegPath,
                "-y",
                "-i", input.toString(),
                "-ac", "1",
                "-ar", "16000",
                "-sample_fmt", "s16",
                outputPath
        );

        logger.info("Converting audio to ASR WAV: {} -> {}", inputPath, outputPath);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Audio conversion timed out");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("Audio conversion failed with exit code " + process.exitValue());
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("FFmpeg is required to convert " + extension.toUpperCase(Locale.ROOT)
                    + " audio. Install FFmpeg and make sure it is available in PATH, or set audio.ffmpeg.path.", e);
        }

        File output = new File(outputPath);
        if (!output.exists() || output.length() == 0) {
            throw new RuntimeException("Audio conversion produced an empty WAV file");
        }

        return outputPath;
    }

    private boolean isConvertible(String extension) {
        return "mp3".equals(extension) || "m4a".equals(extension) || "aac".equals(extension);
    }

    private String getExtension(String path) {
        if (path == null) {
            return "";
        }
        int idx = path.lastIndexOf('.');
        if (idx < 0 || idx == path.length() - 1) {
            return "";
        }
        return path.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String path) {
        int idx = path.lastIndexOf('.');
        if (idx < 0) {
            return path;
        }
        return path.substring(0, idx);
    }
}
