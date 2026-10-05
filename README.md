# Assemble

Aplicativo Android para **descobrir personagens de quadrinhos e conversar com eles**. Você desliza cards de personagens, dá **Pass** ou **Assemble**, e quando o personagem também decide pelo match, abre uma conversa com uma versão **ficcional** dele, gerada por inteligência artificial.

> **Projeto acadêmico.** Não é afiliado, patrocinado ou endossado pela Marvel, pela Comic Vine ou por qualquer editora. Nomes e marcas pertencem aos seus donos. Toda conversa é ficção gerada por IA: não é canon nem material aprovado pela editora.

## Estado atual

Este repositório contém o **aplicativo Android completo, funcionando com dados simulados** (sem servidor, sem login real, sem IA real). Serve para validar a experiência de ponta a ponta antes da integração.

| Já existe | Ainda não existe |
|---|---|
| Splash, login simulado, onboarding (5 passos) | Login real (Firebase Auth) |
| Discover com swipe, Undo, pull to refresh | Persistência na nuvem (Firestore) |
| Pré-visualização bloqueada, pop-up de match, perfil completo | Catálogo real (Comic Vine) |
| Chat com respostas simuladas, aviso de nova mensagem | Chat com IA real |
| Perfil, preferências, configurações, tema claro/escuro | Backend |
| Design system próprio, animações respeitando a configuração do sistema | Tradução para português (a interface está em inglês) |

### Como rodar

Requisitos: Android Studio com suporte ao **AGP 9.0.0**, **JDK 17**, Android 8.0+ (minSdk 26).

```bash
./gradlew assembleDebug        # compila
./gradlew testDebugUnitTest    # testes unitários
./gradlew installDebug         # instala em um aparelho/emulador conectado
```

O build de debug instala **dois ícones**: *Assemble* (o app) e *Assemble DS* (catálogo interno do design system, só em debug). Sem backend, os dados voltam ao estado inicial quando o app é fechado; só as configurações (tema e notificações) persistem.

## Como o produto funciona

1. **Descobrir.** O Discover mostra um baralho de **até 30 personagens por dia**. Ao reabrir o app, os cards restantes do dia são embaralhados; no dia seguinte vem um baralho novo. Quem recebeu **Pass nunca volta**. O card mostra arte, nome e traços em comum; a compatibilidade não aparece antes do match.
2. **Escolher.** **Pass** descarta. **Assemble** demonstra interesse.
3. **Match.** O personagem também decide. A compatibilidade com suas preferências é **um fator**, não a regra. A decisão combina três coisas:

   ```
   chance = p1 × compatibilidade + p2 × afinidade com a persona do personagem + p3 × acaso
   ```

   A decisão é tomada **uma única vez** por par usuário–personagem e fica registrada. O pop-up de match mostra os traços em comum.
4. **Conversar.** Cada conexão abre um chat com uma versão ficcional do personagem. Toda resposta carrega o selo **"AI-generated · fictional"**.
5. **Conhecer.** O perfil completo só é liberado depois da conexão. Dados vêm da fonte e dizem qual é; campo ausente é omitido, nunca inventado.

**Princípios:** personagens, nunca pessoas; sem linguagem de namoro nem conteúdo sexual; ficção e fatos claramente separados.

### Compatibilidade

Estimativa explicável entre as preferências do usuário e as características do personagem, com pesos Origin 25, Powers 30, Teams 15, Style 20 e Fame 10. Por categoria, pontua-se `peso × (itens em comum ÷ menor entre escolhidos e os do personagem)`; conjunto vazio ("Any") vale o peso cheio. O valor só aparece depois da conexão, como porcentagem exata. Não é uma avaliação psicológica.

## Versão de produção (arquitetura planejada)

```
App Android ──(token do Firebase)──▶ Backend Python (repositório separado, Hugging Face Space)
                                     ├─ Firestore: usuários, decisões, conexões, mensagens, baralho
                                     ├─ Ingestão da Comic Vine + fichas de persona (cache global)
                                     ├─ Decisão de match + baralho diário
                                     └─ IA: LiteLLM + guardrail Laya (entrada e saída)
                                          ▶ Qwen 3.7 Flash via Command Code (retenção zero)
GitHub Actions agendado ──▶ rotas protegidas do backend (ingestão, fichas, faxina)
```

