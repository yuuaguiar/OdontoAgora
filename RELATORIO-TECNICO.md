# RELATÓRIO TÉCNICO DE DESENVOLVIMENTO: ODONTOAGORA
## Atividade Avaliativa 2 – Atividade Problematizadora (Peso: 4,0)
**Componente Curricular:** Desenvolvimento Mobile  
**Acadêmico:** Yuri Aguiar Urbano  
**Projeto:** OdontoAgora – Aplicativo Mobile para Atendimento Odontológico de Urgência  
**Tecnologias:** Android Studio, Java (JDK 17), Gradle Wrapper, REST API (HTTP/JSON), SQLite  
**Data:** Setembro de 2026  

---

## 1. RESUMO EXECUTIVO E CONTEXTO DO PROBLEMA

O **OdontoAgora** é uma solução móvel projetada para solucionar uma dor real e frequente na área de saúde suplementar: a extrema dificuldade enfrentada por pacientes para encontrar atendimento odontológico emergencial fora do horário comercial (noites, madrugadas, finais de semana e feriados), sobretudo em cidades do interior e de médio porte.

Nessas ocasiões, pacientes com quadros agudos (como dor de dente lancinante, fraturas coronárias, hemorragias após extrações ou inchaços faciais) costumam enfrentar uma busca exaustiva por telefone ou são forçados a aguardar o próximo dia útil. Do outro lado da cadeia, clínicas e dentistas dispostos a realizar plantões de urgência carecem de um canal digital centralizado e seguro para triagem, recebimento de chamados e controle de fluxo.

Como continuidade direta da **Atividade Avaliativa 1**, este projeto implementou o MVP (Produto Mínimo Viável) planejado, transformando a concepção teórica e os protótipos em um aplicativo nativo Android 100% funcional, integrado a uma API de comunicação cliente-servidor com persistência transacional.

---

## 2. ESCOLHAS TÉCNICAS E ARQUITETURA DE DESENVOLVIMENTO

### 2.1. Transição da Plataforma: De Híbrido para Nativo (Java + Android Studio)
Na Atividade 1, foi aventado o uso preliminar de frameworks híbridos (React Native/Expo). Contudo, para atender plenamente às diretrizes pedagógicas e aos requisitos da Atividade 2, consolidou-se a decisão pelo desenvolvimento **nativo para Android**:
1. **Domínio do Ferramental Estudado:** Utilização profunda do ecossistema padrão da plataforma Android (Android Studio Iguana/Ladybug, Gradle Wrapper 9.5, Android Gradle Plugin 9.3, Java 17 nativo).
2. **Desempenho e Confiabilidade em Emergências:** Em situações de urgência e dor severa, o aplicativo deve abrir instantaneamente e apresentar baixíssimo consumo de memória, sem a sobrecarga de bridges JavaScript ou webviews.
3. **Ciclo de Vida Previsível:** O controle granular do ciclo de vida das Activities (`onCreate`, `onResume`, `onPause`, `onDestroy`, `onSaveInstanceState`) permitiu garantir que dados da triagem não sejam perdidos caso o usuário alterne de aplicativo.

### 2.2. Arquitetura da Solução
A aplicação foi estruturada em camadas bem definidas e desacopladas:

```
                  +-------------------------------------------------+
                  |               DISPOSITIVO ANDROID               |
                  |                                                 |
                  |   [MainActivity]  <--->  [Ui.java Componentes]   |
                  |          |                                      |
                  |   [Session.java] (SharedPreferences Cripto)     |
                  |          |                                      |
                  |   [ApiClient.java] (HttpURLConnection Assíncrono)|
                  +-------------------------------------------------+
                                         |
                            Requisições HTTP / JSON
                            (Bearer Token / Port 8080)
                                         v
                  +-------------------------------------------------+
                  |               SERVIDOR DE BACKEND               |
                  |                                                 |
                  |   [server.py] (Python REST Engine / HTTP 1.1)   |
                  |          |                                      |
                  |   [SQLite Transacional: BEGIN IMMEDIATE]        |
                  |   - Usuários / Senhas (PBKDF2-HMAC-SHA256)      |
                  |   - Fila de Chamados e Ciclo de 45 min          |
                  +-------------------------------------------------+
```

