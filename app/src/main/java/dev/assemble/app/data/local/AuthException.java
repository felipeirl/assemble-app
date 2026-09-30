package dev.assemble.app.data.local;

/** Erro explícito de autenticação/rede de auth (nunca silencioso). */
public class AuthException extends Exception {
    private final String code;

    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AuthException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
