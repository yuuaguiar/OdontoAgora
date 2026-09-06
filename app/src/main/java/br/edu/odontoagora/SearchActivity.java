package br.edu.odontoagora;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import org.json.JSONObject;

/** Tela 3 do Figma: Em Busca ("Procurando dentista disponível") */
public class SearchActivity extends Activity {
    private long callId;
    private ApiClient api;
    private Session session;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active = true;

    private final Runnable pollStatus = new Runnable() {
        @Override
        public void run() {
            if (!active) return;
            api.request(session.base(), session.token(), "GET", "/calls/" + callId, null, (result, error, status) -> {
                if (result != null && result.has("call")) {
                    JSONObject call = result.optJSONObject("call");
                    if (call != null && "accepted".equals(call.optString("status"))) {
                        goToConfirmed(call);
                        return;
                    }
                }
                if (active) handler.postDelayed(pollStatus, 4000);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        api = new ApiClient();
        session = new Session(this);
        callId = getIntent().getLongExtra("callId", 2481);

        Button btnSimular = findViewById(R.id.btn_simular_aceite);
        Button btnCancelar = findViewById(R.id.btn_cancelar_busca);

        // Fluxo ao clicar no botão de avanço/aceite -> Abre Tela 5 (Dentista Encontrado)
        btnSimular.setOnClickListener(v -> {
            api.request(session.base(), session.token(), "POST", "/calls/" + callId + "/accept", new JSONObject(), (r, e, s) -> {
                goToConfirmed(null);
            });
        });

        // Cancelar chamado -> Volta para a tela anterior
        btnCancelar.setOnClickListener(v -> {
            active = false;
            finish();
        });

        handler.postDelayed(pollStatus, 3000);
    }

    private void goToConfirmed(JSONObject call) {
        active = false;
        Intent intent = new Intent(SearchActivity.this, ConfirmedActivity.class);
        intent.putExtra("callId", callId);
        if (call != null) {
            intent.putExtra("callJson", call.toString());
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        active = false;
        handler.removeCallbacks(pollStatus);
        api.close();
        super.onDestroy();
    }
}
