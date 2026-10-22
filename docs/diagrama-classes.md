# Diagrama de Classes UML — API da Biblioteca do Campus

Este documento apresenta o **Diagrama de Classes de Domínio** da API da Biblioteca do Campus, detalhando as entidades JPA, suas propriedades, enumerações e cardinalidades de relacionamento.

---

## 1. Diagrama de Classes (Mermaid)

```mermaid
classDiagram
    direction TB

    class Usuario {
        -Long id
        -String nome
        -String email
        -String senha
        -Role role
        -LocalDateTime dataCadastro
        +onCreate() void
    }

    class Role {
        <<enumeration>>
        LEITOR
        BIBLIOTECARIO
    }

    class Livro {
        -Long id
        -String titulo
        -String autor
        -String isbn
        -String categoria
        -Integer anoPublicacao
        -List~Exemplar~ exemplares
    }

    class Exemplar {
        -Long id
        -String codigoPatrimonio
        -StatusExemplar status
        -String motivoIndisponibilidade
        -Livro livro
    }

    class StatusExemplar {
        <<enumeration>>
        DISPONIVEL
        EMPRESTADO
        SEPARADO
        INDISPONIVEL
    }

    class Emprestimo {
        -Long id
        -Exemplar exemplar
        -Usuario leitor
        -LocalDate dataEmprestimo
        -LocalDate dataPrevistaDevolucao
        -LocalDate dataDevolucao
        -boolean renovado
        -StatusEmprestimo status
        -Multa multa
    }

    class StatusEmprestimo {
        <<enumeration>>
        ATIVO
        DEVOLVIDO
    }

    class Multa {
        -Long id
        -Emprestimo emprestimo
        -BigDecimal valor
        -long diasAtraso
        -StatusMulta status
        -LocalDateTime dataGeracao
        -LocalDateTime dataPagamento
        +onCreate() void
    }

    class StatusMulta {
        <<enumeration>>
        PENDENTE
        PAGA
    }

    class Reserva {
        -Long id
        -Livro livro
        -Usuario leitor
        -Exemplar exemplarSeparado
        -LocalDateTime dataReserva
        -LocalDate dataLimiteRetirada
        -StatusReserva status
        +onCreate() void
    }

    class StatusReserva {
        <<enumeration>>
        AGUARDANDO
        PRONTA_PARA_RETIRADA
        ATENDIDA
        CANCELADA
        EXPIRADA
    }

    %% Relacionamentos de Entidades e Cardinalidades
    Livro "1" *-- "0..*" Exemplar : possui >
    Exemplar "1" <-- "0..*" Emprestimo : refere-se a <
    Usuario "1" <-- "0..*" Emprestimo : realiza (leitor) <
    Emprestimo "1" -- "0..1" Multa : gera >
    Livro "1" <-- "0..*" Reserva : é reservado em <
    Usuario "1" <-- "0..*" Reserva : solicita (leitor) <
    Exemplar "0..1" <-- "0..*" Reserva : separado para <

    %% Relacionamentos com Enumerações
    Usuario ..> Role : usa
    Exemplar ..> StatusExemplar : usa
    Emprestimo ..> StatusEmprestimo : usa
    Multa ..> StatusMulta : usa
    Reserva ..> StatusReserva : usa
```

---

## 2. Código PlantUML Equivalente

Caso deseje exportar o diagrama utilizando o [PlantUML](https://www.planttext.com/):

```plantuml
@startuml
skinparam classAttributeIconSize 0
title Diagrama de Classes - API da Biblioteca do Campus

enum Role {
  LEITOR
  BIBLIOTECARIO
}

enum StatusExemplar {
  DISPONIVEL
  EMPRESTADO
  SEPARADO
  INDISPONIVEL
}

enum StatusEmprestimo {
  ATIVO
  DEVOLVIDO
}

enum StatusMulta {
  PENDENTE
  PAGA
}

enum StatusReserva {
  AGUARDANDO
  PRONTA_PARA_RETIRADA
  ATENDIDA
  CANCELADA
  EXPIRADA
}

class Usuario {
  - id : Long
  - nome : String
  - email : String
  - senha : String
  - role : Role
  - dataCadastro : LocalDateTime
}

class Livro {
  - id : Long
  - titulo : String
  - autor : String
  - isbn : String
  - categoria : String
  - anoPublicacao : Integer
}

class Exemplar {
  - id : Long
  - codigoPatrimonio : String
  - status : StatusExemplar
  - motivoIndisponibilidade : String
}

class Emprestimo {
  - id : Long
  - dataEmprestimo : LocalDate
  - dataPrevistaDevolucao : LocalDate
  - dataDevolucao : LocalDate
  - renovado : boolean
  - status : StatusEmprestimo
}

class Multa {
  - id : Long
  - valor : BigDecimal
  - diasAtraso : long
  - status : StatusMulta
  - dataGeracao : LocalDateTime
  - dataPagamento : LocalDateTime
}

class Reserva {
  - id : Long
  - dataReserva : LocalDateTime
  - dataLimiteRetirada : LocalDate
  - status : StatusReserva
}

Livro "1" *-- "0..*" Exemplar : possui >
Exemplar "1" <-- "0..*" Emprestimo : refere-se a
Usuario "1" <-- "0..*" Emprestimo : leitor
Emprestimo "1" -- "0..1" Multa : gera >
Livro "1" <-- "0..*" Reserva : livro reservado
Usuario "1" <-- "0..*" Reserva : leitor
Exemplar "0..1" <-- "0..*" Reserva : exemplarSeparado

Usuario .right.> Role
Exemplar .right.> StatusExemplar
Emprestimo .right.> StatusEmprestimo
Multa .right.> StatusMulta
Reserva .right.> StatusReserva
@enduml
```

---

## 3. Detalhamento das Entidades e Relacionamentos

| Entidade | Tabela no Banco | Relacionamentos Principais | Regras de Integridade e Domínio |
| :--- | :--- | :--- | :--- |
| **`Usuario`** | `usuarios` | `1:N` com `Emprestimo` e `Reserva` | `email` único; senha criptografada com BCrypt; perfil `LEITOR` ou `BIBLIOTECARIO`. |
| **`Livro`** | `livros` | `1:N` com `Exemplar` (`CascadeType.ALL`, `orphanRemoval = true`) | `isbn` único; representa a obra catalográfica e agrega seus exemplares físicos. |
| **`Exemplar`** | `exemplares` | `N:1` com `Livro` | `codigoPatrimonio` único; controla a máquina de estados física (`DISPONIVEL`, `EMPRESTADO`, `SEPARADO`, `INDISPONIVEL`). |
| **`Emprestimo`** | `emprestimos` | `N:1` com `Exemplar` e `Usuario`; `1:1` opcional com `Multa` | Prazo inicial de **14 dias**; permite no máximo **1 renovação** (+7 dias) se não houver atraso, multa pendente ou fila de reserva. |
| **`Multa`** | `multas` | `1:1` com `Emprestimo` (`unique = true`) | Gerada automaticamente na devolução com atraso: **R$ 1,00 por dia de atraso** (sem teto máximo). Bloqueia novos empréstimos, renovações e reservas até ser `PAGA`. |
| **`Reserva`** | `reservas` | `N:1` com `Livro`, `Usuario` e `Exemplar` (opcional) | Fila FIFO ordenada por `dataReserva ASC`. Quando um exemplar é devolvido, vincula o `exemplarSeparado`, muda para `PRONTA_PARA_RETIRADA` e define `dataLimiteRetirada` (+3 dias). |
