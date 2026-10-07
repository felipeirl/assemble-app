# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). As versões contam como um odômetro: cada release é uma grande adição e o número sobe de um em um (`0.0.8`, `0.0.9`, `0.1.0`, `0.1.1`...). Ao chegar em 9, avança a casa seguinte. Correções pequenas entram na release seguinte.

## [0.1.3] - 2026-10-07

**Exige a API 0.1.3 ou mais nova:** o envio de mensagem, "gerar outra resposta" e o Assemble seguem a fila do backend.

### Adicionado
- Ajuste da foto do perfil antes de salvar: arrastar, zoom com pinça e rotação.
- Pass sem rede vai para uma fila local e é reenviado em segundo plano com a mesma chave; o card não volta mais ao baralho.
- Repetição automática: leituras e pedidos com Idempotency-Key são repetidos até 3 vezes quando a rede cai ou o backend está ocupado (`503`), respeitando o `Retry-After`.

### Alterado
- Chat: a mensagem é aceita na hora e a resposta do personagem chega pelo Firestore. "Digitando", falha e bloqueio vêm do estado da mensagem no servidor; uma resposta perdida num reinício do servidor vira "tentar de novo" depois de 3 minutos. Acaba o erro de "tentar de novo" em mensagens que o servidor já tinha recebido.
- "Gerar outra resposta": a conversa fica marcada até o texto novo aparecer.
- Assemble: o pop-up de conexão abre a partir da conexão gravada no Firestore. Se a decisão demorar, nada aparece e a conexão, quando vier, fica na lista de conversas.
- Tempo limite de leitura das chamadas ao backend: 30 s (antes, 90 s).

### Removido
- Escolha de avatar pré-definido: o perfil usa foto.

## [0.1.2] - 2026-10-07

### Adicionado
- Conquistas v2: 16 conquistas (antes 6) em quatro categorias (Conexões, Conversas, Descoberta e Perfil) e três raridades (Bronze, Prata e Ouro). As novas medem conversas com personagens diferentes, dias com o app aberto e o perfil completo.
- Cada conquista libera uma recompensa para o perfil: títulos (Recruta, Quebra-gelo, Explorador, Diplomata, Lenda viva e Alter ego), molduras (Escudo, Cósmica e Raio), capas (Manchete, Planta e Cosmos) e cores (Esmeralda e Prata). Molduras, capas e cores que já eram livres continuam livres.
- Título no perfil, escolhido no Editar perfil e exibido abaixo do nome.

### Alterado
- A tela de conquistas agrupa por categoria, mostra o anel da raridade, uma barra de progresso e a recompensa de cada conquista. Tocar numa conquista desbloqueada abre o Editar perfil já na recompensa.
- O aviso de conquista desbloqueada cita a recompensa.
- No Editar perfil, capas, cores e títulos bloqueados aparecem com cadeado e a conquista que os libera, como já acontecia com as molduras.

## [0.1.1] - 2026-10-07

### Adicionado
- Login v2: botão "Continuar com Google" com o logo "G" e as cores oficiais do Google, no tema claro e no escuro.

### Alterado
- A tela de login não rola mais em repouso: logo e título usam o espaço que sobra acima do formulário e encolhem ou somem em telas baixas. Com o teclado aberto, o topo sai e a tela rola, para o campo em uso ficar visível. "Esqueci minha senha" fica logo abaixo do campo de senha.
- Onboarding: a barra de progresso ficou vermelha sólida, sem o gradiente para violeta, e os personagens do duelo aparecem enquadrados pelo topo da imagem, onde costuma estar o rosto.

## [0.1.0] - 2026-10-07

### Adicionado
- APK assinado de release, anexado a cada release do GitHub. A assinatura lê a keystore do `local.properties` (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`); sem elas, como no CI, o build de release sai sem assinar.
- Sons novos para conquista, Assemble, conexão, mensagens e passar de personagem; o som da abertura foi realinhado ao encaixe do logo.

### Alterado
- O backend passou a rodar na Discloud (`https://assemble.discloud.dev`), em vez do PC com túnel.

## [0.0.7] - 2026-10-05

### Adicionado
- Tela "Confirme o seu e-mail": no cadastro por e-mail e senha, o app manda o e-mail de confirmação (em HTML, com o design system) e só libera a conta depois do clique no link. Reenvio com espera de 60 s, conferência automática ao voltar do app de e-mail e atalho para usar outra conta. O login com Google já vem confirmado.
- CI no GitHub Actions: compila o app e roda os testes unitários a cada push e pull request.

### Alterado
- Os textos dizem "Assemble" e "conexão" no lugar de "match" ("Why you connect", "Avg. compatibility").
- Os 22 arquivos Java do app antigo saíram do repositório; `MyApplication` virou Kotlin.

## [0.0.6] - 2026-10-05

### Adicionado
- Foto do perfil enviada ao Cloudinary com uma assinatura feita pelo backend; sem Cloudinary, a foto segue no Firestore.
- `LICENSE`, `CHANGELOG.md` e README reescrito para o estado real do app.

### Alterado
- O rascunho antigo de `firestore.rules` saiu do repositório: as regras agora ficam no repositório da API.

## [0.0.5] - 2026-10-05

### Adicionado
- Cadastro novo: uma pergunta própria em cada passo, barra de progresso em segmentos com gradiente, chips maiores e slider de fama com exemplos.
- Rodada "este ou aquele": dois personagens por vez, que ensinam o gosto sem virar decisão.
- Revelação do perfil de herói em cartão, com a frase opcional "o que você procura numa conversa?".
- Menu lateral sem caixinhas nos ícones, com ícones de linha novos; o cabeçalho mostra a capa e a foto do perfil e abre o perfil ao toque.

### Corrigido
- Mensagem não duplica ao tentar de novo, e a resposta antiga não aparece enquanto outra é gerada.
- O aviso de espera fala em "Assemble", e um Assemble que não vira conexão não mostra mais mensagem.

## [0.0.4] - 2026-10-04

### Adicionado
- Gerar outra resposta e voltar a conversa no chat.
- Foto do perfil escolhida na galeria.

## [0.0.3] - 2026-10-04

### Adicionado
- Login com a conta Google e tela de login e cadastro redesenhada.
- Compatibilidade com "Qualquer" neutro e rivalidades entre grupos, igual ao backend.

## [0.0.2] - 2026-10-04

O app passa a falar com o backend de verdade.

### Adicionado
- Integração com a API por HTTP e com o Firebase (login e leitura em tempo real do Firestore); sem backend configurado, o app segue com dados simulados.
- Recursos em português e inglês, camada de dados local com sessão simulada persistida, regras de conquistas e feedback de som e vibração.
- Telas de Discover, personagem, chat, perfil, configurações, login, cadastro e conta desativada ligadas ao backend.
- Testes unitários das camadas de dados, rede, Discover e login.

### Alterado
- Chaves do Firebase e URL do backend vêm do `local.properties`.

## [0.0.1] - 2026-09-30

Primeira versão: o app completo funcionando com dados simulados.

### Adicionado
- Design system próprio (tema claro e escuro, tipografia, ícones e componentes) e navegação com Navigation 3.
- Login e cadastro simulados, Discover com swipe, Undo e atualização por arrastar.
- Pré-visualização do personagem, pop-up de Assemble e perfil completo liberado depois da conexão.
- Chat com respostas simuladas e aviso de nova mensagem, perfil e preferências, configurações, sobre e ajuda.
- Transições, acessibilidade, animações que respeitam o sistema e escala de fonte.
- Licença MIT e README com a arquitetura planejada.
