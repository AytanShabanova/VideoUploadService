package com.example.videoupload.controller;


import com.example.videoupload.service.inter.VideoService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/uploads/video")
public class VideoController {

    private final VideoService videoService;

      @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
     public ResponseEntity<Map<String,String>> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {

        Map<String,String > filenames = videoService.uploadVideo(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(filenames);
    }





    @DeleteMapping("/name/{filename}")
    public ResponseEntity<Void> deleteVideoByFilename(@PathVariable String filename) {
        videoService.deleteVideoByFilename(filename);
        return ResponseEntity.noContent().build();
    }
    //
}
