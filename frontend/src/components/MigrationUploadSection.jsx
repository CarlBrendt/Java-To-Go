import { useEffect, useMemo, useRef, useState } from "react";

const ALLOWED_ARCHIVE_TYPE = ".zip";
const API_BASE = "/api/v1/minio";
const POLL_INTERVAL_MS = 5000;
const DEFAULT_LLM_MODEL = "mws-gpt-alpha";

const LLM_OPTIONS = [
  {
    value: "mws-gpt-alpha",
    label: "mws-gpt-alpha",
  },
  { value: "mws-cotype-pro-vl-32b", label: "mws-cotype-pro-vl-32b" },
  { value: "mws-cotype-pro2.5", label: "mws-cotype-pro2.5" },
  { value: "mws-gpt-oss-120b", label: "mws-gpt-oss-120b" },
  { value: "mws-gpt-oss-20b", label: "mws-gpt-oss-20b" },
  {
    value: "mws-qwen3.5-35b-a3b",
    label: "mws-qwen3.5-35b-a3b",
  },
];

const PIPELINE_STEPS = [
  { id: "upload", title: "Загрузка", subtitle: "Отправляем ZIP в хранилище" },
  { id: "queued", title: "Постановка", subtitle: "Создаем задачу миграции" },
  { id: "migrating", title: "Конвертация", subtitle: "Java -> Go и проверка" },
  { id: "packaging", title: "Упаковка", subtitle: "Собираем итоговый архив" },
  {
    id: "completed",
    title: "Готово",
    subtitle: "Результат доступен для скачивания",
  },
];
const ORBIT_STEPS = PIPELINE_STEPS.filter((step) => step.id !== "completed");
const ORBIT_NODE_ANGLES = [180, 230, 310, 0];
const ORBIT_NODE_RADIUS_PERCENT = 47.8;

const PIPELINE_STEP_INDEX = PIPELINE_STEPS.reduce((acc, step, index) => {
  acc[step.id] = index;
  return acc;
}, {});

const PHASE_TO_PIPELINE_STEP = {
  idle: "queued",
  accepted: "queued",
  queued: "queued",
  pending: "queued",
  preparing: "queued",
  starting: "queued",
  parsing: "migrating",
  in_progress: "migrating",
  processing: "migrating",
  running: "migrating",
  migrating: "migrating",
  converting: "migrating",
  packaging: "packaging",
  zipping: "packaging",
  uploading: "packaging",
  uploading_ready: "packaging",
  finalizing: "packaging",
  completed: "completed",
  done: "completed",
  success: "completed",
};

const COMPLETE_STATUSES = new Set(["completed", "done", "success"]);
const FAILED_STATUSES = new Set(["error", "failed"]);

function resolvePipelineStepFromPhase(phase) {
  if (!phase) return "migrating";
  return PHASE_TO_PIPELINE_STEP[phase] ?? "migrating";
}

function resolvePipelineStepFromStatus(status) {
  if (!status) return "migrating";
  if (status === "accepted" || status === "awaiting_migration") return "queued";
  if (status === "not_found") return "queued";
  if (status === "in_progress") return "migrating";
  return resolvePipelineStepFromPhase(status);
}

function resolveDownloadEndpoint(rawUrl, userId) {
  const fallback = `${API_BASE}/minio-download-ready-zip?user_id=${userId}`;
  if (!rawUrl || typeof rawUrl !== "string") return fallback;
  if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://"))
    return rawUrl;
  if (rawUrl.startsWith("/")) return rawUrl;
  return `/${rawUrl}`;
}

function extractFilename(response, fallbackName) {
  const contentDisposition = response.headers.get("content-disposition") || "";
  const utf8 = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (utf8) return decodeURIComponent(utf8).replace(/[/\\]/g, "_");
  const plain = contentDisposition.match(/filename="?([^"]+)"?/i)?.[1];
  if (plain) return plain.replace(/[/\\]/g, "_");
  return fallbackName;
}

