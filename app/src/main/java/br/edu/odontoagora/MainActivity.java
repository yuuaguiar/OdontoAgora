package br.edu.odontoagora;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import org.json.JSONObject;

/** Tela 1 do Figma: Boas-vindas / Entrada do OdontoAgora */
public class MainActivity extends Activity {
    private Session session;
    private ApiClient api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        session = new Session(this);
        api = new ApiClient();

        Button btnPaciente = findViewById(R.id.btn_sou_paciente);
        Button btnDentista = findViewById(R.id.btn_sou_dentista);

        // Fluxo ao clicar em Sou Paciente -> Abre Tela 2 (Triagem) instantaneamente
        btnPaciente.setOnClickListener(v -> {
            autoLogin("patient");
            Intent intent = new Intent(MainActivity.this, TriageActivity.class);
            startActivity(intent);
        });

        // Fluxo ao clicar em Sou Dentista -> Abre Tela 4 (Tela do Dentista) instantaneamente
        btnDentista.setOnClickListener(v -> {
            autoLogin("dentist");
            Intent intent = new Intent(MainActivity.this, DentistActivity.class);
            startActivity(intent);
        });
    }

    private void autoLogin(String role) {
        JSONObject data = new JSONObject();
        try {
            data.put("email", role.equals("patient") ? "paciente@demo.local" : "dentista@demo.local");
            data.put("password", "Odonto123!");
        } catch (Exception ignored) {}

        api.request(session.base(), session.token(), "POST", "/auth/login", data, (result, error, status) -> {
            if (result != null && result.has("token")) {
                session.save(result);
            }
        });
    }

    @Override
    protected void onDestroy() {
        api.close();
        super.onDestroy();
    }
}