* **Camada de Apresentação (`MainActivity.java` & `Ui.java`):** Coordena os estados de tela, renderização de layouts XML com `ConstraintLayout`/`LinearLayout`, componentes ergonômicos e validação dinâmica de formulários.
* **Camada de Sessão (`Session.java`):** Gerencia persistência local segura via `SharedPreferences`. O token de acesso JWT/Bearer é armazenado de forma isolada (`MODE_PRIVATE`), e senhas nunca são persistidas no cliente.
* **Camada de Conectividade (`ApiClient.java`):** Executa todas as chamadas de rede fora da *Main Thread* (UI Thread) por meio de um `ExecutorService` dedicado, reportando os resultados na UI via `Handler(Looper.getMainLooper())`. Implementa timeouts estritos de 8 segundos, limite de buffer seguro de 2 MB e tratamento de falhas de conexão.
* **Camada de Servidor e Persistência (`server/server.py`):** Servidor HTTP/JSON que implementa autenticação com hash PBKDF2 com 200.000 iterações, bloqueio de concorrência com transações imediatas no SQLite (`BEGIN IMMEDIATE`) e controle rigoroso de expiração e transição de estados dos chamados.

---

## 3. MAPEAMENTO E IMPLEMENTAÇÃO DOS REQUISITOS (RF E RNF)

Todos os requisitos levantados no documento de planejamento da Atividade 1 foram atendidos e verificados no aplicativo:

| ID | Requisito Planejado (Atividade 1) | Solução Adotada e Implementada na Atividade 2 | Status |
|---|---|---|---|
| **RF01** | Cadastro e acesso de pacientes e dentistas | Telas de autenticação dedicadas para cada perfil, com validação de formato de e-mail e força de senha (mínimo 8 caracteres). | **Atendido** |
| **RF02** | Localização por cidade ou dispositivo | Informação da cidade normalizada na triagem e no perfil; suporte a integração externa com apps de GPS via intent `geo:0,0?q=`. | **Atendido** |
| **RF03** | Triagem inicial com tipos de ocorrência | Seleção ergonômica de ocorrência através de botões com ícones vetoriais: Dor intensa, Dente quebrado, Sangramento, Inchaço. | **Atendido** |
| **RF04** | Intensidade da dor de 0 a 10 | Componente deslizante (`SeekBar`) de 0 a 10 com feedback textual em tempo real ("5 / 10"). | **Atendido** |
| **RF05** | Informações adicionais do problema | Campo de texto livre com limite estrito de até 500 caracteres para relato complementar do paciente. | **Atendido** |
| **RF06** | Criação e encaminhamento do chamado | Rota `POST /calls` envia a ocorrência ao servidor, que a aloca na fila de espera dos dentistas da mesma cidade. | **Atendido** |
| **RF07** | Organização da fila de atendimento | Fila ordenada rigorosamente por ordem de chegada com prioridade e garantia transacional atômica contra race conditions. | **Atendido** |
| **RF08** | Aceite e visualização pelo dentista | O dentista visualiza a queixa clínica detalhada antes de aceitar. Apenas um profissional pode reservar o chamado. | **Atendido** |
| **RF09** | Notificação sonora e visual no aceite | Quando o status muda de `queued` para `accepted`, o app emite aviso sonoro nativo (`ToneGenerator`) e exibe mensagem Toast. | **Atendido** |
| **RF10** | Tempo de 45 minutos para chegada | Contagem regressiva ativa baseada no relógio do servidor, impedindo que o paciente manipule o relógio do aparelho. | **Atendido** |
| **RF11** | Acompanhamento completo dos estados | O chamado transita pelos estados: `queued` (Na fila) $\rightarrow$ `accepted` (Aceito) $\rightarrow$ `in_care` (Em atendimento) $\rightarrow$ `return_needed` (Retorno) / `completed` (Concluído). | **Atendido** |
| **RF12** | Cancelamento com justificativa | Tanto paciente quanto dentista podem cancelar o atendimento ativo mediante preenchimento obrigatório do motivo. | **Atendido** |
| **RF13** | Orientação de segurança hospitalar | Mensagem clara de alerta institucional na tela inicial e na triagem: *"Em sinais graves, procure um serviço hospitalar"*. | **Atendido** |

