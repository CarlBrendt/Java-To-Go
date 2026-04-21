import { useEffect, useId, useLayoutEffect, useMemo, useRef, useState } from "react";

const ALLOWED_ARCHIVE_TYPE = ".zip";
const API_BASE = "/api/v1/minio";
const POLL_INTERVAL_MS = 5000;

const PIPELINE_STEPS = [
  { id: "upload", title: "Загрузка", subtitle: "Отправляем ZIP в хранилище" },
  { id: "queued", title: "Постановка", subtitle: "Создаем задачу миграции" },
  { id: "migrating", title: "Конвертация", subtitle: "Java -> Go и проверка" },
  { id: "packaging", title: "Упаковка", subtitle: "Собираем итоговый архив" },
  { id: "completed", title: "Готово", subtitle: "Результат доступен для скачивания" },
];

const PIPELINE_STROKE_D =
  "M0,40 C20,22 38,30 50,26 C62,22 78,34 100,26";
const PIPELINE_FILL_D = `${PIPELINE_STROKE_D} L100,100 L0,100 Z`;

const PIPELINE_STEP_INDEX = PIPELINE_STEPS.reduce((acc, step, index) => {
  acc[step.id] = index;
  return acc;
}, {});

function MigrationUploadSection() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [downloadUrl, setDownloadUrl] = useState("");
  const [downloadName, setDownloadName] = useState("migrated-service.zip");
  const [migrationStatus, setMigrationStatus] = useState("");
  const [pipelineStep, setPipelineStep] = useState("idle");
  const [failedStep, setFailedStep] = useState("");
  const abortControllerRef = useRef(null);
  const pollRef = useRef(null);
  const userIdRef = useRef(null);
  const strokePathRef = useRef(null);
  const [curvePoints, setCurvePoints] = useState(null);
  const gradientId = useId().replace(/:/g, "");

  const selectedFileSize = useMemo(() => {
    if (!selectedFile) return "";
    const sizeInMb = selectedFile.size / (1024 * 1024);
    return `${sizeInMb.toFixed(2)} MB`;
  }, [selectedFile]);

  const activePipelineStepId = pipelineStep === "failed" ? failedStep : pipelineStep;
  const activePipelineStepIndex = PIPELINE_STEP_INDEX[activePipelineStepId] ?? -1;
  const pipelineProgressPercent =
    activePipelineStepIndex < 0
      ? 0
      : Math.round(
          ((activePipelineStepIndex + (pipelineStep === "completed" ? 1 : 0)) /
            PIPELINE_STEPS.length) *
            100
        );

  const pipelineCaption =
    pipelineStep === "failed"
      ? "Пайплайн остановлен на ошибке"
      : pipelineStep === "completed"
        ? "Все этапы завершены"
        : pipelineStep === "idle"
          ? "Ожидаем запуск миграции"
          : "Идёт выполнение этапов";

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

  useLayoutEffect(() => {
    const measure = () => {
      const path = strokePathRef.current;
      if (!path || typeof path.getTotalLength !== "function") return;
      const length = path.getTotalLength();
      if (!length) return;
      const fractions = [0, 0.25, 0.5, 0.75, 1];
      setCurvePoints(
        fractions.map((fraction) => {
          const point = path.getPointAtLength(length * fraction);
          return { x: point.x, y: point.y };
        })
      );
    };

    const raf = requestAnimationFrame(() => {
      measure();
    });
    window.addEventListener("resize", measure);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", measure);
    };
  }, []);

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
          setPipelineState("packaging");
          setMigrationStatus("Миграция завершена. Скачиваем результат...");

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
          setPipelineState("completed");
          setIsUploading(false);
        } else if (data.status === "error") {
          stopPolling();
          setErrorMessage(data.message || "Ошибка миграции.");
          setMigrationStatus("");
          markPipelineAsFailed("migrating");
          setIsUploading(false);
        } else {
          setPipelineState("migrating");
          setMigrationStatus(
            `Статус: ${data.status}. Файлов: ${data.files_count || "..."}`
          );
        }
      } catch (err) {
        console.error("Poll error:", err);
        stopPolling();
        setErrorMessage("Не удалось получить статус миграции. Попробуйте снова.");
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
      setSuccessMessage("");
      setErrorMessage("Допустим только ZIP-архив (.zip).");
      return;
    }

    resetGeneratedFile();
    setSelectedFile(file);
    setErrorMessage("");
    setSuccessMessage("");
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
    setSuccessMessage("");
    setErrorMessage("");
    setMigrationStatus("");
    setPipelineStep("idle");
    setFailedStep("");
    userIdRef.current = null;
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
    setSuccessMessage("");
    setMigrationStatus("Загружаем файл...");
    setPipelineState("upload");
    setIsUploading(true);

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);
      const params = new URLSearchParams({
        user_id: userId,
        auto_migrate: "true",
      });

      const uploadRes = await fetch(
        `${API_BASE}/minio-upload-zip?${params.toString()}`,
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

      setPipelineState("queued");
      setMigrationStatus("Файл загружен. Миграция запущена...");

      startPolling(userId);
    } catch (error) {
      if (error.name === "AbortError") {
        setErrorMessage("Запрос остановлен пользователем.");
      } else {
        setErrorMessage(error.message || "Что-то пошло не так. Попробуйте еще раз.");
      }
      setMigrationStatus("");
      markPipelineAsFailed("upload");
      setIsUploading(false);
    }
  };

  const handleStopMigration = () => {
    abortControllerRef.current?.abort();
    stopPolling();
    setIsUploading(false);
    setMigrationStatus("");
    setPipelineStep("idle");
    setFailedStep("");

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

  const pipelinePanel = (
    <div className="mc-pipeline">
      {isUploading ? <span className="mc-pipeline__pulse" aria-hidden="true" /> : null}
      <div className="mc-pipeline__surface">
        <p className="mc-pipeline__caption">{pipelineCaption}</p>
        <div className="mc-chart">
          <div className="mc-chart__value">{pipelineProgressPercent}%</div>
          <svg
            className="mc-chart__svg mc-chart__glow"
            viewBox="0 0 100 48"
            preserveAspectRatio="xMidYMid meet"
            role="img"
            aria-label="Статус этапов миграции"
          >
            <defs>
              <linearGradient
                id={`${gradientId}-stroke`}
                x1="0%"
                y1="0%"
                x2="100%"
                y2="0%"
              >
                <stop offset="0%" stopColor="#f87171" />
                <stop offset="45%" stopColor="#ef4444" />
                <stop offset="100%" stopColor="#be123c" />
              </linearGradient>
              <linearGradient
                id={`${gradientId}-fill`}
                x1="0%"
                y1="0%"
                x2="0%"
                y2="100%"
              >
                <stop offset="0%" stopColor="rgba(239, 68, 68, 0.32)" />
                <stop offset="100%" stopColor="rgba(21, 5, 7, 0.04)" />
              </linearGradient>
            </defs>
            <path d={PIPELINE_FILL_D} fill={`url(#${gradientId}-fill)`} />
            <path
              ref={strokePathRef}
              d={PIPELINE_STROKE_D}
              fill="none"
              stroke={`url(#${gradientId}-stroke)`}
              strokeWidth="1.45"
              vectorEffect="nonScalingStroke"
              className="mc-pipeline-stroke"
            />
            {curvePoints?.map((point, index) => {
              const step = PIPELINE_STEPS[index];
              const status = getStepStatus(step.id);
              const drop = 48 - point.y - 1.5;
              return (
                <g
                  key={step.id}
                  className={`mc-dot mc-dot--${status}`}
                  transform={`translate(${point.x} ${point.y})`}
                >
                  <line
                    className="mc-dot__stem"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2={drop}
                  />
                  <circle className="mc-dot__knob" r="2.1" cx="0" cy="0" />
                  <text
                    className="mc-dot__label"
                    x="0"
                    y="-5.5"
                    textAnchor="middle"
                  >
                    {step.title}
                  </text>
                </g>
              );
            })}
          </svg>
        </div>
      </div>
    </div>
  );

  return (
    <section id="migration-console" className="mc">
      <div className="mc__inner">
        <header className="mc__head">
          <p className="mc__eyebrow">migration console</p>
          <h2 className="mc__title">Загрузка и пайплайн</h2>
          <p className="mc__subtitle">
            Выберите ZIP с Java Spring Boot сервисом. После запуска здесь же
            отображается ход этапов: загрузка, постановка, конвертация, упаковка,
            готовый архив.
          </p>
        </header>

        <div className="mc__board">
          <div className="mc__col mc__col--inputs">
            {!selectedFile && (
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
            )}

            {selectedFile && (
              <div className="mc-file">
                <p className="mc-file__name">
                  {selectedFile.name} ({selectedFileSize})
                </p>
                {!isUploading && (
                  <button
                    type="button"
                    className="mc-btn mc-btn--outline"
                    onClick={handleResetSelection}
                  >
                    Удалить файл
                  </button>
                )}
              </div>
            )}

            <div className="mc-actions">
              {isUploading && (
                <button
                  type="button"
                  className="mc-btn mc-btn--white"
                  onClick={handleStopMigration}
                >
                  Остановить
                </button>
              )}
              {!isUploading && downloadUrl && (
                <>
                  <a
                    className="mc-btn mc-btn--white"
                    href={downloadUrl}
                    download={downloadName}
                  >
                    Скачать результат
                  </a>
                  <button
                    type="button"
                    className="mc-btn mc-btn--primary"
                    onClick={() => handleStartMigration({ isRetry: true })}
                  >
                    Повторить попытку
                  </button>
                </>
              )}
              {!isUploading && !downloadUrl && !!errorMessage && (
                <button
                  type="button"
                  className="mc-btn mc-btn--primary"
                  onClick={() => handleStartMigration()}
                >
                  Повторить
                </button>
              )}
              {!isUploading && !downloadUrl && !errorMessage && selectedFile && (
                <button
                  type="button"
                  className="mc-btn mc-btn--primary"
                  onClick={() => handleStartMigration()}
                >
                  Запустить миграцию
                </button>
              )}
            </div>

            {migrationStatus && (
              <p className="mc-msg mc-msg--info">{migrationStatus}</p>
            )}
            {successMessage && (
              <p className="mc-msg mc-msg--ok">{successMessage}</p>
            )}
            {errorMessage && (
              <p className="mc-msg mc-msg--err">{errorMessage}</p>
            )}
          </div>

          <div className="mc__col mc__col--viz">{pipelinePanel}</div>
        </div>
      </div>
    </section>
  );
}

export default MigrationUploadSection;
