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
    private final UserRepository userRepository;
    private final SavedItemRepository savedItemRepository;
    private final ShareRecordRepository shareRecordRepository;
    private final ShareVisitRepository shareVisitRepository;
    private final StorageService storageService;

    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAllByActiveTrueOrderByCreatedAtDesc(pageable);
    }

    public List<PostDto> findAllAsList() {
        return postRepository.findAllByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** All posts (active + inactive) for admin table. */
    public List<PostDto> findAllIncludingInactive() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<Post> search(String query, Pageable pageable) {
        return postRepository.searchActiveByText(query.trim(), pageable);
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
                .active(true)
                .build();
        return postRepository.save(post);
    }

    @Transactional
    public Optional<Post> setActive(Long id, boolean active) {
        return postRepository.findById(id)
                .map(p -> {
                    p.setActive(active);
                    return postRepository.save(p);
                });
    }

    /** Permanently delete a post and its related saved items, share records, and share visits. */
    @Transactional
    public boolean deleteById(Long id) {
        return postRepository.findById(id)
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
    }

    public PostDto toDto(Post post) {
        String imageUrl = storageService.getPublicUrl(post.getImagePath());
        String uploaderName = null;
        if (post.getUserId() != null) {
            uploaderName = userRepository.findById(post.getUserId())
                    .map(u -> (u.getName() != null && !u.getName().isBlank()) ? u.getName() : u.getEmail())
                    .orElse(null);
        }
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
