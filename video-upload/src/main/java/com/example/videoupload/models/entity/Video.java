package com.example.videoupload.models.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;




@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    private Long id;
    private String filename;
    private String filePath;
}
