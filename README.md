# delivery-system — Projeto Diamante 02 (Java Advanced)

Sistema de pedidos de delivery com microsserviços (Spring Boot 4, Spring Cloud 2025.1, Java 25).

## Integrantes

| Nome                           | RM       |
|--------------------------------|----------|
| _João Vitor Biribilli Ravelli_ | _565594_ |
| _Pietro Paranhos Wilhelm_      | _561378_ |
| _Gabriel Neris Losano_         | _564093_ |

## Arquitetura

```
App React Native (professor) ── :8080 ──► order-service ──LB + retry──► payment-service :8081 / :8082
                             └─ :8083 ─┐       │ publica (AMQP)
                                       │       ▼
                                       └─ review-service ◄── RabbitMQ: delivery.exchange ─► reviews.queue
                       eureka-server :8761 (todos os serviços se registram)
```

| Projeto Gradle | Porta | Responsabilidade |
|----------------|-------|------------------|
| `eureka-server` | 8761 | Service discovery |
| `order-service` | 8080 | Cardápio, pedidos com lock pessimista, producer de avaliações, assistente com Spring AI, rate limit (bônus) |
| `payment-service` | 8081 e 8082 | Pagamento instável (~50% de falhas), duas instâncias |
| `review-service` | 8083 | Consumer RabbitMQ, buffer em memória + flush a cada 5 s no H2, ranking |
| RabbitMQ (Docker) | 5672 / 15672 | `delivery.exchange` → `reviews.queue` (routing key `reviews.new`) |

```
delivery-system/
├── docker-compose.yml        RabbitMQ (rabbitmq:4-management)
├── settings.gradle           agrupa os 4 projetos para o IntelliJ (opcional)
├── eureka-server/            cada pasta é um projeto Gradle independente, com seu gradlew
├── order-service/            config · controller · dto · exception · model · repository · service
├── payment-service/
└── review-service/
```

Regras do enunciado atendidas: nenhuma URL `localhost` no código entre serviços (o `order-service` chama `http://PAYMENT-SERVICE/payments` pelo nome no Eureka), todo o código em inglês e nenhuma chave de API no repositório.

---

## Como executar

### Pré-requisitos
- **JDK 25** (se faltar, o Gradle baixa sozinho na primeira execução)
- **Docker Desktop** aberto
- Chave de API para o assistente (veja "Assistente de IA")

### 1. RabbitMQ
Na raiz do projeto:

```bash
docker compose up -d
```

### 2. Os quatro serviços (um terminal para cada, **nesta ordem**)

```bash
cd eureka-server   && ./gradlew bootRun
cd payment-service && ./gradlew bootRun
cd payment-service && ./gradlew bootRun --args='--server.port=8082'
cd review-service  && ./gradlew bootRun
cd order-service   && ./gradlew bootRun
```

No Windows (PowerShell): `cd eureka-server; .\gradlew bootRun`, e assim por diante.
Espere o Eureka subir antes dos demais. Confira em <http://localhost:8761>: devem aparecer `ORDER-SERVICE`, `REVIEW-SERVICE` e `PAYMENT-SERVICE` (2 instâncias).

### Pelo IntelliJ
Abra a pasta raiz `delivery-system` (o `settings.gradle` da raiz importa os 4 projetos). Se a aba **Gradle** (barra lateral direita) estiver vazia, clique em *Reload All Gradle Projects*. Em *Settings → Build Tools → Gradle*, defina o **Gradle JVM** como o JDK 25. Rode `bootRun` de cada projeto em *Tasks → application*. A segunda instância do pagamento precisa de uma configuração com o argumento `--server.port=8082`.

### Assistente de IA (a chave nunca vai para o repositório)

O `order-service` usa por padrão o **Groq** (gratuito, API compatível com OpenAI) com o modelo `openai/gpt-oss-120b`. Informe só a chave, de uma destas formas:

