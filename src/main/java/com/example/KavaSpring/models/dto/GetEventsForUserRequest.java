package com.example.KavaSpring.models.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GetEventsForUserRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String coffeeOrderId;
}
