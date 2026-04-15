import { useEffect, useMemo, useRef, useState } from "react";
import UploadMiniGame from "./UploadMiniGame";

const ALLOWED_ARCHIVE_TYPE = ".zip";
const SIMULATED_DELAY_MS = 90_000;

const buildAbortError = () =>
  new DOMException("The operation was aborted.", "AbortError");

const simulateMigrationRequest = (file, signal) =>
  new Promise((resolve, reject) => {
    let isSettled = false;

    const cleanup = () => {
      signal.removeEventListener("abort", handleAbort);
      clearTimeout(timeoutId);
    };

    const handleAbort = () => {
      if (isSettled) {
        return;
      }
      isSettled = true;
      cleanup();
      reject(buildAbortError());
    };

    if (signal.aborted) {
      reject(buildAbortError());
      return;
    }

    signal.addEventListener("abort", handleAbort, { once: true });

    const timeoutId = setTimeout(() => {
      if (isSettled) {
        return;
      }

      isSettled = true;
      cleanup();
      resolve({
        blob: file,
        fileName: `migrated-${file.name}`,
      });
    }, SIMULATED_DELAY_MS);
  });

function MigrationUploadSection() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [downloadUrl, setDownloadUrl] = useState("");
  const [downloadName, setDownloadName] = useState("migrated-service.zip");
  const abortControllerRef = useRef(null);

  const selectedFileSize = useMemo(() => {
    if (!selectedFile) {
      return "";
    }

    const sizeInMb = selectedFile.size / (1024 * 1024);
    return `${sizeInMb.toFixed(2)} MB`;
  }, [selectedFile]);

  const resetGeneratedFile = () => {
    if (!downloadUrl) {
      return;
    }

    URL.revokeObjectURL(downloadUrl);
    setDownloadUrl("");
    setDownloadName("migrated-service.zip");
  };

  const handleZipValidation = (file) => {
    if (!file) {
      return;
    }

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
    if (isUploading) {
      return;
    }

    resetGeneratedFile();
    setSelectedFile(null);
    setSuccessMessage("");
    setErrorMessage("");
  };

  const handleStartMigration = async () => {
    if (!selectedFile || isUploading) {
      return;
    }

    resetGeneratedFile();

    const controller = new AbortController();
    abortControllerRef.current = controller;

    setErrorMessage("");
    setSuccessMessage("");
    setIsUploading(true);

    try {
      const result = await simulateMigrationRequest(selectedFile, controller.signal);
      const nextDownloadUrl = URL.createObjectURL(result.blob);

      setDownloadName(result.fileName);
      setDownloadUrl(nextDownloadUrl);
      setSuccessMessage(
        "Миграция завершена успешно. Архив готов к скачиванию.",
      );
    } catch (error) {
      if (error.name === "AbortError") {
        setErrorMessage("Запрос остановлен пользователем.");
      } else {
        setErrorMessage("Что-то пошло не так. Попробуйте еще раз.");
      }
    } finally {
      setIsUploading(false);
      abortControllerRef.current = null;
    }
  };

  const handleStopMigration = () => {
    abortControllerRef.current?.abort();
  };

  useEffect(
    () => () => {
      if (downloadUrl) {
        URL.revokeObjectURL(downloadUrl);
      }
      abortControllerRef.current?.abort();
    },
    [downloadUrl],
  );

  return (
    <section className="upload-wrapper">
      <h2>Загрузка ZIP-архива</h2>
      <p className="upload-wrapper__hint">
        Загрузите архив с Java-микросервисом для дальнейшей обработки.
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
              Остановить запрос
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
