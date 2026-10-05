# Assemble

Aplicativo Android para **descobrir personagens de quadrinhos e conversar com eles**. Você desliza cards de personagens, dá **Pass** ou **Assemble** e, quando o personagem também decide pela conexão, abre uma conversa com uma versão **ficcional** dele, gerada por inteligência artificial.

> **Projeto acadêmico.** Não é afiliado, patrocinado ou endossado pela Marvel, pela Comic Vine ou por qualquer editora. Nomes e marcas pertencem aos seus donos. Toda conversa é ficção gerada por IA: não é canon nem material aprovado pela editora.

Este repositório é o **app Android**. O servidor fica em outro repositório: [assemble-api](https://github.com/felipeirl/assemble-api).

## O que o app faz

- **Cadastro:** login por e-mail e senha ou com a conta Google, cinco passos de preferências (origem, poderes, equipes, estilo e fama), a rodada "este ou aquele" (que ensina o gosto sem virar decisão) e a revelação do seu perfil de herói, com o aviso de IA.
- **Descobrir:** baralho de até 40 personagens por dia, diferente para cada pessoa e sorteado de novo a cada abertura. Pass, Assemble e Undo do último Pass. Quem recebeu Pass não volta. A compatibilidade só aparece depois da conexão.
- **Assemble:** o backend decide combinando compatibilidade, afinidade do personagem pelo usuário e acaso. O pop-up de Assemble entra em fila quando há vários.
- **Conversar:** chat com a versão ficcional do personagem, com respostas sugeridas, gerar outra resposta e voltar a conversa. O personagem lembra do que foi dito, mesmo em conversas longas.
- **Conhecer:** perfil completo do personagem (atributos, aparência, colegas de equipe, fontes) liberado depois da conexão.
- **Perfil:** foto, capa, destaque, moldura, frase de apresentação, conquistas e preferências editáveis.
- **Configurações:** tema claro, escuro ou do sistema, som, vibração e notificações; excluir conversas e a conta, com 30 dias de carência.
- Interface em **português e inglês**, com animações que respeitam a configuração do sistema.

## Como rodar

Requisitos: Android Studio com suporte ao **AGP 9.0.0**, **JDK 17** e Android 8.0+ (minSdk 26).

```bash
./gradlew assembleDebug        # compila
./gradlew testDebugUnitTest    # testes unitários
./gradlew installDebug         # instala em um aparelho ou emulador conectado
```

O build de debug instala **dois ícones**: *Assemble* (o app) e *Assemble DS* (catálogo interno do design system, só em debug).

### Com ou sem backend

O app escolhe sozinho. Crie o arquivo `local.properties` na raiz (ele fica fora do git) com as chaves abaixo:

```properties
BACKEND_URL=https://seu-dominio.ngrok-free.dev
FIREBASE_API_KEY=...
FIREBASE_APP_ID=...
FIREBASE_PROJECT_ID=...
GOOGLE_WEB_CLIENT_ID=...
```

- **Com `BACKEND_URL` e as três chaves do Firebase:** o app usa o backend, o login real e o Firestore.
- **Sem elas:** roda com dados simulados no aparelho (sem servidor, sem login real, sem IA real). Serve para ver as telas e rodar os testes.
- `GOOGLE_WEB_CLIENT_ID` só é necessário para o login com Google.

### Configurando o Firebase

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/) e adicione um app Android com o pacote **`dev.assemble.app`**.
2. Ative **Authentication** com e-mail e senha e com Google. Para o Google, cadastre o **SHA-1** da chave de debug do seu computador (`./gradlew signingReport`).
3. Crie o **Firestore** em modo de produção, na região `us-central1` (a região não pode ser mudada depois).
4. Copie `apiKey`, `appId` e `projectId` das configurações do app para o `local.properties`. O app inicializa o Firebase em código, **sem** `google-services.json`.
5. Publique as regras de segurança que estão no repositório do backend (`firestore.rules`).
6. A credencial do Admin SDK fica só no backend, nunca no app.

## Como o produto funciona

