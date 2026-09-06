package br.edu.odontoagora;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONObject;

/** Tela 2 do Figma: Triagem ("O que aconteceu?") */
public class TriageActivity extends Activity {
    private boolean swelling = true;
    private boolean bleeding = false;
    private int painLevel = 8;
    private String selectedSymptom = "Dor intensa";
    private ApiClient api;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triage);

        api = new ApiClient();
        session = new Session(this);

        Button btnVoltar = findViewById(R.id.btn_voltar_triagem);
        RadioGroup rgSintomas = findViewById(R.id.rg_sintomas);
        SeekBar seekDor = findViewById(R.id.seek_dor);
        TextView tvValorDor = findViewById(R.id.tv_valor_dor);
        LinearLayout cardInchaco = findViewById(R.id.card_toggle_inchaco);
        TextView tvInchaco = findViewById(R.id.tv_toggle_inchaco);
        LinearLayout cardSangramento = findViewById(R.id.card_toggle_sangramento);
        TextView tvSangramento = findViewById(R.id.tv_toggle_sangramento);
        EditText etCidade = findViewById(R.id.et_cidade);
        Button btnBuscar = findViewById(R.id.btn_buscar_atendimento);

        btnVoltar.setOnClickListener(v -> finish());

        // Sintoma selecionado
        rgSintomas.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_dor_intensa) selectedSymptom = "Dor intensa";
            else if (checkedId == R.id.rb_dente_quebrado) selectedSymptom = "Dente quebrado";
            else if (checkedId == R.id.rb_sangramento) selectedSymptom = "Sangramento";
            else if (checkedId == R.id.rb_inchaco) selectedSymptom = "Inchaço";
        });

        // Slider de Dor
        seekDor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                painLevel = progress;
                tvValorDor.setText(progress + " / 10");
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Toggle Inchaço Facial
        cardInchaco.setOnClickListener(v -> {
            swelling = !swelling;
            tvInchaco.setText(swelling ? "SIM" : "NÃO");
            tvInchaco.setTextColor(getColor(swelling ? R.color.danger : R.color.muted));
        });

        // Toggle Sangramento Intenso
        cardSangramento.setOnClickListener(v -> {
            bleeding = !bleeding;
            tvSangramento.setText(bleeding ? "SIM" : "NÃO");
            tvSangramento.setTextColor(getColor(bleeding ? R.color.danger : R.color.muted));
        });

        // Ao buscar, abre a tela de acompanhamento.
        btnBuscar.setOnClickListener(v -> {
            String rawCity = etCidade.getText().toString().trim();
            final String city = rawCity.isEmpty() ? "Joaçaba" : rawCity;

            JSONObject data = new JSONObject();
            try {
                data.put("symptom", selectedSymptom);
                data.put("pain", painLevel);
                data.put("swelling", swelling);
                data.put("bleeding", bleeding);
                data.put("city", city);
                data.put("description", "Dor intensa relatada pelo paciente na triagem móvel.");
            } catch (Exception ignored) {}

            // Envia para a API em paralelo e abre a Tela 3 imediatamente
            api.request(session.base(), session.token(), "POST", "/calls", data, (result, error, status) -> {});

            Intent intent = new Intent(TriageActivity.this, SearchActivity.class);
            intent.putExtra("callId", 2481L);
            intent.putExtra("symptom", selectedSymptom);
            intent.putExtra("pain", painLevel);
            intent.putExtra("city", city);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        api.close();
        super.onDestroy();
    }
}
