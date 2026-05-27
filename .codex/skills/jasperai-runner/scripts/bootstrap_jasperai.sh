#!/usr/bin/env bash
set -euo pipefail

DEFAULT_REPO_URL="git@github.com:den4ik12/jasperAI.git"
DEFAULT_PORT="8080"

repo_url="$DEFAULT_REPO_URL"
app_dir=""
provider=""
api_key=""
model=""
port="$DEFAULT_PORT"
run_app="true"
build_app="true"
gigachat_scope="GIGACHAT_API_CORP"
gigachat_unsafe_ssl="true"

usage() {
  cat <<'USAGE'
Bootstrap JasperAI: clone if needed, build, and run.

Required:
  --dir PATH                 Target app directory
  --provider deepseek|gigachat
  --api-key KEY              API key for the selected provider

Optional:
  --model NAME               Defaults to deepseek-chat or GigaChat-2-Max
  --repo-url URL             Defaults to git@github.com:den4ik12/jasperAI.git
  --port PORT                Defaults to 8080
  --no-run                   Clone/build only
  --skip-build               Run without compiling first
  --gigachat-scope SCOPE     Defaults to GIGACHAT_API_CORP
  --gigachat-unsafe-ssl BOOL Defaults to true
  -h, --help

Examples:
  bootstrap_jasperai.sh --dir ~/apps/JasperAI --provider deepseek --api-key "$DEEPSEEK_API_KEY"
  bootstrap_jasperai.sh --dir ~/apps/JasperAI --provider gigachat --api-key "$GIGACHAT_API_KEY" --model GigaChat-2-Max
USAGE
}

die() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dir)
      app_dir="${2:-}"
      shift 2
      ;;
    --provider)
      provider="${2:-}"
      shift 2
      ;;
    --api-key)
      api_key="${2:-}"
      shift 2
      ;;
    --model)
      model="${2:-}"
      shift 2
      ;;
    --repo-url)
      repo_url="${2:-}"
      shift 2
      ;;
    --port)
      port="${2:-}"
      shift 2
      ;;
    --no-run)
      run_app="false"
      shift
      ;;
    --skip-build)
      build_app="false"
      shift
      ;;
    --gigachat-scope)
      gigachat_scope="${2:-}"
      shift 2
      ;;
    --gigachat-unsafe-ssl)
      gigachat_unsafe_ssl="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "unknown argument: $1"
      ;;
  esac
done

[[ -n "$app_dir" ]] || die "--dir is required"
[[ -n "$provider" ]] || die "--provider is required"
[[ -n "$api_key" ]] || die "--api-key is required"

case "$provider" in
  deepseek)
    model="${model:-deepseek-chat}"
    ;;
  gigachat)
    model="${model:-GigaChat-2-Max}"
    ;;
  *)
    die "--provider must be either deepseek or gigachat"
    ;;
esac

if ! command -v git >/dev/null 2>&1; then
  die "git is required"
fi

if [[ ! -d "$app_dir" ]]; then
  parent_dir="$(dirname "$app_dir")"
  mkdir -p "$parent_dir"
  git clone "$repo_url" "$app_dir"
fi

[[ -d "$app_dir/.git" ]] || die "$app_dir exists but is not a Git checkout"
[[ -x "$app_dir/mvnw" ]] || die "$app_dir/mvnw is missing or not executable"

if ! command -v java >/dev/null 2>&1; then
  die "Java 17+ is required"
fi

java_version="$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
printf 'Using Java %s\n' "${java_version:-unknown}"

if [[ "$build_app" == "true" ]]; then
  (
    cd "$app_dir"
    ./mvnw clean package -Dmaven.test.skip=true
  )
fi

if [[ "$run_app" != "true" ]]; then
  printf 'JasperAI is ready in %s\n' "$app_dir"
  exit 0
fi

printf 'Starting JasperAI on http://localhost:%s with provider %s and model %s\n' "$port" "$provider" "$model"

if [[ "$provider" == "deepseek" ]]; then
  (
    cd "$app_dir"
    SPRING_AI_MODEL_CHAT="deepseek" \
    SPRING_AI_DEEPSEEK_API_KEY="$api_key" \
    SPRING_AI_DEEPSEEK_CHAT_OPTIONS_MODEL="$model" \
    SERVER_PORT="$port" \
    ./mvnw spring-boot:run
  )
else
  (
    cd "$app_dir"
    SPRING_AI_MODEL_CHAT="gigachat" \
    SPRING_AI_GIGACHAT_AUTH_BEARER_API_KEY="$api_key" \
    SPRING_AI_GIGACHAT_AUTH_SCOPE="$gigachat_scope" \
    SPRING_AI_GIGACHAT_AUTH_UNSAFE_SSL="$gigachat_unsafe_ssl" \
    SPRING_AI_GIGACHAT_CHAT_OPTIONS_MODEL="$model" \
    SERVER_PORT="$port" \
    ./mvnw spring-boot:run
  )
fi
