package dev.assemble.app.data.repository;

/** Callback simples de repositório: sucesso ou exceção explícita (rede/auth/API). */
public interface Callback<T> {
    void onSuccess(T value);

    void onError(Exception e);
}
