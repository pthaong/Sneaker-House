package com.sneakerhouse.backend.service;
import com.sneakerhouse.backend.dto.ProfileDtos.*;
import com.sneakerhouse.backend.entity.UserAccount;
import com.sneakerhouse.backend.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class ProfileService {
private final UserAccountRepository users;
public ProfileService(UserAccountRepository users){this.users=users;}
@Transactional(readOnly=true) public ProfileResponse get(String email){return response(find(email));}
@Transactional public ProfileResponse update(String email,UpdateProfileRequest r){
UserAccount u=find(email);u.setFullName(r.fullName().trim());u.setPhoneNumber(blankNull(r.phoneNumber()));u.setBirthDate(r.birthDate());u.setGender(blankNull(r.gender()));return response(users.save(u));}
@Transactional public ProfileResponse avatar(String email,String url){UserAccount u=find(email);u.setAvatarUrl(url);return response(users.save(u));}
private UserAccount find(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new AuthException(404,"USER_NOT_FOUND","Không tìm thấy tài khoản."));}
private String blankNull(String s){return s==null||s.isBlank()?null:s.trim();}
private ProfileResponse response(UserAccount u){boolean p=u.getPasswordHash()!=null&&!u.getPasswordHash().isBlank(),g=u.getGoogleSub()!=null&&!u.getGoogleSub().isBlank();return new ProfileResponse(u.getId(),u.getEmail(),u.getFullName(),u.getPhoneNumber(),u.getBirthDate(),u.getGender(),u.getAvatarUrl(),p&&g?"BOTH":g?"GOOGLE":"EMAIL_PASSWORD",g,u.getRole());}
}
