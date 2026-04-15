import { useEffect, useMemo, useRef, useState } from "react";
import UploadMiniGame from "./UploadMiniGame";

const ALLOWED_ARCHIVE_TYPE = ".zip";
const API_BASE = "/api/v1/minio";
const POLL_INTERVAL_MS = 5000;

function MigrationUploadSection() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [downloadUrl, setDownloadUrl] = useState("");
  const [downloadName, setDownloadName] = useState("migrated-service.zip");
  const [migrationStatus, setMigrationStatus] = useState("");
  const abortControllerRef = useRef(null);
  const pollRef = useRef(null);
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
    if (isUploading) return;
    stopPolling();
    resetGeneratedFile();
    setSelectedFile(null);
    setSuccessMessage("");
    setErrorMessage("");
    setMigrationStatus("");
    userIdRef.current = null;
  };

  const handleStartMigration = async () => {
    if (!selectedFile || isUploading) return;

    resetGeneratedFile();

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

  useEffect(
    () => () => {
      if (downloadUrl) URL.revokeObjectURL(downloadUrl);
      abortControllerRef.current?.abort();
      stopPolling();
    },
    [downloadUrl]
  );

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
          {!isUploading && (
            <button
              className="action-button action-button--neutral"
              type="button"
              onClick={handleResetSelection}
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
            >
              Повторить
            </button>
          )}
          {!isUploading && !downloadUrl && !errorMessage && (
            <button
              className="action-button action-button--primary"
              type="button"
              onClick={handleStartMigration}
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
    </section>
  );
}

export default MigrationUploadSection;