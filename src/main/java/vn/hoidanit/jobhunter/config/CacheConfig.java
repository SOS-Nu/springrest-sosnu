package vn.hoidanit.jobhunter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public JCacheCacheManager cacheManager() {
        try {
            // Tạo CacheManager sử dụng Ehcache với file ehcache.xml
            URI ehcacheUri = getClass().getResource("/ehcache.xml").toURI();
            if (ehcacheUri == null) {
                log.error("Không tìm thấy file ehcache.xml trong classpath");
                throw new IllegalStateException("File ehcache.xml không tồn tại trong classpath");
            }
            javax.cache.CacheManager cacheManager = Caching.getCachingProvider().getCacheManager(
                    ehcacheUri,
                    getClass().getClassLoader());

            // Khai báo cache jobMatches nếu cần
            MutableConfiguration<String, Object> jobMatchesConfig = new MutableConfiguration<String, Object>()
                    .setTypes(String.class, Object.class)
                    .setStoreByValue(true)
                    .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_DAY));
            cacheManager.createCache("jobMatches", jobMatchesConfig);

            log.info("Khởi tạo CacheManager với Ehcache thành công");
            return new JCacheCacheManager(cacheManager);
        } catch (URISyntaxException e) {
            log.error("Lỗi khi chuyển đổi URL sang URI cho ehcache.xml: {}", e.getMessage());
            throw new IllegalStateException("Không thể khởi tạo CacheManager do lỗi URI", e);
        }
    }
}