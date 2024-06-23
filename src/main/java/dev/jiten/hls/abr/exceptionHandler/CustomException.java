package dev.jiten.hls.abr.exceptionHandler;

public class CustomException extends Exception{

    private String exceptionMessage;
    private Integer exceptionCode;

    public CustomException(String message, Integer code){
        super(message);
        this.exceptionMessage = message;
        this.exceptionCode = code;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public Integer getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(Integer exceptionCode) {
        this.exceptionCode = exceptionCode;
    }
}
