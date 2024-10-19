package com.example.chat2;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
public class FileUploadController {

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                String uploadDir = "";
                File uploadFile = new File(uploadDir + file.getOriginalFilename());
                file.transferTo(uploadFile);
                return "File uploaded successfully: " + uploadFile.getAbsolutePath();
            } catch (IOException e) {
                return "File upload failed: " + e.getMessage();
            }
        }
        return "File is empty!";
    }
}
