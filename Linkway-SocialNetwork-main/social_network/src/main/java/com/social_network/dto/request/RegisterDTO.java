package com.social_network.dto.request;

import com.social_network.util.validator.RegisterChecked;
import com.social_network.util.validator.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterChecked
public class RegisterDTO {

    @NotNull(message = "Username cannot be empty")
    private String username;

    @StrongPassword
    private String password;

    private String confirmPassword;

    @Email(message = "Email is not in the correct format")
    private String email;

    @NotNull(message = "Display name cannot be empty")
    private String displayName;

}
