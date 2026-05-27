---
name: jasperai-runner
description: Clone, build, and run the JasperAI Spring Boot app. Use when a user wants to install, distribute, launch, compile, or bootstrap JasperAI with a DeepSeek or GigaChat API key and selected model.
metadata:
  short-description: Bootstrap and run JasperAI
---

# JasperAI Runner

Use this skill to make JasperAI available on a machine, compile it, and run it.

## Inputs

Required:

- Target directory where the app should exist.
- LLM provider: `deepseek` or `gigachat` only.
- API key for the selected provider.

Optional:

- Model name. Defaults:
  - `deepseek`: `deepseek-chat`
  - `gigachat`: `GigaChat-2-Max`
- Git repository URL. Defaults to `git@github.com:den4ik12/jasperAI.git`.
- Server port. Defaults to `8080`.

## Workflow

1. If the target directory does not exist, clone the repository into it.
2. If the target directory exists, verify it is a Git checkout with `mvnw`.
3. Compile with Maven Wrapper using `./mvnw clean package -Dmaven.test.skip=true`.
4. Run the app with Spring Boot using environment variables for secrets and model configuration.

Do not write API keys to `src/main/resources/application.properties`, shell history snippets, README files, or generated documentation. Prefer passing secrets through environment variables or the bundled script argument.

## Script

Use `scripts/bootstrap_jasperai.sh` from this skill directory whenever possible:

```bash
./scripts/bootstrap_jasperai.sh \
  --dir /path/to/JasperAI \
  --provider deepseek \
  --api-key "$DEEPSEEK_API_KEY" \
  --model deepseek-chat
```

For GigaChat:

```bash
./scripts/bootstrap_jasperai.sh \
  --dir /path/to/JasperAI \
  --provider gigachat \
  --api-key "$GIGACHAT_API_KEY" \
  --model GigaChat-2-Max
```

The script runs the app in the foreground. Stop it with Ctrl+C.

## Notes

- Java 17+ is required.
- The frontend editor is `editor.html` in the repository root after the backend starts.
- The backend default URL is `http://localhost:8080`.
- If dependency resolution fails because the network is unavailable, report that Maven needs network access.
