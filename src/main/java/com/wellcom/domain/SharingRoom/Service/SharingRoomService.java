package com.wellcom.domain.SharingRoom.Service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.wellcom.domain.Item.Item;
import com.wellcom.domain.Item.ItemStatus;
import com.wellcom.domain.Item.Repository.ItemRepository;
import com.wellcom.domain.Member.Member;
import com.wellcom.domain.Member.Repository.MemberRepository;
import com.wellcom.domain.SharingRoom.Dto.SharingRoomReqDto;
import com.wellcom.domain.SharingRoom.Dto.SharingRoomResDto;
import com.wellcom.domain.SharingRoom.Repository.SharingRoomRepository;
import com.wellcom.domain.SharingRoom.SharingRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@PropertySource("classpath:application-s3.yml")
public class SharingRoomService {
    private final SharingRoomRepository sharingRoomRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    private final AmazonS3Client amazonS3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public SharingRoom create(SharingRoomReqDto sharingRoomReqDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("not found email"));

//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.IMAGE_JPEG);

        MultipartFile file = sharingRoomReqDto.getItemImage();
        String fileUrl = saveFile(file);

//        String fileName = multipartFile.getOriginalFilename();

        Item item = Item.builder()
                .name(sharingRoomReqDto.getItemName())
                .imagePath(fileUrl)
                .itemStatus(ItemStatus.SHARING)
                .build();
//        Item item = itemRepository.save(new_item);
//        Path path = Paths.get("C:/Users/Playdata/Desktop/wellcom/", item.getId() +"_"+ fileName);
//        item.setImagePath(fileUrl);

//        try {
//            byte[] bytes = multipartFile.getBytes();
//            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//        } catch (IOException e) {
//            throw new IllegalArgumentException("image is not available");
//        }

        // SharingRoom 객체가 생성될 때 Item 객체도 함께 생성 : Cascading PERSIST
        SharingRoom sharingRoom = SharingRoom.builder()
                .member(member)
                .title(sharingRoomReqDto.getTitle())
                .contents(sharingRoomReqDto.getContents())
                .cntPeople(sharingRoomReqDto.getCntPeople())
                .item(item).build();

        return sharingRoomRepository.save(sharingRoom);
    }

    public String saveFile(MultipartFile file) {
        String fileUrl = "";
        try {
            fileUrl = UUID.randomUUID() + "_" + file.getOriginalFilename(); //1d6d8f68-8575-435e-ab82-d3b1c2ead1f6_berry.jpg
            ObjectMetadata metadata= new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            amazonS3Client.putObject(bucket, fileUrl, file.getInputStream(), metadata);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return amazonS3Client.getUrl(bucket, fileUrl).toString();
    }

    public List<SharingRoomResDto> findAll() {
        List<SharingRoom> sharingRooms = sharingRoomRepository.findAll();
        return sharingRooms.stream().map(o -> SharingRoomResDto.toDto(o)).collect(Collectors.toList());
    }

    public SharingRoomResDto findById(Long id){
        // 로그인 한 사용자만 Sharing Room 상세 조회 가능
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("not found email"));
        SharingRoom sharingRoom = sharingRoomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("sharingRoom not found"));
        return SharingRoomResDto.toDto(sharingRoom);
    }

    public SharingRoomResDto update(Long id, SharingRoomReqDto sharingRoomReqDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        SharingRoom sharingRoom = sharingRoomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("sharingRoom not found"));

        // sharingRoom의 member의 email과 token에서 가져온 email과 비교
        // 자신이 생성한 SharingRoom이 아니라면 접근 에러
        if(!sharingRoom.getMember().getEmail().equals(email)){
            throw new AccessDeniedException("Access denied");
        }

        MultipartFile multipartFile = sharingRoomReqDto.getItemImage();
        String fileName = multipartFile.getOriginalFilename();
        Path path = Paths.get("C:/Users/Playdata/Desktop/wellcom/", sharingRoom.getItem().getId() +"_"+ fileName);

        sharingRoom.getItem().updateItem(sharingRoomReqDto.getItemName(), path.toString());
        try {
            byte[] bytes = multipartFile.getBytes();
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalArgumentException("image is not available");
        }

        sharingRoom.updateSharingRoom(
                sharingRoomReqDto.getTitle(),
                sharingRoomReqDto.getContents(),
                sharingRoomReqDto.getCntPeople()
        );

        return SharingRoomResDto.toDto(sharingRoom);
    }

    public void delete(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        SharingRoom sharingRoom = sharingRoomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("sharingRoom not found"));

        // 자신이 생성한 SharingRoom이 아니라면 접근 에러
        if(!sharingRoom.getMember().getEmail().equals(email)){
            throw new AccessDeniedException("Access denied");
        }

        // 이미 나눔이 끝난 SharingRoom에 대해서는 삭제 불가능
        if(sharingRoom.getItem().getItemStatus() == ItemStatus.DONE){
            throw new IllegalArgumentException("already canceled sharing room");
        }

        sharingRoom.getItem().doneItem();
        sharingRoom.deleteSharingRoom();
    }
}