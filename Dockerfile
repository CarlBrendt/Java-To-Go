FROM python:3.13-slim

WORKDIR /app

# ── Системные зависимости + Java + Go ──
RUN apt-get update --fix-missing && \
    apt-get install -y --no-install-recommends --fix-missing \
    gcc \
    libc6-dev \
    g++ \
    libssl-dev \
    tzdata \
    libicu-dev \
    curl \
    git \
    wget \
    default-jdk-headless \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ── Go 1.22 ──
ENV GO_VERSION=1.22.5
RUN curl -fsSL https://go.dev/dl/go${GO_VERSION}.linux-arm64.tar.gz \
    | tar -C /usr/local -xzf -
ENV PATH="/usr/local/go/bin:${PATH}"
ENV GOPATH="/root/go"
ENV PATH="${GOPATH}/bin:${PATH}"

# ── golangci-lint (установка через официальный скрипт) ──
ENV GOLANGCI_LINT_VERSION=v1.59.1
RUN curl -sSfL https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh \
    | sh -s -- -b /usr/local/bin ${GOLANGCI_LINT_VERSION}

# ── goimports (дополнительный инструмент) ──
RUN go install golang.org/x/tools/cmd/goimports@latest

# ── gofumpt (более строгое форматирование) ──
RUN go install mvdan.cc/gofumpt@latest

# Проверяем установку
RUN java -version && \
    go version && \
    golangci-lint --version && \
    goimports -h 2>&1 | head -1 && \
    gofumpt -h 2>&1 | head -1

# ── Python зависимости ──
COPY pyproject.toml .
RUN pip install --no-cache-dir uv && \
    uv pip install --system .

# ── JavaParser tool ──
COPY java-tools ./java-tools

# ── Приложение ──
COPY main.py .
COPY src ./src
COPY entrypoint.sh .
COPY .golangci.yml .

RUN sed -i 's/\r$//' ./entrypoint.sh && \
    chmod +x ./entrypoint.sh

# Создаём директорию для кэша линтера
RUN mkdir -p /tmp/golangci-lint-cache

CMD ["/app/entrypoint.sh"]