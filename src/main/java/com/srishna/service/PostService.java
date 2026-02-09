package com.srishna.service;

import com.srishna.dto.PostDto;
import com.srishna.entity.Post;
import com.srishna.entity.ShareRecord;
import com.srishna.repository.PostRepository;
import com.srishna.repository.SavedItemRepository;
import com.srishna.repository.ShareRecordRepository;
import com.srishna.repository.ShareVisitRepository;
import com.srishna.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final DbSyncHelper dbSyncHelper;
    private final SavedItemRepository savedItemRepository;
    private final ShareRecordRepository shareRecordRepository;
    private final ShareVisitRepository shareVisitRepository;
    private final StorageService storageService;

    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAllByActiveTrueOrderByCreatedAtDesc(pageable);
    }

    /** Paginated list as DTOs with batched user lookup (no N+1). */
    public Page<PostDto> findAllDtos(Pageable pageable) {
        Page<Post> page = postRepository.findAllByActiveTrueOrderByCreatedAtDesc(pageable);
        List<PostDto> dtos = toDtoList(page.getContent());
        return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
    }

    /** Search results as DTOs with batched user lookup (no N+1). */
    public Page<PostDto> searchDtos(String query, Pageable pageable) {
        Page<Post> page = search(query, pageable);
        List<PostDto> dtos = toDtoList(page.getContent());
        return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
    }

    public List<PostDto> findAllAsList() {
        List<Post> posts = postRepository.findAllByActiveTrueOrderByCreatedAtDesc();
        return toDtoList(posts);
    }

    /** All posts (active + inactive) for admin table. */
    public List<PostDto> findAllIncludingInactive() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return toDtoList(posts);
    }

    /** Convert post list to DTOs with one batched user lookup to avoid N+1. */
    private List<PostDto> toDtoList(List<Post> posts) {
        if (posts.isEmpty()) return List.of();
        Set<Long> userIds = new HashSet<>();
        for (Post p : posts) {
            if (p.getUserId() != null) userIds.add(p.getUserId());
        }
        Map<Long, String> uploaderNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u ->
                    uploaderNames.put(u.getId(), (u.getName() != null && !u.getName().isBlank()) ? u.getName() : u.getEmail()));
        }
        return posts.stream()
                .map(p -> toDto(p, uploaderNames.get(p.getUserId())))
                .collect(Collectors.toList());
    }

    public Page<Post> search(String query, Pageable pageable) {
        return postRepository.searchActiveByText(query.trim(), pageable);
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "postLists", allEntries = true)
    public Post create(Long userId, String imagePath, String textPath, String textContent) {
        Post post = Post.builder()
                .userId(userId)
                .imagePath(imagePath)
                .textPath(textPath)
                .textContent(textContent)
                .active(true)
                .build();
        post = postRepository.save(post);
        dbSyncHelper.syncToGcsAfterCommit();
        return post;
    }

    @Transactional
    @CacheEvict(cacheNames = "postLists", allEntries = true)
    public Optional<Post> setActive(Long id, boolean active) {
        Optional<Post> result = postRepository.findById(id)
                .map(p -> {
                    p.setActive(active);
                    return postRepository.save(p);
                });
        result.ifPresent(p -> dbSyncHelper.syncToGcsAfterCommit());
        return result;
    }

    /** Permanently delete a post and its related saved items, share records, and share visits. */
    @Transactional
    @CacheEvict(cacheNames = "postLists", allEntries = true)
    public boolean deleteById(Long id) {
        boolean deleted = postRepository.findById(id)
                .map(post -> {
                    for (ShareRecord record : shareRecordRepository.findByPostIdOrderByCreatedAtDesc(id)) {
                        shareVisitRepository.deleteByShareRecordId(record.getId());
                    }
                    shareRecordRepository.deleteByPostId(id);
                    savedItemRepository.deleteByPostId(id);
                    postRepository.delete(post);
                    return true;
                })
                .orElse(false);
        if (deleted) dbSyncHelper.syncToGcsAfterCommit();
        return deleted;
    }

    public PostDto toDto(Post post) {
        String uploaderName = post.getUserId() != null
                ? userRepository.findById(post.getUserId())
                        .map(u -> (u.getName() != null && !u.getName().isBlank()) ? u.getName() : u.getEmail())
                        .orElse(null)
                : null;
        return toDto(post, uploaderName);
    }

    private PostDto toDto(Post post, String uploaderName) {
        String imageUrl = storageService.getPublicUrl(post.getImagePath());
        return PostDto.builder()
                .id(post.getId())
                .imageUrl(imageUrl)
                .imagePath(post.getImagePath())
                .textContent(post.getTextContent())
                .active(post.getActive())
                .uploaderName(uploaderName)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
