# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). O projeto segue [Versionamento Semântico](https://semver.org/lang/pt-BR/): cada versão `0.x.0` marca um conjunto grande de novidades, e as correções pequenas entram na versão seguinte.

## [0.6.0] - 2026-10-05

### Adicionado
- Foto do perfil enviada ao Cloudinary com uma assinatura feita pelo backend; sem Cloudinary, a foto segue no Firestore.
- `LICENSE`, `CHANGELOG.md` e README reescrito para o estado real do app.

### Alterado
- O rascunho antigo de `firestore.rules` saiu do repositório: as regras agora ficam no repositório da API.

## [0.5.0] - 2026-10-05

### Adicionado
- Cadastro novo: uma pergunta própria em cada passo, barra de progresso em segmentos com gradiente, chips maiores e slider de fama com exemplos.
- Rodada "este ou aquele": dois personagens por vez, que ensinam o gosto sem virar decisão.
- Revelação do perfil de herói em cartão, com a frase opcional "o que você procura numa conversa?".
- Menu lateral sem caixinhas nos ícones, com ícones de linha novos; o cabeçalho mostra a capa e a foto do perfil e abre o perfil ao toque.

### Corrigido
- Mensagem não duplica ao tentar de novo, e a resposta antiga não aparece enquanto outra é gerada.
- O aviso de espera fala em "Assemble", não em "match", e um Assemble sem match não mostra mais mensagem.

## [0.4.0] - 2026-10-04

### Adicionado
- Gerar outra resposta e voltar a conversa no chat.
- Foto do perfil escolhida na galeria.

## [0.3.0] - 2026-10-04

### Adicionado
- Login com a conta Google e tela de login e cadastro redesenhada.
- Compatibilidade com "Qualquer" neutro e rivalidades entre grupos, igual ao backend.

## [0.2.0] - 2026-10-04

O app passa a falar com o backend de verdade.

### Adicionado
- Integração com a API por HTTP e com o Firebase (login e leitura em tempo real do Firestore); sem backend configurado, o app segue com dados simulados.
- Recursos em português e inglês, camada de dados local com sessão simulada persistida, regras de conquistas e feedback de som e vibração.
- Telas de Discover, personagem, chat, perfil, configurações, login, cadastro e conta desativada ligadas ao backend.
- Testes unitários das camadas de dados, rede, Discover e login.

### Alterado
- Chaves do Firebase e URL do backend vêm do `local.properties`.

## [0.1.0] - 2026-09-30

Primeira versão: o app completo funcionando com dados simulados.

### Adicionado
- Design system próprio (tema claro e escuro, tipografia, ícones e componentes) e navegação com Navigation 3.
- Login e cadastro simulados, Discover com swipe, Undo e atualização por arrastar.
- Pré-visualização do personagem, pop-up de match e perfil completo liberado depois da conexão.
- Chat com respostas simuladas e aviso de nova mensagem, perfil e preferências, configurações, sobre e ajuda.
- Transições, acessibilidade, animações que respeitam o sistema e escala de fonte.
- Licença MIT e README com a arquitetura planejada.