function normalizeStatusPayload(data) {
  const status = String(data?.status || "").toLowerCase();
  const phase = String(data?.migration?.phase || "").toLowerCase();
  const counts = data?.counts || {};
  const filesCount = Number(data?.files_count ?? 0);
  const readyOutputZips = Number(counts?.ready_output_zips ?? 0);
  const readyBucketObjects = Number(counts?.ready_bucket_objects ?? 0);
  const hasReadyZip =
    Boolean(data?.has_ready_zip) ||
    Boolean(data?.has_zip) ||
    Boolean(data?.download_url) ||
    readyOutputZips > 0 ||
    readyBucketObjects > 0;
  const isFailed = FAILED_STATUSES.has(status) || FAILED_STATUSES.has(phase);
  const isCompleted =
    COMPLETE_STATUSES.has(status) ||
    COMPLETE_STATUSES.has(phase) ||
    hasReadyZip;
  const inferredStep = phase
    ? resolvePipelineStepFromPhase(phase)
    : resolvePipelineStepFromStatus(status);
  const pipelineStep = isCompleted ? "completed" : inferredStep;
  return {
    status,
    phase,
    message: data?.message || "",
    downloadUrl: data?.download_url || "",
    filesCount,
    hasReadyZip,
    isFailed,
    isCompleted,
    pipelineStep,
  };
}

