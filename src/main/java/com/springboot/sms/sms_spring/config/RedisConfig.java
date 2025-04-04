package com.springboot.sms.sms_spring.config;

import lombok.Getter;
import redis.clients.jedis.Jedis;

public class RedisConfig {
    @Getter
    private static Jedis jedis;
    static {
        jedis=new Jedis("localhost",6379);
    }
}
