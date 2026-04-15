.PHONY: help up down logs logs-server logs-minio logs-init build restart clean

# Вывод помощи
help:
	@echo "Доступные команды:"
	@echo "  make up              - Запустить MinIO, инициализацию и сервер"
	@echo "  make down            - Остановить и удалить все контейнеры"
	@echo "  make logs            - Показать логи сервера (FastAPI)"
	@echo "  make logs-minio      - Показать логи MinIO сервера"
	@echo "  make logs-init       - Показать логи инициализации MinIO"
	@echo "  make build           - Пересобрать образ сервера"
	@echo "  make restart         - Перезапустить сервер"
	@echo "  make clean           - Удалить контейнеры и тома (полная очистка)"

# Запуск всего (MinIO -> Init -> Server)
up:
	@echo "Запуск инфраструктуры..."
	@# Запускаем инициализацию в фоне, чтобы не блокировать, но она выполнится
	docker-compose up -d minio
	@echo "Ожидание готовности MinIO..."
	docker-compose up -d minio-init
	@echo "Запуск сервера..."
	docker-compose up -d server

# Остановка всех сервисов
down:
	@echo "Остановка всех сервисов..."
	docker-compose down

# Просмотр логов сервера (FastAPI)
logs:
	docker-compose logs -f server

# Просмотр логов MinIO сервера
logs-minio:
	docker-compose logs -f minio

# Просмотр логов инициализации (одноразовый скрипт)
logs-init:
	docker-compose logs -f minio-init

# Пересборка образа (если изменился код)
build:
	docker-compose build server

# Перезапуск сервера (сохраняя контейнеры MinIO)
restart:
	docker-compose restart server

# Полная очистка (удаление томов с данными MinIO!)
clean:
	@echo "Внимание! Удаляются все данные MinIO (volumes)!"
	docker-compose down -v
	docker-compose rm -f