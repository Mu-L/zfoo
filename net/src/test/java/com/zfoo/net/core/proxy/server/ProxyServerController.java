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
 */

package com.zfoo.net.core.proxy.server;

import com.zfoo.net.NetContext;
import com.zfoo.net.anno.PacketReceiver;
import com.zfoo.net.packet.proxy.ProxyHelloRequest;
import com.zfoo.net.packet.proxy.ProxyHelloResponse;
import com.zfoo.net.packet.tcp.TcpHelloResponse;
import com.zfoo.net.session.Session;
import com.zfoo.protocol.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author jaysunxiao
 */
@Component
public class ProxyServerController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServerController.class);

    @PacketReceiver
    public void atProxyHelloRequest(Session session, ProxyHelloRequest request) {
        logger.info("receive [packet:{}] from client", JsonUtils.object2String(request));

        var response = new ProxyHelloResponse();
        response.setMessage("Hello, this is the proxy server! -> " + request.getMessage());

        NetContext.getRouter().send(session, response);
    }

}
