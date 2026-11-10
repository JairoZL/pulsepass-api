# PulsePass API

Proyecto académico (Programación para Web, UniMagdalena): plataforma de eventos,
artistas y entradas. El estudiante sustentará el proyecto oralmente y debe poder
explicar cada línea de código.

## Stack
Java 21, Spring Boot 4.1.x, Maven, Spring Data JPA/Hibernate, PostgreSQL 18
(Docker, compose.yaml), Flyway, Testcontainers, JUnit 5, Mockito, AssertJ.
Más adelante: MapStruct, Spring Security, JJWT.

## Fuente de verdad
Los PRD en docs/ mandan sobre cualquier otra cosa. Léelos solo cuando la tarea
toque esa capa:
- Persistencia: docs/PRD_PulsePass.md
- Servicios: docs/PRD_PulsePass_Capa_Servicios.pdf
- Controladores: docs/PRD_PulsePass_Capa_Controladores_v2.pdf
- Seguridad: docs/PRD_PulsePass_Capa_Seguridad_Spring_Security_JJWT.pdf
- Pruebas E2E (Postman): docs/PRD_PulsePass_Pruebas_E2E_Postman_Manual.pdf
- Frontend: docs/PRD_PulsePass_Frontend_React_TypeScript_Tailwind.pdf
  Al justificar una decisión cita el ID del requisito (FR-..., BR-...).
  No inventes contratos que el PRD no define.

## Reglas técnicas
- Flyway es el único que crea o cambia el esquema; ddl-auto=validate.
- Nunca edites una migración ya aplicada (V1 está aplicada). Todo cambio va en una nueva.
- Sin H2: pruebas de integración con Testcontainers + PostgreSQL.
- Sin SQL nativo en las consultas del taller: Query Methods para lo simple, JPQL para lo complejo.
- Sin @Data de Lombok en entidades. Enums con EnumType.STRING. Dinero con BigDecimal / NUMERIC(10,2).
- Inyección por constructor. Los Services devuelven DTOs (record), nunca entidades.
  Los Controllers son delgados: sin lógica de negocio ni acceso a Repository.
- SQL en snake_case y tablas en plural: venues, events, artists, event_artists,
  users, user_profiles, tickets.
- spring.jpa.open-in-view=false.

## Estado actual (actualizar al cerrar cada fase)
- V1 aplicada (7 tablas). Rama de trabajo: feature/persistence-model.
- Siguiente: enums, entidades JPA, repositories, V2 (artistas iniciales),
  V3 (streaming_url), tests de integración.

## Flujo de trabajo
- git flow: feature/* desde develop, PR a develop, release/* a main con tag.
  Nunca commits directos en main ni develop.
- Commits en español, cortos, con el ID del requisito cuando aplique.
- No hagas commit ni push sin que yo lo pida.

## Cómo debes ayudarme
- Responde en español y de forma concisa.
- Antes de generar algo grande, dime el plan en pocas líneas y espera mi OK.
- Las piezas centrales las escribo yo (esquema, entidades, TicketService.purchase,
  JwtService, AuthorizationService). Tú haces tests, boilerplate repetitivo,
  revisión contra el PRD y depuración.
- Al revisar, indica requisito y archivo:línea de cada discrepancia; no reescribas todo.
- Si el PRD es ambiguo o se contradice (p. ej. capacidad del venue 5000 vs 3),
  pregúntame: no elijas en silencio.
- Pide confirmación antes de tocar pom.xml o ejecutar comandos que borren datos
  (docker compose down -v).