package org.apache.ibatis.executor.loader;

import java.io.ObjectStreamException;

/**
 * @author Eduardo Macarron
 */
public interface WriteReplaceInterface {

    Object writeReplace() throws ObjectStreamException;
}
