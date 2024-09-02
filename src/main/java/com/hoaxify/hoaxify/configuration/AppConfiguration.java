package com.hoaxify.hoaxify.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hoaxify")
@Data
public class AppConfiguration {
    String uploadPath;
    String profileImageFolderPath = "profile";
    String attachmentsFolderPath = "attachments";

    public String getProfileImageFolderPath() {
        return uploadPath + "/" + profileImageFolderPath;
    }

    public String getAttachmentsFolderPath() {
        return uploadPath + "/" + attachmentsFolderPath;
    }
}
