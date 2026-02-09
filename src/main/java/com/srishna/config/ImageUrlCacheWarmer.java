package com.srishna.config;

import com.srishna.entity.Post;
import com.srishna.repository.PostRepository;
import com.srishna.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Warms the image signed-URL cache in the background after startup so list APIs return
 * imageUrl immediately. First batch (e.g. 100) is warmed; when a trigger ratio (e.g. 80%)
 * is reached, the next batch is started. No API or response behaviour change.
 */
@Component
@Slf4j
public class ImageUrlCacheWarmer {

    private final PostRepository postRepository;
    private final StorageService storageService;
    private final Executor cacheWarmerExecutor;

    public ImageUrlCacheWarmer(PostRepository postRepository,
                               StorageService storageService,
                               @Qualifier("cacheWarmerExecutor") Executor cacheWarmerExecutor) {
        this.postRepository = postRepository;
        this.storageService = storageService;
        this.cacheWarmerExecutor = cacheWarmerExecutor;
    }

    @Value("${gcp.url-cache-warm.enabled:true}")
    private boolean enabled;

    @Value("${gcp.url-cache-warm.delay-seconds:5}")
    private int delaySeconds;

    @Value("${gcp.url-cache-warm.batch-size:100}")
    private int batchSize;

    @Value("${gcp.url-cache-warm.trigger-ratio:0.8}")
    private double triggerRatio;

    @Value("${gcp.url-cache-warm.max-batches:2}")
    private int maxBatches;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!enabled) return;
        cacheWarmerExecutor.execute(this::runWarming);
    }

    private void runWarming() {
        if (delaySeconds > 0) {
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("URL cache warming interrupted during delay");
                return;
            }
        }
        log.info("Image URL cache warming started (batchSize={}, triggerRatio={}, maxBatches={})", batchSize, triggerRatio, maxBatches);
        warmBatch(0);
    }

    /**
     * Warms the signed-URL cache for posts in the given page. When triggerRatio of the
     * batch is reached, submits the next page to the executor so it runs in parallel.
     */
    private void warmBatch(int pageIndex) {
        if (pageIndex >= maxBatches) return;
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(pageIndex, batchSize)
        ).getContent();
        if (posts.isEmpty()) return;

        int triggerAt = (int) Math.ceil(posts.size() * triggerRatio);
        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            String path = post.getImagePath();
            if (path != null && !path.isBlank()) {
                try {
                    storageService.getPublicUrl(path);
                } catch (Exception e) {
                    log.trace("Failed to warm URL for {}: {}", path, e.getMessage());
                }
            }
            if (i + 1 == triggerAt && pageIndex + 1 < maxBatches) {
                final int nextPage = pageIndex + 1;
                cacheWarmerExecutor.execute(() -> warmBatch(nextPage));
            }
        }
        log.debug("Image URL cache warmed batch page {} ({} posts)", pageIndex, posts.size());
    }
}
