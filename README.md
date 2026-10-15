# PediuPatrão

Sistema de pedidos de pizzaria evoluído na Atividade Prática M1: controle de acesso por perfil,
pedidos com itens e valores calculados no servidor, fluxo de status com entrada e saída,
cancelamentos e descontos exclusivos do gerente e auditoria persistente no MongoDB.

**Tecnologias:** Java 21, Spring Boot 3.4, Spring Security, Thymeleaf e MongoDB.

## Como executar

Pré-requisitos: Java 21 e acesso a um MongoDB (Atlas ou local).

1. Copie o `.env.example` para `.env` e preencha a conexão:

   ```properties
   MONGODB_URI=mongodb+srv://usuario:senha@cluster0.exemplo.mongodb.net/?appName=Cluster0
   MONGODB_DATABASE=pizzaria_seunome
   ```

2. Execute:

   ```bash
   ./mvnw spring-boot:run        # Linux/macOS
   mvnw.cmd spring-boot:run      # Windows
   ```

3. Acesse `http://localhost:8080`.

Para rodar os testes: `./mvnw test`. Eles não precisam do MongoDB.

### Usuários de demonstração

Criados automaticamente na inicialização, caso não existam:

| Usuário       | Senha      | Perfil    |
|---------------|------------|-----------|
| `admin`       | `teste123` | ADMIN     |
| `gerente01`   | `teste123` | GERENTE   |
| `atendente01` | `teste123` | ATENDENTE |

## Requisitos implementados

### RF01 — Controle de usuários e permissões

- Perfis `ADMIN`, `GERENTE` e `ATENDENTE` no enum `Role`. O `UsuarioDetailsService` atribui a role
  gravada no usuário (antes atribuía sempre `USER`); usuário sem perfil não autentica.
- Autorização no backend em duas camadas:
  - telas: regras por rota no `SecurityConfig`;
  - APIs: `@PreAuthorize` em cada endpoint dos controllers.
- API sem login responde **401**; perfil sem permissão responde **403** em JSON.
- Gestão de usuários exclusiva do ADMIN (`/api/usuarios` deixou de ser pública). A API usa DTOs e
  nunca devolve a senha.
- Proteção contra autoelevação: ninguém altera o próprio perfil nem exclui o próprio usuário;
  username duplicado é rejeitado (409).
- CSRF habilitado em todo o sistema, inclusive nas APIs, porque a autenticação é por cookie de
  sessão. As telas enviam o token nos formulários e no header das requisições `fetch`.
- Menu por perfil, usuário logado e botão **Sair** (os botões ocultos são apenas conforto visual:
  a regra está no backend).

| Recurso                                   | ADMIN | GERENTE | ATENDENTE |
|-------------------------------------------|:-----:|:-------:|:---------:|
| Usuários (listar, criar, editar, excluir) |   ✅   |    ❌    |     ❌     |
| Pedidos — consultar                       |   ✅   |    ✅    |     ✅     |
| Pedidos — registrar e avançar status      |   ❌   |    ✅    |     ✅     |
| Pedidos — desconto e cancelamento         |   ❌   |    ✅    |     ❌     |
| Clientes — consultar                      |   ❌   |    ✅    |     ✅     |
| Clientes — cadastrar e alterar            |   ❌   |    ❌    |     ✅     |
| Clientes — excluir                        |   ❌   |    ✅    |     ❌     |
| Produtos — cadastrar, alterar e excluir   |   ❌   |    ✅    |     ❌     |
| Auditoria — consultar                     |   ✅   |    ✅    |     ❌     |

### RF02 — Evolução do cadastro de pedidos

- `Pedido` com itens (`ItemPedido`), quantidades, preço unitário, subtotal, percentual e valor de
  desconto, valor total, tipo de atendimento, responsável pelo registro e data/hora de entrada.
- A requisição (`InsertPedidoDTO`) só aceita cliente, tipo de atendimento, itens (produto e
  quantidade) e observação. Preço, total, status, responsável e data/hora são definidos no servidor.
- Validações: cliente existente, ao menos um item, quantidade de 1 a 50, produto existente e ativo.
- Valores em `BigDecimal` (Decimal128 no MongoDB), arredondados em 2 casas.

### RF03 — Entrada, saída e transições

```
RECEBIDO → EM_PREPARACAO → PRONTO ─┬→ SAIU_PARA_ENTREGA → FINALIZADO   (ENTREGA)
                                   └→ RETIRADO ─────────→ FINALIZADO   (RETIRADA)
RECEBIDO / EM_PREPARACAO / PRONTO → CANCELADO   (somente GERENTE)
```

- Transições definidas em `StatusPedido`; qualquer outra responde **409**
  (ex.: "Não é possível alterar de EM_PREPARACAO para FINALIZADO").
- Ao sair (`SAIU_PARA_ENTREGA` ou `RETIRADO`) o servidor grava o responsável e a data/hora da saída.
- `FINALIZADO` e `CANCELADO` são estados finais. `CANCELADO` não é alcançado pelo endpoint de status.

