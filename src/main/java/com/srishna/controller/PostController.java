package com.srishna.controller;

import com.srishna.dto.ActivityDto;
import com.srishna.dto.PostDto;
import com.srishna.entity.Post;
import com.srishna.service.PostActivityService;
import com.srishna.service.PostService;
import com.srishna.service.StorageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final StorageService storageService;
    private final PostActivityService postActivityService;

    @GetMapping
    public Page<PostDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        PageRequest pr = PageRequest.of(page, size);
        if (q != null && !q.isBlank()) {
            return postService.search(q, pr).map(postService::toDto);
        }
        return postService.findAll(pr).map(postService::toDto);
    }

    /** Returns all posts as a list (newest first), no pagination. */
    @GetMapping("/list")
    public List<PostDto> listAll() {
        return postService.findAllAsList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getById(@PathVariable Long id) {
        return postService.findById(id)
                .map(postService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<ActivityDto> getActivity(@PathVariable Long id) {
        return ResponseEntity.ok(postActivityService.getActivity(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDto> create(
            Authentication auth,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "text", required = false) String text) throws IOException {
        Long userId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        String imagePath = storageService.uploadImage(image);
        String textPath = null;
        String textContent = text;
        if (text != null && !text.isBlank()) {
            textPath = storageService.uploadText(text);
        }
        Post post = postService.create(userId, imagePath, textPath, textContent);
        return ResponseEntity.ok(postService.toDto(post));
    }
}
