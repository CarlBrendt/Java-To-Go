import { useRef, useState } from "react";
import NeuralNexusVisual from "./NeuralNexusVisual";

const MIGRATION_ANCHOR_ID = "migration-console";

function scrollToMigration() {
  document.getElementById(MIGRATION_ANCHOR_ID)?.scrollIntoView({
    behavior: "smooth",
    block: "start",
  });
}

const STORY_SECTIONS = [
  {
    body: (
      <>
        <strong>Проблематика:</strong> поддержка набора Java-сервисов требует
        заметных затрат на инфраструктуру и сопровождение. Полная ручная миграция
        на Go дорогая и долгая, а «чёрный ящик» без контроля часто ломает
        контракты API и оставляет скрытые расхождения в поведении.
      </>
    ),
  },
  {
    body: (
      <>
        <strong>Что делает решение:</strong> предлагается полуавтоматический
        pipeline — анализ Java-кода, перенос бизнес-логики, преобразование
        структур данных, генерация черновиков Go, проверки и сравнение исходного
        и целевого сервиса так, чтобы инженер оставался в контуре.
      </>
    ),
  },
  {
    body: (
      <>
        <strong>Цель:</strong> сохранить API-контракт и семантику: маршруты,
        форматы запросов и ответов, статусы и ключевое поведение должны
        оставаться максимально близкими к исходной системе.
      </>
    ),
  },
];

function LandingHero() {
  const neuralRef = useRef(null);
  const [storyStep, setStoryStep] = useState(0);
  const [scanning, setScanning] = useState(false);
  const [scanKey, setScanKey] = useState(0);
  const scanSessionActive = useRef(false);
  const pendingStepRef = useRef(null);

  const startStepTransition = (target) => {
    if (scanning) return;
    if (target === storyStep) return;
    if (target < 0 || target >= STORY_SECTIONS.length) return;
    pendingStepRef.current = target;
    scanSessionActive.current = true;
    setScanKey((k) => k + 1);
    setScanning(true);
  };

  const handleScanEnd = (event) => {
    if (event.animationName !== "hero-viz-scan-sweep") return;
    if (!scanSessionActive.current) return;
    scanSessionActive.current = false;

    const target = pendingStepRef.current;
    pendingStepRef.current = null;

    if (target == null) {
      setScanning(false);
      return;
    }

    setStoryStep(target);
    queueMicrotask(() => {
      neuralRef.current?.goToVisualization(target);
    });
    setScanning(false);
  };

  return (
    <header className="hero">
      <div className="hero__grid">
        <div className="hero__copy">
          <p className="hero__eyebrow">MWS GPT ToolCall · beta</p>
          <h1 className="hero__title">
            Java<span className="hero__title-accent">2</span>Go
          </h1>
          <p className="hero__lede">
            Полуавтоматическая миграция Java Spring Boot микросервисов в Go: вы
            загружаете ZIP, система прогоняет инженерный пайплайн и отдаёт
            готовый архив с черновиком сервиса и отчётами.
          </p>

          <div className="hero__story-frame">
            <p className="hero__body hero__story-text">
              {STORY_SECTIONS[storyStep].body}
            </p>
            <div className="hero__story-nav">
              {storyStep > 0 ? (
                <button
                  type="button"
                  className="hero__story-nav-btn hero__story-nav-btn--ghost"
                  onClick={() => startStepTransition(storyStep - 1)}
                  disabled={scanning}
                >
                  Назад
                </button>
              ) : null}
              {storyStep < STORY_SECTIONS.length - 1 ? (
                <button
                  type="button"
                  className="hero__story-nav-btn hero__story-nav-btn--primary"
                  onClick={() => startStepTransition(storyStep + 1)}
                  disabled={scanning}
                >
                  Далее
                </button>
              ) : null}
              {storyStep === STORY_SECTIONS.length - 1 ? (
                <button
                  type="button"
                  className="hero__story-nav-btn hero__story-nav-btn--primary"
                  onClick={() => startStepTransition(0)}
                  disabled={scanning}
                >
                  К проблематике
                </button>
              ) : null}
            </div>
          </div>

          <div className="hero__cta-row">
            <button
              type="button"
              className="hero__cta hero__cta--ghost"
              onClick={scrollToMigration}
            >
              К загрузке ZIP
            </button>
          </div>
        </div>

        <div className="hero__visual" aria-hidden="true">
          <NeuralNexusVisual ref={neuralRef} />
          {scanning ? (
            <div
              key={scanKey}
              className="hero__visual-scan hero__visual-scan--active"
              onAnimationEnd={handleScanEnd}
            />
          ) : null}
        </div>
      </div>

      <button
        type="button"
        className="hero__scroll"
        onClick={scrollToMigration}
        aria-label="Прокрутить к консоли миграции"
      >
        <span className="hero__scroll-ring" />
        <span className="hero__scroll-label">вниз к консоли</span>
        <span className="hero__scroll-arrow">↓</span>
      </button>
    </header>
  );
}

export default LandingHero;
