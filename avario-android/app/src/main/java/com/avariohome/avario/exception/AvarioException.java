package com.avariohome.avario.exception;


import com.google.firebase.crash.FirebaseCrash;

/**
 * Created by aeroheart-c6 on 10/01/2017.
 */
public class AvarioException extends Exception {
    private Object[] arguments;
    private int code; // note that this is hexadecimal

    public AvarioException(int code, Throwable throwable) {
        this(code, throwable, new Object[0]);
    }

    public AvarioException(int code, Throwable throwable, Object[] arguments) {
        super(throwable);
        this.code = code;
        this.arguments = arguments;
        FirebaseCrash.report(throwable);
    }

    public Object[] getMessageArguments() {
        return this.arguments;
    }

    public String getCode() {
        return String.format("0x%05X", this.code);
    }

    public int getCodeValue() {
        return this.code;
    }
}
