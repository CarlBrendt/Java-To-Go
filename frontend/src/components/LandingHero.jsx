import { useEffect, useRef, useState } from "react";
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
    id: "problem",
    title: "Проблематика",
    body: (
      <>
        Поддержка набора Java-сервисов требует заметных затрат на инфраструктуру
        и сопровождение. Полная ручная миграция на Go дорогая и долгая, а
        «чёрный ящик» без контроля часто ломает контракты API и оставляет
        скрытые расхождения в поведении.
      </>
    ),
  },
  {
    id: "solution",
    title: "Решение",
    body: (
      <>
        Предлагается полуавтоматический pipeline — анализ Java-кода, перенос
        бизнес-логики, преобразование структур данных, генерация черновиков Go,
        проверки и сравнение исходного и целевого сервиса так, чтобы инженер
        оставался в контуре.
      </>
    ),
  },
  {
    id: "goal",
    title: "Цель",
    body: (
      <>
        Сохранить API-контракт и семантику: маршруты, форматы запросов и
        ответов, статусы и ключевое поведение должны оставаться максимально
        близкими к исходной системе.
      </>
    ),
  },
];

function LandingHero() {
  const heroRef = useRef(null);
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

  useEffect(() => {
    const heroEl = heroRef.current;
    if (!heroEl) return undefined;

    let rafId = 0;
    let lastFrameTime = 0;
    let targetProgress = 0;
    let currentProgress = 0;
    const reduceMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    const smoothingRate = reduceMotion ? 8 : 11;
    const settleThreshold = 0.0008;

    const setProgressVars = (value) => {
      const asString = value.toFixed(3);
      heroEl.style.setProperty("--hero-overlap-progress", asString);
      document.documentElement.style.setProperty(
        "--hero-overlap-progress",
        asString,
      );
    };

    const readTargetProgress = () => {
      const raw = Math.min(
        1,
        Math.max(0, window.scrollY / window.innerHeight),
      );
      // Smoothstep easing for softer start/end of overlap.
      return raw * raw * (3 - 2 * raw);
    };

    const animateTowardsTarget = (timestamp) => {
      if (!lastFrameTime) lastFrameTime = timestamp;
      const deltaMs = Math.min(48, timestamp - lastFrameTime);
      lastFrameTime = timestamp;

      const alpha = 1 - Math.exp(-(deltaMs / 1000) * smoothingRate);
      currentProgress += (targetProgress - currentProgress) * alpha;
      setProgressVars(currentProgress);

      if (Math.abs(targetProgress - currentProgress) > settleThreshold) {
        rafId = requestAnimationFrame(animateTowardsTarget);
      } else {
        currentProgress = targetProgress;
        setProgressVars(currentProgress);
        rafId = 0;
        lastFrameTime = 0;
      }
    };

    const onScroll = () => {
      targetProgress = readTargetProgress();
      if (!rafId) {
        rafId = requestAnimationFrame(animateTowardsTarget);
      }
    };

    targetProgress = readTargetProgress();
    currentProgress = targetProgress;
    setProgressVars(currentProgress);
    window.addEventListener("scroll", onScroll, { passive: true });

    return () => {
      window.removeEventListener("scroll", onScroll);
      if (rafId) cancelAnimationFrame(rafId);
    };
  }, []);

  const totalSteps = STORY_SECTIONS.length;

  return (
    <header ref={heroRef} className="hero">
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
            <div className="hero__story-head">
              <p className="hero__story-headline">
                Этап: {STORY_SECTIONS[storyStep].title}
              </p>
              <div className="hero__story-track" aria-hidden="true">
                {STORY_SECTIONS.map((section, index) => (
                  <span
                    key={section.id}
                    className={`hero__story-step ${
                      index < storyStep
                        ? "hero__story-step--done"
                        : index === storyStep
                          ? "hero__story-step--active"
                          : ""
                    }`}
                  />
                ))}
              </div>
            </div>

            <div className="hero__story-card">
              <p className="hero__body hero__story-text">
                {STORY_SECTIONS[storyStep].body}
              </p>
              <div className="hero__story-nav">
                <button
                  type="button"
                  className="hero__story-nav-btn hero__story-nav-btn--ghost"
                  onClick={() =>
                    startStepTransition(
                      (storyStep - 1 + totalSteps) % totalSteps,
                    )
                  }
                  disabled={scanning}
                  aria-label="Предыдущий этап"
                >
                  <span aria-hidden="true">←</span>
                </button>
                <button
                  type="button"
                  className="hero__story-nav-btn hero__story-nav-btn--primary"
                  onClick={() =>
                    startStepTransition((storyStep + 1) % totalSteps)
                  }
                  disabled={scanning}
                  aria-label="Следующий этап"
                >
                  <span aria-hidden="true">→</span>
                </button>
              </div>
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
    </header>
  );
}

export default LandingHero;
