# Whatsummary - Politica de Privacidade

*Ultima atualizacao: Abril 2026*

## Resumo

O Whatsummary e um aplicativo que gera resumos diarios das suas conversas de grupo do WhatsApp usando inteligencia artificial. Levamos sua privacidade a serio.

## Dados Coletados

### Mensagens de Grupo do WhatsApp
- O app le **apenas notificacoes do WhatsApp** (pacotes `com.whatsapp` e `com.whatsapp.w4b`) usando a API oficial `NotificationListenerService` do Android.
- Somente mensagens de **grupos** sao capturadas. Mensagens individuais (DMs) sao ignoradas.
- Os dados capturados incluem: nome do grupo, autor da mensagem, texto da mensagem e horario.
- Conteudo de midia (fotos, audios, videos) **nao e acessivel** — apenas o tipo de midia e registrado.

### Chave da API
- Sua chave da API da Anthropic e armazenada localmente no dispositivo usando `EncryptedSharedPreferences` (criptografia AES-256).

## Armazenamento de Dados

- **Todos os dados ficam exclusivamente no seu dispositivo.**
- O banco de dados local e criptografado com SQLCipher.
- Nenhum dado e enviado para servidores proprios.
- A unica comunicacao externa e com a API da Anthropic (`api.anthropic.com`), usando sua propria chave de API, para gerar os resumos.

## Compartilhamento de Dados

- **Nao compartilhamos nenhum dado com terceiros.**
- Nao utilizamos analytics, telemetria, Firebase, Crashlytics ou qualquer servico de rastreamento.
- O texto das mensagens e enviado a API da Anthropic apenas para geracao do resumo, e esta sujeito a [politica de privacidade da Anthropic](https://www.anthropic.com/privacy).

## Permissoes Utilizadas

| Permissao | Motivo |
|-----------|--------|
| Acesso a Notificacoes | Capturar mensagens de grupo do WhatsApp via notificacoes |
| Internet | Enviar mensagens a API da Anthropic para geracao de resumos |
| Notificacoes (POST_NOTIFICATIONS) | Enviar notificacao quando o resumo diario esta pronto |
| Boot Completed | Reagendar o worker de resumo apos reinicializacao do dispositivo |

## Retencao de Dados

- Mensagens e resumos sao automaticamente apagados apos o periodo configurado pelo usuario (padrao: 30 dias).
- O usuario pode apagar todos os dados a qualquer momento nas configuracoes do app.

## Seguranca

- Chave da API: criptografada com AES-256 via EncryptedSharedPreferences.
- Banco de dados: criptografado com SQLCipher.
- Comunicacao de rede: apenas HTTPS, restrita a `api.anthropic.com`.
- Sem coleta de dados anonimizados ou agregados.

## Seus Direitos

Voce tem controle total sobre seus dados:
- **Visualizar**: todos os dados estao acessiveis no proprio app.
- **Exportar**: exporte seus resumos em formato JSON nas configuracoes.
- **Apagar**: apague todos os dados (mensagens, resumos, chave da API) nas configuracoes.
- **Desinstalar**: ao desinstalar o app, todos os dados locais sao removidos.

## Limitacoes

- Mensagens lidas diretamente no WhatsApp (sem gerar notificacao) nao sao capturadas.
- Conteudo de midia nao e acessivel — apenas o tipo e registrado.
- O modo "Nao Perturbe" pode suprimir notificacoes e impedir a captura.

## Contato

Para duvidas sobre privacidade, entre em contato pelo repositorio do projeto no GitHub.

## Alteracoes nesta Politica

Quaisquer alteracoes nesta politica serao refletidas neste documento com a data de atualizacao revisada.
