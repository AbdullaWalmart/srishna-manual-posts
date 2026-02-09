package com.srishna.controller;

import com.srishna.dto.PostDto;
import com.srishna.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * HTML endpoint for a single post (for sharing). Returns a full HTML page with OG and Twitter Card
 * meta tags so social crawlers get a rich preview. Does not replace the JSON API (GET /api/posts, GET /api/posts/:id).
 */
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostViewController {

    private static final String MOBILE_CSS =
            "  *{box-sizing:border-box;margin:0;padding:0}\n"
            + "  html{font-size:clamp(14px,2.5vw,16px);-webkit-text-size-adjust:100%;height:100%}\n"
            + "  body{background:#0b1220;color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,sans-serif;"
            + "min-height:100vh;min-height:100dvh;height:100%;-webkit-font-smoothing:antialiased;"
            + "display:flex;flex-direction:column;overflow:hidden}\n"
            + "  .vp{display:flex;flex-direction:column;min-height:100vh;min-height:100dvh;height:100%;width:100%;max-width:100vw}\n"
            + "  .vp-header{flex-shrink:0;width:100%;padding:clamp(10px,2.5vw,16px) clamp(12px,4vw,20px);"
            + "padding-top:max(env(safe-area-inset-top),clamp(10px,2.5vw,16px));"
            + "padding-left:max(env(safe-area-inset-left),clamp(12px,4vw,20px));"
            + "padding-right:max(env(safe-area-inset-right),clamp(12px,4vw,20px));"
            + "background:#0f172a;border-bottom:1px solid rgba(255,255,255,0.06);"
            + "font-size:clamp(0.8rem,2vw,0.95rem);color:#94a3b8;letter-spacing:0.02em;"
            + "display:flex;align-items:center;gap:clamp(8px,2.5vw,12px)}\n"
            + "  .vp-logo{height:clamp(28px,7vw,36px);width:auto;display:block;flex-shrink:0}\n"
            + "  .vp-brand{font-weight:700;color:#f1f5f9}\n"
            + "  .vp-main{flex:1;min-height:0;overflow-y:auto;overflow-x:hidden;-webkit-overflow-scrolling:touch;"
            + "padding:clamp(8px,2vw,16px) max(env(safe-area-inset-left),clamp(12px,4vw,24px)) clamp(8px,2vw,16px) max(env(safe-area-inset-right),clamp(12px,4vw,24px));"
            + "display:flex;flex-direction:column;align-items:center}\n"
            + "  .vp-card{width:100%;max-width:min(680px,100%);display:flex;flex-direction:column;flex-shrink:0;"
            + "background:linear-gradient(180deg,rgba(30,41,59,0.6) 0%,rgba(15,23,42,0.95) 100%);"
            + "border-radius:clamp(12px,3vw,20px);border:1px solid rgba(255,255,255,0.06);"
            + "box-shadow:0 4px 24px rgba(0,0,0,0.4);overflow:hidden}\n"
            + "  .vp-media{width:100%;background:#0f172a;overflow:hidden;line-height:0}\n"
            + "  .vp-media img{width:100%;height:auto;display:block;object-fit:contain;max-height:min(82vh,800px);vertical-align:middle}\n"
            + "  .vp-body{padding:clamp(14px,3.5vw,24px)}\n"
            + "  .vp-title{font-size:clamp(1.1rem,3.5vw,1.45rem);font-weight:700;line-height:1.35;margin-bottom:clamp(6px,1.5vw,10px);"
            + "color:#f1f5f9;letter-spacing:-0.01em;word-break:break-word}\n"
            + "  .vp-meta{font-size:clamp(0.72rem,1.8vw,0.82rem);color:#94a3b8;margin-bottom:clamp(8px,2vw,14px);line-height:1.4}\n"
            + "  .vp-text{font-size:clamp(0.9rem,2vw,1.02rem);line-height:1.6;color:#e2e8f0;white-space:pre-wrap;word-break:break-word}\n"
            + "  .vp-footer{flex-shrink:0;width:100%;padding:clamp(10px,2.5vw,14px) clamp(12px,4vw,20px);"
            + "padding-bottom:max(env(safe-area-inset-bottom),clamp(10px,2.5vw,14px));"
            + "padding-left:max(env(safe-area-inset-left),clamp(12px,4vw,20px));"
            + "padding-right:max(env(safe-area-inset-right),clamp(12px,4vw,20px));"
            + "background:#0f172a;border-top:1px solid rgba(255,255,255,0.06);"
            + "font-size:clamp(0.7rem,1.8vw,0.8rem);color:#94a3b8;display:flex;align-items:center;justify-content:flex-end}\n"
            + "  .vp-footer-copy{min-width:0}\n"
            + "  @media (max-width:360px){.vp-body{padding:12px}.vp-card{border-radius:10px}}\n"
            + "  @media (min-width:480px){.vp-media{border-radius:14px 14px 0 0}.vp-media img{max-height:min(78vh,700px)}}\n"
            + "  @media (min-width:768px){.vp-card{max-width:640px}.vp-main{padding:16px 24px}}\n"
            + "  @media (max-height:500px){.vp-media img{max-height:70vh}}\n";

    private final PostService postService;

    /** Base URL for og:url (e.g. backend public URL when this view is served from backend). */
    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    /** Base URL for static assets (logo). Always points to this backend so logo loads even when view is opened via frontend proxy. */
    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    /**
     * GET /post/:id/view — returns HTML page with OG tags, not JSON.
     * Uses the same post data as GET /api/posts/:id internally.
     */
    @GetMapping(value = "/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> view(@PathVariable Long id, HttpServletRequest request) {
        Optional<PostDto> opt = postService.findById(id).map(postService::toDto);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PostDto post = opt.get();
        String html = buildPostHtml(post, request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.TEXT_HTML_VALUE))
                .body(html);
    }

    private String buildPostHtml(PostDto post, HttpServletRequest request) {
        String title = titleFrom(post);
        String desc = post.getTextContent() != null ? post.getTextContent() : "";
        String imageUrl = post.getImageUrl() != null ? post.getImageUrl() : "";
        String viewUrl = baseUrl.replaceAll("/$", "") + "/post/" + post.getId() + "/view";
        String logoFileName = "Srishna Iogo.png";
        String staticBase = backendUrl.replaceAll("/$", "");
        String logoUrl = staticBase + "/" + URLEncoder.encode(logoFileName, StandardCharsets.UTF_8).replace("+", "%20");

        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeDesc = HtmlUtils.htmlEscape(truncate(desc, 300));
        String safeImageUrl = HtmlUtils.htmlEscape(imageUrl);
        String safeViewUrl = HtmlUtils.htmlEscape(viewUrl);

        String bodyDesc = post.getTextContent() != null ? HtmlUtils.htmlEscape(post.getTextContent()) : "";
        String bodyImg = HtmlUtils.htmlEscape(imageUrl);
        String safeLogoUrl = HtmlUtils.htmlEscape(logoUrl);

        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">\n"
                + "  <meta name=\"theme-color\" content=\"#0f172a\">\n"
                + "  <meta name=\"apple-mobile-web-app-capable\" content=\"yes\">\n"
                + "  <meta name=\"apple-mobile-web-app-status-bar-style\" content=\"black-translucent\">\n"
                + "  <title>" + safeTitle + "</title>\n"
                + "  <!-- Open Graph -->\n"
                + "  <meta property=\"og:title\" content=\"" + safeTitle + "\" />\n"
                + "  <meta property=\"og:description\" content=\"" + safeDesc + "\" />\n"
                + "  <meta property=\"og:image\" content=\"" + safeImageUrl + "\" />\n"
                + "  <meta property=\"og:url\" content=\"" + safeViewUrl + "\" />\n"
                + "  <meta property=\"og:type\" content=\"article\" />\n"
                + "  <!-- Twitter Card -->\n"
                + "  <meta name=\"twitter:card\" content=\"summary_large_image\" />\n"
                + "  <meta name=\"twitter:title\" content=\"" + safeTitle + "\" />\n"
                + "  <meta name=\"twitter:description\" content=\"" + safeDesc + "\" />\n"
                + "  <meta name=\"twitter:image\" content=\"" + safeImageUrl + "\" />\n"
                + "  <style>\n"
                + MOBILE_CSS
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <div class=\"vp\">\n"
                + "    <header class=\"vp-header\"><img src=\"" + safeLogoUrl + "\" alt=\"Srishna\" class=\"vp-logo\" /><span class=\"vp-brand\">Srishna</span> Political</header>\n"
                + "    <main class=\"vp-main\">\n"
                + "      <article class=\"vp-card\">\n"
                + (bodyImg.isEmpty() ? "" : "        <div class=\"vp-media\"><img src=\"" + bodyImg + "\" alt=\"\" loading=\"eager\" decoding=\"async\" /></div>\n")
                + "        <div class=\"vp-body\">\n"
                + (bodyDesc.isEmpty() ? "" : "          <div class=\"vp-text\">" + bodyDesc + "</div>\n")
                + "        </div>\n"
                + "      </article>\n"
                + "    </main>\n"
                + "    <footer class=\"vp-footer\"><span class=\"vp-footer-copy\">© 2026 Srishna. All rights reserved.</span></footer>\n"
                + "  </div>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String titleFrom(PostDto post) {
        if (post.getTextContent() != null && !post.getTextContent().isBlank()) {
            return truncate(post.getTextContent().trim(), 60);
        }
        return "Post #" + post.getId();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    private static String formatDateForDisplay(Instant instant) {
        if (instant == null) return "";
        return DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")
                .withZone(ZoneOffset.UTC)
                .format(instant);
    }
}
