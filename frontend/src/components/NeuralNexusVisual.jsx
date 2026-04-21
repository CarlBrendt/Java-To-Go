import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
} from "react";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { EffectComposer } from "three/examples/jsm/postprocessing/EffectComposer.js";
import { RenderPass } from "three/examples/jsm/postprocessing/RenderPass.js";
import { OutputPass } from "three/examples/jsm/postprocessing/OutputPass.js";
import { ShaderPass } from "three/examples/jsm/postprocessing/ShaderPass.js";

const NeuralNexusVisual = forwardRef(function NeuralNexusVisual(_, ref) {
  const rootRef = useRef(null);
  const mountRef = useRef(null);
  const apiRef = useRef({ goToVisualization: () => {} });

  useImperativeHandle(
    ref,
    () => ({
      goToVisualization: (index) => apiRef.current.goToVisualization(index),
    }),
    [],
  );

  useEffect(() => {
    const root = rootRef.current;
    const mount = mountRef.current;
    if (!root || !mount) return undefined;

    let cancelled = false;
    let rafId = 0;

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)")
      .matches;
    const nodeCount = reduceMotion ? 8000 : 28000;
    const trailCount = reduceMotion ? 3200 : 10000;
    const bgCount = reduceMotion ? 1200 : 3000;
    const transformSpeed = reduceMotion ? 0.028 : 0.018;

    let scene;
    let camera;
    let renderer;
    let dataNodes;
    let trailSystem;
    let backgroundNodes;
    let composer;
    let controls;
    let distortionPass;
    let time = 0;
    let currentVisualization = 0;
    let isTransforming = false;
    let transformProgress = 0;

    const dataFlow = true;
    const showTrails = true;

    const baseColors = ["#991b1b", "#ef4444", "#fecaca"];
    let colorSchemes = baseColors.map(generateScheme);

    function generateScheme(baseHex) {
      const base = new THREE.Color(baseHex);
      const hsl = { h: 0, s: 0, l: 0 };
      base.getHSL(hsl);
      const h0 = hsl.h;
      const scheme = [];
      for (let i = 0; i < 10; i += 1) {
        const s = Math.min(1, Math.max(0.32, hsl.s + (i - 5) * 0.04));
        const l = Math.min(0.9, Math.max(0.08, hsl.l + (i - 5) * 0.035));
        scheme.push(new THREE.Color().setHSL(h0, s, l));
      }
      return scheme;
    }

    function updateCSSColors(baseHex) {
      const base = new THREE.Color(baseHex);
      const hsl = { h: 0, s: 0, l: 0 };
      base.getHSL(hsl);
      const h0 = hsl.h;

      const accent = new THREE.Color().setHSL(
        h0,
        Math.min(1, hsl.s * 0.62),
        Math.min(0.96, hsl.l + 0.36),
      );
      const accentHex = `#${accent.getHexString()}`;

      const secondary = new THREE.Color().setHSL(
        h0,
        Math.min(1, hsl.s * 1.05),
        Math.max(0.07, hsl.l * 0.44),
      );
      const secondaryHex = `#${secondary.getHexString()}`;

      const startColor = new THREE.Color().setHSL(
        h0,
        Math.min(1, hsl.s * 0.92),
        Math.max(0.06, hsl.l * 0.34),
      );
      const startHex = startColor.getHexString();

      const endColor = new THREE.Color().setHSL(
        h0,
        Math.min(1, hsl.s * 0.72),
        Math.min(0.9, hsl.l * 0.88),
      );
      const endHex = endColor.getHexString();

      root.style.setProperty("--nn-primary-color", baseHex);
      root.style.setProperty("--nn-accent-color", accentHex);
      root.style.setProperty("--nn-secondary-color", secondaryHex);
      root.style.setProperty("--nn-gradient-start", `#${startHex}`);
      root.style.setProperty("--nn-gradient-middle", baseHex);
      root.style.setProperty("--nn-gradient-end", `#${endHex}`);

      const primaryRgb = `${Math.round(base.r * 255)}, ${Math.round(base.g * 255)}, ${Math.round(base.b * 255)}`;
      root.style.setProperty("--nn-primary-rgb", primaryRgb);

      const accentC = new THREE.Color(accentHex);
      const accentRgb = `${Math.round(accentC.r * 255)}, ${Math.round(accentC.g * 255)}, ${Math.round(accentC.b * 255)}`;
      root.style.setProperty("--nn-accent-rgb", accentRgb);

      const secondaryC = new THREE.Color(secondaryHex);
      const secondaryRgb = `${Math.round(secondaryC.r * 255)}, ${Math.round(secondaryC.g * 255)}, ${Math.round(secondaryC.b * 255)}`;
      root.style.setProperty("--nn-secondary-rgb", secondaryRgb);
    }

    function generateHarmonicSphere(i, count) {
      const t = i / count;
      const phi = Math.acos(1 - 2 * t);
      const theta = Math.PI * (1 + Math.sqrt(5)) * i;
      const radius = 80;
      const perturbation =
        0.2 *
        (Math.sin(5 * phi) * Math.cos(3 * theta) +
          Math.cos(4 * phi) * Math.sin(2 * theta));
      const r = radius * (1 + perturbation);
      const x = r * Math.sin(phi) * Math.cos(theta);
      const y = r * Math.sin(phi) * Math.sin(theta);
      const z = r * Math.cos(phi);
      return new THREE.Vector3(x, y, z);
    }

    function generateToroidalVortex(i, count) {
      const t = i / count;
      const theta = t * Math.PI * 2;
      const phi = theta * 10 + Math.PI * (1 + Math.sqrt(5)) * t * 10;
      const major = 70;
      const minor = 25 + Math.sin(theta * 3) * 5;
      const x = (major + minor * Math.cos(phi)) * Math.cos(theta);
      const y = (major + minor * Math.cos(phi)) * Math.sin(theta);
      const z = minor * Math.sin(phi) + Math.cos(theta * 4) * 10;
      return new THREE.Vector3(x, y, z);
    }

    function generateNebulaCore(i, count) {
      const coreRatio = 0.25;
      const coreCount = Math.floor(count * coreRatio);
      if (i < coreCount) {
        const t = i / coreCount;
        const phi = Math.acos(1 - 2 * t);
        const theta = Math.PI * (1 + Math.sqrt(5)) * i + Math.sin(t * 10) * 0.5;

        const radius = 25 * Math.random() ** 1.5;
        const x = radius * Math.sin(phi) * Math.cos(theta);
        const y = radius * Math.sin(phi) * Math.sin(theta);
        const z = radius * Math.cos(phi) + Math.sin(theta * 2) * 2;
        return new THREE.Vector3(x, y, z);
      }
      const ringParticles = count - coreCount;
      const ringIndex = i - coreCount;

      const numRings = 8;
      const ringNum = Math.floor(ringIndex / (ringParticles / numRings));
      const nodeInRing = ringIndex % Math.floor(ringParticles / numRings);
      const nodeInRingT = nodeInRing / Math.floor(ringParticles / numRings);
      const angle = nodeInRingT * Math.PI * 2 + Math.sin(ringNum * 2) * 0.3;
      const baseRadius = 35 + ringNum * 10;
      const ringRadius = baseRadius + Math.sin(angle * 4) * 5;
      const tiltAngle =
        (ringNum % 2 === 0 ? 1 : -1) * (Math.PI / 6 + (ringNum * Math.PI) / 15);
      const axisAngle = (ringNum * Math.PI) / 4;
      const axis = new THREE.Vector3(
        Math.sin(axisAngle),
        Math.cos(axisAngle),
        Math.sin(axisAngle * 2),
      ).normalize();
      const rotationMatrix = new THREE.Matrix4().makeRotationAxis(
        axis,
        tiltAngle,
      );
      const pos = new THREE.Vector3(
        Math.cos(angle) * ringRadius,
        0,
        Math.sin(angle) * ringRadius,
      );
      pos.applyMatrix4(rotationMatrix);
      return pos;
    }

    const visualizations = [
      generateHarmonicSphere,
      generateToroidalVortex,
      generateNebulaCore,
    ];

    function createTrailTexture() {
      const canvas = document.createElement("canvas");
      canvas.width = 64;
      canvas.height = 64;
      const context = canvas.getContext("2d");
      const gradient = context.createRadialGradient(32, 32, 0, 32, 32, 32);
      gradient.addColorStop(0, "rgba(255, 240, 240, 1.0)");
      gradient.addColorStop(0.4, "rgba(255, 200, 200, 0.55)");
      gradient.addColorStop(0.8, "rgba(255, 160, 160, 0.2)");
      gradient.addColorStop(1, "rgba(0, 0, 0, 0)");
      context.fillStyle = gradient;
      context.fillRect(0, 0, 64, 64);
      const texture = new THREE.CanvasTexture(canvas);
      texture.needsUpdate = true;
      return texture;
    }

    function createTrailSystem() {
      const trailGeometry = new THREE.BufferGeometry();
      const trailPositions = new Float32Array(trailCount * 3);
      const trailColors = new Float32Array(trailCount * 3);
      const trailSizes = new Float32Array(trailCount);
      const trailOpacities = new Float32Array(trailCount);

      for (let i = 0; i < trailCount; i += 1) {
        trailPositions[i * 3] = (Math.random() - 0.5) * 120;
        trailPositions[i * 3 + 1] = (Math.random() - 0.5) * 120;
        trailPositions[i * 3 + 2] = (Math.random() - 0.5) * 120;

        const colorScheme = colorSchemes[currentVisualization];
        const color =
          colorScheme[Math.floor(Math.random() * colorScheme.length)];
        trailColors[i * 3] = color.r;
        trailColors[i * 3 + 1] = color.g;
        trailColors[i * 3 + 2] = color.b;

        trailSizes[i] = Math.random() * 2 + 0.8;
        trailOpacities[i] = Math.random() * 0.6 + 0.3;
      }

      trailGeometry.setAttribute(
        "position",
        new THREE.BufferAttribute(trailPositions, 3),
      );
      trailGeometry.setAttribute(
        "color",
        new THREE.BufferAttribute(trailColors, 3),
      );
      trailGeometry.setAttribute(
        "size",
        new THREE.BufferAttribute(trailSizes, 1),
      );
      trailGeometry.setAttribute(
        "opacity",
        new THREE.BufferAttribute(trailOpacities, 1),
      );

      const trailMaterial = new THREE.ShaderMaterial({
        uniforms: {
          time: { value: 0 },
          textureMap: { value: createTrailTexture() },
        },
        vertexShader: `
          attribute vec3 color;
          attribute float size;
          attribute float opacity;
          varying vec3 vColor;
          varying float vOpacity;
          uniform float time;
          void main() {
            vColor = color;
            vOpacity = opacity * (0.5 + 0.5 * sin(time + position.x * 0.1));
            vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
            gl_PointSize = size * (300.0 / -mvPosition.z);
            gl_Position = projectionMatrix * mvPosition;
          }
        `,
        fragmentShader: `
          uniform sampler2D textureMap;
          varying vec3 vColor;
          varying float vOpacity;
          void main() {
            vec4 tex = texture2D(textureMap, gl_PointCoord);
            gl_FragColor = vec4(vColor * tex.rgb, tex.a * vOpacity);
          }
        `,
        transparent: true,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });

      trailSystem = new THREE.Points(trailGeometry, trailMaterial);
      scene.add(trailSystem);
    }

    function createBackgroundParticles() {
      const bgGeometry = new THREE.BufferGeometry();
      const bgPositions = new Float32Array(bgCount * 3);
      const bgColors = new Float32Array(bgCount * 3);
      const bgSizes = new Float32Array(bgCount);

      for (let i = 0; i < bgCount; i += 1) {
        const radius = 250 + Math.random() * 350;
        const phi = Math.random() * Math.PI * 2;
        const theta = Math.random() * Math.PI;

        bgPositions[i * 3] = radius * Math.sin(theta) * Math.cos(phi);
        bgPositions[i * 3 + 1] = radius * Math.sin(theta) * Math.sin(phi);
        bgPositions[i * 3 + 2] = radius * Math.cos(theta);

        const intensity = Math.random() * 0.45 + 0.18;
        bgColors[i * 3] = intensity * 0.72;
        bgColors[i * 3 + 1] = intensity * 0.14;
        bgColors[i * 3 + 2] = intensity * 0.12;

        bgSizes[i] = Math.random() * 3 + 1;
      }

      bgGeometry.setAttribute(
        "position",
        new THREE.BufferAttribute(bgPositions, 3),
      );
      bgGeometry.setAttribute(
        "color",
        new THREE.BufferAttribute(bgColors, 3),
      );
      bgGeometry.setAttribute("size", new THREE.BufferAttribute(bgSizes, 1));

      const bgMaterial = new THREE.PointsMaterial({
        size: 1.5,
        vertexColors: true,
        transparent: true,
        opacity: 0.5,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });

      backgroundNodes = new THREE.Points(bgGeometry, bgMaterial);
      scene.add(backgroundNodes);
    }

    function createEnhancedParticleTexture() {
      const canvas = document.createElement("canvas");
      canvas.width = 256;
      canvas.height = 256;
      const context = canvas.getContext("2d");
      const centerX = 128;
      const centerY = 128;

      const outerGradient = context.createRadialGradient(
        centerX,
        centerY,
        0,
        centerX,
        centerY,
        128,
      );
      outerGradient.addColorStop(0, "rgba(255, 255, 255, 1.0)");
      outerGradient.addColorStop(0.28, "rgba(255, 210, 210, 0.75)");
      outerGradient.addColorStop(0.55, "rgba(255, 120, 120, 0.45)");
      outerGradient.addColorStop(1, "rgba(0, 0, 0, 0)");
      context.fillStyle = outerGradient;
      context.fillRect(0, 0, 256, 256);

      const coreGradient = context.createRadialGradient(
        centerX,
        centerY,
        0,
        centerX,
        centerY,
        20,
      );
      coreGradient.addColorStop(0, "rgba(255, 255, 255, 1.0)");
      coreGradient.addColorStop(1, "rgba(255, 170, 170, 0.35)");
      context.fillStyle = coreGradient;
      context.beginPath();
      context.arc(centerX, centerY, 20, 0, Math.PI * 2);
      context.fill();

      const texture = new THREE.CanvasTexture(canvas);
      texture.needsUpdate = true;
      return texture;
    }

    function assignParticleProperties(i, colors, sizes, vizIndex) {
      const colorScheme = colorSchemes[vizIndex];
      let color;
      let brightness = 1.0;
      if (sizes) {
        if (vizIndex === 0) {
          const channelIndex = Math.floor((i / nodeCount) * 12);
          color = colorScheme[channelIndex % colorScheme.length];
          brightness = 0.8 + Math.random() * 0.7;
          sizes[i] = 1.0 + Math.random() * 2.5;
        } else if (vizIndex === 1) {
          const layerIndex = Math.floor(i / (nodeCount / 10));
          color = colorScheme[layerIndex % colorScheme.length];
          brightness = 0.9 + Math.random() * 0.6;
          sizes[i] = 1.0 + Math.random() * 2.5;
        } else {
          const coreRatio = 0.25;
          const coreCount = Math.floor(nodeCount * coreRatio);
          if (i < coreCount) {
            color = colorScheme[i % 4];
            brightness = 1.2 + Math.random() * 0.6;
            sizes[i] = 2.0 + Math.random() * 2.5;
          } else {
            color = colorScheme[4 + (i % (colorScheme.length - 4))];
            brightness = 0.8 + Math.random() * 0.6;
            sizes[i] = 1.0 + Math.random() * 2.0;
          }
        }
      } else {
        const channelIndex = Math.floor((i / nodeCount) * 12);
        color = colorScheme[channelIndex % colorScheme.length];
        brightness = 1.0;
      }

      colors[i * 3] = color.r * brightness;
      colors[i * 3 + 1] = color.g * brightness;
      colors[i * 3 + 2] = color.b * brightness;
    }

    function createDataVisualization() {
      const geometry = new THREE.BufferGeometry();
      const positions = new Float32Array(nodeCount * 3);
      const colors = new Float32Array(nodeCount * 3);
      const sizes = new Float32Array(nodeCount);

      const initialVisualization = visualizations[currentVisualization];

      for (let i = 0; i < nodeCount; i += 1) {
        const pos = initialVisualization(i, nodeCount);
        positions[i * 3] = pos.x;
        positions[i * 3 + 1] = pos.y;
        positions[i * 3 + 2] = pos.z;

        assignParticleProperties(i, colors, sizes, currentVisualization);
      }

      geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
      geometry.setAttribute("color", new THREE.BufferAttribute(colors, 3));
      geometry.setAttribute("size", new THREE.BufferAttribute(sizes, 1));

      geometry.userData.currentColors = new Float32Array(colors);

      const material = new THREE.ShaderMaterial({
        uniforms: {
          time: { value: 0 },
          textureMap: { value: createEnhancedParticleTexture() },
        },
        vertexShader: `
          attribute vec3 color;
          attribute float size;
          varying vec3 vColor;
          uniform float time;
          void main() {
            vColor = color * (1.0 + 0.06 * sin(time * 2.0 + position.y * 0.05));
            vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
            gl_PointSize = size * (350.0 / -mvPosition.z) * (1.0 + 0.04 * sin(time + position.x));
            gl_Position = projectionMatrix * mvPosition;
          }
        `,
        fragmentShader: `
          uniform sampler2D textureMap;
          uniform float time;
          varying vec3 vColor;
          void main() {
            vec2 uv = gl_PointCoord - vec2(0.5);
            float r = length(uv) * 2.0;
            vec4 tex = texture2D(textureMap, gl_PointCoord);
            float alpha = tex.a * (1.0 - smoothstep(0.8, 1.0, r));
            gl_FragColor = vec4(vColor, alpha);
          }
        `,
        transparent: true,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });

      dataNodes = new THREE.Points(geometry, material);
      scene.add(dataNodes);
    }

    function setupPostProcessing() {
      composer = new EffectComposer(renderer);
      composer.addPass(new RenderPass(scene, camera));

      const distortionShader = {
        uniforms: {
          tDiffuse: { value: null },
          time: { value: 0.0 },
          intensity: { value: 0.02 },
        },
        vertexShader: `
          varying vec2 vUv;
          void main() {
            vUv = uv;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
          }
        `,
        fragmentShader: `
          uniform sampler2D tDiffuse;
          uniform float time;
          uniform float intensity;
          varying vec2 vUv;
          void main() {
            vec2 uv = vUv;
            uv.x += sin(uv.y * 10.0 + time) * intensity;
            uv.y += cos(uv.x * 10.0 + time) * intensity;
            gl_FragColor = texture2D(tDiffuse, uv);
          }
        `,
      };
      distortionPass = new ShaderPass(distortionShader);
      composer.addPass(distortionPass);

      composer.addPass(new OutputPass());
    }

    function transformToVisualization(newVisualization) {
      updateCSSColors(baseColors[newVisualization]);

      isTransforming = true;
      transformProgress = 0;

      const positions = dataNodes.geometry.attributes.position.array;
      const colors = dataNodes.geometry.attributes.color.array;
      const sizes = dataNodes.geometry.attributes.size.array;

      const currentPositions = new Float32Array(positions);
      const currentColors = new Float32Array(
        dataNodes.geometry.userData.currentColors,
      );
      const currentSizes = new Float32Array(sizes);

      const visualizationFunction = visualizations[newVisualization];
      const newPositions = new Float32Array(positions.length);
      const newColors = new Float32Array(colors.length);
      const newSizes = new Float32Array(sizes.length);

      for (let i = 0; i < nodeCount; i += 1) {
        const pos = visualizationFunction(i, nodeCount);
        newPositions[i * 3] = pos.x;
        newPositions[i * 3 + 1] = pos.y;
        newPositions[i * 3 + 2] = pos.z;
        assignParticleProperties(i, newColors, newSizes, newVisualization);
      }

      dataNodes.userData.fromPositions = currentPositions;
      dataNodes.userData.toPositions = newPositions;
      dataNodes.userData.fromColors = currentColors;
      dataNodes.userData.toColors = newColors;
      dataNodes.userData.fromSizes = currentSizes;
      dataNodes.userData.toSizes = newSizes;
      dataNodes.userData.targetVisualization = newVisualization;

      if (trailSystem) {
        const trailColors = trailSystem.geometry.attributes.color.array;
        const newColorScheme = colorSchemes[newVisualization];
        for (let i = 0; i < trailCount; i += 1) {
          const color =
            newColorScheme[Math.floor(Math.random() * newColorScheme.length)];
          trailColors[i * 3] = color.r;
          trailColors[i * 3 + 1] = color.g;
          trailColors[i * 3 + 2] = color.b;
        }
        trailSystem.geometry.attributes.color.needsUpdate = true;
      }
    }

    function goToVisualization(rawIndex) {
      if (cancelled || !dataNodes) return;
      if (isTransforming) return;
      const target = Math.max(0, Math.min(2, rawIndex | 0));
      if (target === currentVisualization) return;
      transformToVisualization(target);
    }

    apiRef.current.goToVisualization = goToVisualization;

    function animateTrailSystem() {
      if (!trailSystem || !showTrails) return;

      const positions = trailSystem.geometry.attributes.position.array;
      const opacities = trailSystem.geometry.attributes.opacity.array;

      for (let i = 0; i < trailCount; i += 1) {
        const ix = i * 3;
        const iy = ix + 1;
        const iz = ix + 2;

        switch (currentVisualization) {
          case 0:
            positions[iy] += 0.35;
            if (positions[iy] > 70) positions[iy] = -70;
            positions[ix] += Math.sin(time * 2.2 + i * 0.12) * 0.12;
            positions[iz] += Math.cos(time * 2.0 + i * 0.12) * 0.12;
            break;
          case 1: {
            const distance = Math.sqrt(
              positions[ix] * positions[ix] + positions[iz] * positions[iz],
            );
            if (distance > 0.1) {
              const expansion = 1 + Math.sin(time * 3.5 + i * 0.06) * 0.006;
              positions[ix] *= expansion;
              positions[iz] *= expansion;
            }
            positions[iy] += Math.sin(time * 2.8 + i * 0.04) * 0.25;
            break;
          }
          case 2: {
            const orbitSpeed = 0.012 + (i % 6) * 0.006;
            const x = positions[ix];
            const z = positions[iz];
            positions[ix] = x * Math.cos(orbitSpeed) - z * Math.sin(orbitSpeed);
            positions[iz] = x * Math.sin(orbitSpeed) + z * Math.cos(orbitSpeed);
            positions[iy] += Math.sin(time * 1.5 + i * 0.08) * 0.15;
            break;
          }
          default:
            break;
        }

        opacities[i] = 0.4 + Math.sin(time * 3.5 + i * 0.12) * 0.35;
      }

      trailSystem.geometry.attributes.position.needsUpdate = true;
      trailSystem.geometry.attributes.opacity.needsUpdate = true;
      trailSystem.material.uniforms.time.value = time;
    }

    function animateDataFlow() {
      if (!dataNodes || isTransforming || !dataFlow) return;

      const positions = dataNodes.geometry.attributes.position.array;

      switch (currentVisualization) {
        case 0:
          for (let i = 0; i < nodeCount; i += 1) {
            const ix = i * 3;
            const iz = i * 3 + 2;
            const x = positions[ix];
            const z = positions[iz];
            positions[ix] = x * Math.cos(0.005) - z * Math.sin(0.005);
            positions[iz] = x * Math.sin(0.005) + z * Math.cos(0.005);
          }
          dataNodes.geometry.attributes.position.needsUpdate = true;
          break;
        case 1:
          for (let i = 0; i < nodeCount; i += 1) {
            const ix = i * 3;
            const iy = i * 3 + 1;
            const x = positions[ix];
            const y = positions[iy];
            positions[ix] = x * Math.cos(0.004) - y * Math.sin(0.004);
            positions[iy] = x * Math.sin(0.004) + y * Math.cos(0.004);
          }
          dataNodes.geometry.attributes.position.needsUpdate = true;
          break;
        case 2: {
          const coreRatio = 0.25;
          const coreCount = Math.floor(nodeCount * coreRatio);
          for (let i = 0; i < nodeCount; i += 1) {
            const ix = i * 3;
            const iz = i * 3 + 2;
            const x = positions[ix];
            const z = positions[iz];
            let rotationSpeed;
            if (i < coreCount) {
              rotationSpeed = 0.0015;
            } else {
              const ringParticles = nodeCount - coreCount;
              const ringIndex = i - coreCount;
              const numRings = 8;
              const ringNum = Math.floor(ringIndex / (ringParticles / numRings));
              rotationSpeed = 0.0025 + ringNum * 0.002;
            }
            positions[ix] = x * Math.cos(rotationSpeed) - z * Math.sin(rotationSpeed);
            positions[iz] = x * Math.sin(rotationSpeed) + z * Math.cos(rotationSpeed);
          }
          dataNodes.geometry.attributes.position.needsUpdate = true;
          break;
        }
        default:
          break;
      }
    }

    function onResize() {
      if (!mount || cancelled) return;
      const w = Math.max(1, mount.clientWidth);
      const h = Math.max(1, mount.clientHeight);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h, false);
      composer.setSize(w, h);
    }

    const ro = new ResizeObserver(() => onResize());

    function disposeObject3D(obj) {
      if (!obj) return;
      obj.traverse((child) => {
        if (child.geometry) child.geometry.dispose();
        if (child.material) {
          const mats = Array.isArray(child.material)
            ? child.material
            : [child.material];
          mats.forEach((m) => {
            if (m.map) m.map.dispose();
            if (m.uniforms?.textureMap?.value)
              m.uniforms.textureMap.value.dispose?.();
            m.dispose?.();
          });
        }
      });
    }

    function teardown() {
      ro.disconnect();
      window.removeEventListener("resize", onResize);
      apiRef.current.goToVisualization = () => {};

      if (controls) controls.dispose();
      if (composer) composer.dispose();
      if (renderer) {
        renderer.dispose();
        if (renderer.domElement.parentNode === mount) {
          mount.removeChild(renderer.domElement);
        }
      }
      disposeObject3D(scene);
    }

    function executeMainLoop() {
      if (cancelled) return;

      if (reduceMotion) {
        time += 0.004;
      } else {
        time += 0.012;
      }

      controls.update();

      if (backgroundNodes) {
        backgroundNodes.rotation.y += 0.0004;
        backgroundNodes.rotation.x += 0.00015;
      }

      if (isTransforming) {
        transformProgress += transformSpeed;

        if (transformProgress >= 1.0) {
          const positions = dataNodes.geometry.attributes.position.array;
          const colors = dataNodes.geometry.attributes.color.array;
          const sizes = dataNodes.geometry.attributes.size.array;

          positions.set(dataNodes.userData.toPositions);
          colors.set(dataNodes.userData.toColors);
          sizes.set(dataNodes.userData.toSizes);

          dataNodes.geometry.attributes.position.needsUpdate = true;
          dataNodes.geometry.attributes.color.needsUpdate = true;
          dataNodes.geometry.attributes.size.needsUpdate = true;

          dataNodes.geometry.userData.currentColors = new Float32Array(
            dataNodes.userData.toColors,
          );
          currentVisualization = dataNodes.userData.targetVisualization;
          isTransforming = false;
          transformProgress = 0;
        } else {
          const positions = dataNodes.geometry.attributes.position.array;
          const colors = dataNodes.geometry.attributes.color.array;
          const sizes = dataNodes.geometry.attributes.size.array;

          const t = transformProgress;
          const ease =
            t < 0.5 ? 4 * t * t * t : 1 - (-2 * t + 2) ** 3 / 2;

          for (let i = 0; i < positions.length; i += 1) {
            positions[i] =
              dataNodes.userData.fromPositions[i] * (1 - ease) +
              dataNodes.userData.toPositions[i] * ease;
            colors[i] =
              dataNodes.userData.fromColors[i] * (1 - ease) +
              dataNodes.userData.toColors[i] * ease;
          }

          for (let i = 0; i < sizes.length; i += 1) {
            sizes[i] =
              dataNodes.userData.fromSizes[i] * (1 - ease) +
              dataNodes.userData.toSizes[i] * ease;
          }

          dataNodes.geometry.attributes.position.needsUpdate = true;
          dataNodes.geometry.attributes.color.needsUpdate = true;
          dataNodes.geometry.attributes.size.needsUpdate = true;
        }
      } else {
        animateDataFlow();
      }

      animateTrailSystem();
      if (dataNodes) {
        dataNodes.material.uniforms.time.value = time;
      }
      if (distortionPass) {
        distortionPass.uniforms.time.value = time;
      }
      composer.render();

      if (!cancelled) {
        rafId = requestAnimationFrame(executeMainLoop);
      }
    }

    scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x140606, 0.0005);

    camera = new THREE.PerspectiveCamera(75, 1, 0.1, 2500);
    camera.position.set(0, 0, 155);

    renderer = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true,
      powerPreference: "high-performance",
    });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    mount.appendChild(renderer.domElement);

    controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.1;
    controls.rotateSpeed = 0.6;
    controls.zoomSpeed = 0.9;
    controls.minDistance = 30;
    controls.maxDistance = 350;
    controls.enablePan = false;
    controls.autoRotate = !reduceMotion;
    controls.autoRotateSpeed = 1.0;

    window.addEventListener("resize", onResize);

    setupPostProcessing();
    createDataVisualization();
    createTrailSystem();
    createBackgroundParticles();

    updateCSSColors(baseColors[currentVisualization]);

    ro.observe(mount);
    onResize();

    rafId = requestAnimationFrame(executeMainLoop);

    return () => {
      cancelled = true;
      cancelAnimationFrame(rafId);
      teardown();
    };
  }, []);

  return (
    <div ref={rootRef} className="nn-visual">
      <div ref={mountRef} className="nn-visual__canvas-mount" />
      <div className="nn-visual__grid-overlay" />
      <div className="nn-visual__tech-pattern" />
      <div className="nn-visual__circuit-lines" />
    </div>
  );
});

NeuralNexusVisual.displayName = "NeuralNexusVisual";

export default NeuralNexusVisual;
