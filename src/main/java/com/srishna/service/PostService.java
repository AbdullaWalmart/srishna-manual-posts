package com.srishna.service;

import com.srishna.dto.PostDto;
import com.srishna.entity.Post;
import com.srishna.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final StorageService storageService;

    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<PostDto> findAllAsList() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<Post> search(String query, Pageable pageable) {
        return postRepository.searchByText(query.trim(), pageable);
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    @Transactional
    public Post create(Long userId, String imagePath, String textPath, String textContent) {
        Post post = Post.builder()
                .userId(userId)
                .imagePath(imagePath)
                .textPath(textPath)
                .textContent(textContent)
                .build();
        return postRepository.save(post);
    }

    public PostDto toDto(Post post) {
        String imageUrl = storageService.getPublicUrl(post.getImagePath());
        return PostDto.builder()
                .id(post.getId())
                .imageUrl(imageUrl)
                .imagePath(post.getImagePath())
                .textContent(post.getTextContent())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
