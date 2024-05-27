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

package com.zfoo.protocol.generate;

import com.zfoo.protocol.ProtocolManager;
import com.zfoo.protocol.collection.CollectionUtils;
import com.zfoo.protocol.exception.UnknownException;
import com.zfoo.protocol.registration.IProtocolRegistration;
import com.zfoo.protocol.registration.ProtocolAnalysis;
import com.zfoo.protocol.registration.ProtocolRegistration;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.serializer.es.GenerateEsUtils;
import com.zfoo.protocol.serializer.gdscript.GenerateGdUtils;
import com.zfoo.protocol.serializer.go.GenerateGoUtils;
import com.zfoo.protocol.serializer.python.GeneratePyUtils;
import com.zfoo.protocol.serializer.typescript.GenerateTsUtils;
import com.zfoo.protocol.util.FileUtils;
import com.zfoo.protocol.util.ReflectionUtils;
import com.zfoo.protocol.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

import static com.zfoo.protocol.util.StringUtils.TAB;
import static com.zfoo.protocol.util.StringUtils.TAB_ASCII;

/**
 * @author godotg
 */
public abstract class GenerateProtocolFile {

    /**
     * EN: Filter for generating protocol files. If no filter is set, all protocols will be generated by default.
     * CN: 生成协议文件的过滤器，如果没有设置过滤器默认生成全部的协议
     */
    public static Predicate<IProtocolRegistration> generateProtocolFilter = registration -> true;

    public static int localVariableId = 0;

    public static StringBuilder addTab(StringBuilder builder, int deep) {
        builder.append(TAB.repeat(Math.max(0, deep)));
        return builder;
    }

    public static StringBuilder addTabAscii(StringBuilder builder, int deep) {
        builder.append(TAB_ASCII.repeat(Math.max(0, deep)));
        return builder;
    }

    /**
     * 给每行新增若干Tab
     */
    public static String addTabs(String str, int deep) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        var splits = str.split(FileUtils.LS_REGEX);
        var builder = new StringBuilder();
        for (var split : splits) {
            builder.append(TAB.repeat(Math.max(0, deep)));
            builder.append(split).append(FileUtils.LS);
        }
        return builder.toString();
    }


    /**
     * EN: Generate protocol files to various languages
     * CN: 生成各种语言的协议文件
     */
    public static void generate(GenerateOperation generateOperation) throws IOException, ClassNotFoundException {
        var protocols = ProtocolManager.protocols;

        // 如果没有需要生成的协议则直接返回
        if (generateOperation.getGenerateLanguages().isEmpty()) {
            return;
        }

        // 外层需要生成的协议
        var outsideGenerateProtocols = Arrays.stream(protocols)
                .filter(it -> Objects.nonNull(it))
                .filter(it -> generateProtocolFilter.test(it))
                .toList();

        // 需要生成的子协议，因为外层协议的内部有其它协议
        var insideGenerateProtocols = outsideGenerateProtocols.stream()
                .map(it -> ProtocolAnalysis.getAllSubProtocolIds(it.protocolId()))
                .flatMap(it -> it.stream())
                .map(it -> protocols[it])
                .distinct()
                .toList();

        var allGenerateProtocols = new HashSet<IProtocolRegistration>();
        allGenerateProtocols.addAll(outsideGenerateProtocols);
        allGenerateProtocols.addAll(insideGenerateProtocols);

        // 通过协议号，从小到大排序
        var generateProtocols = allGenerateProtocols.stream()
                .sorted((a, b) -> a.protocolId() - b.protocolId())
                .map(it -> (ProtocolRegistration) it)
                .toList();

        // 解析协议的文档注释
        GenerateProtocolNote.initProtocolNote(generateProtocols);
        // 计算协议生成的路径
        GenerateProtocolPath.initProtocolPath(generateProtocols);

        // 生成C++协议
        var generateLanguages = generateOperation.getGenerateLanguages();

        // 生成Golang协议
        if (generateLanguages.contains(CodeLanguage.Go)) {
            GenerateGoUtils.init(generateOperation);
            GenerateGoUtils.createProtocolManager(generateProtocols);
            for (var protocolRegistration : generateProtocols) {
                GenerateGoUtils.createGoProtocolFile((ProtocolRegistration) protocolRegistration);
            }
        }

        // 生成Javascript协议
        if (generateLanguages.contains(CodeLanguage.ES)) {
            GenerateEsUtils.init(generateOperation);
            for (var protocolRegistration : generateProtocols) {
                GenerateEsUtils.createEsProtocolFile((ProtocolRegistration) protocolRegistration);
            }
            GenerateEsUtils.createProtocolManager(generateProtocols);
        }

        // 生成TypeScript协议
        if (generateLanguages.contains(CodeLanguage.TypeScript)) {
            GenerateTsUtils.init(generateOperation);
            for (var protocolRegistration : generateProtocols) {
                GenerateTsUtils.createTsProtocolFile((ProtocolRegistration) protocolRegistration);
            }
            GenerateTsUtils.createProtocolManager(generateProtocols);
        }

        // 生成GdScript协议
        if (generateLanguages.contains(CodeLanguage.GdScript)) {
            GenerateGdUtils.init(generateOperation);
            GenerateGdUtils.createProtocolManager(generateProtocols);
            for (var protocolRegistration : generateProtocols) {
                GenerateGdUtils.createGdProtocolFile((ProtocolRegistration) protocolRegistration);
            }
        }

        // 生成Python协议
        if (generateLanguages.contains(CodeLanguage.Python)) {
            GeneratePyUtils.init(generateOperation);
            GeneratePyUtils.createProtocolManager(generateProtocols);
            for (var protocolRegistration : generateProtocols) {
                GeneratePyUtils.createPyProtocolFile((ProtocolRegistration) protocolRegistration);
            }
        }

        for (var language : generateOperation.getGenerateLanguages()) {
            if (language.codeGenerateClass == null) {
                continue;
            }
            var codeGenerate = ReflectionUtils.newInstance(language.codeGenerateClass);
            codeGenerate.init(generateOperation);
            if (generateOperation.isMergeProtocol()) {
                codeGenerate.mergerProtocol(generateProtocols);
            } else if (generateOperation.isFoldProtocol()) {
                codeGenerate.foldProtocol(generateProtocols);
            } else {
                codeGenerate.defaultProtocol(generateProtocols);
            }
        }

        // 预留参数，以后可能会用，比如给Lua修改一个后缀名称
        var protocolParam = generateOperation.getProtocolParam();
    }


    public static int indexOf(Field[] fields, Field field) {
        for (int index = 0; index < fields.length; index++) {
            if (fields[index].equals(field)) {
                return index;
            }
        }
        throw new UnknownException();
    }

    // 子协议优先生成
    public static List<ProtocolRegistration> subProtocolFirst(List<ProtocolRegistration> registrations) {
        var registrations1 = new ArrayList<ProtocolRegistration>(registrations);
        var subFirstRegistrations = new ArrayList<ProtocolRegistration>();
        var set = new HashSet<Short>();
        while (!registrations1.isEmpty()) {
            for (var i = 0; i < registrations1.size(); i++) {
                var registration = registrations1.get(i);
                var protocolId = registration.protocolId();
                var subProtocols = ProtocolAnalysis.getAllSubProtocolIds(protocolId);
                subProtocols.removeAll(set);
                if (CollectionUtils.isEmpty(subProtocols)) {
                    subFirstRegistrations.add(registration);
                    set.add(protocolId);
                    registrations1.remove(registration);
                    break;
                }
            }
        }
        return subFirstRegistrations;
    }

}
