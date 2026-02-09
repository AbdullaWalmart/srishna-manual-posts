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
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
            return postService.searchDtos(q, pr);
        }
        return postService.findAllDtos(pr);
    }

    private static final CacheControl LIST_CACHE = CacheControl.maxAge(60, TimeUnit.SECONDS);

    /** Returns all posts as a list (newest first), no pagination. */
    @GetMapping("/list")
    public ResponseEntity<List<PostDto>> listAll() {
        return ResponseEntity.ok()
                .cacheControl(LIST_CACHE)
                .body(postService.findAllAsList());
    }

    /** Returns all active posts as a list (newest first), no pagination. Same as /list. */
    @GetMapping("/all")
    public ResponseEntity<List<PostDto>> listAllActive() {
        return ResponseEntity.ok()
                .cacheControl(LIST_CACHE)
                .body(postService.findAllAsList());
    }

    /** Returns all posts (active + inactive) for admin list. */
    @GetMapping("/admin/list")
    public ResponseEntity<List<PostDto>> listAllForAdmin() {
        return ResponseEntity.ok()
                .cacheControl(LIST_CACHE)
                .body(postService.findAllIncludingInactive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getById(@PathVariable Long id) {
        return postService.findById(id)
                .map(postService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Set post active/inactive (excluded from list when inactive). */
    @PatchMapping("/{id}/active")
    public ResponseEntity<PostDto> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return postService.setActive(id, active)
                .map(postService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Permanently delete a post. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return postService.deleteById(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
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
        Long userId = getUserIdFromAuth(auth);
        String imagePath = storageService.uploadImage(image);
        String textPath = null;
        String textContent = text;
        if (text != null && !text.isBlank()) {
            textPath = storageService.uploadText(text);
        }
        Post post = postService.create(userId, imagePath, textPath, textContent);
        return ResponseEntity.ok(postService.toDto(post));
    }

    /** Extract user ID from JWT auth principal (Long or numeric String). */
    private static Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof Long) return (Long) p;
        if (p instanceof String) {
            try { return Long.parseLong((String) p); } catch (NumberFormatException ignored) { }
        }
        return null;
    }
}
