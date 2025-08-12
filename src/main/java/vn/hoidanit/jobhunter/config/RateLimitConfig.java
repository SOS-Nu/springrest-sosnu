package vn.hoidanit.jobhunter.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

import vn.hoidanit.jobhunter.config.codec.StringByteArrayCodec;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean(destroyMethod = "shutdown")
    RedisClient rateLimitRedisClient(RedisProperties props) {
        RedisURI.Builder builder = RedisURI.Builder.redis(props.getHost(), props.getPort());
        if (props.getPassword() != null && !props.getPassword().isEmpty()) {
            builder.withPassword(props.getPassword().toCharArray());
        }
        // getDatabase() là int -> set thẳng, KHÔNG check null
        builder.withDatabase(props.getDatabase());
        return RedisClient.create(builder.build());
    }

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient client) {
        // Dùng codec tự viết (key=String, value=byte[])
        return client.connect(new StringByteArrayCodec());
    }

    @Bean
    ProxyManager<String> rateLimitProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }

    @Bean
    BucketConfiguration defaultBucketConfig(RateLimitProperties props) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(props.getBurstCapacity())
                        .refillGreedy(props.getRequestsPerSecond(), Duration.ofSeconds(1))
                        .id("per-second"))
                .build();
    }
}
