package org.apache.ibatis.exceptions;

/**
 * @author Clinton Begin
 */

/**
 * 结果太多异常,一般是预想select出一条记录，结果得到多于一条记录时会抛此异常
 */
public class TooManyResultsException extends PersistenceException {

    private static final long serialVersionUID = 8935197089745865786L;

    public TooManyResultsException() {
        super();
    }

    public TooManyResultsException(String message) {
        super(message);
    }

    public TooManyResultsException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooManyResultsException(Throwable cause) {
        super(cause);
    }
}
