package hasler.fpaaapp.utils;

/**
 * Created by Brian on 12/4/17.
 */

public class ReadTimeOutException extends Exception {
    // Parameterless Constructor
    public ReadTimeOutException() {}

    // Constructor that accepts a message
    public ReadTimeOutException(String message)
    {
        super(message);
    }
}
