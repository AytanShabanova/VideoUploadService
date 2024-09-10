package com.example.videoupload.service.impl;


import com.example.videoupload.exceptions.VideoTooSmallException;
import com.example.videoupload.service.inter.VideoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RequiredArgsConstructor
@Service
public class VideoServiceImpl implements VideoService {


    @Value("${spring.upload.path}")
    private String uploadPath;
    @Value("${spring.upload.video-quality}")
    private List<Integer> videoQualities;
    @Value("${spring.upload.ffmpeg-path}")
    private String ffmpegPath;

    @Value("${spring.upload.ffprobe-path}")
    private String ffprobePath;
    private final Tika tika = new Tika();
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    @PostConstruct
    public void initFFmpeg() {
        try {
            this.ffmpeg = new FFmpeg(ffmpegPath);
            this.ffprobe = new FFprobe(ffprobePath);
        } catch (IOException e) {
            throw new RuntimeException("FFmpeg və ya FFprobe-in başlanğıcında xəta baş verdi: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> uploadVideo(MultipartFile file) {

        try {
            String mimeType = tika.detect(file.getInputStream());
            if (!mimeType.startsWith("video/")) {
                throw new IllegalArgumentException("Göndərilən fayl video formatında deyil: " + mimeType);
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("Faylın adı mövcud deyil");
            }

            // Fayl yüklənmədən əvvəl ölçüləri yoxla
            String tempFilePath = uploadPath + "/temp_" + generateUniqueFilename() + "_" + originalFilename;
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);

            FFmpegProbeResult probeResult = ffprobe.probe(tempFilePath);
            int width = probeResult.getStreams().get(0).width;
            int height = probeResult.getStreams().get(0).height;

            // Ölçü yoxlaması
            if (width < 480 || height < 480) {
                // Müvəqqəti faylı sil
                Files.delete(Paths.get(tempFilePath));
                throw new VideoTooSmallException("Video ölçüləri çox kiçikdir: " + width + "x" + height);
            }

            // Fayl ölçüləri uyğun gəlirsə, daimi olaraq saxla və keyfiyyətli versiyalarını yarat
            String uniqueFilename = generateUniqueFilename();
            String filePath = saveFile(tempFile, uniqueFilename);

            Map<String, String> filenames = new HashMap<>();
            for (Integer quality : videoQualities) {
                adjustAndConvertVideo(uniqueFilename, quality);
                String outputFilename = uniqueFilename + "-" + quality + ".mp4";
                filenames.put(quality + "p", outputFilename);
            }
        //
            // Müvəqqəti faylı sil
            Files.delete(Paths.get(tempFilePath)); // Müvəqqəti faylı sil
            Files.delete(Paths.get(filePath));  // Orijinal faylı prosesdən sonra sil

            return filenames;
        } catch (IOException e) {
            throw new RuntimeException("Fayl yükləmə və ya konvertasiya zamanı xəta baş verdi: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Faylın adı düzgün deyil: " + e.getMessage(), e);
        }
    }

    private String saveFile(File file, String uniqueFilename) throws IOException {
        File destinationDir = new File(uploadPath);
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw new IOException("Fayl qovluğu yaradılmadı: " + uploadPath);
        }
        File destinationFile = new File(destinationDir, uniqueFilename);

        Files.copy(file.toPath(), destinationFile.toPath());
        return destinationFile.getAbsolutePath();
    }


    @Override

    public void deleteVideoByFilename(String filename) {
        String baseFilename = filename.split("\\.")[0];
        boolean isDeleted = false;


        for (Integer quality : videoQualities) {
            // Construct the full path for each quality version of the video
            String filePath = uploadPath + "/" + baseFilename + "-" + quality + ".mp4";
            File file = new File(filePath);

            // Check if the file exists and is a file
            if (file.exists() && file.isFile()) {
                // Try to delete the file
                if (file.delete()) {
                    isDeleted = true;
                } else {
                    throw new RuntimeException("Fayl silinərkən xəta baş verdi: " + file.getAbsolutePath());
                }
            }
        }

        // Check if at least one file was deleted
        if (!isDeleted) {
            throw new RuntimeException("Video mövcud deyil.");
        }

    }

    private String generateUniqueFilename() {
        return UUID.randomUUID().toString();
    }



    private void adjustAndConvertVideo(String uniqueFileName, int quality) throws IOException {
        try {
            FFmpegProbeResult probeResult = ffprobe.probe(uploadPath + "/" + uniqueFileName);
            int width = probeResult.getStreams().get(0).width;
            int height = probeResult.getStreams().get(0).height;
            if (width < 480 || height < 480) {
                throw new RuntimeException("Video ölçüləri çox kiçikdir: " + width + "x" + height);

            }

            resizeVideoToMaximum(uniqueFileName, quality);

        } catch (IOException e) {
            throw new RuntimeException("Videonun ölçülərini yoxlamaq və ya konvertasiya etmək uğursuz oldu: " + e.getMessage(), e);
        }

    }


    private void resizeVideoToMaximum(String uniqueFileName, int quality) throws IOException {

        try {


            String outputFilePath = uploadPath + "/" +
                    uniqueFileName + "-" + quality;

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(uploadPath + "/" + uniqueFileName)     // Filename, or a FFmpegProbeResult
                    .overrideOutputFiles(true) // Override the output if it exists

                    .addOutput(outputFilePath + ".mp4")   // Filename for the destination
                    .setFormat("mp4")        // Format is inferred from filename, or can be set


                    .setAudioChannels(1)         // Mono audio
                    .setAudioCodec("aac")        // using the aac codec
                    .setAudioSampleRate(48_000)  // at 48KHz
                    .setAudioBitRate(32768)      // at 32 kbit/s

                    .setVideoCodec("libx264")     // Video using x264
                    .setVideoFrameRate(24, 1)     // at 24 frames per second

                    .setVideoFilter("scale='if(gt(iw,ih),-2," + quality + "):if(gt(ih,iw),-2," + quality + ")'")

                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)

                    .done();
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();


        } catch (Exception e) {

            System.out.println(e.getMessage());
        }

    }




}

