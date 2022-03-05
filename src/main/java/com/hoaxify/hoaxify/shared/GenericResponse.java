package com.hoaxify.hoaxify.shared;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenericResponse {
    public GenericResponse(String message) {
        this.message = message;
    }

    private String message;
}