### Requisitos Não Funcionais (RNF)
* **RNF01 (Usabilidade):** Fluxo simplificado em poucas etapas, botões largos (altura mínima de 48 a 56 dp) e contraste cromático elevado.
* **RNF02 (Desempenho):** Tempo de resposta instantâneo na UI e compilação otimizada com Android Gradle Plugin 9.3.
* **RNF03 e RNF04 (Segurança e LGPD):** Nenhuma senha ou credencial sensível é gravada em logs ou exposta em endpoints de listagem; dados transmitidos estritamente sob autenticação via Bearer token.
* **RNF05 (Disponibilidade):** O servidor e o app suportam consultas contínuas com mecanismo de polling leve (5 segundos em primeiro plano) sem consumir bateria excessiva.
* **RNF06 (Acessibilidade):** Elementos interativos contam com descrições de conteúdo acessíveis (`contentDescription`), e campos de texto possuem rótulos vinculados (`labelFor`).

---

## 4. INTEGRAÇÃO COM API E COMUNICAÇÃO COM SERVIDORES (CRITÉRIO 3 PONTOS)

O núcleo da comunicação cliente-servidor foi desenvolvido com foco em robustez, tolerância a falhas e desacoplamento.

### 4.1. Contrato de Endpoints REST
O aplicativo consome e produz payloads no formato `application/json`, suportando as seguintes rotas fundamentais:

1. **`GET /health`**: Verificação do status do servidor e sincronização de horário para contagem regressiva.
2. **`POST /auth/register` & `POST /auth/login`**: Criação de conta e emissão de token Bearer seguro.
3. **`GET /me` & `POST /me/availability`**: Consulta de perfil e alternância do plantão do dentista (ativo/pausado).
4. **`POST /calls`**: Criação de nova ocorrência pelo paciente com dados de triagem.
5. **`GET /calls` & `GET /calls/{id}`**: Obtenção de chamados ativos, histórico e acompanhamento em tempo real.
6. **`POST /calls/{id}/accept`**: Reserva exclusiva do chamado por um dentista de plantão.
7. **`POST /calls/{id}/status`**: Validação de código de chegada (`arrival_code`) de 4 dígitos para transição para `in_care`, ou conclusão com desfecho clínico.
8. **`POST /calls/{id}/cancel`**: Registro de cancelamento motivado.

### 4.2. Segurança e Controle de Concorrência
Para evitar que múltiplos dentistas aceitem a mesma urgência simultaneamente (*race condition*), o backend utiliza o mecanismo `BEGIN IMMEDIATE` do SQLite. Em testes automatizados com threads concorrentes disparando requisições paralelas para o mesmo chamado, exatamente **um** dentista obteve sucesso (código 200), enquanto os demais receberam retorno 409 (Conflito), sendo direcionados para o próximo chamado da fila.

### 4.3. Resiliência do Cliente Android (`ApiClient.java`)
O cliente HTTP nativo implementa:
* **Execução em Segundo Plano:** `ExecutorService` desacopla a requisição da renderização, evitando os erros de `NetworkOnMainThreadException` e ANR (*Application Not Responding*).
* **Timeouts Parametrizados:** 8.000 ms para conexão e leitura.
* **Tratamento de Sessão Expirada:** Interceptação automática de erros HTTP 401 com limpeza de cache e retorno seguro à tela de autenticação.
* **Preservação de Dados de Formulário:** Em caso de perda de sinal de internet, os dados preenchidos na tela de triagem permanecem intactos, permitindo novo envio sem redigitação.

---

## 5. EXPERIÊNCIA DO USUÁRIO (UX) E FIDELIDADE AO FIGMA (CRITÉRIO 2 PONTOS)

A interface gráfica foi construída para refletir com rigor o protótipo aprovado no Figma:

```
+------------------------+  +------------------------+  +------------------------+
|      OdontoAgora       |  |   O que aconteceu?     |  |      ● EM BUSCA        |
|                        |  |                        |  |                        |
|   [ Logo OdontoAgora ] |  |  ( ) Dor intensa       |  |  [ Radar / Mapa ]      |
|                        |  |  ( ) Dente quebrado    |  |                        |
|   "Quando a dor não    |  |  ( ) Sangramento       |  |  "Você é o próximo     |
|    pode esperar"       |  |  ( ) Inchaço           |  |   da fila (1º)"        |
|                        |  |                        |  |                        |
|  [ Sou paciente ]      |  |  NÍVEL DA DOR: 8 / 10  |  |  [✓] Chamado enviado   |
|  [ Sou dentista ]      |  |  [=======O==]          |  |  [✓] Profissionais OK  |
|                        |  |                        |  |  [⏳] Aguardando aceite|
|  Em sinais graves,     |  |  [ Buscar atendimento ]|  |                        |
|  procure o hospital.   |  |                        |  |  [ Cancelar chamado ]   |
+------------------------+  +------------------------+  +------------------------+
       Tela 1: Início             Tela 2: Triagem            Tela 3: Em Busca
```

