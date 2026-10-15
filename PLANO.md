# Plano de implementação — PediuPatrão (Atividade Prática M1)

Evolução do projeto PediuPatrão para atender aos requisitos RF01–RF05 do enunciado,
mantendo Java 21, Spring Boot 3.4, Spring Security, Thymeleaf e MongoDB.

**Forma de trabalho:** uma etapa por vez. Cada etapa = um commit. Ao fim de cada etapa:
revisar o diff, rodar a aplicação, testar e commitar antes de seguir para a próxima.

---

## Sumário

0. [Preparação](#0-preparação)
1. [RF01 — Controle de usuários e permissões](#1-rf01--controle-de-usuários-e-permissões-20)
2. [RF02 — Evolução do cadastro de pedidos](#2-rf02--evolução-do-cadastro-de-pedidos-10)
3. [RF03 — Entrada, saída e transições](#3-rf03--entrada-saída-e-transições-15)
4. [RF04 — Cancelamentos e descontos](#4-rf04--cancelamentos-e-descontos-15)
5. [RF05 — Auditoria](#5-rf05--auditoria-30)
6. [Qualidade — testes, README e vídeo](#6-qualidade--testes-readme-e-vídeo-10)
7. [Lista completa de commits](#7-lista-completa-de-commits)
8. [Decisões tomadas](#8-decisões-tomadas)

---

## 0. Preparação

**Já feito (antes do commit base):**

- `.gitignore`: `/target/`, `.idea/`, `*.docx`, `.env`.
- Credenciais do MongoDB removidas do `application.properties`; agora vêm do `.env`
  (ignorado no Git) via `spring.config.import=optional:file:.env[.properties]`.
- `.env.example` versionado como modelo.
- Banco próprio (`MONGODB_DATABASE=pizzaria_gkowalski`) para não misturar dados com a turma
  e não conflitar com documentos antigos (`role: "USER"`).

**A fazer:**

| Commit | Descrição |
|---|---|
| C00 | `git init` + commit do projeto base, push para o repositório pessoal |

---

## 1. RF01 — Controle de usuários e permissões (2,0)

### Situação atual (problemas encontrados)

| Onde | Problema |
|---|---|
| `SecurityConfig` | `/api/usuarios` com `permitAll()` → qualquer pessoa sem login cria usuários, inclusive ADMIN |
| `SecurityConfig` | CSRF desabilitado globalmente, mas as APIs se autenticam por cookie de sessão |
| `SecurityConfig` | Nenhuma regra por perfil; só `authenticated()` |
| `UsuarioDetailsService` | Sempre atribui `roles("USER")`, ignorando o campo `role` |
| `Usuario` | `role` é `String` livre — aceita qualquer valor |
| `UsuarioController` | Recebe/devolve a entidade → expõe hash da senha e permite enviar `role` arbitrária |
| `UsuarioController.atualizarUsuario` | Senha `null` → `passwordEncoder.encode(null)` lança exceção |
| `DataInitializer` | Cria `admin` sem perfil |
| `application.properties` | `spring.security.user.*` sem efeito (existe `UserDetailsService` próprio) |
| Templates | Não há botão de logout; menu igual para todos |
| `HomeController` | Rotas `/admin` (demo AdminLTE) e `/teste` (sem template) abertas |

### Matriz de permissões

| Recurso | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Usuários — listar / criar / editar / excluir | ✅ | ❌ | ❌ |
| Pedidos — listar / consultar | ✅ | ✅ | ✅ |
| Pedidos — registrar | ❌ | ✅ | ✅ |
| Pedidos — avançar status (fluxo normal) | ❌ | ✅ | ✅ |
| Pedidos — aplicar desconto | ❌ | ✅ | ❌ |
| Pedidos — cancelar | ❌ | ✅ | ❌ |
| Clientes — consultar | ❌ | ✅ | ✅ |
| Clientes — cadastrar / alterar | ❌ | ❌ | ✅ |
| Clientes — excluir | ❌ | ✅ | ❌ |
| Produtos — consultar | ✅ | ✅ | ✅ |
| Produtos — cadastrar / alterar / excluir | ❌ | ✅ | ❌ |
| Auditoria — consultar | ✅ | ✅ | ❌ |
| Auditoria — alterar / excluir | ❌ | ❌ | ❌ |

> ADMIN fica fora de clientes de propósito: o enunciado lhe atribui apenas "consultar pedidos",
> e o RF05 pede restrição da exposição de dados pessoais.

### Estratégia de autorização (defesa em profundidade)

1. **Camada HTTP** — `SecurityConfig.authorizeHttpRequests` com regras por rota **e por método HTTP**.
2. **Camada de serviço** — `@PreAuthorize` nos métodos dos services. Telas (`HomeController`) e
   APIs (`*Controller`) chamam os mesmos services, então a regra vale para os dois caminhos.
3. **Interface** — `sec:authorize` apenas para esconder links/botões (conforto, não segurança).

### Etapas

#### C01 — Dependências
`pom.xml`:
- `+ spring-boot-starter-validation` (Bean Validation nos DTOs)
- `+ org.thymeleaf.extras:thymeleaf-extras-springsecurity6` (`sec:authorize`, `sec:authentication`)
- `+ spring-boot-starter-test` (test)
- `+ spring-security-test` (test)

#### C02 — Enum `Perfil` e roles reais
- **Novo** `entity/Perfil.java`: `ADMIN`, `GERENTE`, `ATENDENTE` (com descrição para exibir na tela).
- `Usuario`: `String role` → `Perfil perfil`.
- `UsuarioDetailsService`: `.roles(usuario.getPerfil().name())`; usuário sem perfil não autentica.
- `application.properties`: remover `spring.security.user.name/password`.

#### C03 — Usuários iniciais
- `DataInitializer`: cria, se não existirem, `admin` (ADMIN), `gerente01` (GERENTE), `atendente01` (ATENDENTE).
- Senhas lidas de propriedades (`app.seed.*`) com valores no `.env`, não fixas no código.

#### C04 — DTOs de usuário e proteção contra autoelevação
- **Novos** `dto/UsuarioForm.java` (`username`, `password`, `perfil`, com `@NotBlank`, `@Size`, `@NotNull`)
  e `dto/UsuarioResponse.java` (`id`, `username`, `perfil` — **sem senha**).
- `UsuarioController` e `HomeController` passam a usar os DTOs.
- Regras em `UsuarioService`:
  - username único → `ConflictException` (409);
  - o `id` da URL prevalece; `id` do corpo é ignorado;
  - ADMIN **não pode alterar o próprio perfil** nem **excluir a si mesmo**;
  - não é possível rebaixar/excluir o **último ADMIN**;
  - senha em branco na edição mantém a atual (corrige o bug do `encode(null)`).
- `usuarios/form.html`: opções do `<select>` geradas de `Perfil.values()`.
- `usuarios/lista.html`: badge por perfil.

#### C05 — `SecurityConfig`: matriz de permissões
- `@EnableMethodSecurity`.
- Remover `permitAll()` de `/api/usuarios`.
- Regras da matriz por rota e método (`HttpMethod.GET/POST/PUT/PATCH/DELETE`), já incluindo
  rotas futuras (`/api/pedidos/*/cancelar`, `/api/pedidos/*/desconto`, `/auditoria/**`, `/api/auditoria/**`).
- `exceptionHandling`:
  - `/api/**` → **401 JSON** (não autenticado) e **403 JSON** (sem permissão), sem redirect para o login;
  - telas → redirect para `/login` e página `403.html`.
- Logout via `POST /logout`.
- Remover rotas `/admin` e `/teste` do `HomeController`.

#### C06 — `@PreAuthorize` nos services
- `UsuarioService` → `hasRole('ADMIN')` na classe.
- `ClienteService` → consulta `GERENTE, ATENDENTE`; salvar `ATENDENTE`; excluir `GERENTE`.
- `ProdutoService` → escrita `GERENTE`; leitura autenticada.
- `PedidoService` → criar e avançar `GERENTE, ATENDENTE`; listar todos.

#### C07 — Tratamento de erros da API
Reaproveitado do projeto lista-jogos:
- **Novos** `exception/ConflictException`, `ResourceNotFoundException`, `RegraNegocioException`.
- **Novos** `exception/dto/StandardError`, `ValidationError`, `FieldMessage`.
- **Novo** `exception/handler/ApiExceptionHandler` (`@RestControllerAdvice` só para `@RestController`):
  404, 409, 422 (validação), 400 (regra de negócio) e **403 para `AccessDeniedException`**
  (tratado explicitamente para não virar 500).
- **Novo** `templates/403.html`.

#### C08 — CSRF nas telas
- **CSRF permanece habilitado em todo o sistema**, inclusive `/api/**` (autenticação por sessão = vulnerável a CSRF).
- `layout.html`: `<meta name="_csrf">` e `<meta name="_csrf_header">`.
- `static/js/script.js`: função `csrfHeaders()` reutilizada pelos `fetch`.
- `clientes/lista.html`: `DELETE` passa a enviar o header CSRF.
- Formulários com `th:action` recebem o token automaticamente (sem mudança).
- `layout.html`/`sidebar.html`: usuário logado + perfil, botão **Sair** (form POST), links por `sec:authorize`.

#### C09 — Testes de autorização
`MockMvc` + `@WithMockUser(roles = ...)`:
- ATENDENTE → 403 em `GET /api/usuarios`;
- GERENTE → 403 em `POST /api/usuarios`;
- ADMIN → 403 em `POST /api/clientes`;
- anônimo → 401 em `/api/**`;
- `POST` sem token CSRF → 403;
- resposta de `/api/usuarios` sem campo `password`.

Unitários (`Mockito`) do `UsuarioService`: alterar o próprio perfil, último ADMIN, username duplicado.

---

## 2. RF02 — Evolução do cadastro de pedidos (1,0)

### Modelo

```
Pedido
├── id
├── clienteId
├── tipoAtendimento      ENTREGA | RETIRADA
├── itens: List<ItemPedido>
│     ├── produtoId
│     ├── nomeProduto      (cópia no momento do pedido)
│     ├── precoUnitario    (cópia do Produto — nunca vem da requisição)
│     ├── quantidade
│     └── subtotal
├── subtotal             soma dos itens
├── descontoPercentual   0–20
├── valorDesconto
├── valorTotal
├── status               StatusPedido (RF03)
├── registradoPor        username autenticado
├── dataHoraEntrada
├── responsavelSaida / dataHoraSaida            (RF03)
├── canceladoPor / dataHoraCancelamento / justificativaCancelamento   (RF04)
├── historicoStatus: List<{status, usuario, dataHora}>
└── version              @Version (evita atualização concorrente)
```

- Valores monetários em `BigDecimal` com `@Field(targetType = DECIMAL128)`, arredondamento `HALF_EVEN`, 2 casas.
- Campos antigos (`tipoPizza`, `sabor`, `quantidade`) removidos.
- Remover `@Entity` (JPA) de `Pedido`/`Produto` e a dependência `jakarta.persistence-api` (não se aplica ao MongoDB).

### Regras (em `PedidoService`)
- Request (`NovoPedidoRequest`) só aceita `clienteId`, `tipoAtendimento`, `itens[{produtoId, quantidade}]`, `observacao`.
  Qualquer campo de valor enviado é ignorado.
- Cliente deve existir; pelo menos 1 item; quantidade entre 1 e 50; produto deve existir e estar ativo.
- Preço buscado no `Produto`; subtotal, desconto e total calculados no servidor.
- `registradoPor` e `dataHoraEntrada` preenchidos pelo servidor (`SecurityContext` + `Clock`).

### Etapas

| Commit | Descrição |
|---|---|
| C10 | Modelo `Pedido` + `ItemPedido` + `TipoAtendimento`; remove JPA |
| C11 | `NovoPedidoRequest`/`PedidoResponse`, cálculo de valores e validações no `PedidoService` |
| C12 | Tela de novo pedido (`pedidos/form.html`) e listagem atualizada (`pedidos.html`) |
| C13 | Testes unitários do cálculo (itens, arredondamento, produto inativo, quantidade inválida) |

---

## 3. RF03 — Entrada, saída e transições (1,5)

### Máquina de estados

```
RECEBIDO ──► EM_PREPARACAO ──► PRONTO ──┬──► SAIU_PARA_ENTREGA ──► FINALIZADO   (ENTREGA)
   │              │               │     └──► RETIRADO ───────────► FINALIZADO   (RETIRADA)
   └──────────────┴───────────────┴──► CANCELADO   (somente GERENTE, via RF04)
```

- `enum StatusPedido` com `proximosPermitidos()`, `isSaida()` e `permiteCancelamento()`.
- `FINALIZADO` e `CANCELADO` são terminais: nenhuma alteração posterior.
- `PRONTO → SAIU_PARA_ENTREGA` só se `tipoAtendimento = ENTREGA`; `PRONTO → RETIRADO` só se `RETIRADA`.
- `CANCELADO` **não** pode ser alcançado pelo endpoint de status — só pelo de cancelamento.
- Ao entrar em `SAIU_PARA_ENTREGA`/`RETIRADO`: `responsavelSaida` e `dataHoraSaida` **obrigatórios** (preenchidos pelo servidor).
- Toda transição adiciona entrada em `historicoStatus`.
- Transição inválida → `TransicaoInvalidaException` → **409** com mensagem clara
  (ex.: "Não é possível alterar de EM_PREPARACAO para FINALIZADO").

### Endpoints
- `PATCH /api/pedidos/{id}/status` `{ "novoStatus": "PRONTO" }` — GERENTE, ATENDENTE.
- Remove o antigo `PUT /api/pedidos/{id}/status?status=...` (aceitava qualquer texto).

### Etapas

| Commit | Descrição |
|---|---|
| C14 | `StatusPedido` com transições válidas |
| C15 | `PedidoService.avancarStatus` com bloqueios, saída e histórico; novo endpoint |
| C16 | Botões de avanço na tela de pedidos (só os próximos estados válidos) |
| C17 | Testes parametrizados de todas as transições (válidas e inválidas) |

---

## 4. RF04 — Cancelamentos e descontos (1,5)

### Cancelamento — `POST /api/pedidos/{id}/cancelar`
- `@PreAuthorize("hasRole('GERENTE')")` — ADMIN também recebe 403.
- Corpo: `{ "justificativa": "..." }` obrigatória, mínimo 10 caracteres.
- Permitido apenas em `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`.
- Preserva itens e valores; grava `canceladoPor`, `dataHoraCancelamento`, `justificativaCancelamento`.
- Depois de cancelado, qualquer operação (status, desconto) → 409.

### Desconto — `POST /api/pedidos/{id}/desconto`
- `@PreAuthorize("hasRole('GERENTE')")`.
- Corpo: `{ "percentual": 10.0, "justificativa": "..." }`.
- `0 < percentual ≤ 20`; percentual **substitui** o anterior (não acumula).
- Somente antes da saída (`RECEBIDO`, `EM_PREPARACAO`, `PRONTO`) e se não cancelado.
- Total recalculado no servidor.

### Etapas

| Commit | Descrição |
|---|---|
| C18 | Cancelamento com justificativa e bloqueios |
| C19 | Desconto percentual limitado e recalculo do total |
| C20 | Modais de cancelar/desconto na tela (visíveis só ao GERENTE) |
| C21 | Testes: limites do desconto, cancelamento após saída, ADMIN/ATENDENTE → 403 |

---

## 5. RF05 — Auditoria (3,0)

### Modelo — coleção `auditoria`

```
Auditoria (imutável: campos final, sem setters)
├── id
├── dataHora
├── usuario
├── perfil
├── operacao      TipoOperacao
├── entidade      "Pedido" | "Cliente"
├── entidadeId
├── alteracoes: List<CampoAlterado{campo, valorAnterior, valorNovo}>
└── justificativa (quando exigida)
```

`TipoOperacao`: `CRIACAO_PEDIDO`, `ALTERACAO_STATUS_PEDIDO`, `DESCONTO_PEDIDO`, `CANCELAMENTO_PEDIDO`,
`CRIACAO_CLIENTE`, `ALTERACAO_CLIENTE`, `EXCLUSAO_CLIENTE`.

### Integridade
- `AuditoriaRepository extends Repository<Auditoria, String>` (e não `MongoRepository`): expõe apenas
  `save` e consultas — **não existem métodos de update/delete**.
- Sem endpoints `PUT/PATCH/DELETE` para auditoria (→ 405); `SecurityConfig` nega por garantia.
- Gravação na mesma transação da operação auditada (`MongoTransactionManager`; o Atlas é replica set).
  Coleção `auditoria` criada no `MongoInitConfig` (transações não criam coleções).

### Proteção de dados
- Usuário obtido do `SecurityContext` — nunca da requisição.
- Só os **campos alterados** são gravados, não o documento inteiro.
- Nunca registra senhas, tokens ou credenciais (usuários não passam pelo diff; logs sem objetos completos).
- Na consulta, telefone exibido mascarado (ex.: `(11) 9****-4321`).

### Diff de clientes
`ClienteService.salvar` carrega o estado anterior, compara campo a campo
(`nome`, `telefone`, `cep`, `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `estado`)
e audita apenas o que mudou. Alteração sem mudanças não gera registro.

### Consulta
- Tela `/auditoria` e `GET /api/auditoria` — ADMIN, GERENTE.
- Filtros: entidade, id da entidade, usuário, operação, período. Paginação, mais recentes primeiro.

### Etapas

| Commit | Descrição |
|---|---|
| C22 | `Auditoria`, `CampoAlterado`, `TipoOperacao`, `AuditoriaRepository` (somente inserção) |
| C23 | `AuditoriaService` + transações MongoDB |
| C24 | Auditoria de pedidos (criação, status, desconto, cancelamento) |
| C25 | Auditoria de clientes com diff de campos (criação, alteração, exclusão) |
| C26 | Consulta: `/api/auditoria` e tela `/auditoria` com filtros e mascaramento |
| C27 | Testes: diff de cliente, registros gerados por operação, 403 para ATENDENTE, 405 em DELETE |

---

## 6. Qualidade — testes, README e vídeo (1,0)

| Commit | Descrição |
|---|---|
| C28 | Limpeza: template AdminLTE não usado (`admin.html`, `sidebar_1.html`, assets sem referência), correção do form de produtos (enviava form-urlencoded para API JSON), mensagem "Cliente não encontrado" em `ProdutoService` |
| C29 | GitHub Actions: `mvn test` em push/PR |
| C30 | `README.md` |

### README
- Pré-requisitos (Java 21) e execução (`.env`, `./mvnw spring-boot:run`).
- Usuários de demonstração por perfil.
- Requisitos implementados (RF01–RF05) e principais alterações por arquivo.
- Matriz de permissões e máquina de estados.
- Exemplos `curl` de acesso negado para a demonstração.

### Roteiro do vídeo
1. Login com os três perfis — mostrar menu diferente para cada um.
2. ATENDENTE: cadastrar cliente, alterar endereço/telefone, registrar pedido.
3. Tentativas bloqueadas pelo backend (DevTools/curl): ATENDENTE cancelando, ADMIN aplicando desconto → 403.
4. Transições: RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO;
   tentativa inválida (EM_PREPARACAO → FINALIZADO) → 409.
5. GERENTE: desconto de 10%; tentativa de 25% → rejeitado; cancelamento com justificativa;
   tentativa de alterar pedido cancelado → 409.
6. Tela de auditoria mostrando os registros das operações acima.

---

## 7. Lista completa de commits

| # | Requisito | Mensagem sugerida |
|---|---|---|
| C00 | — | Projeto base PediuPatrão |
| C01 | RF01 | Adiciona dependências de validação, segurança no Thymeleaf e testes |
| C02 | RF01 | Cria enum Perfil e atribui roles reais na autenticação |
| C03 | RF01 | Cria usuários iniciais para cada perfil |
| C04 | RF01 | Usa DTOs no cadastro de usuários e impede autoelevação |
| C05 | RF01 | Define permissões por perfil no SecurityConfig |
| C06 | RF01 | Adiciona @PreAuthorize nos services |
| C07 | RF01 | Padroniza respostas de erro da API |
| C08 | RF01 | Habilita CSRF nas telas e adiciona logout e menu por perfil |
| C09 | RF01 | Testes de autorização |
| C10 | RF02 | Novo modelo de pedido com itens |
| C11 | RF02 | Calcula valores do pedido no servidor |
| C12 | RF02 | Tela de cadastro de pedidos |
| C13 | RF02 | Testes do cálculo de pedidos |
| C14 | RF03 | Enum StatusPedido com transições permitidas |
| C15 | RF03 | Controla transições, saída e histórico do pedido |
| C16 | RF03 | Botões de avanço de status na tela |
| C17 | RF03 | Testes das transições de status |
| C18 | RF04 | Cancelamento de pedidos pelo gerente |
| C19 | RF04 | Desconto percentual limitado a 20% |
| C20 | RF04 | Ações de cancelamento e desconto na tela |
| C21 | RF04 | Testes de cancelamento e desconto |
| C22 | RF05 | Modelo e repositório de auditoria |
| C23 | RF05 | Serviço de auditoria com transação |
| C24 | RF05 | Audita operações de pedidos |
| C25 | RF05 | Audita alterações de clientes |
| C26 | RF05 | Consulta de auditoria |
| C27 | RF05 | Testes da auditoria |
| C28 | — | Remove arquivos não utilizados e corrige formulário de produtos |
| C29 | — | Pipeline de testes no GitHub Actions |
| C30 | — | README |

---

## 8. Decisões tomadas

| Tema | Decisão | Motivo |
|---|---|---|
| Autenticação | Sessão + form login (sem JWT) | Telas Thymeleaf usam sessão; JWT não agrega e complica o CSRF |
| CSRF | Habilitado em tudo, inclusive `/api/**` | APIs autenticadas por cookie de sessão são vulneráveis a CSRF |
| Avançar status | GERENTE e ATENDENTE | Alguém precisa registrar a saída; cancelar/desconto continuam exclusivos do GERENTE |
| Excluir cliente | Somente GERENTE | Funcionalidade existente preservada, sem dar ao ATENDENTE poder destrutivo |
| Produtos (escrita) | Somente GERENTE | Catálogo e preços influenciam o valor dos pedidos |
| ADMIN e clientes | Sem acesso | Enunciado não atribui; minimização de dados pessoais |
| Desconto | Substitui o anterior, não acumula | Garante o teto de 20% sem lógica de soma |
| Dinheiro | `BigDecimal` / Decimal128 | Evita erro de arredondamento de `double` |
| Banco | Próprio (`pizzaria_gkowalski`) | Cluster compartilhado com a turma; dados antigos incompatíveis com o enum |
