package org.example.ridehailing.identityservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.example.ridehailing.identityservice.entity.User.Role;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Password không được để trống")
    private String password;

    private String fullName;
    private String email;

    @JsonProperty("phoneNumber")
    private String phone;

    private Role role;
}