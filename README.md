# Bank Platform - Architecture micro-services agentique


Réalisé par : **Fatima EL ABBADI**

Plateforme bancaire permettant de consulter des comptes, d'enregistrer des transactions et des virements, de publier des événements métier et d'interroger les données via un assistant conversationnel agentique.

Rapport complet : [Rapport_EL_ABBADI_Fatima.pdf](./Rapport_EL_ABBADI_Fatima.pdf)

---

## Stack technique

| Domaine | Technologies |
|---|---|
| Micro-services | Java 21, Spring Boot 3.5, Spring Cloud 2025.0 (Eureka, Config, Gateway, OpenFeign) |
| Exposition | REST (Swagger/OpenAPI), GraphQL, MCP (Model Context Protocol) |
| Tolérance aux pannes | Resilience4J (circuit breaker, time limiter, fallback) |
| Assistant conversationnel | Spring AI 1.0, MCP Client, LLM Groq (openai/gpt-oss-120b), mémoire de conversation |
| Asynchrone | Apache Kafka 3.9 (KRaft) |
| Client web | React + Vite |
| Data / ML | Python, pandas, scikit-learn |

---

## Modules (projet Maven multi-modules)

| Module | Port | Rôle |
|---|---|---|
| discovery-service | 8761 | Annuaire Eureka |
| config-service | 9999 | Configuration centralisée (dossier config-repo) |
| gateway-service | 8888 | Point d'entrée unique, routage lb://, CORS |
| account-service | 8081 | Comptes bancaires : REST, GraphQL, MCP |
| transaction-service | 8082 | Transactions et virements : REST, GraphQL, MCP, OpenFeign + Resilience4J, producteur Kafka |
| bank-agent | 8085 | Assistant conversationnel (Spring AI + MCP Client) |
| audit-service | 8086 | Consommateur Kafka (journalisation) |
| web-client | 5173 | Client React |
| fraud-detection | - | Détection de fraude en Python |

Chaque service métier suit la même structure en couches :

```
domain/ · repository/ · service/ · web/rest/ · web/graphql/ · mcp/
```

Les contrôleurs REST, GraphQL et les outils MCP appellent la même couche service.

---

## Règles métier (transaction-service)

- Montant strictement positif et inférieur ou égal au plafond `bank.transaction.max-amount=1000` (fourni par le config-service)
- Le compte doit exister et être actif (ACTIVE)
- La devise de la transaction doit correspondre à celle du compte
- Consultation du solde avant la transaction, mise à jour du solde après l'enregistrement
- Virement d'un compte vers un autre (débit + crédit, avec compensation en cas d'échec)

---

## Lancement en local

### Prérequis

Java 21, Maven, Docker (pour Kafka), Node.js 18+, Python 3.10+

### 1. Kafka

```bash
docker compose -f docker-compose.kafka.yml up -d
```

Kafka UI : http://localhost:8090

### 2. Micro-services (dans cet ordre, un terminal par service)

```bash
cd discovery-service   && ./mvnw spring-boot:run
cd config-service      && ./mvnw spring-boot:run
cd account-service     && ./mvnw spring-boot:run
cd transaction-service && ./mvnw spring-boot:run
cd audit-service       && ./mvnw spring-boot:run
cd gateway-service     && ./mvnw spring-boot:run
```

### 3. Assistant conversationnel

```bash
# PowerShell : $env:GROQ_API_KEY="gsk_..."
export GROQ_API_KEY=gsk_...
cd bank-agent && ./mvnw spring-boot:run
```

La clé API n'est jamais commitée : elle est fournie par une variable d'environnement.

### 4. Client web

```bash
cd web-client && npm install && npm run dev
```

Accès : http://localhost:5173

### 5. Détection de fraude

```bash
cd fraud-detection
pip install -r requirements.txt
python fraud_detection.py
```

Les résultats (métriques, matrice de confusion, courbe précision-rappel) sont générés dans `fraud-detection/outputs/`.

---

## URLs utiles

| Ressource | URL |
|---|---|
| Eureka | http://localhost:8761 |
| Gateway - comptes | http://localhost:8888/api/accounts |
| Gateway - transactions | http://localhost:8888/api/transactions |
| Swagger account-service | http://localhost:8081/swagger-ui.html |
| Swagger transaction-service | http://localhost:8082/swagger-ui.html |
| GraphiQL | http://localhost:8081/graphiql et http://localhost:8082/graphiql |
| MCP (SSE) | http://localhost:8081/sse et http://localhost:8082/sse |
| Assistant | http://localhost:8888/api/chat?message=Liste%20les%20comptes |
| Configuration | http://localhost:9999/transaction-service/default |

---

## Exemples de questions pour l'assistant

- Liste tous les comptes
- Quel est le solde du client CUST-001 ?
- Fais un paiement de 200 EUR chez GROCERY au Maroc pour CUST-001
- Fais un virement de 50 EUR de CUST-001 vers CUST-002

---

## Kafka

Après chaque transaction persistée (`@TransactionalEventListener(AFTER_COMMIT)`), un événement `TransactionCreated` est publié dans le topic `bank.transactions.created`, avec `accountId` comme clé. Il est consommé et journalisé par audit-service, sans modification des données bancaires.

---

## Détection de fraude - résultats

| Indicateur | Valeur |
|---|---|
| Taux de fraude | 5,14 % (classes déséquilibrées) |
| Modèle retenu | Régression logistique (class_weight="balanced") |
| Average precision (test) | 0,189 (hasard : 0,051) |
| Rappel fraude (seuil 0,5) | 0,63 |

Le prétraitement est ajusté uniquement sur les données d'entraînement, et le seuil de décision est choisi sur un jeu de validation.
