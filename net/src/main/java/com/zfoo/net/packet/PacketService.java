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

package com.zfoo.net.packet;

import com.zfoo.net.NetContext;
import com.zfoo.net.router.attachment.SignalAttachment;
import com.zfoo.protocol.ProtocolManager;
import com.zfoo.protocol.buffer.ByteBufUtils;
import com.zfoo.protocol.collection.CollectionUtils;
import com.zfoo.protocol.exception.ExceptionUtils;
import com.zfoo.protocol.generate.GenerateOperation;
import com.zfoo.protocol.generate.GenerateProtocolFile;
import com.zfoo.protocol.registration.IProtocolRegistration;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.util.DomUtils;
import com.zfoo.protocol.util.NumberUtils;
import com.zfoo.protocol.util.StringUtils;
import com.zfoo.protocol.xml.XmlProtocols;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author godotg
 */
public class PacketService implements IPacketService {

    private static final Logger logger = LoggerFactory.getLogger(PacketService.class);

    /**
     * 包体的头部的长度，一个int字节长度
     */
    public static final int PACKET_HEAD_LENGTH = 4;


    public PacketService() {

    }

    @Override
    public void init() {
        var applicationContext = NetContext.getApplicationContext();

        var netConfig = NetContext.getConfigManager().getLocalConfig();
        var protocolLocation = netConfig.getProtocolLocation();

        var generateOperation = new GenerateOperation();
        generateOperation.setMergeProtocol(netConfig.isMergeProtocol());
        generateOperation.setFoldProtocol(netConfig.isFoldProtocol());
        generateOperation.setProtocolPath(netConfig.getProtocolPath());
        generateOperation.setProtocolParam(netConfig.getProtocolParam());
        var codeLanguageArr = StringUtils.tokenize(netConfig.getCodeLanguages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

        for (var codeLanguage : codeLanguageArr) {
            var codeLanguageSet = getProtocolList(codeLanguage);
            if (CollectionUtils.isEmpty(codeLanguageSet)) {
                continue;
            }
            generateOperation.getGenerateLanguages().addAll(codeLanguageSet);
        }
        // 设置生成协议的过滤器
        GenerateProtocolFile.generateProtocolFilter = GenerateProtocolFile.DefaultNetGenerateProtocolFilter;

        // 解析protocol.xml文件，并将协议生成ProtocolRegistration
        var resource = applicationContext.getResource(ResourceUtils.CLASSPATH_URL_PREFIX + protocolLocation);
        try {
            var xmlProtocols = DomUtils.inputStream2Object(resource.getInputStream(), XmlProtocols.class);
            ProtocolManager.initProtocol(xmlProtocols, generateOperation);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getMessage(e));
            throw new RuntimeException(e);
        }

        // 注册协议接收器
        var componentBeans = applicationContext.getBeansWithAnnotation(Component.class);
        for (var bean : componentBeans.values()) {
            NetContext.getRouter().registerPacketReceiverDefinition(bean);
        }
    }

    /**
     * 获取要生成协议列表
     */
    private Set<CodeLanguage> getProtocolList(String codeLanguage) {
        var languageSet = new HashSet<CodeLanguage>();
        var isNumeric = NumberUtils.isNumeric(codeLanguage);
        for (var language : CodeLanguage.values()) {
            if (isNumeric) {
                var code = Integer.valueOf(codeLanguage);
                if ((code & language.id) != 0) {
                    languageSet.add(language);
                }
            } else if (language.name().equalsIgnoreCase(codeLanguage)) {
                languageSet.add(language);
                break;
            }
        }
        return languageSet;
    }

    @Override
    public DecodedPacketInfo read(ByteBuf buffer) {
        // 包的长度在上一层已经解析过

        // 解析包体
        var packet = ProtocolManager.read(buffer);
        // 解析包的附加包
        var hasAttachment = ByteBufUtils.tryReadBool(buffer);
        var attachment = hasAttachment ? (ProtocolManager.read(buffer)) : null;
        return DecodedPacketInfo.valueOf(packet, attachment);
    }

    @Override
    public void write(ByteBuf buffer, Object packet, Object attachment) {
        // 写入包packet
        ProtocolManager.write(buffer, packet);

        // 写入包的附加包attachment
        if (attachment == null) {
            ByteBufUtils.writeBool(buffer, false);
        } else {
            ByteBufUtils.writeBool(buffer, true);
            // 写入包的附加包attachment
            ProtocolManager.write(buffer, attachment);
        }
    }

    @Override
    public void writeHeaderAndBody(ByteBuf buffer, Object packet, Object attachment) {
        try {
            // 预留写入包的长度，一个int字节大小
            buffer.ensureWritable(7);
            buffer.writerIndex(PACKET_HEAD_LENGTH);

            write(buffer, packet, attachment);

            writeHeaderBefore(buffer);
        } catch (Exception e) {
            logger.error("write packet exception", e);
        } catch (Throwable t) {
            logger.error("write packet error", t);
        }
    }

    @Override
    public void writeHeaderBefore(ByteBuf buffer) {
        int length = buffer.writerIndex();

        int packetLength = length - PACKET_HEAD_LENGTH;

        buffer.writerIndex(0);

        buffer.writeInt(packetLength);

        buffer.writerIndex(length);
    }
}
