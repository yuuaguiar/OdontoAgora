# OdontoAgora

Projeto desenvolvido para a disciplina de Desenvolvimento Mobile da Unoesc.

O OdontoAgora é um aplicativo Android voltado a situações de urgência odontológica. A ideia é permitir que o paciente relate o problema e encontre um dentista disponível, enquanto o profissional recebe e pode aceitar o chamado.

O protótipo das telas foi criado na Atividade Avaliativa 1 e está disponível no [Figma](https://www.figma.com/design/VKtK6nW2FOCpDoYLZ8ci4l/OdontoAgora---FINAL?node-id=0-1).

## Tecnologias usadas

- Java
- Android Studio
- Gradle
- XML para as telas
- Python para a API
- SQLite para armazenar os dados da demonstração

## Como executar

1. Abra a pasta do projeto no Android Studio.
2. Inicie a API pelo arquivo `INICIAR-DEMONSTRACAO.cmd` ou, no terminal, execute:

```sh
python server/server.py --demo
```

3. Execute o aplicativo em um emulador Android.

Para a demonstração, o emulador acessa a API local pelo endereço `http://10.0.2.2:8080`.

## Contas de demonstração

| Perfil | E-mail | Senha |
|---|---|---|
| Paciente | paciente@demo.local | Odonto123! |
| Dentista | dentista@demo.local | Odonto123! |

## Fluxo principal

1. O paciente informa o tipo e a intensidade da dor.
2. Um chamado é enviado para dentistas disponíveis.
3. O dentista visualiza e aceita o atendimento.
4. O paciente recebe os dados da clínica e o prazo de chegada.
5. O atendimento pode ser concluído no aplicativo.

## Observações

Este é um projeto acadêmico e utiliza dados fictícios. A API roda localmente para fins de demonstração. Recursos como GPS, pagamento, notificações em segundo plano e validação profissional ficam como possibilidades para versões futuras.
