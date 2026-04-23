.PHONY: help up down logs logs-server logs-minio logs-init build restart clean validation-build validation-up validation-up-java validation-test validation-down validation-logs validation-logs-java

VALIDATION_COMPOSE := docker compose -f validation/workflow-engine-parity-tests/validation-stack/docker-compose.yml

# Вывод помощи
help:
	@echo "Доступные команды:"
	@echo "  make up              - Запустить MinIO, инициализацию и сервер"
	@echo "  make down            - Остановить и удалить все контейнеры"
	@echo "  make logs            - Показать логи сервера (FastAPI)"
	@echo "  make logs-minio      - Показать логи MinIO сервера"
	@echo "  make logs-init       - Показать логи инициализации MinIO"
	@echo "  make logs-front      - Показать логи фронтенда"
	@echo "  make build           - Пересобрать образ сервера"
	@echo "  make restart         - Перезапустить сервер"
	@echo "  make clean           - Удалить контейнеры и тома (полная очистка)"
	@echo "  make validation-build - Собрать validation images"
	@echo "  make validation-up   - Поднять весь validation stack"
	@echo "  make validation-up-java - Поднять только reference workflow-engine-java"
	@echo "  make validation-test - Запустить parity tests в validation stack"
	@echo "  make validation-down - Остановить validation stack"
	@echo "  make validation-logs - Показать логи validation stack"
	@echo "  make validation-logs-java - Показать логи workflow-engine-java"

# Запуск всего (MinIO -> Init -> Server -> Frontend)
up:
	docker compose up --build -d

# Остановка всех сервисов
down:
	@echo "Остановка всех сервисов..."
	docker compose down

# Просмотр логов сервера (FastAPI)
logs:
	docker compose logs -f server

# Просмотр логов MinIO сервера
logs-minio:
	docker compose logs -f minio

# Просмотр логов инициализации (одноразовый скрипт)
logs-init:
	docker compose logs -f minio-init

# Frontend logs
logs-front:
	docker compose logs -f frontend

# Пересборка образа (если изменился код)
build:
	docker compose build server

# Перезапуск сервера (сохраняя контейнеры MinIO)
restart:
	docker compose restart server

# Полная очистка (удаление томов с данными MinIO!)
clean:
	@echo "Внимание! Удаляются все данные MinIO (volumes)!"
	docker compose down -v
	docker compose rm -f

# Собрать validation runtime
validation-build:
	$(VALIDATION_COMPOSE) build

# Поднять весь validation stack
validation-up:
	$(VALIDATION_COMPOSE) up --build

# Поднять только reference Java service и его зависимости
validation-up-java:
	$(VALIDATION_COMPOSE) up workflow-engine-java

# Запустить parity tests внутри validation stack
validation-test:
	$(VALIDATION_COMPOSE) up --build parity-tests

# Остановить validation stack
validation-down:
	$(VALIDATION_COMPOSE) down

# Логи всего validation stack
validation-logs:
	$(VALIDATION_COMPOSE) logs -f

# Логи reference workflow-engine-java
validation-logs-java:
	$(VALIDATION_COMPOSE) logs -f workflow-engine-java
