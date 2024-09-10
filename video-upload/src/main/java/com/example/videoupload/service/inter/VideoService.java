package com.example.videoupload.service.inter;

import com.example.videoupload.models.entity.Video;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface VideoService {
    Map<String, String> uploadVideo(MultipartFile file) throws IOException;


    void deleteVideoByFilename(String filename);
}
