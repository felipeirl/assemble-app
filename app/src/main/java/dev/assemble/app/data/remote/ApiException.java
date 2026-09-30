package dev.assemble.app.data.remote;

/** Erro explícito de rede/API (nunca silencioso, nunca dado inventado). */
public class ApiException extends Exception {
    private final int httpStatus;
    private final String provider;

    public ApiException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.provider = null;
    }

    public ApiException(int httpStatus, String provider, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.provider = provider;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
        this.provider = null;
    }

    public ApiException(String message) {
        super(message);
        this.httpStatus = -1;
        this.provider = null;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getProvider() {
        return provider;
    }
}
