# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project shape

Spring Boot 3.2.2 sandbox on Java 21, used as a personal scratchpad rather than a deployable service. Multiple unrelated demos coexist under `com.example.demo`:

- **Third-party HTTP client demo** — `controller/ApiDemoController` → `service/ApiService(Impl)` → `utils/HttpClientUtils` → `RestTemplate` (configured in `config/RestTemplateConfig` with 15s connect / 30s read timeouts). The standard request flow goes through `HttpClientUtils.exchange(...)`; `ApiResponse<T>` is the uniform `{code, message, data}` envelope returned by the controllers.
- **RocketMQ 5.x client examples** — `ProducerExample` and `PushConsumerExample` are standalone `main()` programs pointing at `localhost:8081`; they require a running RocketMQ proxy with topic `TestTopic` and group `YourConsumerGroup` pre-created. Not wired into Spring.
- **Apache POI docx signature stamper** — `Test.java` (`main()` is the entry point) reads a `.docx` from `src/main/java/com/example/demo/doc/`, finds paragraphs/tables containing role keywords (`编写`/`审核`/`批准`), and overlays signature PNGs with date stamps. Heavy use of low-level `org.openxmlformats.schemas.wordprocessingml` CT* types — when editing, preserve the existing patterns (paragraph spacing reset, run clean-up, vertical cell merging via `mergeCellsVertically`). All file paths inside `Test.java` are relative to the project root, so it must be run from `/Users/yumeng/IdeaProjects/demo`.
- **Algorithm scratch** — `DemoApplication.maxSubArray2` / `plusOne` are leetcode-style snippets, unrelated to the Spring app.

## Quirk: DemoApplication.main is intentionally disabled

`DemoApplication.main(...)` has `SpringApplication.run(...)` commented out. Running `mvn spring-boot:run` will start the web context, but `java -jar` of the built artifact will exit immediately. If you need the Spring context to boot (e.g. for `ApiDemoController`), use `mvn spring-boot:run` or uncomment that line — don't assume the application is broken.

`src/main/resources/application.properties` is empty, so the app runs with all Spring Boot defaults (port 8080, no profiles).

## Common commands

```bash
./mvnw clean package                                  # full build
./mvnw spring-boot:run                                # start web app (needs main() uncommented to be useful via java -jar)
./mvnw test                                           # run all tests
./mvnw test -Dtest=DemoApplicationTests#contextLoads  # single test method
```

The only test (`DemoApplicationTests.contextLoads`) is a `@SpringBootTest` that boots the full context — it will fail if a bean wiring breaks even when no behavior is asserted.

## Dependency notes

- `pom.xml` imports `spring-cloud-dependencies` BOM but no spring-cloud starters are actually used; `HELP.md` mentions Eureka, but no Eureka code exists. Don't treat HELP.md as authoritative.
- POI is pinned to `4.1.2` (old) while Spring Boot 3.2.2 is current — don't bump POI without re-testing `Test.java`, the schema package paths changed in later POI versions.
- RocketMQ client is `5.0.8` (gRPC-based `rocketmq-client-java`, not the legacy 4.x `rocketmq-client`).

## Conventions in this repo

- Logging uses SLF4J directly (`LoggerFactory.getLogger(...)`); no Lombok.
- Code comments and log messages are predominantly Chinese — keep new comments consistent with surrounding code in whichever language is already used in that file.
- The top-level markdown files (`180-DAYS-OUTLINE.md`, `JD-Java-Interview-180Days-Plan.md`, `LEARNING-PLAN-STATUS.md`, `PDF-CONVERSION-GUIDE.md`) are personal study materials, not project docs — don't edit them as part of code tasks.
