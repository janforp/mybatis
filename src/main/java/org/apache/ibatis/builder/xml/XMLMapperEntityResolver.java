/*
 *    Copyright 2009-2014 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.apache.ibatis.builder.xml;

import org.apache.ibatis.io.Resources;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.1 何为 EntityResolver :
 * 官方解释: 如果ＳＡＸ应用程序实现自定义处理外部实体,则必须实现此接口,
 * 并使用setEntityResolver方法向SAX 驱动器注册一个实例.
 * 也就是说,对于解析一个xml,sax
 * 首先会读取该xml文档上的声明,根据声明去寻找相应的dtd定义,以便对文档的进行验证,
 * 默认的寻找规则,(即:通过网络,实现上就是声明DTD的地址URI地址来下载DTD声明),
 * 并进行认证,下载的过程是一个漫长的过程,而且当网络不可用时,这里会报错,就是因为相应的dtd没找到,
 *
 * 1.2 EntityResolver 的作用就是项目本身就可以提供一个如何寻找DTD 的声明方法,
 * 即:由程序来实现寻找DTD声明的过程,比如我们将DTD放在项目的某处在实现时直接将此文档读取并返回个SAX即可,这样就避免了通过网络来寻找DTD的声明
 * ————————————————
 * 版权声明：本文为CSDN博主「sicofield」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
 * 原文链接：https://blog.csdn.net/sicofield/article/details/79282918
 *
 * Offline entity resolver for the MyBatis DTDs
 * 目的是未联网的情况下也能做DTD验证，实现原理就是将DTD搞到本地，然后用org.xml.sax.EntityResolver，最后调用DocumentBuilder.setEntityResolver来达到脱机验证
 * EntityResolver
 * public InputSource resolveEntity (String publicId, String systemId)
 * 应用程序可以使用此接口将系统标识符重定向到本地 URI
 * 但是用DTD是比较过时的做法，新的都改用xsd了
 * 这个类的名字并不准确，因为它被两个类都用到了（XMLConfigBuilder,XMLMapperBuilder）
 *
 * @author Clinton Begin
 */
public class XMLMapperEntityResolver implements EntityResolver {

    private static final Map<String, String> doctypeMap = new HashMap<String, String>();

    // <?xml version="1.0" encoding="UTF-8" ?>
    // <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    // 常量定义
    private static final String IBATIS_CONFIG_PUBLIC = "-//ibatis.apache.org//DTD Config 3.0//EN".toUpperCase(Locale.ENGLISH);

    private static final String IBATIS_CONFIG_SYSTEM = "http://ibatis.apache.org/dtd/ibatis-3-config.dtd".toUpperCase(Locale.ENGLISH);

    private static final String IBATIS_MAPPER_PUBLIC = "-//ibatis.apache.org//DTD Mapper 3.0//EN".toUpperCase(Locale.ENGLISH);

    private static final String IBATIS_MAPPER_SYSTEM = "http://ibatis.apache.org/dtd/ibatis-3-mapper.dtd".toUpperCase(Locale.ENGLISH);

    private static final String MYBATIS_CONFIG_PUBLIC = "-//mybatis.org//DTD Config 3.0//EN".toUpperCase(Locale.ENGLISH);

    private static final String MYBATIS_CONFIG_SYSTEM = "http://mybatis.org/dtd/mybatis-3-config.dtd".toUpperCase(Locale.ENGLISH);

    private static final String MYBATIS_MAPPER_PUBLIC = "-//mybatis.org//DTD Mapper 3.0//EN".toUpperCase(Locale.ENGLISH);

    private static final String MYBATIS_MAPPER_SYSTEM = "http://mybatis.org/dtd/mybatis-3-mapper.dtd".toUpperCase(Locale.ENGLISH);

    private static final String MYBATIS_CONFIG_DTD = "org/apache/ibatis/builder/xml/mybatis-3-config.dtd";

    private static final String MYBATIS_MAPPER_DTD = "org/apache/ibatis/builder/xml/mybatis-3-mapper.dtd";

    static {
        // 将DOCTYPE和URL都映射到本地类路径下的DTD
        doctypeMap.put(IBATIS_CONFIG_SYSTEM, MYBATIS_CONFIG_DTD);
        doctypeMap.put(IBATIS_CONFIG_PUBLIC, MYBATIS_CONFIG_DTD);

        doctypeMap.put(IBATIS_MAPPER_SYSTEM, MYBATIS_MAPPER_DTD);
        doctypeMap.put(IBATIS_MAPPER_PUBLIC, MYBATIS_MAPPER_DTD);

        doctypeMap.put(MYBATIS_CONFIG_SYSTEM, MYBATIS_CONFIG_DTD);
        doctypeMap.put(MYBATIS_CONFIG_PUBLIC, MYBATIS_CONFIG_DTD);

        doctypeMap.put(MYBATIS_MAPPER_SYSTEM, MYBATIS_MAPPER_DTD);
        doctypeMap.put(MYBATIS_MAPPER_PUBLIC, MYBATIS_MAPPER_DTD);
    }

    /**
     * 核心就是覆盖这个方法，达到转public DTD到本地DTD的目的
     * Converts a public DTD into a local one
     *
     * @param publicId The public id that is what comes after "PUBLIC"
     * @param systemId The system id that is what comes after the public id.
     * @return The InputSource for the DTD
     * @throws org.xml.sax.SAXException If anything goes wrong
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {

        if (publicId != null) {
            publicId = publicId.toUpperCase(Locale.ENGLISH);
        }
        if (systemId != null) {
            systemId = systemId.toUpperCase(Locale.ENGLISH);
        }

        InputSource inputSource = null;
        try {
            //先找publicId，找不到再找systemId，貌似不可能找不到的说
            String path = doctypeMap.get(publicId);
            inputSource = getInputSource(path, inputSource);
            if (inputSource == null) {
                path = doctypeMap.get(systemId);
                inputSource = getInputSource(path, inputSource);
            }
        } catch (Exception e) {
            throw new SAXException(e.toString());
        }
        return inputSource;
    }

    private InputSource getInputSource(String path, InputSource source) {
        if (path != null) {
            InputStream in;
            try {
                in = Resources.getResourceAsStream(path);
                source = new InputSource(in);
            } catch (IOException e) {
                // ignore, null is ok
            }
        }
        return source;
    }

}