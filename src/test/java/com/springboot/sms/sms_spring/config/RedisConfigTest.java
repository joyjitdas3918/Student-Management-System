package com.springboot.sms.sms_spring.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Jedis jedis;

    @Test
    void testJedisBeanLoadedInContext() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.containsBean("getJedis")).isTrue();
        assertThat(jedis).isNotNull();

         try {
             jedis.ping();
             System.out.println("Jedis connection successful (pinged Redis).");
         } catch (Exception e) {
             System.err.println("Jedis connection failed during ping: " + e.getMessage());
         }
    }
}