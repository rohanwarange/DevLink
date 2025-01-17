package com.social_network.util.validator;

import com.social_network.dao.UserRepository;
import com.social_network.dto.request.RegisterDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Service;

@Service
public class RegisterValidator implements ConstraintValidator<RegisterChecked, RegisterDTO> {

    private final UserRepository userRepository;

    public RegisterValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(RegisterDTO user, ConstraintValidatorContext context) {
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            context.buildConstraintViolationWithTemplate("Passwords do not match")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }

        if (this.userRepository.existsByUsername(user.getUsername())) {
            context.buildConstraintViolationWithTemplate("Username already exists")
                    .addPropertyNode("username")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }

        if (this.userRepository.existsByEmail(user.getEmail())) {
            context.buildConstraintViolationWithTemplate("Email already exists")
                    .addPropertyNode("email")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }

        return true;
    }
}
