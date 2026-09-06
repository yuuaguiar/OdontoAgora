package br.edu.odontoagora;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import org.json.JSONObject;

/** Tela 4 do Figma: Tela do Dentista ("Chamado urgente #OA-2481") */
public class DentistActivity extends Activity {
    private ApiClient api;
    private Session session;
    private long callId = 2481;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dentist);

        api = new ApiClient();
        session = new Session(this);

        Button btnVoltar = findViewById(R.id.btn_voltar_dentista);
        Button btnAceitar = findViewById(R.id.btn_aceitar_atendimento);
        Button btnRecusar = findViewById(R.id.btn_recusar_atendimento);

        btnVoltar.setOnClickListener(v -> finish());
        btnRecusar.setOnClickListener(v -> finish());

        // Ao aceitar, abre a tela de confirmação.
        btnAceitar.setOnClickListener(v -> {
            api.request(session.base(), session.token(), "POST", "/calls/" + callId + "/accept", new JSONObject(), (result, error, status) -> {
                Intent intent = new Intent(DentistActivity.this, ConfirmedActivity.class);
                intent.putExtra("callId", callId);
                intent.putExtra("isDentist", true);
                startActivity(intent);
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        api.close();
        super.onDestroy();
    }
}
