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

RUN java -version && go version

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

RUN sed -i 's/\r$//' ./entrypoint.sh && \
    chmod +x ./entrypoint.sh

CMD ["/app/entrypoint.sh"]