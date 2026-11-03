package com.sneakerhouse.backend.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public final class ProfileDtos {
private ProfileDtos(){}
public record UpdateProfileRequest(@NotBlank @Size(min=2,max=120) String fullName,

@Pattern(
    regexp = "^(?:\\+?[0-9]{8,15})?$",
    message = "Số điện thoại phải gồm 8-15 chữ số, có thể bắt đầu bằng dấu +"
)
String phoneNumber,
LocalDate birthDate,@Pattern(regexp="^(MALE|FEMALE|OTHER)?$",message="gender không hợp lệ") String gender){}
public record ProfileResponse(Long id,String email,String fullName,String phoneNumber,LocalDate birthDate,String gender,String avatarUrl,String loginMethod,boolean googleLinked,String role){}
public record MessageResponse(String message){}
public record ErrorResponse(String error,String message){}
}
