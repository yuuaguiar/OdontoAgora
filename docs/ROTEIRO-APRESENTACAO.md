# Roteiro da apresentação final

Sugestão: 7 a 10 minutos. Use o PowerPoint como apoio e reserve a maior parte do tempo para o aplicativo.

## 1. Problema e continuidade (1 minuto)

Explique que a Atividade 1 apresentou uma proposta para conectar pessoas com urgências odontológicas a dentistas disponíveis fora do horário comercial. Mostre que o aplicativo preserva o tema grafite, o azul e o fluxo do Figma.

## 2. Escolhas técnicas (1 minuto)

Explique a troca de React Native por Java para aplicar Android Studio e Gradle. O Android usa componentes nativos e recursos XML. A API Python recebe JSON, valida as ações e grava em SQLite. O servidor é local nesta demonstração.

## 3. Demonstração (4 minutos)

1. Abra `INICIAR-DEMONSTRACAO.cmd` antes de começar a fala.
2. Entre como paciente usando o botão de demonstração.
3. Solicite atendimento e explique os campos de ocorrência e dor.
4. Mostre a posição na fila.
5. Saia da conta e entre como dentista. Abra o chamado e aceite.
6. Volte ao perfil de paciente e mostre o prazo e o código de chegada. Anote o código.
7. Volte ao dentista, confirme a chegada usando esse código e conclua com um desfecho fictício.
8. Mostre que o paciente também vê a conclusão.

Se houver dois dispositivos, deixe um conectado como paciente e o outro como dentista. Assim o público verá a atualização sem trocar de conta. Se a conexão falhar, confira se a API está ligada e use as capturas do aplicativo como apoio.

## 4. Integração e testes (1 minuto)

Mostre `ApiClient.java` para explicar a requisição fora da thread principal. Mostre uma rota de criação ou aceite em `server.py`. Explique que o servidor impede dois dentistas de aceitarem o mesmo chamado e impede acesso a chamados de outro paciente. Cite os testes executados, sem prometer cobertura de produção.

## 5. Limites e evolução (1 minuto)

Explique que o MVP usa dados fictícios, busca por cidade e consulta periódica em primeiro plano. GPS, push em segundo plano, verificação de profissionais e hospedagem de produção são evoluções futuras. Pagamento, chat e videochamada permanecem fora do escopo original do MVP.

## Perguntas que você deve saber responder

- **O que é uma API?** É a interface HTTP que o aplicativo usa para consultar ou alterar dados no servidor.
- **Onde estão os dados?** Em um arquivo SQLite no servidor. O aplicativo guarda apenas a sessão e a configuração da conexão.
- **O que o Gradle faz?** Resolve ferramentas/dependências e executa compilação, empacotamento e verificações.
- **Por que usar uma thread separada?** Para a rede não travar os toques e a rolagem da interface.
- **Por que o prazo fica no servidor?** Para fechar o app ou mudar o relógio do celular não renovar a reserva.
- **O mapa é real?** A figura é ilustrativa. A busca do MVP usa a cidade informada, e a rota abre em um app de mapas externo.
- **Já pode atender pacientes reais?** A versão é acadêmica. Uma operação real precisa de validação clínica, credenciamento e infraestrutura de segurança.

Revise o código e ensaie o fluxo para conseguir explicar suas escolhas com clareza.