### Decisões Ergonômicas:
1. **Paleta de Cores Noturna (Dark Mode por Padrão):** Fundo grafite profundo (`#121214` e `#1C1C20`) associado ao azul de destaque (`#0001FF`). Essa escolha reduz a fadiga visual e a fotofobia comum em episódios de dor de dente noturna.
2. **Semântica Visual Clara:** Verde (`#00D060`) para confirmação e sucesso, Amarelo (`#FFB800`) para fila e contagem regressiva, Vermelho (`#FF4444`) para níveis altos de dor e cancelamentos.
3. **Código de Chegada Seguro:** Ao ter o chamado aceito, o paciente recebe um código numérico de 4 dígitos na tela. O dentista digita esse código ao receber o paciente, garantindo a presença física antes de liberar o início do procedimento clínico.
4. **Alerta Sonoro no Aceite:** A emissão de um tom sonoro auditivo previne que o paciente em crise precise ficar olhando fixamente para a tela enquanto aguarda o profissional.

---

## 6. PLANO DE TESTES E RESULTADOS OBTIDOS (CRITÉRIO 3 PONTOS)

A estabilidade da solução foi comprovada por baterias de testes automatizados e validações funcionais no emulador Android.

### 6.1. Testes Automatizados da API (`test_api.py`)
Foram executados 13 testes de integração com banco temporário SQLite real e servidor HTTP em execução:

```
test_authentication_and_logout ......................................... OK
test_cancellation_requires_reason_and_releases_patient ................. OK
test_city_and_availability_filter ...................................... OK
test_duplicate_active_call ............................................. OK
test_expiration_enforced_by_server ..................................... OK
test_full_lifecycle_and_persistence .................................... OK
test_patient_isolation_and_role_permissions ............................ OK
test_queue_appears_before_closed_history ............................... OK
test_queue_order_and_decline ........................................... OK
test_return_status ..................................................... OK
test_simultaneous_accepts_have_one_winner (Concorrência/Race condition)  OK
test_validation ........................................................ OK
test_wrong_arrival_code_and_transition ................................. OK
----------------------------------------------------------------------
Ran 13 tests in 9.746s - RESULTADO: APROVADO (100% de sucesso)
```

### 6.2. Testes de Compilação do Aplicativo Android
* **Compilação Gradle:** `gradlew.bat assembleDebug` executado com sucesso (BUILD SUCCESSFUL em 10s, 34 tasks validadas).
* **Compatibilidade:** Android 8.0 (API 26) até Android 15 (API 35).
* **Tratamento de Estado:** Testado com rotação de tela e encerramento de processo em segundo plano, preservando o formulário preenchido via `Bundle onSaveInstanceState`.

---

## 7. LIMITES DO PROJETO ACADÊMICO E EVOLUÇÕES FUTURAS

Como delimitação consciente do escopo de um MVP acadêmico:
* **Simulação de Localização:** A busca utiliza a cidade digitada pelo usuário. Em uma versão comercial, será integrado o Google Location Services (GPS) com geocodificação reversa e cálculo de rota por distância quilométrica real.
* **Validação de CRO:** O registro do Conselho Regional de Odontologia no MVP é declaratório. Uma versão final consumirá webservices dos Conselhos Regionais (CFO/CRO) para validação de regularidade do registro profissional.
* **Notificações Push:** O aplicativo atual realiza atualização por consulta contínua (polling de 5 segundos) em primeiro plano. A arquitetura futura prevê a substituição pelo Firebase Cloud Messaging (FCM) para notificações push com o app em segundo plano.

---

## 8. CONCLUSÃO

A **Atividade Avaliativa 2** cumpriu integralmente todos os requisitos pedagógicos estabelecidos para o componente curricular de Desenvolvimento Mobile. O projeto **OdontoAgora** consolidou-se como um aplicativo nativo robusto, intuitivo e com arquitetura escalável, articulando conhecimentos avançados de interface gráfica Android, manipulação do ciclo de vida, programação assíncrona, integração com serviços web REST e persistência de dados.

O código-fonte encontra-se pronto e estruturado para publicação em repositório público no GitHub, acompanhado de roteiro de demonstração prático para a banca de apresentação final.
