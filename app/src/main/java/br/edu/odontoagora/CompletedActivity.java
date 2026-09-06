package br.edu.odontoagora;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/** Tela 6 do Figma: Atendimento concluído & Avaliação com 5 estrelas */
public class CompletedActivity extends Activity {
    private int selectedRating = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);

        TextView[] stars = new TextView[]{
            findViewById(R.id.star1),
            findViewById(R.id.star2),
            findViewById(R.id.star3),
            findViewById(R.id.star4),
            findViewById(R.id.star5)
        };

        // Interatividade com as 5 estrelas do Figma
        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating = starIndex;
                for (int j = 0; j < stars.length; j++) {
                    stars[j].setTextColor(getColor(j < starIndex ? R.color.warning : R.color.muted));
                }
            });
        }

        Button btnAvaliar = findViewById(R.id.btn_avaliar_atendimento);
        Button btnInicio = findViewById(R.id.btn_voltar_inicio);

        // Fluxo ao clicar em Avaliar atendimento -> Retorna ao início
        btnAvaliar.setOnClickListener(v -> {
            Toast.makeText(this, "Avaliação de " + selectedRating + " estrelas enviada com sucesso!", Toast.LENGTH_LONG).show();
            goToHome();
        });

        // Fluxo ao clicar em Voltar ao início -> Retorna à Tela 1 (Boas-vindas)
        btnInicio.setOnClickListener(v -> goToHome());
    }

    private void goToHome() {
        Intent intent = new Intent(CompletedActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
