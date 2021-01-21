package exceptions;

/**
 * Exception when do not have require data
 */
public class NoSuchDataException extends RuntimeException{

    public NoSuchDataException() {
    }

    public NoSuchDataException(String s) {
        super(s);
    }

    public NoSuchDataException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NoSuchDataException(Throwable throwable) {
        super(throwable);
    }

    public NoSuchDataException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
