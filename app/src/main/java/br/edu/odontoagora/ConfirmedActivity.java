package br.edu.odontoagora;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

/** Tela 5 do Figma: Dentista encontrado ("Seu horário está reservado por 45 minutos") */
public class ConfirmedActivity extends Activity {
    private CountDownTimer timer;
    private long callId = 2481;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmed);

        callId = getIntent().getLongExtra("callId", 2481);
        boolean isDentist = getIntent().getBooleanExtra("isDentist", false);

        TextView tvTimer = findViewById(R.id.tv_countdown_timer);
        TextView tvClinicaEndereco = findViewById(R.id.tv_clinica_endereco);
        Button btnConfirmar = findViewById(R.id.btn_confirmar_chegada);
        Button btnCancelar = findViewById(R.id.btn_cancelar_confirmado);

        // RF09: Notificação sonora de confirmação
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 400);
        } catch (Exception ignored) {}

        if (isDentist) {
            btnConfirmar.setText("Confirmar atendimento presencial");
        }

        // RF02: Abrir mapa/GPS ao clicar no endereço da clínica
        tvClinicaEndereco.setOnClickListener(v -> {
            Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode("Rua das Flores, 248, Joaçaba"));
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Endereço: Rua das Flores, 248", Toast.LENGTH_SHORT).show();
            }
        });

        // RF10: Cronômetro ativo de 45 minutos (44:32 decrescente em tempo real)
        long totalMillis = (44 * 60 + 32) * 1000;
        timer = new CountDownTimer(totalMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60));
            }
            public void onFinish() {
                tvTimer.setText("00:00");
            }
        }.start();

        // Ao confirmar, abre a tela de conclusão.
        btnConfirmar.setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmedActivity.this, CompletedActivity.class);
            intent.putExtra("callId", callId);
            startActivity(intent);
            finish();
        });

        // Cancelar chamado
        btnCancelar.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
