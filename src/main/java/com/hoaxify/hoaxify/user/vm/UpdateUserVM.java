package com.hoaxify.hoaxify.user.vm;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class UpdateUserVM {
    @NotNull
    @Size(min = 4, max = 255)
    private String displayName;
    private String image;
}
