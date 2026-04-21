import { useEffect, useMemo, useRef, useState } from "react";
import UploadMiniGame from "./UploadMiniGame";

const ALLOWED_ARCHIVE_TYPE = ".zip";
const API_BASE = "/api/v1/minio";
const VALIDATION_API_BASE = "/api/v1/validation";
const POLL_INTERVAL_MS = 5000;
const VALIDATION_STAGE_LABELS = {
  queued: "В очереди",
  preparing_reference_source: "Подготовка Java reference",
  starting_migration: "Запуск миграции reference-сервиса",
  waiting_for_go_artifact: "Ожидание сгенерированного Go-артефакта",
  starting_reference_runtime: "Запуск Java reference",
  downloading_go_artifact: "Загрузка Go-артефакта",
  preparing_go_source: "Подготовка Go-кода",
  building_go_runtime: "Сборка Go-сервиса",
  starting_go_runtime: "Запуск Go-сервиса",
  running_parity_tests: "Запуск parity-тестов",
  finished: "Проверка завершена",
  failed: "Проверка завершилась с ошибкой",
};
function MigrationUploadSection() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [downloadUrl, setDownloadUrl] = useState("");
  const [downloadName, setDownloadName] = useState("migrated-service.zip");
  const [migrationStatus, setMigrationStatus] = useState("");
  const [validationRunId, setValidationRunId] = useState("");
  const [validationStatus, setValidationStatus] = useState(null);
  const [isValidationStarting, setIsValidationStarting] = useState(false);
  const abortControllerRef = useRef(null);
  const pollRef = useRef(null);
  const validationPollRef = useRef(null);
  const userIdRef = useRef(null);

  const selectedFileSize = useMemo(() => {
    if (!selectedFile) return "";
    const sizeInMb = selectedFile.size / (1024 * 1024);
    return `${sizeInMb.toFixed(2)} MB`;
  }, [selectedFile]);

  // Генерируем user_id один раз
  const getUserId = () => {
    if (!userIdRef.current) {
      userIdRef.current = `user_${Date.now()}`;
    }
    return userIdRef.current;
  };

  const resetGeneratedFile = () => {
    if (downloadUrl) {
      URL.revokeObjectURL(downloadUrl);
      setDownloadUrl("");
      setDownloadName("migrated-service.zip");
    }
  };

  const stopPolling = () => {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  };

  const stopValidationPolling = () => {
    if (validationPollRef.current) {
      clearInterval(validationPollRef.current);
      validationPollRef.current = null;
    }
  };

  const resetValidationState = () => {
    stopValidationPolling();
    setValidationRunId("");
    setValidationStatus(null);
    setIsValidationStarting(false);
  };

  // Поллинг статуса миграции
  const startPolling = (userId) => {
    stopPolling();

    pollRef.current = setInterval(async () => {
      try {
        const res = await fetch(
          `${API_BASE}/migrate/status?user_id=${userId}`
        );
        const data = await res.json();

        if (data.status === "completed") {
          stopPolling();
          setMigrationStatus("Миграция завершена. Скачиваем результат...");

          // Скачиваем ZIP
          const zipRes = await fetch(
            `${API_BASE}/minio-download-ready-zip?user_id=${userId}`
          );

          if (!zipRes.ok) {
            throw new Error(`Download failed: ${zipRes.status}`);
          }

          const blob = await zipRes.blob();
          const url = URL.createObjectURL(blob);

          setDownloadUrl(url);
          setDownloadName(`${userId}.zip`);
          setSuccessMessage("Миграция завершена успешно. Архив готов к скачиванию.");
          setMigrationStatus("");
          setIsUploading(false);
        } else if (data.status === "error") {
          stopPolling();
          setErrorMessage(data.message || "Ошибка миграции.");
          setMigrationStatus("");
          setIsUploading(false);
        } else {
          setMigrationStatus(
            `Статус: ${data.status}. Файлов: ${data.files_count || "..."}`
          );
        }
      } catch (err) {
        console.error("Poll error:", err);
      }
    }, POLL_INTERVAL_MS);
  };

  const handleZipValidation = (file) => {
    if (validationInProgress || isValidationStarting) return;
    if (!file) return;

    const isZipByMime = file.type === "application/zip";
    const isZipByName = file.name.toLowerCase().endsWith(ALLOWED_ARCHIVE_TYPE);

    if (!isZipByMime && !isZipByName) {
      setSelectedFile(null);
      setSuccessMessage("");
      setErrorMessage("Допустим только ZIP-архив (.zip).");
      return;
    }

    resetGeneratedFile();
    resetValidationState();
    setSelectedFile(file);
    setErrorMessage("");
    setSuccessMessage("");
    setMigrationStatus("");
    // Новый user_id для каждого файла
    userIdRef.current = null;
  };

  const handleInputChange = (event) => {
    const [file] = event.target.files;
    handleZipValidation(file);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragActive(false);
    const [file] = event.dataTransfer.files;
    handleZipValidation(file);
  };

  const handleResetSelection = () => {
    if (isUploading || validationInProgress || isValidationStarting) return;
    stopPolling();
    resetValidationState();
    resetGeneratedFile();
    setSelectedFile(null);
    setSuccessMessage("");
    setErrorMessage("");
    setMigrationStatus("");
    userIdRef.current = null;
  };

  const handleStartMigration = async () => {
    if (!selectedFile || isUploading || validationInProgress || isValidationStarting) return;

    resetGeneratedFile();
    resetValidationState();

    const controller = new AbortController();
    abortControllerRef.current = controller;
    const userId = getUserId();

    setErrorMessage("");
    setSuccessMessage("");
    setMigrationStatus("Загружаем файл...");
    setIsUploading(true);

    try {
      // 1. Загружаем ZIP с auto_migrate=true
      const formData = new FormData();
      formData.append("file", selectedFile);

      const uploadRes = await fetch(
        `${API_BASE}/minio-upload-zip?user_id=${userId}&auto_migrate=true`,
        {
          method: "POST",
          body: formData,
          signal: controller.signal,
        }
      );

      if (!uploadRes.ok) {
        const errData = await uploadRes.json().catch(() => ({}));
        throw new Error(errData.message || `Upload failed: ${uploadRes.status}`);
      }

      const uploadData = await uploadRes.json();

      if (uploadData.status === "error") {
        throw new Error(uploadData.message);
      }

      setMigrationStatus("Файл загружен. Миграция запущена...");

      // 2. Начинаем поллинг статуса
      startPolling(userId);

    } catch (error) {
      if (error.name === "AbortError") {
        setErrorMessage("Запрос остановлен пользователем.");
      } else {
        setErrorMessage(error.message || "Что-то пошло не так. Попробуйте еще раз.");
      }
      setMigrationStatus("");
      setIsUploading(false);
    }
  };

  const handleStopMigration = () => {
    abortControllerRef.current?.abort();
    stopPolling();
    resetValidationState();
    setIsUploading(false);
    setMigrationStatus("");

    // Удаляем файлы пользователя
    const userId = userIdRef.current;
    if (userId) {
      fetch(`${API_BASE}/minio-delete-user?user_id=${userId}`, {
        method: "DELETE",
      }).catch(() => {});
    }
  };

  const pollValidationRun = async (runId) => {
    const response = await fetch(`${VALIDATION_API_BASE}/runs/${runId}`);
    if (!response.ok) {
      throw new Error(`Validation status failed: ${response.status}`);
    }

    const data = await response.json();
    setValidationStatus(data);

    if (data.status !== "queued" && data.status !== "running") {
      stopValidationPolling();
    }
  };

  const startValidationPolling = (runId) => {
    stopValidationPolling();
    validationPollRef.current = setInterval(() => {
      pollValidationRun(runId).catch((error) => {
        stopValidationPolling();
        setValidationStatus({
          status: "failed",
          stage: "failed",
          summary: error.message || "Не удалось получить статус проверки.",
          parity_percent: null,
          tests_total: 0,
          tests_passed: 0,
          tests_failed: 0,
        });
      });
    }, POLL_INTERVAL_MS);
  };

  const handleStartValidation = async () => {
    if (isValidationStarting || validationInProgress) return;

    resetValidationState();
    setIsValidationStarting(true);

    try {
      const response = await fetch(`${VALIDATION_API_BASE}/runs`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({}),
      });

      if (!response.ok) {
        throw new Error(`Validation start failed: ${response.status}`);
      }

      const createdRun = await response.json();
      setValidationRunId(createdRun.validation_run_id);
      await pollValidationRun(createdRun.validation_run_id);
      startValidationPolling(createdRun.validation_run_id);
    } catch (error) {
      setValidationStatus({
        status: "failed",
        stage: "failed",
        summary: error.message || "Не удалось запустить проверку.",
        parity_percent: null,
        tests_total: 0,
        tests_passed: 0,
        tests_failed: 0,
      });
    } finally {
      setIsValidationStarting(false);
    }
  };

  useEffect(
    () => () => {
      if (downloadUrl) URL.revokeObjectURL(downloadUrl);
      abortControllerRef.current?.abort();
      stopPolling();
      stopValidationPolling();
    },
    [downloadUrl]
  );

  const validationStageLabel = validationStatus?.stage
    ? VALIDATION_STAGE_LABELS[validationStatus.stage] ?? validationStatus.stage
    : "";
  const validationPercent = validationStatus?.parity_percent;
  const migrationInProgress = isUploading;
  const validationInProgress =
    validationStatus?.status === "queued" || validationStatus?.status === "running";
  const canStartValidation = !isValidationStarting && !validationInProgress;
  const canStartMigration = !validationInProgress && !isValidationStarting;
  const canEditArchive = !migrationInProgress && !validationInProgress && !isValidationStarting;

  return (
    <section className="upload-wrapper">
      <h2>Загрузка ZIP-архива</h2>
      <p className="upload-wrapper__hint">
        Загрузите архив с Java-микросервисом для автоматической миграции в Go.
      </p>

      {!selectedFile && (
        <label
          className={`dropzone ${isDragActive ? "dropzone--active" : ""}`}
          onDragOver={(event) => {
            if (!canEditArchive) return;
            event.preventDefault();
            setIsDragActive(true);
          }}
          onDragLeave={() => setIsDragActive(false)}
          onDrop={handleDrop}
        >
          <input
            type="file"
            accept={ALLOWED_ARCHIVE_TYPE}
            onChange={handleInputChange}
            disabled={!canEditArchive}
          />
          <span className="dropzone__icon" aria-hidden="true">
            &#8682;
          </span>
          <span className="dropzone__title">Перетащите ZIP сюда</span>
          <span className="dropzone__subtitle">или выберите файл с устройства</span>
          <span className="dropzone__button">Выбрать архив</span>
        </label>
      )}

      {selectedFile && (
        <div className="uploaded-card">
          <p className="upload-result upload-result--success">
            Файл: {selectedFile.name} ({selectedFileSize})
          </p>
          {!migrationInProgress && (
            <button
              className="action-button action-button--neutral"
              type="button"
              onClick={handleResetSelection}
              disabled={!canEditArchive}
            >
              Удалить
            </button>
          )}
        </div>
      )}

      {selectedFile && (
        <div className="actions">
          {isUploading && (
            <button
              className="action-button action-button--danger"
              type="button"
              onClick={handleStopMigration}
            >
              Остановить миграцию
            </button>
          )}
          {!isUploading && downloadUrl && (
            <a
              className="action-button action-button--download"
              href={downloadUrl}
              download={downloadName}
            >
              Скачать результат
            </a>
          )}
          {!isUploading && !downloadUrl && !!errorMessage && (
            <button
              className="action-button action-button--primary"
              type="button"
              onClick={handleStartMigration}
              disabled={!canStartMigration}
            >
              Повторить
            </button>
          )}
          {!isUploading && !downloadUrl && !errorMessage && (
            <button
              className="action-button action-button--primary"
              type="button"
              onClick={handleStartMigration}
              disabled={!canStartMigration}
            >
              Запустить миграцию
            </button>
          )}
        </div>
      )}

      {isUploading && <UploadMiniGame />}

      {migrationStatus && (
        <p className="upload-result upload-result--info">
          🔄 {migrationStatus}
        </p>
      )}
      {successMessage && (
        <p className="upload-result upload-result--success">{successMessage}</p>
      )}
      {errorMessage && (
        <p className="upload-result upload-result--error">{errorMessage}</p>
      )}

      <div className="validation-card">
        <div className="validation-card__header">
          <div>
            <h3>Проверка</h3>
            <p>
              Берет Java reference из lowcode, прогоняет миграцию через основной backend и показывает общую сходимость.
            </p>
          </div>
          <button
            className="action-button action-button--primary"
            type="button"
            onClick={handleStartValidation}
            disabled={!canStartValidation}
          >
            {isValidationStarting || validationInProgress ? "Проверка идет" : "Запустить"}
          </button>
        </div>

        {validationInProgress && (
          <p className="validation-card__empty">
            Во время проверки ручная миграция и изменение архива временно заблокированы.
          </p>
        )}

        {!validationStatus && (
          <p className="validation-card__empty">
            Нажмите «Запустить», чтобы orchestrator сам подготовил reference-проект, сгенерировал Go-кандидат и начал проверку.
          </p>
        )}

        {validationStatus && (
          <div className="validation-card__grid">
            <div className="validation-metric">
              <span className="validation-metric__label">Статус</span>
              <strong>{validationStatus.status}</strong>
            </div>
            <div className="validation-metric">
              <span className="validation-metric__label">Стадия</span>
              <strong>{validationStageLabel || "—"}</strong>
            </div>
            <div className="validation-metric">
              <span className="validation-metric__label">Прохождение</span>
              <strong>{validationPercent == null ? "—" : `${validationPercent}%`}</strong>
            </div>
            <div className="validation-metric">
              <span className="validation-metric__label">Тесты</span>
              <strong>
                {validationStatus.tests_passed}/{validationStatus.tests_total}
              </strong>
            </div>
          </div>
        )}

        {validationRunId && (
          <p className="validation-card__run-id">
            Run ID: <code>{validationRunId}</code>
          </p>
        )}

        {validationStatus?.summary && (
          <p
            className={`upload-result ${
              validationStatus.status === "failed"
                ? "upload-result--error"
                : validationStatus.status === "finished"
                  ? "upload-result--success"
                  : "upload-result--info"
            }`}
          >
            {validationStatus.summary}
          </p>
        )}
      </div>
    </section>
  );
}

export default MigrationUploadSection;
