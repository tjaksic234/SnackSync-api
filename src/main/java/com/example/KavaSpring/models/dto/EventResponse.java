package com.example.KavaSpring.models.dto;

import com.example.KavaSpring.models.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventResponse {

    @NotBlank
    @Size(max = 50)
    private String creatorId;

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(max = 120)
    private String description;

    @NotBlank
    @Size(max = 50)
    private String groupId;

    @NotBlank
    private EventType eventType;
}
