import { useEffect, useState } from "react";

const GRAVITY = 0.62;
const JUMP_POWER = 10.8;
const PLAYER_X = 52;
const PLAYER_SIZE = 34;
const BASE_ARENA_WIDTH = 480;
const TICK_MS = 16;
const STATUS_SWITCH_INTERVAL_MS = 9000;

const MIGRATION_STATUSES = [
  "Анализируем структуру проекта...",
  "Переносим модели и DTO в Go...",
  "Сверяем API-контракты и маршруты...",
  "Генерируем черновики обработчиков...",
  "Проверяем совместимость ответов...",
  "Собираем итоговый архив результата...",
];

const createInitialState = () => ({
  playerY: 0,
  velocityY: 0,
  obstacles: [],
  spawnInMs: 850,
  score: 0,
  isGameOver: false,
});

const randomFromRange = (min, max) => Math.random() * (max - min) + min;

function UploadMiniGame() {
  const [arenaWidth, setArenaWidth] = useState(BASE_ARENA_WIDTH);
  const [gameState, setGameState] = useState(createInitialState);
  const [bestScore, setBestScore] = useState(0);
  const [statusIndex, setStatusIndex] = useState(0);

  useEffect(() => {
    const statusTimer = setInterval(() => {
      setStatusIndex((previous) => (previous + 1) % MIGRATION_STATUSES.length);
    }, STATUS_SWITCH_INTERVAL_MS);

    return () => clearInterval(statusTimer);
  }, []);

  useEffect(() => {
    const onResize = () => {
      const nextWidth = Math.min(window.innerWidth - 128, 640);
      setArenaWidth(Math.max(280, nextWidth));
    };

    onResize();
    window.addEventListener("resize", onResize);

    return () => window.removeEventListener("resize", onResize);
  }, []);

  useEffect(() => {
    const gameTimer = setInterval(() => {
      setGameState((previous) => {
        if (previous.isGameOver) {
          return previous;
        }

        const nextVelocity = previous.velocityY - GRAVITY;
        let nextPlayerY = previous.playerY + nextVelocity;
        let normalizedVelocity = nextVelocity;

        if (nextPlayerY < 0) {
          nextPlayerY = 0;
          normalizedVelocity = 0;
        }

        let nextSpawnIn = previous.spawnInMs - TICK_MS;
        let nextObstacles = previous.obstacles.map((obstacle) => ({
          ...obstacle,
          x: obstacle.x - obstacle.speed,
        }));

        if (nextSpawnIn <= 0) {
          nextObstacles.push({
            id: Date.now() + Math.random(),
            x: arenaWidth + 20,
            width: randomFromRange(18, 28),
            height: randomFromRange(22, 54),
            speed: randomFromRange(5.8, 7.3),
            scored: false,
          });
          nextSpawnIn = randomFromRange(900, 1600);
        }

        let gainedPoints = 0;
        let hasCollision = false;
        nextObstacles = nextObstacles
          .map((obstacle) => {
            if (!obstacle.scored && obstacle.x + obstacle.width < PLAYER_X) {
              gainedPoints += 1;
              return { ...obstacle, scored: true };
            }

            return obstacle;
          })
          .filter((obstacle) => obstacle.x + obstacle.width > -10);

        for (const obstacle of nextObstacles) {
          const overlapsHorizontally =
            PLAYER_X < obstacle.x + obstacle.width &&
            PLAYER_X + PLAYER_SIZE > obstacle.x;
          const overlapsVertically = nextPlayerY < obstacle.height;

          if (overlapsHorizontally && overlapsVertically) {
            hasCollision = true;
            break;
          }
        }

        const nextScore = previous.score + gainedPoints;
        setBestScore((previousBest) => Math.max(previousBest, nextScore));

        return {
          playerY: nextPlayerY,
          velocityY: normalizedVelocity,
          obstacles: nextObstacles,
          spawnInMs: nextSpawnIn,
          score: nextScore,
          isGameOver: hasCollision,
        };
      });
    }, TICK_MS);

    return () => clearInterval(gameTimer);
  }, [arenaWidth]);

  const handleJump = () => {
    setGameState((previous) => {
      if (previous.isGameOver) {
        return createInitialState();
      }

      if (previous.playerY > 0) {
        return previous;
      }

      return {
        ...previous,
        velocityY: JUMP_POWER,
      };
    });
  };

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.code === "Space") {
        event.preventDefault();
        handleJump();
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  return (
    <div className="upload-game" role="status" aria-live="polite">
      <div className="upload-game__top">
        <p>Мини-раннер: перепрыгивайте регрессии, пока идет миграция.</p>
        <div className="upload-game__stats">
          <span>Счет: {gameState.score}</span>
          <span>Рекорд: {bestScore}</span>
          <span>{MIGRATION_STATUSES[statusIndex]}</span>
        </div>
      </div>

      <div
        className="upload-game__runner"
        aria-label="Игровое поле"
        role="presentation"
      >
        <div className="upload-game__skyline" />
        <div
          className="upload-game__player"
          style={{ bottom: `${gameState.playerY + 14}px` }}
        >
          🤖
        </div>
        {gameState.obstacles.map((obstacle) => (
          <div
            key={obstacle.id}
            className="upload-game__obstacle"
            style={{
              left: `${obstacle.x}px`,
              width: `${obstacle.width}px`,
              height: `${obstacle.height}px`,
            }}
          >
            ⚠️
          </div>
        ))}
        <div className="upload-game__ground" />
      </div>

      {!gameState.isGameOver && (
        <p className="upload-game__hint">
          Нажимайте <strong>Пробел</strong> для прыжка.
        </p>
      )}

      {gameState.isGameOver && (
        <div className="upload-game__game-over">
          <p>Упс, словили регрессию. Попробуете еще?</p>
          <button
            className="action-button action-button--neutral"
            type="button"
            onClick={handleJump}
          >
            Новая попытка
          </button>
        </div>
      )}
    </div>
  );
}

export default UploadMiniGame;
