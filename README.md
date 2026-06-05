# 📚 Backend Presença

Sistema backend para gerenciamento de presença de estudantes utilizando RFID, desenvolvido com Java + Spring Boot. Para o Projetaí, evento criado pela Faculdade Senac Pernambuco para demonstrar e avaliar as atividades dos alunos do 4ºPeríodo 2026.1.

---

## Sobre o Projeto

O **Backend Presença** é uma API REST responsável pelo gerenciamento de estudantes e controle de chamadas automatizadas através de tags RFID.

O sistema permite:

* Cadastro de estudantes
* Associação de UID RFID
* Registro automático de presença
* Prevenção de múltiplas presenças no mesmo dia
* Atualização e remoção de estudantes
* Integração com frontend em Next.js

---

## Tecnologias Utilizadas

* Java 24
* Spring Boot 3
* Spring Data JPA
* MySQL
* Maven
* Lombok
* jSerialComm
* REST API

---

## Estrutura do Projeto

```bash
src/main/
├── java/br/com/chamada/estudante
│   ├── controller
│   ├── model
│   ├── repository
│   └── service
└── resources
    └── application.properties
```

---

## Funcionalidades

### Estudantes

* Listar estudantes
* Cadastrar estudante
* Atualizar dados
* Atualização parcial do UID RFID
* Remover estudante

### Presença

* Registrar presença por RFID
* Validação de presença única diária
* Controle automático de data/hora

---

##  Endpoints

###  Listar estudantes

```http
GET /api/estudantes
```

---

### Cadastrar estudante

```http
POST /api/estudantes/cadastrar
```

#### Parâmetros

| Parâmetro | Tipo   | Descrição         |
| --------- | ------ | ----------------- |
| uid       | String | UID da tag RFID   |
| nome      | String | Nome do estudante |

---

### Registrar presença

```http
POST /api/estudantes/chamada
```

#### Parâmetros

| Parâmetro | Tipo   |
| --------- | ------ |
| uid       | String |

---

### Atualizar estudante

```http
PUT /api/estudantes/atualizar
```

---

### Atualização parcial do UID

```http
PATCH /api/estudantes/atualizar-parcial/{id}
```

---

### Deletar estudante

```http
DELETE /api/estudantes/deletar/{id}
```

---

## Banco de Dados

O projeto utiliza MySQL com JPA/Hibernate.

Configure o arquivo:

```properties
src/main/resources/application.properties
```

Exemplo:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/presenca
spring.datasource.username=root
spring.datasource.password=sua_senha
```

---

## ▶️ Como Executar

### Pré-requisitos

* Java 24
* Maven
* MySQL

---

### Clone o repositório

```bash
git clone https://github.com/LuizFelipeMoraesSantos/Backend_Presenca.git
```

---

### Execute o projeto

```bash
./mvnw spring-boot:run
```

ou

```bash
mvn spring-boot:run
```

---

## Validação Inteligente de Presença

O sistema impede que um estudante registre presença múltiplas vezes no mesmo dia, garantindo maior integridade dos dados.

---

## Integração

Este backend foi desenvolvido para integração com:

* FrontEnd Presença (Next.js)
* Arduino + RFID
* Sistemas acadêmicos

---

## Melhorias Futuras

* Autenticação JWT
* Dashboard administrativo
* Relatórios PDF
* Exportação CSV
* Histórico completo de presença
* Dockerização
* Deploy em nuvem

---

## Autores

Desenvolvido por Luiz Felipe Moraes Santos.
