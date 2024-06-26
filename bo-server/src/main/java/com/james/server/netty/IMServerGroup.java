/**
 * Copyright [2024] [bo-IM]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.james.server.netty;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.james.common.constant.IMRedisKey;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author james
 */
@Slf4j
@Component
@AllArgsConstructor
public class IMServerGroup implements CommandLineRunner {

    public static  volatile long SERVER_ID = 0;

    RedisTemplate<String, Object> redisTemplate;

    private final List<IMServer> imServerList;



    public boolean isReady()
    {
        for (IMServer imServer : imServerList) {
            if (!imServer.isReady()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run(String... args) throws Exception {
        String key = IMRedisKey.IM_MAX_SERVER_ID;
        SERVER_ID = redisTemplate.opsForValue().increment(key, 1);
        // 启动服务
        for (IMServer imServer : imServerList) {
            imServer.start();
        }
    }

    @PreDestroy
    public void  destroy()
    {
        // 停止服务
        for (IMServer imServer : imServerList) {
            imServer.stop();
        }
    }

}
