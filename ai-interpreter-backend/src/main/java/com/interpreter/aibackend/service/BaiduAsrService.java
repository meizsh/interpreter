package com.interpreter.aibackend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.config.BaiduApiConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class BaiduAsrService {
    private static final Logger logger = LoggerFactory.getLogger(BaiduAsrService.class);
    private static final int MAX_ASR_AUDIO_BYTES = 1_900_000;

    @Autowired
    private BaiduApiService baiduApiService;

    @Autowired
    private BaiduApiConfig baiduApiConfig;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build();

    public String transcribeAudio(String audioFilePath) throws Exception {
        return transcribeAudio(audioFilePath, 1737);
    }

    public String transcribeAudio(String audioFilePath, int devPid) throws Exception {
        logger.info("Starting ASR for {}", audioFilePath);

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            throw new IOException("Audio file does not exist: " + audioFilePath);
        }

        byte[] audioData = readFileToByteArray(audioFile);
        logger.info("Audio file size: {} bytes", audioData.length);

        String accessToken = baiduApiService.getAccessToken(true);
        String format = getAsrFormat(audioFile.getName());
        int rate = getAsrRate(audioFile);

        if (audioData.length > MAX_ASR_AUDIO_BYTES) {
            if (!"wav".equals(format)) {
                throw new RuntimeException("Long audio must be converted to WAV before ASR.");
            }
            return transcribeLongWav(audioData, accessToken, devPid);
        }

        String result = callBaiduAsrApi(audioData, accessToken, format, rate, devPid);
        logger.info("ASR transcription completed");
        return result;
    }

    private String transcribeLongWav(byte[] wavData, String accessToken, int devPid) throws Exception {
        WavInfo wavInfo = parseWavInfo(wavData);
        int maxDataBytes = MAX_ASR_AUDIO_BYTES - 44;
        maxDataBytes = Math.max(wavInfo.blockAlign, (maxDataBytes / wavInfo.blockAlign) * wavInfo.blockAlign);

        List<String> parts = new ArrayList<>();
        int chunkIndex = 1;
        int dataEnd = wavInfo.dataOffset + wavInfo.dataSize;

        for (int offset = wavInfo.dataOffset; offset < dataEnd; offset += maxDataBytes) {
            int remaining = dataEnd - offset;
            int chunkDataSize = Math.min(maxDataBytes, remaining);
            chunkDataSize = (chunkDataSize / wavInfo.blockAlign) * wavInfo.blockAlign;
            if (chunkDataSize <= 0) {
                break;
            }

            byte[] chunk = buildWavChunk(wavData, offset, chunkDataSize, wavInfo);
            logger.info("Sending ASR chunk {} ({} bytes)", chunkIndex, chunk.length);
            String text = callBaiduAsrApi(chunk, accessToken, "wav", wavInfo.sampleRate, devPid);
            if (text != null && !text.isBlank()) {
                parts.add(text.trim());
            }
            chunkIndex++;
        }

        String result = String.join(" ", parts).trim();
        logger.info("Long ASR transcription completed with {} chunks", parts.size());
        return result;
    }

    private String callBaiduAsrApi(byte[] audioData, String accessToken, String format, int rate, int devPid) throws Exception {
        String speech = BaiduApiService.encodeBase64(audioData);

        JSONObject payload = new JSONObject();
        payload.put("format", format);
        payload.put("rate", rate);
        payload.put("channel", 1);
        payload.put("cuid", String.valueOf(System.currentTimeMillis()));
        payload.put("token", accessToken);
        payload.put("speech", speech);
        payload.put("len", audioData.length);
        payload.put("dev_pid", devPid);

        RequestBody jsonBody = RequestBody.create(
                payload.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://vop.baidu.com/server_api")
                .post(jsonBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("ASR HTTP error: " + response.code());
            }

            String responseBody = response.body() == null ? "" : response.body().string();
            logger.debug("ASR response: {}", responseBody);

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            if (jsonResponse.getIntValue("err_no") != 0) {
                String errMsg = jsonResponse.getString("err_msg");
                logger.error("ASR error: {}", errMsg);
                throw new RuntimeException("ASR error: " + errMsg);
            }

            if (jsonResponse.getJSONArray("result") == null || jsonResponse.getJSONArray("result").isEmpty()) {
                throw new RuntimeException("ASR returned empty result");
            }

            return jsonResponse.getJSONArray("result").getString(0);
        }
    }

    private String getAsrFormat(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".wav")) {
            return "wav";
        }
        if (lower.endsWith(".pcm")) {
            return "pcm";
        }
        return baiduApiConfig.getAsr().getFormat();
    }

    private int getAsrRate(File file) {
        String filename = file.getName().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".wav")) {
            try {
                return readWavSampleRate(file);
            } catch (Exception e) {
                logger.warn("Failed to read WAV sample rate, using default {}", baiduApiConfig.getAsr().getRate(), e);
            }
        }
        return baiduApiConfig.getAsr().getRate();
    }

    private int readWavSampleRate(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[44];
            int read = fis.read(header);
            if (read < 44) {
                throw new IOException("WAV header is too short");
            }
            return readLeInt(header, 24);
        }
    }

    private byte[] readFileToByteArray(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int total = 0;
            while (total < data.length) {
                int read = fis.read(data, total, data.length - total);
                if (read < 0) {
                    break;
                }
                total += read;
            }
        }
        return data;
    }

    private WavInfo parseWavInfo(byte[] wavData) {
        if (wavData.length < 44 || !asciiEquals(wavData, 0, "RIFF") || !asciiEquals(wavData, 8, "WAVE")) {
            throw new RuntimeException("Only standard WAV audio can be split for long ASR.");
        }

        int channels = readLeShort(wavData, 22);
        int sampleRate = readLeInt(wavData, 24);
        int bitsPerSample = readLeShort(wavData, 34);
        int dataHeader = findChunk(wavData, "data");
        if (dataHeader < 0) {
            throw new RuntimeException("WAV data chunk not found.");
        }

        int dataSize = readLeInt(wavData, dataHeader + 4);
        int dataOffset = dataHeader + 8;
        int blockAlign = Math.max(1, channels * bitsPerSample / 8);
        return new WavInfo(channels, sampleRate, bitsPerSample, blockAlign, dataOffset, dataSize);
    }

    private byte[] buildWavChunk(byte[] source, int sourceOffset, int dataSize, WavInfo info) {
        byte[] output = new byte[44 + dataSize];
        writeAscii(output, 0, "RIFF");
        writeLeInt(output, 4, 36 + dataSize);
        writeAscii(output, 8, "WAVE");
        writeAscii(output, 12, "fmt ");
        writeLeInt(output, 16, 16);
        writeLeShort(output, 20, 1);
        writeLeShort(output, 22, info.channels);
        writeLeInt(output, 24, info.sampleRate);
        writeLeInt(output, 28, info.sampleRate * info.blockAlign);
        writeLeShort(output, 32, info.blockAlign);
        writeLeShort(output, 34, info.bitsPerSample);
        writeAscii(output, 36, "data");
        writeLeInt(output, 40, dataSize);
        System.arraycopy(source, sourceOffset, output, 44, dataSize);
        return output;
    }

    private int findChunk(byte[] data, String chunkName) {
        int offset = 12;
        while (offset + 8 <= data.length) {
            int chunkSize = readLeInt(data, offset + 4);
            if (asciiEquals(data, offset, chunkName)) {
                return offset;
            }
            offset += 8 + chunkSize + (chunkSize % 2);
        }
        return -1;
    }

    private boolean asciiEquals(byte[] data, int offset, String value) {
        if (offset + value.length() > data.length) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (data[offset + i] != (byte) value.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void writeAscii(byte[] data, int offset, String value) {
        for (int i = 0; i < value.length(); i++) {
            data[offset + i] = (byte) value.charAt(i);
        }
    }

    private int readLeShort(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private int readLeInt(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }

    private void writeLeShort(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xff);
        data[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    private void writeLeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xff);
        data[offset + 1] = (byte) ((value >> 8) & 0xff);
        data[offset + 2] = (byte) ((value >> 16) & 0xff);
        data[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private record WavInfo(int channels, int sampleRate, int bitsPerSample, int blockAlign, int dataOffset, int dataSize) {}
}
