# Diagrama de Casos de Uso — API da Biblioteca do Campus

Este documento apresenta o **Diagrama de Casos de Uso UML** do sistema de gerenciamento da Biblioteca do Campus, contemplando todos os requisitos funcionais (**RF01 a RF17**) e os três perfis de interação (**Público / Visitante**, **Leitor** e **Bibliotecário**).

---

## 1. Visualização em Mermaid

```mermaid
flowchart LR
    %% Atores
    Visitante(["👤 Visitante / Público"])
    Leitor(["🎓 Leitor Autenticado"])
    Bibliotecario(["📚 Bibliotecário Autenticado"])

    %% Herança de Atores
    Leitor -.->|herda acesso público| Visitante
    Bibliotecario -.->|herda acesso público| Visitante

    subgraph Sistema["Sistema de Biblioteca do Campus (API REST)"]
        direction TB

        subgraph Publico["Acesso Público (RF01, RF02, RF05)"]
            UC01(["UC01: Cadastrar-se como Leitor (RF01)"])
            UC02(["UC02: Autenticar-se / Login JWT (RF02)"])
            UC05(["UC05: Consultar Acervo de Livros com Filtros e Paginação (RF05)"])
        end

        subgraph AreaLeitor["Operações do Leitor (RF08, RF10, RF11, RF13)"]
            UC08(["UC08: Renovar Próprio Empréstimo Ativo (+7 dias, máx. 1x) (RF08)"])
            UC10A(["UC10a: Consultar Próprios Empréstimos e Multas (RF10)"])
            UC11(["UC11: Solicitar Reserva de Livro Indisponível (Fila FIFO) (RF11)"])
            UC13(["UC13: Cancelar Reserva Ativa (RF13)"])
        end

        subgraph AreaBibliotecario["Operações do Bibliotecário (RF03, RF04, RF06, RF07, RF09, RF10, RF12, RF14, RF15-17)"]
            UC03(["UC03: Gerenciar Livros (Criar, Editar, Remover) (RF03)"])
            UC04(["UC04: Gerenciar Exemplares Físicos e Disponibilidade (RF04)"])
            UC06(["UC06: Registrar Empréstimo (14 dias, máx. 3 ativos) (RF06)"])
            UC07(["UC07: Registrar Devolução de Exemplar (RF07)"])
            UC07_Multa(["Gerar Multa Automática (R$ 1,00/dia de atraso)"])
            UC07_Fila(["Separar Exemplar para 1º da Fila de Reserva (3 dias)"])
            UC09(["UC09: Registrar Pagamento de Multa e Desbloquear Leitor (RF09)"])
            UC10B(["UC10b: Consultar Empréstimos e Multas de Qualquer Leitor (RF10)"])
            UC12(["UC12: Registrar Retirada de Reserva e Converter em Empréstimo (RF12)"])
            UC14(["UC14: Consultar Fila de Reservas e Processar Expiradas em Lote (RF14)"])
            UC15(["UC15: Emitir Relatório Top 10 Livros Mais Emprestados (RF15)"])
            UC16(["UC16: Emitir Relatório de Empréstimos em Atraso (RF16)"])
            UC17(["UC17: Emitir Relatório de Multas Pendentes por Leitor (RF17)"])
        end
    end

    %% Associações Visitante
    Visitante --> UC01
    Visitante --> UC02
    Visitante --> UC05

    %% Associações Leitor
    Leitor --> UC08
    Leitor --> UC10A
    Leitor --> UC11
    Leitor --> UC13

    %% Associações Bibliotecário
    Bibliotecario --> UC03
    Bibliotecario --> UC04
    Bibliotecario --> UC06
    Bibliotecario --> UC07
    Bibliotecario --> UC09
    Bibliotecario --> UC10B
    Bibliotecario --> UC12
    Bibliotecario --> UC13
    Bibliotecario --> UC14
    Bibliotecario --> UC15
    Bibliotecario --> UC16
    Bibliotecario --> UC17

    %% Relacionamentos <<extend>>
    UC07 -.->|"<<extend>> se devolvido com atraso"| UC07_Multa
    UC07 -.->|"<<extend>> se houver fila AGUARDANDO"| UC07_Fila
```

---

## 2. Especificação em PlantUML

Caso deseje exportar o diagrama na notação clássica PlantUML:

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle

actor "Visitante / Público" as Visitante
actor "Leitor" as Leitor
actor "Bibliotecário" as Bibliotecario

Visitante <|-- Leitor
Visitante <|-- Bibliotecario

rectangle "API da Biblioteca do Campus" {
    usecase "UC01: Cadastrar-se como Leitor (RF01)" as UC01
    usecase "UC02: Autenticar-se via JWT (RF02)" as UC02
    usecase "UC05: Buscar Livros no Acervo Paginado (RF05)" as UC05

    usecase "UC08: Renovar Próprio Empréstimo (RF08)" as UC08
    usecase "UC10a: Consultar Próprios Empréstimos e Multas (RF10)" as UC10A
    usecase "UC11: Solicitar Reserva de Livro sem Disponíveis (RF11)" as UC11
    usecase "UC13: Cancelar Reserva Ativa (RF13)" as UC13

    usecase "UC03: Manter Cadastro de Livros (RF03)" as UC03
    usecase "UC04: Manter Exemplares e Disponibilidade (RF04)" as UC04
    usecase "UC06: Registrar Empréstimo (RF06)" as UC06
    usecase "UC07: Registrar Devolução (RF07)" as UC07
    usecase "Gerar Multa R$ 1,00/dia" as UC07_Multa
    usecase "Separar para Fila de Reserva (3 dias)" as UC07_Fila
    usecase "UC09: Registrar Pagamento de Multa (RF09)" as UC09
    usecase "UC10b: Consultar Empréstimos/Multas Gerais (RF10)" as UC10B
    usecase "UC12: Converter Reserva em Empréstimo (RF12)" as UC12
    usecase "UC14: Consultar Fila e Expirar Reservas Vencidas (RF14)" as UC14
    usecase "UC15: Relatório Top 10 Livros no Período (RF15)" as UC15
    usecase "UC16: Relatório Empréstimos em Atraso (RF16)" as UC16
    usecase "UC17: Relatório Multas Pendentes por Leitor (RF17)" as UC17

    UC07 <.. UC07_Multa : <<extend>>\n[dataDevolucao > dataPrevista]
    UC07 <.. UC07_Fila : <<extend>>\n[fila de reserva ativa]
}

