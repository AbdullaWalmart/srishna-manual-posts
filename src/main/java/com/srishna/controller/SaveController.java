package com.srishna.controller;

import com.srishna.dto.PostDto;
import com.srishna.entity.Post;
import com.srishna.service.PostService;
import com.srishna.service.SaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/saves")
@RequiredArgsConstructor
public class SaveController {

    private final SaveService saveService;
    private final PostService postService;

    @PostMapping
    public ResponseEntity<Void> save(
            Authentication auth,
            @RequestParam Long postId) {
        Long userId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        saveService.save(postId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public List<PostDto> mySaves(Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        if (userId == null) return List.of();
        List<Post> posts = saveService.getMySaves(userId);
        return posts.stream().map(postService::toDto).collect(Collectors.toList());
    }
}
