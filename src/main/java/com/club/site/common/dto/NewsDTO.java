package com.club.site.common.dto;

import com.google.cloud.Timestamp;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDTO {
    private String id;
    private String title;
    private String content;
    private String body;
    private String imageUrl;
    private String type;
    private Date date;
}