Visitante --> UC01
Visitante --> UC02
Visitante --> UC05

Leitor --> UC08
Leitor --> UC10A
Leitor --> UC11
Leitor --> UC13

Bibliotecario --> UC03
Bibliotecario --> UC04
Bibliotecario --> UC06
Bibliotecario --> UC07
Bibliotecario --> UC09
Bibliotecario --> UC10B
Bibliotecario --> UC12
Bibliotecario --> UC13
Bibliotecario --> UC14
Bibliotecario --> UC15
Bibliotecario --> UC16
Bibliotecario --> UC17
@enduml
```

---

## 3. Matriz de Rastreabilidade dos Casos de Uso (RF01 a RF17)

| Caso de Uso | Requisito | Ator(es) | Endpoint Principal | Resumo das Regras de Negócio |
| :--- | :--- | :--- | :--- | :--- |
| **UC01** | **RF01** | Público | `POST /api/auth/register` | Cadastra novo usuário com perfil `LEITOR`, e-mail único e senha BCrypt (mín. 8 caracteres). Nunca retorna a senha. |
| **UC02** | **RF02** | Público | `POST /api/auth/login` | Autentica por e-mail e senha e emite token JWT RSA. Retorna `401` genérico em falha sem revelar qual campo errou. |
| **UC03** | **RF03** | Bibliotecário | `POST/PUT/DELETE /api/livros` | CRUD de livros com ISBN único. Impede exclusão se houver exemplares emprestados/separados ou reservas ativas. |
| **UC04** | **RF04** | Bibliotecário | `POST /api/livros/{id}/exemplares`<br>`PATCH /api/exemplares/{id}/*` | Cadastra cópias físicas (`codigoPatrimonio` único) e controla transição entre `DISPONIVEL` e `INDISPONIVEL`. |
| **UC05** | **RF05** | Público | `GET /api/livros` | Consulta paginada por título, autor ou categoria, calculando dinamicamente a `quantidadeDisponivel`. |
| **UC06** | **RF06** | Bibliotecário | `POST /api/emprestimos` | Empresta exemplar `DISPONIVEL` por 14 dias. Limite de 3 empréstimos ativos; bloqueia leitores com atraso ou multa pendente. |
| **UC07** | **RF07** | Bibliotecário | `PATCH /api/emprestimos/{id}/devolucao` | Finaliza empréstimo; gera multa de R$ 1,00/dia se atrasado; separa exemplar por 3 dias se houver fila de reserva. |
| **UC08** | **RF08** | Leitor | `PATCH /api/emprestimos/{id}/renovar` | Prorroga o próprio empréstimo em +7 dias (máx. 1 vez), desde que sem atraso, sem bloqueio e sem reserva ativa para o livro. |
| **UC09** | **RF09** | Bibliotecário | `PATCH /api/multas/{id}/pagar` | Quita multa pendente (`status = PAGA`) e libera o leitor caso não possua outras pendências. |
| **UC10** | **RF10** | Leitor / Bibliotecário | `GET /api/emprestimos/meus`<br>`GET /api/emprestimos` | Leitor consulta apenas seus próprios empréstimos/multas; Bibliotecário consulta todos com filtros. |
| **UC11** | **RF11** | Leitor | `POST /api/reservas` | Entra na fila FIFO (`AGUARDANDO`) quando `quantidadeDisponivel == 0`, se não estiver bloqueado nem com o livro em mãos. |
| **UC12** | **RF12** | Bibliotecário | `POST /api/reservas/{id}/retirar` | Converte reserva `PRONTA_PARA_RETIRADA` em `Emprestimo` de 14 dias e marca reserva como `ATENDIDA`. |
| **UC13** | **RF13** | Leitor / Bibliotecário | `PATCH /api/reservas/{id}/cancelar` | Cancela reserva ativa; se tinha exemplar separado, repassa ao próximo da fila (+3 dias) ou devolve para `DISPONIVEL`. |
| **UC14** | **RF14** | Bibliotecário | `GET /api/reservas/livro/{id}/fila`<br>`POST /api/reservas/processar-expiradas` | Lista fila ordenada do livro e expira em lote reservas cujo prazo de 3 dias venceu, repassando os exemplares. |
| **UC15** | **RF15** | Bibliotecário | `GET /api/relatorios/livros-mais-emprestados` | Lista os 10 livros mais emprestados em um intervalo de datas (`inicio` e `fim`). |
| **UC16** | **RF16** | Bibliotecário | `GET /api/relatorios/emprestimos-atrasados` | Lista todos os empréstimos vencidos e não devolvidos, ordenados do maior atraso para o menor. |
| **UC17** | **RF17** | Bibliotecário | `GET /api/relatorios/multas-pendentes-por-leitor` | Lista o montante total de multas `PENDENTE` acumulado por cada leitor. |
