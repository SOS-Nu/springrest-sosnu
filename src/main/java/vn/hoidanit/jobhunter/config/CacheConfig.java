package vn.hoidanit.jobhunter.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching // Bật tính năng Caching của Spring
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Sử dụng một cache manager đơn giản, có thể tạo nhiều "ngăn" cache
        // Ở đây chúng ta sẽ dùng ngăn có tên "jobMatches"
        return new ConcurrentMapCacheManager("jobMatches");
    }
}
