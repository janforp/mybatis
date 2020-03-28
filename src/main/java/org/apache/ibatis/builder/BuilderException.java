package org.apache.ibatis.builder;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * @author Clinton Begin
 */

/**
 * 构建异常,继承PersistenceException，没啥好说的，就是语义分类
 */
public class BuilderException extends PersistenceException {

    private static final long serialVersionUID = -3885164021020443281L;

    public BuilderException() {
        super();
    }

    public BuilderException(String message) {
        super(message);
    }

    public BuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public BuilderException(Throwable cause) {
        super(cause);
    }
}
