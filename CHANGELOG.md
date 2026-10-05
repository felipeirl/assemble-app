# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). As versões contam como um odômetro: cada release é uma grande adição e o número sobe de um em um (`0.0.8`, `0.0.9`, `0.1.0`, `0.1.1`...). Ao chegar em 9, avança a casa seguinte. Correções pequenas entram na release seguinte.

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
