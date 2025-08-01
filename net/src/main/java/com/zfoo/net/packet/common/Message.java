/*
 * Copyright (C) 2020 The zfoo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.zfoo.net.packet.common;

import com.zfoo.protocol.anno.Protocol;
import com.zfoo.protocol.util.StringUtils;

/**
 *
 * EN: Generic returns can be used both remotely and nested in other protocols
 * CN: 通用的返回，既可以用在远程调用，又可以嵌套在其它协议里
 *
 * @author godotg
 */
@Protocol(id = 100)
public class Message {

    /**
     * 1是成功，其它的均视为失败的请求
     */
    private int code;

    private String message;

    @Override
    public String toString() {
        return StringUtils.format("code:[{}] message:[{}]", code, message);
    }

    public boolean success() {
        return code == 1;
    }

    public boolean fail() {
        return code == 0;
    }

    public Message() {
    }

    public Message(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Message valueError(String message) {
        return new Message(0, message);
    }

    public static Message valueSuccess(String message) {
        return new Message(1, message);
    }

    public static Message valueInfo(String message) {
        return new Message(2, message);
    }

    public static Message valueWarn(String message) {
        return new Message(3, message);
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