- **Arquivo local (mais simples):** copie `order-service/local.properties.example` para `order-service/local.properties` e coloque sua chave (criada em <https://console.groq.com>). Esse arquivo está no `.gitignore`.
- **Variável de ambiente:** `OPENAI_API_KEY=sua-chave` (no IntelliJ: *Run → Edit Configurations → Environment variables*).

Outros provedores, por variáveis de ambiente:

| Provedor | Variáveis |
|----------|-----------|
| OpenAI | `OPENAI_API_KEY=sk-...`, `OPENAI_BASE_URL=https://api.openai.com/v1`, `OPENAI_MODEL=gpt-4o-mini` |
| Ollama (local) | `OPENAI_BASE_URL=http://localhost:11434/v1`, `OPENAI_MODEL=<modelo>`, `OPENAI_API_KEY=local` |
| LM Studio (local) | `OPENAI_BASE_URL=http://localhost:1234/v1`, `OPENAI_MODEL=<modelo>`, `OPENAI_API_KEY=local` |

No plano gratuito do Groq o modelo `qwen/qwen3.8-27b` estoura o limite de tokens por minuto (erro 429); por isso o padrão é o `gpt-oss-120b`. Sem chave o sistema sobe normalmente e só o `/assistant` responde 503, com a causa na mensagem.

### App React Native do professor

```bash
git clone https://github.com/joaocarloslima/diamante-delivery.git
cd diamante-delivery
npm install
npx expo start
```
No botão do servidor (canto superior direito) informe o endereço do computador: `localhost` (navegador/iOS), `10.0.2.2` (emulador Android) ou o IP do PC na rede (celular com Expo Go). **Testar conexão** deve mostrar os dois serviços (8080 e 8083) OK. O CORS já está liberado nas duas portas.

### Testes automatizados

```bash
cd order-service   && ./gradlew test    # 50 threads no mesmo prato => exatamente 10 pedidos; falha de pagamento => estoque intacto
cd payment-service && ./gradlew test    # ~50% de falha, resposta informa a porta
cd review-service  && ./gradlew test    # buffer só grava no flush; ranking ordenado pela média
```

---

## Roteiro de testes (por critério de avaliação)

Antes de repetir testes de estoque, **reinicie o `order-service`** (o H2 é em memória). Exemplos em PowerShell; use `curl.exe`. Para corpos JSON, o mais simples é gravar em arquivo:

```powershell
'{"dishId":2,"quantity":1}' | Set-Content -NoNewline order.json
curl.exe -i -X POST localhost:8080/orders -H "Content-Type: application/json" --data-binary "@order.json"
```

### 1. Eureka (10 pts)
Abra <http://localhost:8761>.
**Esperado:** `ORDER-SERVICE`, `REVIEW-SERVICE` e `PAYMENT-SERVICE` registrados; o pagamento com 2 instâncias (`payment-service:8081` e `payment-service:8082`).

### 2. Load balance (15 pts)
Faça 8 a 10 pedidos (comando acima) ou use o app.
**Esperado:** no console do `order-service`, as linhas `Payment approved by instance ...` **alternam** entre 8081 e 8082; os consoles do pagamento mostram `[instance 8081]` e `[instance 8082]` atendendo.

### 3. Retry (15 pts)
- Nos logs, quando um pagamento falha (`FAILED (simulated)`), o `order-service` repete `Calling payment-service...` com intervalos crescentes (backoff exponencial com jitter) e o pedido acaba confirmado.
- **502:** pare as duas instâncias de pagamento, espere ~15 s e faça um pedido:
  ```powershell
  curl.exe -i -X POST localhost:8080/orders -H "Content-Type: application/json" --data-binary "@order.json"
  curl.exe localhost:8080/dishes/2
  ```
  **Esperado:** `502 {"error":"Payment unavailable"}` depois de ~12 s, e o `stock` do prato **não mudou**.
- Configuração: `PaymentClient` (bean separado) com `@Retryable(maxRetries=4, delay=200, multiplier=2, jitter=100, maxDelay=2000)`; `@EnableResilientMethods` em `OrderServiceApplication`.

### 4. Race condition (15 pts)
Com o `order-service` recém-iniciado (prato 1 = promoção, `stock: 10`):

```powershell
'{"dishId":1,"quantity":1}' | Set-Content -NoNewline promo.json
1..50 | ForEach-Object -Parallel {
  curl.exe -s -o NUL -w "%{http_code}`n" -X POST localhost:8080/orders -H "Content-Type: application/json" --data-binary "@promo.json"
} -ThrottleLimit 50 | Group-Object | Select Name, Count
curl.exe localhost:8080/dishes/1
```
(`-Parallel` exige PowerShell 7; sem ele, rode `./gradlew test` em `order-service`.)
**Esperado:** exatamente **10 × `201`**, o restante `409` (estoque) ou `429` (rate limit do bônus) e `"stock": 0`. Resultado medido: `201 ×10, 409 ×10, 429 ×30`.

### 5. Mensageria (15 pts)
1. <http://localhost:15672> (login `myuser` / senha `secret`).
2. **Esperado:** exchange `delivery.exchange` (topic), fila `reviews.queue` durável e binding com a routing key `reviews.new`.
3. Envie avaliações:
   ```powershell
   '{"dishId":1,"rating":5,"comment":"Great"}' | Set-Content -NoNewline review.json
   curl.exe -i -X POST localhost:8080/reviews -H "Content-Type: application/json" --data-binary "@review.json"
   ```
   **Esperado:** `202` sem corpo. Rating `0` ou `6` → `400`; prato inexistente → `404`.

### 6. Backpressure (15 pts)
Envie muitas avaliações de uma vez (por exemplo 200 `POST /reviews` em paralelo, ou avalie pelo app) e espere ~6 s.
**Esperado:**
- Console do `review-service`: muitos `Review received...` e poucos `Flushed N review(s)` (um por prato a cada 5 s).
- `curl.exe localhost:8083/reviews/ranking` → `[{"dishId":..,"dishName":..,"average":..,"count":..}]` ordenado pela média; a soma dos `count` é exatamente o número de avaliações enviadas.
- Resultado medido: 300 requisições simultâneas, todas `202` em ~370 ms, e 300 contabilizadas no ranking.

### 7. Spring AI (10 pts)
```powershell
'{"question":"Tem prato vegetariano ate R$ 40?"}' | Set-Content -NoNewline q1.json
curl.exe -X POST localhost:8080/assistant -H "Content-Type: application/json" --data-binary "@q1.json"
'{"question":"Quem ganhou a Copa de 2002?"}' | Set-Content -NoNewline q2.json
curl.exe -X POST localhost:8080/assistant -H "Content-Type: application/json" --data-binary "@q2.json"
```
**Esperado:** a primeira responde com pratos do cardápio (Chocolate Brownie, Vegan Bowl...); a segunda recusa com educação e volta ao restaurante. O cardápio (nome, descrição, preço e estoque) é lido do banco a cada pergunta: esgote um prato e pergunte por ele, e o assistente diz que acabou.

### 8. Docker e contrato (5 pts)
- `docker compose up -d` sobe o RabbitMQ.
- No app do professor: cardápio, pedido, avaliação, ranking (atualiza a cada 5 s) e assistente funcionam sem ajustes. Erros aparecem como `409 · Prato esgotado`, `502 · Pagamento indisponível` etc.
- Contrato (verificado usando o próprio `src/api.js` do app): `GET /dishes`, `GET /dishes/{id}`, `POST /orders` (201/400/404/409/502), `GET /orders/{id}`, `POST /reviews` (202/400/404), `POST /assistant`, `GET /reviews/ranking` e `POST /payments`.

### 9. Bônus: rate limit (+10)
`POST /orders` aceita no máximo 20 requisições por segundo (token bucket em `OrderRateLimitFilter`); o excedente recebe `429 {"error":"Too many requests, try again in a moment"}`. Para desligar: `rate-limit.orders.enabled=false` em `order-service/src/main/resources/application.properties`.

---

## Decisões de projeto

- **Lock durante o pagamento:** `OrderService.createOrder` mantém a linha do prato bloqueada (`PESSIMISTIC_WRITE`) até o fim da transação, inclusive durante a chamada ao pagamento. O estoque nunca fica negativo e, se o pagamento falhar, o rollback deixa o estoque intacto, sem precisar de compensação.
- **H2 em memória:** `LOCK_TIMEOUT=60000` e pool de conexões maior no `order-service`, para as requisições concorrentes esperarem a vez em vez de falharem.
- **Flush sem perda:** o `review-service` usa `ConcurrentHashMap.remove(key)` (atômico) em vez de `forEach` + `clear()`, então uma avaliação que chega durante o flush não é descartada.
- **Tempos do Eureka reduzidos** (heartbeat de 5 s e cache do load balancer de 5 s) para que subir ou derrubar instâncias fique visível rápido nos testes.
- **Endereços do Eureka e do RabbitMQ** usam os padrões do Spring (portas 8761 e 5672), sem `localhost` escrito nos arquivos.
