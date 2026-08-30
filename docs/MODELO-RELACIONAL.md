# Modelo Relacional — Oficina Mecânica

Modelo extraído das entidades JPA do projeto (schema `oficina_mecanica`, tabelas geradas via Hibernate `ddl-auto`).

> Observação: a tabela `app_users` existente em `db/init.sql` é uma tabela legada de seed/autenticação, sem entidade JPA correspondente — não faz parte do modelo de domínio abaixo.

## Diagrama ER (Mermaid)

```mermaid
erDiagram
    CUSTOMER {
        uuid customer_id PK
        string first_name
        string last_name
        string email
        string phone_number
        string document
        enum document_type
    }

    VEHICLE {
        uuid vehicle_id PK
        uuid customer_id FK
        string license_plate
        string manufacturer
        string model
        string color
        int year
        long km_driven
        enum type
    }

    WORKER {
        uuid worker_id PK
        enum profile
        string first_name
        string last_name
        string email
        string phone_number
        string password_hash
        boolean active
    }

    PARTS {
        uuid part_id PK
        string name
        string description
        decimal unit_price
        int stock_quantity
        string unit
        int minimum_stock
        enum part_type
        long version
    }

    SERVICE_ITEM {
        uuid service_item_id PK
        string name
        string description
        decimal base_price
        int estimated_duration_minutes
        boolean active
    }

    WORK_ORDER {
        uuid work_order_id PK
        uuid customer_id FK
        uuid vehicle_id FK
        uuid assigned_worker_id FK
        string description
        enum priority
        enum status
        datetime opened_at
        datetime closed_at
        datetime cancelled_at
        decimal estimated_value
        decimal final_value
    }

    ESTIMATE {
        uuid estimate_id PK
        uuid work_order_id FK
        enum status
        decimal parts_amount
        decimal labor_amount
        decimal total_amount
        datetime approved_at
        datetime sent_at
    }

    ESTIMATE_ITEM {
        uuid estimate_item_id PK
        uuid estimate_id FK
        uuid part_id FK
        int quantity
        decimal unit_price
        decimal total_price
    }

    WORK_ORDER_HISTORY {
        uuid work_order_history_id PK
        uuid work_order_id FK
        enum previous_status
        enum new_status
        datetime changed_at
    }

    WORK_ORDER_SERVICE {
        uuid work_order_service_id PK
        uuid work_order_id FK
        uuid service_item_id FK
        string description
        decimal price
        datetime performed_at
    }

    CUSTOMER ||--o{ VEHICLE : possui
    CUSTOMER ||--o{ WORK_ORDER : solicita
    VEHICLE ||--o{ WORK_ORDER : "é objeto de"
    WORKER ||--o{ WORK_ORDER : "atende (opcional)"
    WORK_ORDER ||--o{ ESTIMATE : gera
    WORK_ORDER ||--o{ WORK_ORDER_HISTORY : registra
    WORK_ORDER ||--o{ WORK_ORDER_SERVICE : inclui
    SERVICE_ITEM ||--o{ WORK_ORDER_SERVICE : "originou (opcional)"
    ESTIMATE ||--o{ ESTIMATE_ITEM : detalha
    PARTS ||--o{ ESTIMATE_ITEM : usada_em
```

## Entidades isoladas (sem FK no modelo JPA atual)
Nenhuma — todas as entidades possuem ao menos um relacionamento.

## Enums utilizados
| Enum | Valores |
|---|---|
| `DocumentType` | CPF, CNPJ |
| `VehicleType` | CAR, MOTORCYCLE |
| `WorkerProfile` | MECHANIC, ATTENDANT |
| `PartType` | PART, SUPPLY |
| `WorkOrderPriority` | LOW, MEDIUM, HIGH, URGENT |
| `WorkOrderStatus` | RECEIVED, DIAGNOSIS, WAITING_APPROVAL, IN_PROGRESS, COMPLETED, DELIVERED |
| `EstimateStatus` | PENDING, APPROVED, REJECTED |

## Cardinalidades
- Customer (1) → (N) Vehicle
- Customer (1) → (N) WorkOrder
- Vehicle (1) → (N) WorkOrder
- Worker (1) → (N) WorkOrder (mecânico responsável, FK `assigned_worker_id` opcional/nullable)
- WorkOrder (1) → (N) Estimate
- WorkOrder (1) → (N) WorkOrderHistory
- WorkOrder (1) → (N) WorkOrderService
- ServiceItem (1) → (N) WorkOrderService (item de catálogo de origem, FK `service_item_id` opcional/nullable)
- Estimate (1) → (N) EstimateItem
- Part (1) → (N) EstimateItem

