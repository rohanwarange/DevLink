package com.social_network.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {

    private int id;

    @NotEmpty(message = "Display name cannot be empty")
    private String displayName;

    private String introduction;

    @Email(message = "Email is not in the correct format")
    private String email;

    private String avatarImagePath;

}