### RF04 — Cancelamentos e descontos

- Exclusivos do GERENTE, inclusive em relação ao ADMIN (`@PreAuthorize("hasRole('ROLE_GERENTE')")`).
- Cancelamento: justificativa obrigatória (10 a 500 caracteres), somente antes da saída; preserva
  itens e valores e grava quem cancelou, quando e o motivo.
- Desconto: percentual maior que 0 e até 20%, somente antes da saída e se não cancelado; substitui
  o desconto anterior e o total é recalculado no servidor.

### RF05 — Auditoria

- Entidade `Auditoria` (coleção `auditoria`), imutável: usuário autenticado, perfil, data/hora,
  operação, entidade, identificador, campos alterados (valor anterior e novo) e justificativa.
- Operações auditadas: criação de pedido, alteração de status, desconto, cancelamento, criação,
  alteração e exclusão de cliente. Na alteração de cliente, só os campos que mudaram são gravados.
- Integridade: `AuditoriaRepository` estende `Repository` (e não `MongoRepository`), expondo apenas
  `save` e consulta. Não existem endpoints de alteração ou exclusão (`DELETE` → 405).
- Consulta em `/auditoria` (tela) e `GET /api/auditoria`, exclusiva de ADMIN e GERENTE.
- Proteção de dados: o usuário vem do `SecurityContext`, nunca da requisição; senhas e tokens não são
  auditados nem logados; telefones aparecem mascarados na consulta (ex.: `*******4321`).

## Principais alterações

| Arquivo | Alteração |
|---|---|
| `config/SecurityConfig` | Regras por perfil nas telas, 401 nas APIs, CSRF habilitado, `@EnableMethodSecurity` |
| `config/DataInitializer` | Cria um usuário de cada perfil |
| `entity/Role`, `entity/Usuario` | Perfis em enum no lugar de texto livre |
| `service/UsuarioDetailsService` | Atribui a role real do usuário |
| `service/UsuarioService`, `controller/UsuarioController` | DTOs sem senha, username único, bloqueio de autoelevação |
| `entity/Pedido`, `entity/ItemPedido`, `entity/TipoAtendimento` | Novo modelo do pedido |
| `entity/StatusPedido` | Fluxo de status e transições permitidas |
| `service/PedidoService`, `controller/PedidoController` | Cálculo no servidor, transições, saída, cancelamento, desconto e auditoria |
| `service/ClienteService` | Auditoria de clientes com comparação campo a campo |
| `entity/Auditoria`, `entity/CampoAlterado`, `entity/TipoOperacao` | Modelo da auditoria |
| `repository/AuditoriaRepository`, `service/AuditoriaService`, `controller/AuditoriaController` | Gravação e consulta da auditoria |
| `exception/**` | Respostas de erro padronizadas (400, 403, 404, 409, 422) |
| `templates/pedidos.html` | Cadastro de pedido, avanço de status, desconto e cancelamento |
| `templates/auditoria/lista.html` | Consulta da auditoria |
| `templates/fragments/sidebar.html`, `templates/layout.html` | Menu por perfil, logout e token CSRF |

## Endpoints de pedidos

| Método | Rota | Perfis | Corpo |
|---|---|---|---|
| `GET` | `/api/pedidos` | todos | — |
| `POST` | `/api/pedidos` | GERENTE, ATENDENTE | `{"clienteId", "tipoAtendimento", "itens": [{"produtoId", "quantidade"}], "observacao"}` |
| `PATCH` | `/api/pedidos/{id}/status` | GERENTE, ATENDENTE | `{"novoStatus": "EM_PREPARACAO"}` |
| `POST` | `/api/pedidos/{id}/desconto` | GERENTE | `{"percentual": 10}` |
| `POST` | `/api/pedidos/{id}/cancelar` | GERENTE | `{"justificativa": "Cliente desistiu do pedido"}` |

### Testando um acesso negado

Logado como `atendente01`, no console do navegador (F12):

```js
const h = document.querySelector('meta[name="_csrf_header"]').content;
const t = document.querySelector('meta[name="_csrf"]').content;
fetch('/api/pedidos/ID_DO_PEDIDO/cancelar', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', [h]: t },
  body: JSON.stringify({ justificativa: 'Tentativa sem permissão' })
}).then(r => console.log(r.status));   // 403
```

## Testes

- `StatusPedidoTest`: transições válidas e inválidas.
- `PedidoServiceTest`: cálculo do total, produto inativo, transição inválida, registro da saída,
  limite e recálculo do desconto, cancelamento e bloqueios após saída e cancelamento.
- `ClienteServiceTest`: auditoria somente dos campos alterados.
- `AutorizacaoApiTest`: 401 sem login, 403 por perfil (ATENDENTE/ADMIN cancelando, ADMIN com
  desconto, GERENTE criando usuário), 403 sem token CSRF, senha ausente na API e 405 ao excluir auditoria.
