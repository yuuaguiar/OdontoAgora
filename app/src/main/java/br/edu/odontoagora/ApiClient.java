package br.edu.odontoagora;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTP outside the UI thread. No credentials or clinical information are logged. */
public final class ApiClient {
    public interface Callback { void done(JSONObject result, String error, int status); }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    public void request(String base, String token, String method, String path, JSONObject body, Callback callback) {
        executor.execute(() -> {
            JSONObject result = null;
            String error = null;
            int status = 0;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(base + path).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");
                if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (java.io.OutputStream out = connection.getOutputStream()) {
                        out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
                status = connection.getResponseCode();
                InputStream source = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                if (source == null) throw new java.io.IOException("Empty response");
                try (InputStream in = source; java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
                    byte[] chunk = new byte[4096]; int count;
                    while ((count = in.read(chunk)) != -1) {
                        if (buffer.size() + count > 2_000_000) throw new java.io.IOException("Response too large");
                        buffer.write(chunk, 0, count);
                    }
                    result = new JSONObject(buffer.toString("UTF-8"));
                }
                if (status < 200 || status >= 300) error = result.optString("error", "Não foi possível concluir. Tente novamente.");
            } catch (Exception exception) {
                error = "Não foi possível conectar ao servidor. Confira a conexão e tente novamente. Seus dados nesta tela foram mantidos.";
            } finally {
                if (connection != null) connection.disconnect();
            }
            JSONObject value = result; String message = error; int code = status;
            if (!closed) main.post(() -> { if (!closed) callback.done(value, message, code); });
        });
    }
    public void close() { closed = true; executor.shutdownNow(); }
}