- **Um único servidor, em Python.** O app nunca guarda chaves: todas ficam no backend. O `uid` vem sempre do token do Firebase, nunca do corpo da requisição.
- **Catálogo.** Fatos da Comic Vine (nome real, origem, poderes, equipes, primeira aparição, imagem). Personagens entram em níveis de qualidade: **A** (curados, com ficha revisada) e **B** (ficha gerada e validada por regras); quem não tem dados suficientes não entra no baralho.
- **Fichas de persona.** Cada personagem tem uma ficha (voz, valores, jeito de falar, limites, exemplos) gerada **uma vez** e guardada em cache global com versão. O prompt de sistema global é montado a cada mensagem a partir da ficha; o servidor de IA não guarda estado.
- **Segurança da conversa.** Um guardrail local (Laya) filtra a mensagem do usuário e a resposta do modelo: injeção de prompt, dados pessoais, conteúdo sexual ou romântico, autoagressão (com encaminhamento seguro) e fuga de papel. A descrição vinda da fonte (editável por terceiros) é tratada como dado não confiável. Modelos que treinam com os dados **nunca** recebem mensagens de usuários.
- **Idiomas.** Português (pt-BR) e inglês, seguindo o idioma do aparelho. O personagem responde no idioma em que o usuário escrever.

### Dados (Firestore)

```
users/{uid}                       perfil, preferências, consentimento de IA, status da conta
├── decisions/{characterId}       Pass ou Assemble (gravado pelo backend)
├── matches/{characterId}         conexão, score, decisão de match
│   └── messages/{messageId}      conversa (gravada pelo backend)
└── decks/{AAAA-MM-DD}            baralho do dia
characters/{id}  personas/{id}    caches globais, só o backend escreve
```

O app **nunca** escreve score, conexão, mensagem ou baralho; as regras em [`firestore.rules`](firestore.rules) negam qualquer campo autoritativo. E-mail e senha ficam só no Firebase Auth. Tema e notificações ficam no aparelho.

### Privacidade e exclusão (LGPD)

- **Excluir conta** desativa na hora (login bloqueado) e, após **30 dias de carência**, o backend apaga ou anonimiza perfil, preferências, decisões, conexões e mensagens, e remove o usuário do Firebase Auth. Nesse prazo a conta pode ser reativada.
- **Excluir conversas** oculta na hora e remove depois da carência.
- **Registros de acesso** (IP, data e hora) ficam separados por 6 meses, como exige o Marco Civil da Internet, e depois são apagados.
- O usuário aceita o uso de IA no onboarding (`aiConsent`).

> Esta seção descreve o desenho do produto, não é aconselhamento jurídico; valide com orientação especializada antes de publicar.

### Hospedagem planejada

| Peça | Onde |
|---|---|
| Login e banco | Firebase (Auth + Firestore, região nos EUA) |
| Backend Python | Hugging Face Space (Docker; segredos nas variáveis ocultas do Space) |
| Tarefas agendadas | GitHub Actions |
| Distribuição para testes | Firebase App Distribution |

## Configurando o Firebase (quando a integração começar)

O app já traz o código de inicialização manual do Firebase, que fica inativo enquanto não houver `google-services.json`. Para ativar:

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/) e adicione um app Android com o pacote **`dev.assemble.app`**.
2. Ative **Authentication** (Google e e-mail/senha).
3. Crie o **Firestore** em modo de produção, na região `us-central1` (a região não pode ser mudada depois).
4. Baixe o `google-services.json` e coloque em `app/`. **Ele está no `.gitignore`: nunca o envie ao repositório.**
5. Publique as regras de [`firestore.rules`](firestore.rules).
6. No backend (repositório separado), configure a credencial do Admin SDK como segredo; nunca no app.

Segredos locais (chaves de API) ficam em um arquivo `.env`, também ignorado pelo git.

## Estrutura do código

```
app/src/main/java/dev/assemble/app/
├── core/
│   ├── designsystem/   tema, tipografia, cores, ícones e componentes
│   ├── model/          modelos de domínio
│   ├── data/           interfaces de repositório + implementações simuladas e mock
│   └── domain/         cálculo de compatibilidade (função pura, com testes)
├── navigation/         Navigation 3: rotas, back stack por aba, gaveta e barra inferior
└── feature/            splash, login, onboarding, discover, character, chat, profile, settings, about, help
```

Stack: Kotlin, Jetpack Compose + Material 3, Navigation 3, ViewModel + StateFlow, DataStore, Coil, JUnit.

## Fontes e créditos

- Fatos e imagens de personagens: [Comic Vine](https://comicvine.gamespot.com/) (a origem é sempre exibida no app).
- Fontes tipográficas: Barlow Condensed e Inter, sob a licença SIL Open Font License (cópias em `app/src/main/assets/licenses/`).
- Quando usada, a seção "Personality" da Marvel Database (Fandom) segue a licença **CC BY-SA**, com crédito.

## Licença

[MIT](LICENSE) © 2026 Felipe Jorge
