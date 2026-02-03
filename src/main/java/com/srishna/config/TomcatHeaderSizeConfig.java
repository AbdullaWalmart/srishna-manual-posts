package com.srishna.config;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures Tomcat allows large request headers (e.g. long URLs).
 * Use form field "text" for captions instead of ?text=... to avoid huge URLs.
 */
@Configuration
public class TomcatHeaderSizeConfig {

    private static final int MAX_HTTP_HEADER_SIZE = 524288; // 512KB

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatHeaderSizeCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            var handler = connector.getProtocolHandler();
            if (handler instanceof AbstractHttp11Protocol<?> protocol) {
                protocol.setMaxHttpHeaderSize(MAX_HTTP_HEADER_SIZE);
            }
        });
    }
}
