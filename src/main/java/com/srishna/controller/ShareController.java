package com.srishna.controller;

import com.srishna.dto.ShareDto;
import com.srishna.service.ShareTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareTrackingService shareTrackingService;

    @Value("${app.base-url}")
    private String baseUrl;

    /** Create a share link for a post. parentShareToken = ref token from the link they opened (for chain tracking). */
    @PostMapping
    public ShareDto createShare(
            Authentication auth,
            @RequestParam Long postId,
            @RequestParam(required = false) String parentShareToken) {
        Long userId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        Long parentShareId = null;
        if (parentShareToken != null && !parentShareToken.isBlank()) {
            parentShareId = shareTrackingService.getByToken(parentShareToken)
                    .map(com.srishna.entity.ShareRecord::getId)
                    .orElse(null);
        }
        var record = shareTrackingService.createShare(postId, parentShareId, userId);
        String url = baseUrl + "/?ref=" + record.getShareToken();
        return ShareDto.builder()
                .shareToken(record.getShareToken())
                .shareUrl(url)
                .postId(postId)
                .build();
    }

    /** Record that a share was used via a specific app (e.g. whatsapp, twitter). Call before opening the app link. */
    @PostMapping("/record-method")
    public ResponseEntity<Void> recordMethod(
            @RequestParam String shareToken,
            @RequestParam String method) {
        shareTrackingService.recordShareMethod(shareToken, method);
        return ResponseEntity.ok().build();
    }

    /** Record visit when someone opens ?ref=TOKEN. Returns postId to scroll to. */
    @GetMapping("/visit")
    public ResponseEntity<Map<String, Object>> visit(
            @RequestParam String ref,
            @RequestParam(required = false) String visitorId) {
        Optional<Long> postId = shareTrackingService.recordVisit(ref, visitorId);
        if (postId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("postId", postId.get()));
    }
}
