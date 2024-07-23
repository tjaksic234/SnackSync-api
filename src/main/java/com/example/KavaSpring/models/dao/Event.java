package com.example.KavaSpring.models.dao;

import com.example.KavaSpring.models.enums.EventStatus;
import com.example.KavaSpring.models.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "events")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    private String id;

    @NotBlank
    private String creatorId; //? neznam ocu li ostaviti creatorId ili refactorat u userId

    @NotBlank
    private String title;

    @NotBlank
    @Size(max = 120)
    private String description;

    @NotBlank
    private String groupId;

    private EventStatus status = EventStatus.PENDING;

    private EventType eventType;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime completedAt;
}
