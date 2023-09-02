/*
 * Copyright (C) 2020 The zfoo Authors
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package com.zfoo.net.core.json.client;

import com.zfoo.net.NetContext;
import com.zfoo.net.core.json.JsonWebsocketClient;
import com.zfoo.net.packet.json.JsonHelloRequest;
import com.zfoo.protocol.util.ThreadUtils;
import com.zfoo.util.net.HostAndPort;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author godotg
 * @version 3.0
 */
@Ignore
public class JsonWebsocketClientTest {

    @Test
    public void startClient() {
        var context = new ClassPathXmlApplicationContext("config.xml");

        var webSocketClientProtocolConfig = WebSocketClientProtocolConfig.newBuilder()
                .webSocketUri("http://127.0.0.1:9000/websocket")
                .build();

        var client = new JsonWebsocketClient(HostAndPort.valueOf("127.0.0.1:9000"), webSocketClientProtocolConfig);
        var session = client.start();

        var request = new JsonHelloRequest();
        request.setMessage("Hello, this is the json client!");

        for (int i = 0; i < 1000; i++) {
            ThreadUtils.sleep(2000);
            NetContext.getRouter().send(session, request);
        }

        ThreadUtils.sleep(Long.MAX_VALUE);
    }


}