function MigrationUploadSection() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [, setErrorMessage] = useState("");
  const [downloadUrl, setDownloadUrl] = useState("");
  const [downloadName, setDownloadName] = useState("migrated-service.zip");
  const [, setMigrationStatus] = useState("");
  const [pipelineStep, setPipelineStep] = useState("idle");
  const [failedStep, setFailedStep] = useState("");
  const [selectedModel, setSelectedModel] = useState(DEFAULT_LLM_MODEL);
  const [isModelMenuOpen, setIsModelMenuOpen] = useState(false);
  const abortControllerRef = useRef(null);
  const pollRef = useRef(null);
  const userIdRef = useRef(null);
  const modelMenuRef = useRef(null);

  const selectedFileSize = useMemo(() => {
    if (!selectedFile) return "";
    const sizeInMb = selectedFile.size / (1024 * 1024);
    return `${sizeInMb.toFixed(2)} MB`;
  }, [selectedFile]);

  const activePipelineStepId =
    pipelineStep === "failed" ? failedStep : pipelineStep;
  const activeStepData =
    PIPELINE_STEPS.find((step) => step.id === activePipelineStepId) ||
    PIPELINE_STEPS[0];
  const selectedModelOption =
    LLM_OPTIONS.find((option) => option.value === selectedModel) ||
    LLM_OPTIONS[0];
  const orbitStatusLabel =
    pipelineStep === "failed"
      ? "ОШИБКА"
      : pipelineStep === "completed"
        ? "ГОТОВО К СКАЧИВАНИЮ"
        : pipelineStep === "idle"
          ? "ОЖИДАНИЕ ЗАПУСКА"
          : "ВЫПОЛНЕНИЕ";

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

  const setPipelineState = (step) => {
    setFailedStep("");
    setPipelineStep(step);
  };

  const handleDownloadResult = () => {
    if (!downloadUrl) return;
    const anchor = document.createElement("a");
    anchor.href = downloadUrl;
    anchor.download = downloadName;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
  };

  const markPipelineAsFailed = (fallbackStep = "upload") => {
    const stepToMark = pipelineStep === "idle" ? fallbackStep : pipelineStep;
    setFailedStep(stepToMark);
    setPipelineStep("failed");
  };

  const getStepStatus = (stepId) => {
    const currentStepId = pipelineStep === "failed" ? failedStep : pipelineStep;
    const currentIndex = PIPELINE_STEP_INDEX[currentStepId] ?? -1;
    const stepIndex = PIPELINE_STEP_INDEX[stepId];

    if (pipelineStep === "failed" && stepId === failedStep) return "error";
    if (pipelineStep === "completed" || stepIndex < currentIndex) return "done";
    if (stepIndex === currentIndex) return "active";
    return "pending";
  };

  const startPolling = (userId) => {
    stopPolling();

    pollRef.current = setInterval(async () => {
      try {
        const res = await fetch(`${API_BASE}/migrate/status?user_id=${userId}`);
        const data = await res.json();
        const normalized = normalizeStatusPayload(data);

        if (normalized.isCompleted) {
          stopPolling();
          setPipelineState("packaging");

          const downloadEndpoint = resolveDownloadEndpoint(
            normalized.downloadUrl,
            userId,
          );
          const zipRes = await fetch(downloadEndpoint);

          if (!zipRes.ok) {
            throw new Error(`Download failed: ${zipRes.status}`);
          }

          const blob = await zipRes.blob();
          const url = URL.createObjectURL(blob);

          setDownloadUrl(url);
          setDownloadName(extractFilename(zipRes, `${userId}.zip`));
          setPipelineState("completed");
          setIsUploading(false);
        } else if (normalized.isFailed) {
          stopPolling();
          setErrorMessage(normalized.message || "Ошибка миграции.");
          markPipelineAsFailed("migrating");
          setIsUploading(false);
        } else {
          const phaseInfo = normalized.phase
            ? ` (phase: ${normalized.phase})`
            : "";
          const filesInfo =
            normalized.filesCount > 0
              ? ` Файлов: ${normalized.filesCount}.`
              : "";
          setPipelineState(normalized.pipelineStep);
          setMigrationStatus(
            normalized.message
              ? `${normalized.message}${phaseInfo}${filesInfo}`
              : `Статус: ${normalized.status || "in_progress"}${phaseInfo}.${filesInfo}`,
          );
        }
      } catch (err) {
        console.error("Poll error:", err);
        stopPolling();
        setErrorMessage(
          "Не удалось получить статус миграции. Попробуйте снова.",
        );
        setMigrationStatus("");
        markPipelineAsFailed("migrating");
        setIsUploading(false);
      }
    }, POLL_INTERVAL_MS);
  };

  const handleZipValidation = (file) => {
    if (!file) return;

    const isZipByMime = file.type === "application/zip";
    const isZipByName = file.name.toLowerCase().endsWith(ALLOWED_ARCHIVE_TYPE);

    if (!isZipByMime && !isZipByName) {
      setSelectedFile(null);
      setErrorMessage("Допустим только ZIP-архив (.zip).");
      return;
    }

    resetGeneratedFile();
    setSelectedFile(file);
    setErrorMessage("");
    setMigrationStatus("");
    setPipelineStep("idle");
    setFailedStep("");
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
    setErrorMessage("");
    setMigrationStatus("");
    setPipelineStep("idle");
    setFailedStep("");
    userIdRef.current = null;
  };

  const handleRestartView = () => {
    if (isUploading) return;
    stopPolling();
    abortControllerRef.current?.abort();
    if (userIdRef.current) {
      fetch(`${API_BASE}/minio-delete-user?user_id=${userIdRef.current}`, {
        method: "DELETE",
      }).catch(() => {});
      userIdRef.current = null;
    }
    resetGeneratedFile();
    setErrorMessage("");
    setMigrationStatus("");
    setPipelineStep("idle");
    setFailedStep("");
    setIsModelMenuOpen(false);
  };

  const handleStartMigration = async ({ isRetry = false } = {}) => {
    if (!selectedFile || isUploading) return;

    const previousUserId = userIdRef.current;
    if (isRetry && previousUserId) {
      fetch(`${API_BASE}/minio-delete-user?user_id=${previousUserId}`, {
        method: "DELETE",
      }).catch(() => {});
      userIdRef.current = null;
    }

    resetGeneratedFile();

    const controller = new AbortController();
    abortControllerRef.current = controller;
    const userId = getUserId();

    setErrorMessage("");
    setMigrationStatus("Загружаем файл...");
    setPipelineState("upload");
    setIsUploading(true);

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);
      const params = new URLSearchParams({
        user_id: userId,
        auto_migrate: "true",
        llm_model: selectedModel,
      });

      const uploadRes = await fetch(
        `${API_BASE}/minio-upload-zip?${params.toString()}`,
        {
          method: "POST",
          body: formData,
          signal: controller.signal,
        },
      );

      if (!uploadRes.ok) {
        const errData = await uploadRes.json().catch(() => ({}));
        throw new Error(
          errData.message || `Upload failed: ${uploadRes.status}`,
        );
      }

      const uploadData = await uploadRes.json();

      if (uploadData.status === "error") {
        throw new Error(uploadData.message);
      }

      setPipelineState("queued");
      setMigrationStatus("Файл загружен. Миграция запущена...");

      startPolling(userId);
    } catch (error) {
      if (error.name === "AbortError") {
        setErrorMessage("Запрос остановлен пользователем.");
      } else {
        setErrorMessage(
          error.message || "Что-то пошло не так. Попробуйте еще раз.",
        );
      }
      setMigrationStatus("");
      markPipelineAsFailed("upload");
      setIsUploading(false);
    }
  };

  useEffect(
    () => () => {
      if (downloadUrl) URL.revokeObjectURL(downloadUrl);
      abortControllerRef.current?.abort();
      stopPolling();
    },
    [downloadUrl],
  );

  useEffect(() => {
    if (!isModelMenuOpen) return undefined;

    const handlePointerDown = (event) => {
      if (!modelMenuRef.current?.contains(event.target)) {
        setIsModelMenuOpen(false);
      }
    };

    const handleKeyDown = (event) => {
      if (event.key === "Escape") setIsModelMenuOpen(false);
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isModelMenuOpen]);

  return (
    <section id="migration-console" className="mc">
      <div className="mc__inner">
        <header className="mc__head">
          <p className="mc__eyebrow">migration console</p>
          <h2 className="mc__title">Загрузка и пайплайн</h2>
          <p className="mc__subtitle">
            Выберите ZIP с Java Spring Boot сервисом. После запуска здесь же
            отображается ход этапов: загрузка, постановка, конвертация,
            упаковка, готовый архив.
          </p>
        </header>

        <div className="mc__setup">
          <label className="mc-model">
            <span className="mc-model__label">LLM модель</span>
            <div ref={modelMenuRef} className="mc-model__select-wrap">
              <button
                type="button"
                className="mc-model__select"
                onClick={() => setIsModelMenuOpen((open) => !open)}
                disabled={isUploading}
                aria-haspopup="listbox"
                aria-expanded={isModelMenuOpen}
              >
                <span>{selectedModelOption.label}</span>
                <span className="mc-model__chevron" aria-hidden="true">
                  {isModelMenuOpen ? "▲" : "▼"}
                </span>
              </button>
              {isModelMenuOpen ? (
                <div className="mc-model__menu" role="listbox">
                  {LLM_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      className={`mc-model__option ${
                        option.value === selectedModel
                          ? "mc-model__option--active"
                          : ""
                      }`}
                      onClick={() => {
                        setSelectedModel(option.value);
                        setIsModelMenuOpen(false);
                      }}
                      role="option"
                      aria-selected={option.value === selectedModel}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          </label>

          {!selectedFile ? (
            <label
              className={`mc-dropzone ${isDragActive ? "mc-dropzone--active" : ""}`}
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
              <span className="mc-dropzone__icon" aria-hidden="true">
                &#8682;
              </span>
              <span className="mc-dropzone__title">Перетащите ZIP сюда</span>
              <span className="mc-dropzone__hint">
                или нажмите, чтобы выбрать файл с устройства
              </span>
              <span className="mc-dropzone__btn">Выбрать архив</span>
            </label>
          ) : (
            <div className="mc-hud">
              <aside className="mc-status-panel" aria-label="Статусы пайплайна">
                <p className="mc-file__name">
                  Имя файла: {selectedFile.name} ({selectedFileSize})
                </p>
                <ul className="mc-status-list">
                  {ORBIT_STEPS.map((step) => {
                    const status = getStepStatus(step.id);
                    return (
                      <li
                        key={step.id}
                        className={`mc-status-item mc-status-item--${status}`}
                      >
                        <span className="mc-status-item__title">
                          {step.title}
                        </span>
                        <span className="mc-status-item__subtitle">
                          {step.subtitle}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              </aside>

              <div className="mc-orbit">
                <div className="mc-orbit__shell">
                  <div className="mc-ring">
                    <svg
                      className="mc-ring__svg"
                      viewBox="0 0 460 460"
                      role="img"
                      aria-label="Орбитальная диаграмма статусов миграции"
                    >
                      <circle
                        className="mc-ring__inner-circ-2"
                        cx="230"
                        cy="230"
                        r="120"
                      />
                      <circle
                        className="mc-ring__inner-circ-1"
                        cx="230"
                        cy="230"
                        r="160"
                      />
                      <circle
                        className="mc-ring__ticks"
                        cx="230"
                        cy="230"
                        r="220"
                      />
                      <circle
                        className="mc-ring__dashes-2"
                        cx="230"
                        cy="230"
                        r="210"
                      />
                      <circle
                        className="mc-ring__main"
                        cx="230"
                        cy="230"
                        r="195"
                      />
                      <circle
                        className="mc-ring__inner"
                        cx="230"
                        cy="230"
                        r="189"
                      />
                      <circle
                        className="mc-ring__arc-orange"
                        cx="230"
                        cy="230"
                        r="195"
                      />
                      <circle
                        className="mc-ring__arc"
                        cx="230"
                        cy="230"
                        r="195"
                      />
                    </svg>

                    {ORBIT_STEPS.map((step, index) => {
                      const status = getStepStatus(step.id);
                      const isCurrentOnOrbit = status === "active";
                      const isCompletedOnOrbit =
                        pipelineStep === "completed" && step.id === "packaging";
                      const angleDeg =
                        ORBIT_NODE_ANGLES[index] ??
                        (index * 360) / Math.max(1, ORBIT_STEPS.length);
                      const angleRad = (angleDeg * Math.PI) / 180;
                      const nodePosition = {
                        left: `${50 + ORBIT_NODE_RADIUS_PERCENT * Math.cos(angleRad)}%`,
                        top: `${50 + ORBIT_NODE_RADIUS_PERCENT * Math.sin(angleRad)}%`,
                      };
                      return (
                        <div
                          key={step.id}
                          className={`mc-orbit__node mc-orbit__node--${status} ${
                            isCurrentOnOrbit || isCompletedOnOrbit
                              ? "mc-orbit__node--current"
                              : ""
                          }`}
                          style={nodePosition}
                        >
                          <span
                            className="mc-orbit__node-dot"
                            aria-hidden="true"
                          />
                          <span className="mc-orbit__node-label">
                            {step.title}
                          </span>
                        </div>
                      );
                    })}
                  </div>

                  <div className="mc-ring__info">
                    <div className="mc-ring__info-label">
                      {orbitStatusLabel}
                    </div>
                    <div className="mc-ring__info-actions">
                      {isUploading ? (
                        <button
                          type="button"
                          className="mc-btn mc-btn--outline mc-ring__info-btn"
                          disabled
                        >
                          В процессе
                        </button>
                      ) : downloadUrl ? (
                        <button
                          type="button"
                          className="mc-btn mc-btn--white mc-ring__info-btn"
                          onClick={handleDownloadResult}
                        >
                          Скачать
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="mc-btn mc-btn--primary mc-ring__info-btn"
                          onClick={() => handleStartMigration()}
                        >
                          Начать
                        </button>
                      )}
                    </div>
                    <div className="mc-ring__info-subtitle">
                      {activeStepData.subtitle}
                    </div>
                  </div>
                </div>
              </div>

              <div className="mc-actions">
                {!isUploading ? (
                  <button
                    type="button"
                    className="mc-btn mc-btn--outline"
                    onClick={handleResetSelection}
                  >
                    Удалить файл
                  </button>
                ) : null}
                {downloadUrl ? (
                  <button
                    type="button"
                    className="mc-btn mc-btn--white"
                    onClick={handleRestartView}
                  >
                    Перезапустить
                  </button>
                ) : null}
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

export default MigrationUploadSection;
