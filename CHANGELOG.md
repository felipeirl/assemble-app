# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). O projeto segue [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [0.1.0] - 2026-10-05

Primeira versão com o app ligado ao backend, de ponta a ponta.

### Adicionado
- Login por e-mail e senha e com a conta Google, com redefinição de senha.
- Cadastro em cinco passos de preferências, cada um com a sua pergunta, seguido da rodada "este ou aquele" e da revelação do perfil de herói com a frase opcional "o que você procura numa conversa?" e o aceite do aviso de IA.
- Discover com baralho de 40 cards por dia, Pass, Assemble, Undo, fila de pop-ups de match e frase sobre o personagem em cada card.
- Chat com IA: respostas sugeridas, gerar outra resposta, voltar a conversa e mensagens que não duplicam ao tentar de novo.
- Perfil do personagem completo e perfil do usuário personalizável: foto (Cloudinary ou Firestore), capa, destaque, moldura e conquistas.
- Menu lateral com o cabeçalho do perfil (capa e foto) que abre o perfil ao toque.
- Configurações, exclusão de conversas e da conta com carência de 30 dias e reativação.
- Interface em português e inglês, tema claro e escuro.
- Dados simulados quando não há backend configurado, para ver as telas e rodar os testes.

### Alterado
- A compatibilidade passa a valer meia pontuação para "Qualquer" e a considerar rivalidades entre grupos, igual ao backend.
