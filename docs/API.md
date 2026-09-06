# Contrato da API REST

Base de demonstração: `http://127.0.0.1:8080`. Cabeçalho para rotas autenticadas: `Authorization: Bearer <token>`. Envie objetos JSON com `Content-Type: application/json`. Respostas não devem ser armazenadas em cache.

| Método e rota | Acesso | Uso |
|---|---|---|
| GET /health | Público | Saúde do serviço e horário do servidor |
| POST /auth/register | Público | Cadastro e criação de sessão |
| POST /auth/login | Público | Login e criação de sessão |
| POST /auth/logout | Autenticado | Revogação da sessão atual |
| GET /me | Autenticado | Perfil da sessão |
| POST /me/availability | Dentista | Ativar ou pausar plantão |
| POST /calls | Paciente | Criar chamado |
| GET /calls | Autenticado | Histórico próprio ou fila local e casos do dentista |
| GET /calls/{id} | Participante ou dentista local elegível | Consultar chamado |
| POST /calls/{id}/accept | Dentista elegível | Aceitar o primeiro chamado da fila |
| POST /calls/{id}/decline | Dentista elegível | Recusar apenas para este profissional |
| POST /calls/{id}/status | Dentista responsável | Atualizar estado |
| POST /calls/{id}/cancel | Participante | Cancelar com motivo |

## Objetos de entrada

Cadastro de paciente:

```json
{"name":"Paciente Exemplo","email":"exemplo@demo.local","password":"Exemplo123!","role":"patient","city":"Joaçaba"}
```

Para dentista, use `role: "dentist"` e acrescente `clinic`, `address` e `cro`. Login recebe `email` e `password`. Login e cadastro retornam `token` e `user`. Os tokens expiram em 24 horas e o servidor guarda apenas seu hash SHA-256.

Criação de chamado:

```json
{"symptom":"Dor intensa","pain":8,"swelling":true,"bleeding":false,"city":"Joaçaba","description":"Relato fictício para demonstração"}
```

Ocorrências: `Dor intensa`, `Dente quebrado`, `Sangramento`, `Inchaço`. Dor: inteiro de 0 a 10. Descrição: até 500 caracteres. A resposta contém `call` e `server_time`. Lista retorna `calls`.

Disponibilidade: `{"available":true}`. Aceite e recusa recebem `{}`. Cancelamento recebe `{"reason":"Não preciso mais de atendimento"}`.

Confirmação da chegada:

```json
{"status":"in_care","arrival_code":"1234"}
```

O código acima é apenas um exemplo. O servidor gera um código aleatório por reserva e o mostra apenas ao paciente. O dentista informa o código recebido presencialmente.

Conclusão: `{"status":"completed","outcome":"Desfecho registrado"}`.

Retorno: `{"status":"return_needed","outcome":"Reavaliar","return_days":7}`.

## Estados e garantias

`queued -> accepted -> in_care -> completed`.

Alternativa: `in_care -> return_needed -> completed`.

Qualquer estado ativo pode passar para `cancelled`, com motivo. `accepted` passa para `expired` ao terminar o prazo sem confirmação de chegada. A expiração é calculada pelo servidor nas requisições e não depende do relógio do celular.

Um paciente pode ter apenas um chamado ativo. Um dentista pode ter apenas um caso aceito ou em atendimento. O aceite utiliza transação SQLite `BEGIN IMMEDIATE`: duas tentativas concorrentes não conseguem reservar o mesmo caso. A ordenação usa data de criação e ID como desempate. A recusa libera o próximo caso para aquele profissional e mantém o caso original disponível para outros dentistas.

Busca por cidade normalizada sem diferença entre maiúsculas e acentos. Estado/UF e grafia ainda precisam ser consistentes entre cadastro e solicitação. O MVP não calcula distância.

## Erros

Erros retornam `{"error":"Mensagem em português"}`. Códigos: 400 para validação, 401 para sessão ausente ou expirada, 403 para acesso negado, 404 para recurso inexistente, 405 para método inválido, 409 para conflito e 413 para corpo acima de 16 KiB. Sucesso retorna 200.

## Segurança e implantação

Senhas usam PBKDF2-HMAC-SHA256 com salt aleatório e 200 mil iterações. Consultas usam parâmetros. As permissões são verificadas no servidor, independentemente dos botões do aplicativo. As respostas não incluem hashes de senha ou tokens de outros usuários. O código de chegada não aparece nas respostas para dentistas.

O servidor local não implementa limitação de tentativas, verificação de e-mail/CRO, recuperação de senha, monitoramento, políticas de retenção nem auditoria clínica. Uma publicação real exige essas medidas, revisão de segurança e HTTPS com infraestrutura de produção. Não há declaração de conformidade integral com LGPD.