1. **Descobrir.** O card mostra arte, nome, uma frase sobre o personagem e traços em comum. Metade do baralho vem da compatibilidade com as suas preferências; a outra metade, do gosto aprendido pelas suas decisões.
2. **Escolher.** **Pass** descarta. **Assemble** demonstra interesse.
3. **Assemble.** A decisão é tomada **uma única vez** por par usuário–personagem:

   ```
   chance = p1 × compatibilidade + p2 × afinidade com a persona + p3 × acaso
   ```

4. **Conversar.** Toda resposta carrega o selo **"AI-generated · fictional"**.

**Princípios:** personagens, nunca pessoas; sem conteúdo sexual; ficção e fatos claramente separados; campo ausente é omitido, nunca inventado.

### Compatibilidade

Estimativa explicável entre as suas preferências e as características do personagem, com pesos Origin 25, Powers 30, Teams 15, Style 20 e Fame 10. Escolher "Qualquer" em uma categoria vale metade do peso, e grupos rivais (por exemplo Avengers e X-Men) tiram metade do peso quando não há nada em comum. O valor só aparece depois da conexão. A tabela é idêntica à do backend. Não é uma avaliação psicológica.

## Arquitetura

```
App Android ──(token do Firebase)──▶ Backend Python (assemble-api)
     │                                ├─ Firestore: usuários, decisões, conexões, mensagens
     │                                ├─ Catálogo (Comic Vine, Marvel Database, Superhero API)
     └─ lê o Firestore em tempo real   ├─ Baralho, compatibilidade e decisão do Assemble
                                      └─ IA: LiteLLM + guardrail Laya (entrada e saída)
```

O app **nunca** guarda chaves de IA nem escreve score, conexão, mensagem ou baralho; quem escreve é o backend. O `uid` vem sempre do token do Firebase. A foto do perfil sobe para o Cloudinary com uma assinatura feita pelo servidor, ou, sem Cloudinary, segue no Firestore.

### Privacidade e exclusão (LGPD)

- **Excluir conta** desativa na hora e, após **30 dias de carência**, o backend apaga perfil, preferências, decisões, conexões e mensagens, e remove o usuário do Firebase Auth. Nesse prazo a conta pode ser reativada.
- **Excluir conversas** oculta na hora e remove depois da carência.
- **Registros de acesso** (IP, data e hora) ficam separados por 6 meses, como exige o Marco Civil da Internet.
- O usuário aceita o uso de IA no cadastro (`aiConsent`).

> Esta seção descreve o desenho do produto, não é aconselhamento jurídico.

## Estrutura do código

```
app/src/main/java/dev/assemble/app/
├── core/
│   ├── designsystem/   tema, tipografia, cores, ícones e componentes
│   ├── model/          modelos de domínio
│   ├── data/           repositórios: simulados (fake) e remotos (Firebase + API)
│   ├── network/        cliente HTTP do backend e envio de foto
│   ├── domain/         compatibilidade, conquistas e regras do perfil (funções puras)
│   ├── firebase/       Auth e Firestore
│   └── media/ ui/ feedback/
├── navigation/         Navigation 3: rotas, back stack por aba, menu lateral e barra inferior
└── feature/            splash, login, onboarding, discover, character, chat, profile,
                        achievements, settings, account, about, help
```

Stack: Kotlin, Jetpack Compose + Material 3, Navigation 3, ViewModel + StateFlow, DataStore, Firebase Auth e Firestore, Credential Manager (login com Google), Coil e JUnit.

## Versões

As versões contam como um odômetro (`0.0.9` → `0.1.0`: ao chegar em 9, avança a casa seguinte), uma por grande adição, e estão no [`CHANGELOG.md`](CHANGELOG.md); cada uma tem uma tag `vX.Y.Z`. A versão atual está em `app/build.gradle` (`versionName` e `versionCode`). Para lançar: atualize a versão e o changelog, faça o commit e crie a tag (`git tag -a v0.0.7 -m "v0.0.7"`).

## Fontes e créditos

- Fatos e imagens de personagens: [Comic Vine](https://comicvine.gamespot.com/) (a origem é sempre exibida no app).
- Fontes tipográficas: Barlow Condensed e Inter, sob a licença SIL Open Font License (cópias em `app/src/main/assets/licenses/`).
- A seção "Personality" da Marvel Database (Fandom), quando usada, segue a licença **CC BY-SA**, com crédito.

## Licença

[MIT](LICENSE) © 2026 Felipe Jorge
