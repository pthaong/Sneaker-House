package com.sneakerhouse.backend.controller;
import com.sneakerhouse.backend.dto.ProfileDtos.*;
import com.sneakerhouse.backend.service.AuthException;
import com.sneakerhouse.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
@RestController @RequestMapping("/api/profile")
public class ProfileController {
private static final long MAX=2L*1024*1024;
private final ProfileService profiles; private final Path uploadDir;
public ProfileController(ProfileService p,@Value("${app.upload-dir:uploads/avatars}") String d){profiles=p;uploadDir=Paths.get(d).toAbsolutePath().normalize();}
@GetMapping public ProfileResponse me(Authentication a){return profiles.get(a.getName());}
@PutMapping public ProfileResponse update(Authentication a,@Valid @RequestBody UpdateProfileRequest r){
if(r.birthDate()!=null&&r.birthDate().isAfter(java.time.LocalDate.now()))throw new AuthException(400,"INVALID_BIRTH_DATE","Ngày sinh không được ở tương lai.");
return profiles.update(a.getName(),r);}
@PostMapping(value="/avatar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
public ProfileResponse avatar(Authentication a,@RequestPart("file") MultipartFile f){
if(f==null||f.isEmpty())throw new AuthException(400,"EMPTY_FILE","Vui lòng chọn ảnh.");
if(f.getSize()>MAX)throw new AuthException(413,"FILE_TOO_LARGE","Ảnh tối đa 2MB.");
String type=f.getContentType()==null?"":f.getContentType().toLowerCase(Locale.ROOT);
if(!Set.of("image/jpeg","image/png").contains(type))throw new AuthException(415,"UNSUPPORTED_IMAGE_TYPE","Chỉ chấp nhận JPG hoặc PNG.");
String ext=type.equals("image/png")?".png":".jpg";
try{BufferedImage image=ImageIO.read(f.getInputStream());if(image==null)throw new AuthException(400,"INVALID_IMAGE","Tệp không phải ảnh hợp lệ.");
Files.createDirectories(uploadDir);String name=UUID.randomUUID()+ext;Path target=uploadDir.resolve(name).normalize();
if(!target.startsWith(uploadDir))throw new AuthException(400,"INVALID_FILE","Tên tệp không hợp lệ.");
Files.copy(f.getInputStream(),target,StandardCopyOption.REPLACE_EXISTING);
return profiles.avatar(a.getName(),"/uploads/avatars/"+name);
}catch(AuthException e){throw e;}catch(IOException e){throw new AuthException(500,"AVATAR_UPLOAD_FAILED","Không thể lưu ảnh.");}}
}
