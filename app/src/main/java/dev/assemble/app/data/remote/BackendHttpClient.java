package dev.assemble.app.data.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import dev.assemble.app.BuildConfig;

/**
 * HTTP autenticado (contrato V1 §1).
 * Base URL de BuildConfig.BACKEND_URL (local.properties/BACKEND_URL).
 * Toda chamada envia Authorization: Bearer &lt;Firebase ID token&gt;;
 * backend deriva o uid do token.
 *
 * <p>SEM backend real (BACKEND_URL vazio): lança {@link ApiException} explícita
 * (DEV_NO_BACKEND) — stub DEV identificado, sem dados falsos.
 *
 * <p>Erro externo ausente: propaga 502/503 com {error, provider} sem inventar
 * biografia/score.
 */
public class BackendHttpClient {
    public static final String DEV_NO_BACKEND =
            "DEV_NO_BACKEND: BACKEND_URL vazio (local.properties). Sem backend real; "
            + "nenhum dado de rede disponível. Não usar dados mockados como reais.";

    private final String baseUrl;

    public BackendHttpClient() {
        this(BuildConfig.BACKEND_URL);
    }

    public BackendHttpClient(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
    }

    public boolean isAvailable() {
        return !baseUrl.isEmpty();
    }

    public void requireAvailable() throws ApiException {
        if (!isAvailable()) {
            throw new ApiException(DEV_NO_BACKEND, null);
        }
    }

    public String get(String path, String query, String idToken) throws ApiException {
        requireAvailable();
        try {
            URL url = new URL(baseUrl + path + (query != null && !query.isEmpty() ? "?" + query : ""));
            HttpURLConnection conn = open(url, idToken);
            conn.setRequestMethod("GET");
            return read(conn);
        } catch (IOException e) {
            throw new ApiException("network/get-failed: " + e.getMessage(), e);
        }
    }

    public String post(String path, String jsonBody, String idToken) throws ApiException {
        requireAvailable();
        try {
            URL url = new URL(baseUrl + path);
            HttpURLConnection conn = open(url, idToken);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] body = jsonBody != null ? jsonBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            return read(conn);
        } catch (IOException e) {
            throw new ApiException("network/post-failed: " + e.getMessage(), e);
        }
    }

    private HttpURLConnection open(URL url, String idToken) throws IOException, ApiException {
        if (idToken == null || idToken.isEmpty()) {
            throw new ApiException("auth/empty-token: sem ID token para Authorization Bearer");
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Authorization", "Bearer " + idToken);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String read(HttpURLConnection conn) throws IOException, ApiException {
        int status = conn.getResponseCode();
        InputStream in = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String body = in != null ? readAll(in) : "";
        if (status < 200 || status >= 300) {
            // 502/503 de provider externo: propaga {error, provider} sem inventar conteúdo.
            throw new ApiException(status, extractProvider(body), "HTTP " + status + ": " + body);
        }
        return body;
    }

    private static String extractProvider(String body) {
        if (body != null && body.contains("\"provider\"")) {
            int i = body.indexOf("\"provider\"");
            int c = body.indexOf(':', i);
            int q1 = body.indexOf('"', c + 1);
            int q2 = q1 >= 0 ? body.indexOf('"', q1 + 1) : -1;
            if (q1 >= 0 && q2 > q1) {
                return body.substring(q1 + 1, q2);
            }
        }
        return null;
    }

    private static String readAll(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }
}
