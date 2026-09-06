# OdontoAgora

Aplicativo Android de demonstração acadêmica para solicitar atendimento odontológico de urgência. Continuação da Atividade Avaliativa 1 de Desenvolvimento Mobile, de Yuri Aguiar Urbano.

## Tecnologias

- Aplicativo nativo: Java 17, layouts e recursos XML, Android SDK.
- Compilação: Gradle Wrapper 9.5.0 e Android Gradle Plugin 9.3.1.
- API própria: Python 3.10 ou superior, HTTP/JSON, sem dependências externas.
- Persistência: SQLite, transações e consultas parametrizadas.
- Compatibilidade declarada: Android 8.0 ou superior. Teste de execução realizado em Android 11, API 30.
- SDK de compilação: Android 37.0. Target SDK: 35.

## Executar no computador preparado

Abra `INICIAR-DEMONSTRACAO.cmd` na pasta do projeto. Ele inicia a API, abre o emulador configurado e instala o APK. O arquivo `local-demo.json` contém apenas caminhos desta máquina e fica fora do Git.

Abra `ABRIR-ANDROID-STUDIO.cmd` para ver o código no Android Studio. Aguarde a sincronização do Gradle. O botão triangular de execução compila e instala o app no dispositivo selecionado. A API precisa continuar em execução.

## Executar em outra máquina

1. Instale o Android Studio, o SDK Android 37.0, Build Tools 36.0.0 e um emulador Android API 26 ou superior.
2. Abra esta pasta no Android Studio. Use o JDK incluído no Android Studio. O projeto usa código Java 17, e a máquina de desenvolvimento usa JBR 25.
3. Instale Python 3.10 ou superior. Em um terminal na pasta do projeto, execute:

```sh
python server/server.py --demo
```

4. Execute o aplicativo no emulador. A URL padrão do APK debug é `http://10.0.2.2:8080`.
5. Na tela inicial, escolha o perfil e toque em **Entrar na demonstração**.

### Contas fictícias

| Perfil | E-mail | Senha de demonstração |
|---|---|---|
| Paciente | paciente@demo.local | Odonto123! |
| Dentista | dentista@demo.local | Odonto123! |

As contas só são criadas com `--demo`. Clínica, CRO, endereço e nomes são fictícios. Não utilize informações reais de saúde.

### Celular conectado por USB

Ative a depuração USB no seu aparelho e autorize o computador. Execute `adb reverse tcp:8080 tcp:8080`. Na tela inicial do app, abra **Opções > Conexão com o servidor**, informe `http://127.0.0.1:8080` e toque em **Salvar e testar**. Esta conexão HTTP é permitida apenas na variante debug.

## Fluxo de demonstração

1. Paciente: solicite atendimento, preencha a ocorrência e envie o chamado.
2. Saia da conta em **Opções** e entre como dentista. O paciente pode ficar conectado em outro emulador ou aparelho para mostrar a atualização automática.
3. Dentista: mantenha o plantão ativo, abra o primeiro chamado e aceite.
4. Paciente: confira o dentista, a clínica, o prazo de 45 minutos e o código de chegada.
5. Dentista: confirme a chegada com o código mostrado ao paciente, registre o desfecho e conclua ou indique retorno.
6. Paciente: consulte a conclusão no histórico.

A busca usa a cidade digitada. A imagem de mapa é uma ilustração identificada como tal. O aplicativo abre um app de mapas externo para consultar o endereço da clínica, quando há um instalado.

## Compilar e testar

```sh
# Windows
gradlew.bat assembleDebug lintDebug

# Linux/macOS
./gradlew assembleDebug lintDebug

# Testes da API, com banco temporário e servidor HTTP reais
cd server
python -m unittest -v test_api

# Teste das telas: servidor --demo ativo e APK já instalado no emulador
cd ..
python scripts/smoke_android.py --serial emulator-5554
```

O teste das telas limpa os dados apenas deste aplicativo no dispositivo escolhido e encerra chamados ativos da conta fictícia `paciente@demo.local`. Não execute esse teste contra dados reais.

APK gerado: `app/build/outputs/apk/debug/app-debug.apk`.

## Organização

```text
app/src/main/java/br/edu/odontoagora/
    MainActivity.java    Telas, navegação e coordenação dos fluxos
    Ui.java              Componentes nativos reutilizáveis
    ApiClient.java       HTTP assíncrono, timeout e tratamento de erros
    Session.java         Sessão privada, sem guardar a senha
app/src/main/res/        Layouts XML, cores e recursos do Figma
server/server.py         API, autenticação, regras e persistência
server/test_api.py       Testes de integração
scripts/smoke_android.py Teste completo no Android
docs/                   Relatório, contrato da API e capturas
```

## Limites do MVP

O servidor roda localmente e precisa estar ligado. A entrega não inclui hospedagem pública, notificações push em segundo plano, validação de CRO, pagamento, avaliação de profissionais, chat, videochamada, GPS ou classificação clínica automática. A fila usa uma prioridade única com ordem de chegada. A disponibilidade e as atualizações de chamados chegam por consulta HTTP a cada cinco segundos enquanto a tela correspondente está aberta.

O cadastro de dentistas é demonstrativo e não representa credenciamento. A variante release exige HTTPS e contém uma URL inválida intencionalmente, até existir um servidor de produção configurado. O servidor HTTP da biblioteca padrão atende ao teste local, não deve ser exposto diretamente à internet.

## Referências

- [Protótipo original no Figma](https://www.figma.com/design/VKtK6nW2FOCpDoYLZ8ci4l/OdontoAgora---FINAL?node-id=0-1).
- [Compatibilidade do Android Gradle Plugin 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
- [Rede do Android Emulator](https://developer.android.com/studio/run/emulator-networking-address).

O logotipo, os ícones de ocorrência e o mapa ilustrativo foram exportados do arquivo Figma fornecido para este projeto. O desenvolvimento contou com assistência de IA e deve ser revisado e estudado pelo estudante para a apresentação.
