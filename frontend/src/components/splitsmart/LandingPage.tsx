import { motion, AnimatePresence } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import {
  ArrowRight,
  Zap,
  ShieldCheck,
  Cpu,
  TrendingUp,
  Sparkles,
  CheckCircle2,
  Layers,
  BarChart3,
  Wallet,
  Users,
  MessageSquare,
  Send,
  HelpCircle,
  ChevronDown,
  Calculator,
  RefreshCw,
  Check,
  Globe,
  X,
  Minus,
  Plus,
} from "lucide-react";
import { SectionLabel, BrandLogo } from "./primitives";

/* 3D Cosmic Space Canvas: Saturn Ringed World, Atomic Orbit System & Faceted Rocket */
function CosmicSpace3DBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animationFrameId: number;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    const handleResize = () => {
      if (!canvas) return;
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    };
    window.addEventListener("resize", handleResize);

    // Mouse tracking & smooth interpolation
    let rawMouseX = width * 0.35;
    let rawMouseY = height * 0.35;
    let smoothMouseX = rawMouseX;
    let smoothMouseY = rawMouseY;
    let targetMouseX = 0;
    let targetMouseY = 0;

    const handleMouseMove = (e: MouseEvent) => {
      rawMouseX = e.clientX;
      rawMouseY = e.clientY;
      targetMouseX = (e.clientX - width / 2) / (width / 2);
      targetMouseY = (e.clientY - height / 2) / (height / 2);
    };
    window.addEventListener("mousemove", handleMouseMove);

    // 1. Starfield Particles in 3D (x, y, z)
    const starCount = 135;
    const stars: { x: number; y: number; z: number; size: number; alpha: number }[] = [];

    for (let i = 0; i < starCount; i++) {
      stars.push({
        x: (Math.random() - 0.5) * width * 2,
        y: (Math.random() - 0.5) * height * 2,
        z: Math.random() * 900 + 100,
        size: Math.random() * 2.0 + 0.5,
        alpha: Math.random() * 0.65 + 0.2,
      });
    }

    // 2. Shooting Stars Engine
    interface ShootingStar {
      x: number;
      y: number;
      dx: number;
      dy: number;
      length: number;
      speed: number;
      life: number;
      maxLife: number;
    }
    const shootingStars: ShootingStar[] = [];

    const spawnShootingStar = () => {
      const angleRad = Math.PI / 4 + (Math.random() - 0.5) * 0.3;
      const speed = Math.random() * 8 + 11;
      shootingStars.push({
        x: Math.random() * width * 0.8,
        y: Math.random() * height * 0.3,
        dx: Math.cos(angleRad) * speed,
        dy: Math.sin(angleRad) * speed,
        length: Math.random() * 80 + 60,
        speed,
        life: 0,
        maxLife: Math.random() * 28 + 32,
      });
    };

    // 3. Space Rocket Flight System
    let rocketProgress = 0;
    const rocketPath = (t: number) => {
      const rx = (0.15 + 0.68 * Math.sin(t * 0.45)) * width;
      const ry = (0.25 + 0.16 * Math.sin(t * 1.1)) * height;
      const angle = Math.atan2(0.16 * 1.1 * Math.cos(t * 1.1) * height, 0.68 * 0.45 * Math.cos(t * 0.45) * width);
      return { x: rx, y: ry, angle };
    };

    let globalTime = 0;

    const render = () => {
      ctx.clearRect(0, 0, width, height);

      globalTime += 0.016;

      // Smooth mouse position interpolation
      smoothMouseX += (rawMouseX - smoothMouseX) * 0.08;
      smoothMouseY += (rawMouseY - smoothMouseY) * 0.08;

      // Detect light mode vs dark mode
      const isLightMode = document.documentElement.classList.contains("light");

      // -------------------------------------------------------------
      // A. ROTATING WORLDS: GIANT 3D SATURN PLANET (Top Right Sky)
      // -------------------------------------------------------------
      const saturnX = width * 0.84 + targetMouseX * 25;
      const saturnY = height * 0.18 + targetMouseY * 25;
      const planetRadius = 48;

      // 1. Saturn Back Rings (Drawn behind planet for 3D depth)
      ctx.save();
      ctx.translate(saturnX, saturnY);
      ctx.rotate(-0.35);

      ctx.beginPath();
      ctx.ellipse(0, 0, 145, 38, 0, Math.PI, Math.PI * 2);
      ctx.strokeStyle = isLightMode ? "rgba(124, 58, 237, 0.22)" : "rgba(167, 139, 250, 0.30)";
      ctx.lineWidth = 14;
      ctx.stroke();

      ctx.beginPath();
      ctx.ellipse(0, 0, 118, 30, 0, Math.PI, Math.PI * 2);
      ctx.strokeStyle = isLightMode ? "rgba(109, 40, 217, 0.18)" : "rgba(139, 92, 246, 0.22)";
      ctx.lineWidth = 8;
      ctx.stroke();
      ctx.restore();

      // 2. Saturn Planet Sphere Shading
      const saturnGrad = ctx.createRadialGradient(saturnX - 14, saturnY - 14, 4, saturnX, saturnY, planetRadius);
      if (isLightMode) {
        saturnGrad.addColorStop(0, "#DDD6FE");
        saturnGrad.addColorStop(0.5, "#C4B5FD");
        saturnGrad.addColorStop(1, "#8B5CF6");
      } else {
        saturnGrad.addColorStop(0, "#4C3575");
        saturnGrad.addColorStop(0.5, "#2A1D45");
        saturnGrad.addColorStop(1, "#120B24");
      }

      ctx.beginPath();
      ctx.arc(saturnX, saturnY, planetRadius, 0, Math.PI * 2);
      ctx.fillStyle = saturnGrad;
      ctx.shadowColor = isLightMode ? "rgba(124, 58, 237, 0.2)" : "rgba(139, 92, 246, 0.4)";
      ctx.shadowBlur = 30;
      ctx.fill();

      // 3. Saturn Front Rings (Drawn in front of planet for 3D occlusion)
      ctx.save();
      ctx.translate(saturnX, saturnY);
      ctx.rotate(-0.35);

      ctx.beginPath();
      ctx.ellipse(0, 0, 145, 38, 0, 0, Math.PI);
      ctx.strokeStyle = isLightMode ? "rgba(124, 58, 237, 0.35)" : "rgba(167, 139, 250, 0.40)";
      ctx.lineWidth = 14;
      ctx.stroke();

      ctx.beginPath();
      ctx.ellipse(0, 0, 118, 30, 0, 0, Math.PI);
      ctx.strokeStyle = isLightMode ? "rgba(109, 40, 217, 0.28)" : "rgba(196, 181, 253, 0.30)";
      ctx.lineWidth = 8;
      ctx.stroke();
      ctx.restore();

      // -------------------------------------------------------------
      // B. BACKGROUND 3D PLANETARY ORBIT RINGS
      // -------------------------------------------------------------
      const bgCenterX = width * 0.68 + targetMouseX * 40;
      const bgCenterY = height * 0.45 + targetMouseY * 40;

      const bgOrbits = [
        { rx: 340, ry: 110, tilt: -0.32, color: isLightMode ? "rgba(124, 58, 237, 0.15)" : "rgba(139, 92, 246, 0.22)", width: 1.2 },
        { rx: 240, ry: 78, tilt: -0.20, color: isLightMode ? "rgba(109, 40, 217, 0.12)" : "rgba(167, 139, 250, 0.18)", width: 1.0 },
        { rx: 155, ry: 50, tilt: -0.10, color: isLightMode ? "rgba(139, 92, 246, 0.18)" : "rgba(196, 181, 253, 0.25)", width: 1.4 },
      ];

      bgOrbits.forEach((orbit, idx) => {
        ctx.save();
        ctx.translate(bgCenterX, bgCenterY);
        ctx.rotate(orbit.tilt + targetMouseX * 0.07);
        ctx.beginPath();
        ctx.ellipse(0, 0, orbit.rx, orbit.ry, 0, 0, Math.PI * 2);
        ctx.strokeStyle = orbit.color;
        ctx.lineWidth = orbit.width;
        ctx.stroke();

        const planetAngle = globalTime * (0.28 + idx * 0.18) + idx * 2.2;
        const px = orbit.rx * Math.cos(planetAngle);
        const py = orbit.ry * Math.sin(planetAngle);

        ctx.beginPath();
        ctx.arc(px, py, 4.5 + idx * 1.2, 0, Math.PI * 2);
        ctx.fillStyle = idx === 1 ? "#A78BFA" : "#8B5CF6";
        ctx.shadowColor = "rgba(139, 92, 246, 0.7)";
        ctx.shadowBlur = 14;
        ctx.fill();

        ctx.restore();
      });

      // -------------------------------------------------------------
      // C. DRAW 3D STARFIELD & CONSTELLATIONS
      // -------------------------------------------------------------
      const projectedStars: { px: number; py: number; size: number; alpha: number }[] = [];

      stars.forEach((star) => {
        star.z -= 0.32;
        if (star.z <= 0) star.z = 1000;

        const k = 400 / star.z;
        const px = (star.x + targetMouseX * 50) * k + width / 2;
        const py = (star.y + targetMouseY * 50) * k + height / 2;

        if (px >= 0 && px <= width && py >= 0 && py <= height) {
          const size = Math.max(0.5, star.size * k * 0.65);
          const alpha = star.alpha * Math.min(1, k);
          projectedStars.push({ px, py, size, alpha });

          ctx.beginPath();
          ctx.arc(px, py, size, 0, Math.PI * 2);
          ctx.fillStyle = isLightMode ? `rgba(124, 58, 237, ${alpha * 0.7})` : `rgba(241, 245, 249, ${alpha})`;
          ctx.shadowColor = "rgba(139, 92, 246, 0.5)";
          ctx.shadowBlur = size * 2.5;
          ctx.fill();
        }
      });

      for (let i = 0; i < projectedStars.length; i++) {
        for (let j = i + 1; j < projectedStars.length; j++) {
          const s1 = projectedStars[i];
          const s2 = projectedStars[j];
          if (!s1 || !s2) continue;
          const dist = Math.hypot(s1.px - s2.px, s1.py - s2.py);
          if (dist < 100) {
            const lineAlpha = (1 - dist / 100) * (isLightMode ? 0.12 : 0.14) * s1.alpha;
            ctx.beginPath();
            ctx.moveTo(s1.px, s1.py);
            ctx.lineTo(s2.px, s2.py);
            ctx.strokeStyle = isLightMode ? `rgba(124, 58, 237, ${lineAlpha})` : `rgba(139, 92, 246, ${lineAlpha})`;
            ctx.lineWidth = 0.7;
            ctx.stroke();
          }
        }
      }

      // -------------------------------------------------------------
      // D. SHOOTING STARS SYSTEM
      // -------------------------------------------------------------
      if (Math.random() < 0.025 && shootingStars.length < 4) {
        spawnShootingStar();
      }

      for (let i = shootingStars.length - 1; i >= 0; i--) {
        const ss = shootingStars[i];
        if (!ss) continue;
        ss.x += ss.dx;
        ss.y += ss.dy;
        ss.life++;

        const tailX = ss.x - (ss.dx / ss.speed) * ss.length;
        const tailY = ss.y - (ss.dy / ss.speed) * ss.length;
        const fade = 1 - ss.life / ss.maxLife;

        const grad = ctx.createLinearGradient(tailX, tailY, ss.x, ss.y);
        grad.addColorStop(0, "rgba(139, 92, 246, 0)");
        grad.addColorStop(0.7, `rgba(167, 139, 250, ${0.5 * fade})`);
        grad.addColorStop(1, isLightMode ? `rgba(124, 58, 237, ${0.9 * fade})` : `rgba(248, 250, 252, ${0.9 * fade})`);

        ctx.beginPath();
        ctx.moveTo(tailX, tailY);
        ctx.lineTo(ss.x, ss.y);
        ctx.strokeStyle = grad;
        ctx.lineWidth = 1.8;
        ctx.stroke();

        ctx.beginPath();
        ctx.arc(ss.x, ss.y, 2, 0, Math.PI * 2);
        ctx.fillStyle = isLightMode ? `rgba(124, 58, 237, ${fade})` : `rgba(255, 255, 255, ${fade})`;
        ctx.shadowColor = "#A78BFA";
        ctx.shadowBlur = 10;
        ctx.fill();

        if (ss.life >= ss.maxLife) {
          shootingStars.splice(i, 1);
        }
      }

      // -------------------------------------------------------------
      // E. FACETED LOW-POLY ROCKET (Exact to Reference Screenshot!)
      // -------------------------------------------------------------
      rocketProgress += 0.0035;
      const rocketState = rocketPath(rocketProgress);

      ctx.save();
      ctx.translate(rocketState.x, rocketState.y);
      ctx.rotate(rocketState.angle);

      for (let p = 0; p < 6; p++) {
        const exLength = Math.random() * 30 + 12;
        const exY = (Math.random() - 0.5) * 7;
        const exSize = Math.random() * 3.5 + 1.2;
        ctx.beginPath();
        ctx.arc(-exLength, exY, exSize, 0, Math.PI * 2);
        ctx.fillStyle = p % 3 === 0 ? "rgba(249, 115, 22, 0.85)" : p % 3 === 1 ? "rgba(245, 158, 11, 0.8)" : "rgba(139, 92, 246, 0.7)";
        ctx.shadowColor = "#F97316";
        ctx.shadowBlur = 9;
        ctx.fill();
      }

      ctx.beginPath();
      ctx.moveTo(18, 0);
      ctx.lineTo(-6, -9);
      ctx.lineTo(0, 0);
      ctx.closePath();
      ctx.fillStyle = "#E2E8F0";
      ctx.fill();

      ctx.beginPath();
      ctx.moveTo(18, 0);
      ctx.lineTo(-6, 9);
      ctx.lineTo(0, 0);
      ctx.closePath();
      ctx.fillStyle = "#94A3B8";
      ctx.fill();

      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.lineTo(-6, -9);
      ctx.lineTo(-14, -6);
      ctx.lineTo(-10, 0);
      ctx.closePath();
      ctx.fillStyle = "#64748B";
      ctx.fill();

      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.lineTo(-6, 9);
      ctx.lineTo(-14, 6);
      ctx.lineTo(-10, 0);
      ctx.closePath();
      ctx.fillStyle = "#475569";
      ctx.fill();

      ctx.beginPath();
      ctx.ellipse(3, 0, 4, 2.5, 0, 0, Math.PI * 2);
      ctx.fillStyle = "#38BDF8";
      ctx.shadowColor = "#38BDF8";
      ctx.shadowBlur = 12;
      ctx.fill();

      ctx.restore();

      // -------------------------------------------------------------
      // F. ATOMIC MULTI-ORBIT PLANETARY CURSOR (Exact match to Screenshots 2, 3, 4, 5!)
      // -------------------------------------------------------------
      ctx.save();
      ctx.translate(smoothMouseX, smoothMouseY);

      // Orbit 1: Inner Solid Circular Ring
      ctx.beginPath();
      ctx.ellipse(0, 0, 24, 24, 0, 0, Math.PI * 2);
      ctx.strokeStyle = isLightMode ? "rgba(124, 58, 237, 0.45)" : "rgba(167, 139, 250, 0.65)";
      ctx.lineWidth = 1.4;
      ctx.stroke();

      // Orbit 1 Satellite Node (White Node)
      const orb1Angle = globalTime * 2.2;
      const o1x = 24 * Math.cos(orb1Angle);
      const o1y = 24 * Math.sin(orb1Angle);
      ctx.beginPath();
      ctx.arc(o1x, o1y, 3.5, 0, Math.PI * 2);
      ctx.fillStyle = "#FFFFFF";
      ctx.shadowColor = "rgba(167, 139, 250, 0.9)";
      ctx.shadowBlur = 10;
      ctx.fill();

      // Orbit 2: Tilted Middle Ellipse Ring
      ctx.beginPath();
      ctx.ellipse(0, 0, 46, 26, 0.45, 0, Math.PI * 2);
      ctx.strokeStyle = isLightMode ? "rgba(109, 40, 217, 0.35)" : "rgba(139, 92, 246, 0.55)";
      ctx.lineWidth = 1.3;
      ctx.stroke();

      // Orbit 2 Satellite Node (Cyan Node)
      const orb2Angle = -globalTime * 1.7;
      ctx.save();
      ctx.rotate(0.45);
      const o2x = 46 * Math.cos(orb2Angle);
      const o2y = 26 * Math.sin(orb2Angle);
      ctx.beginPath();
      ctx.arc(o2x, o2y, 3.8, 0, Math.PI * 2);
      ctx.fillStyle = "#38BDF8";
      ctx.shadowColor = "#38BDF8";
      ctx.shadowBlur = 12;
      ctx.fill();
      ctx.restore();

      // Orbit 3: Tilted Outer Dashed Ellipse Ring
      ctx.beginPath();
      ctx.ellipse(0, 0, 62, 34, -0.35, 0, Math.PI * 2);
      ctx.setLineDash([4, 6]);
      ctx.strokeStyle = isLightMode ? "rgba(124, 58, 237, 0.4)" : "rgba(196, 181, 253, 0.5)";
      ctx.lineWidth = 1.2;
      ctx.stroke();

      // Orbit 3 Satellite Node (Purple Node)
      const orb3Angle = globalTime * 1.3 + 1.5;
      ctx.save();
      ctx.rotate(-0.35);
      const o3x = 62 * Math.cos(orb3Angle);
      const o3y = 34 * Math.sin(orb3Angle);
      ctx.beginPath();
      ctx.arc(o3x, o3y, 4, 0, Math.PI * 2);
      ctx.fillStyle = "#C084FC";
      ctx.shadowColor = "#C084FC";
      ctx.shadowBlur = 12;
      ctx.fill();
      ctx.restore();

      // Core Sun Star Node (Glowing White Sphere)
      ctx.beginPath();
      ctx.arc(0, 0, 7.5, 0, Math.PI * 2);
      ctx.fillStyle = "#FFFFFF";
      ctx.shadowColor = "rgba(167, 139, 250, 1)";
      ctx.shadowBlur = 18;
      ctx.fill();

      ctx.restore();

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener("resize", handleResize);
      window.removeEventListener("mousemove", handleMouseMove);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return <canvas ref={canvasRef} className="pointer-events-none fixed inset-0 z-0 h-full w-full opacity-95" />;
}

/* 3D Hollow Rotating Planet Word Cloud ΓÇö Problems Solved by SplitSmart */
function RotatingHollowPlanetWordCloud() {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animationFrameId: number;
    let width = (canvas.width = container.clientWidth);
    let height = (canvas.height = Math.max(500, container.clientHeight));

    const handleResize = () => {
      if (!canvas || !container) return;
      width = canvas.width = container.clientWidth;
      height = canvas.height = Math.max(500, container.clientHeight);
    };
    window.addEventListener("resize", handleResize);

    // Balanced list of problems solved by SplitSmart & technical stack terms alternating evenly across the 3D sphere
    const problemItems = [
      { text: "Disputed Receipts", weight: "large" },
      { text: "AWS", weight: "small" },
      { text: "Unbalanced Ledgers", weight: "large" },
      { text: "Python", weight: "small" },
      { text: "Manual Expense Logging", weight: "large" },
      { text: "Java", weight: "small" },
      { text: "Awkward Reminders", weight: "large" },
      { text: "Docker", weight: "small" },
      { text: "Paise Mismatch Errors", weight: "medium" },
      { text: "Kubernetes", weight: "small" },
      { text: "Lost Screenshots", weight: "medium" },
      { text: "TypeScript", weight: "medium" },
      { text: "Delayed Settlements", weight: "large" },
      { text: "Redis", weight: "small" },
      { text: "Zero Audit Trail", weight: "large" },
      { text: "Spring Boot", weight: "medium" },
      { text: "Opaque Allocation", weight: "medium" },
      { text: "PostgreSQL", weight: "medium" },
      { text: "WhatsApp Parsing", weight: "large" },
      { text: "CI/CD", weight: "small" },
      { text: "Cross-Group Leakage", weight: "medium" },
      { text: "REST APIs", weight: "small" },
      { text: "Siloed Transfers", weight: "medium" },
      { text: "Microservices", weight: "small" },
      { text: "Forgotten IOUs", weight: "medium" },
      { text: "System Design", weight: "large" },
      { text: "Unverified Claims", weight: "medium" },
      { text: "Generative AI", weight: "medium" },
      { text: "Currency Hassles", weight: "medium" },
      { text: "LLMs", weight: "large" },
      { text: "Conflicting Views", weight: "medium" },
      { text: "Distributed Systems", weight: "large" },
      { text: "Approval Bottlenecks", weight: "medium" },
      { text: "CQRS Sourcing", weight: "small" },
      { text: "Messy Spreadsheets", weight: "medium" },
      { text: "DP Bitmask Engine", weight: "small" },
      { text: "Unsynchronized Balances", weight: "medium" },
      { text: "Fowler Money Pattern", weight: "small" },
      { text: "Ambiguous Scans", weight: "medium" },
      { text: "1-Tap UPI QR", weight: "small" },
      { text: "Duplicate Entries", weight: "medium" },
      { text: "ONNX NER", weight: "small" },
      { text: "Machine Learning", weight: "medium" },
      { text: "PostgreSQL RLS", weight: "small" },
      { text: "SSE Push Alerts", weight: "small" },
      { text: "Token Bucket Limiter", weight: "small" },
    ];

    // Generate points on a 3D unit sphere using Fibonacci sphere algorithm
    const numPoints = problemItems.length;
    const goldenRatio = (1 + Math.sqrt(5)) / 2;
    const points = problemItems.map((item, i) => {
      const theta = 2 * Math.PI * i / goldenRatio;
      const phi = Math.acos(1 - 2 * (i + 0.5) / numPoints);
      return {
        item,
        x: Math.sin(phi) * Math.cos(theta),
        y: Math.cos(phi),
        z: Math.sin(phi) * Math.sin(theta),
      };
    });

    // Rotation & Hover Zoom State
    let rotX = 0.15; // Slight tilt
    let rotY = 0;    // Y-axis rotation
    let isDragging = false;
    let isHovered = false;
    let currentZoomScale = 1.0;
    let targetZoomScale = 1.0;
    let mouseX = -9999;
    let mouseY = -9999;
    let lastMouseX = 0;
    let lastMouseY = 0;

    const handleMouseEnter = () => {
      isHovered = true;
    };

    const handleMouseLeave = () => {
      isHovered = false;
      isDragging = false;
      mouseX = -9999;
      mouseY = -9999;
    };

    const handleMouseDown = (e: MouseEvent) => {
      isDragging = true;
      lastMouseX = e.clientX;
      lastMouseY = e.clientY;
    };

    const handleMouseMove = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      mouseX = e.clientX - rect.left;
      mouseY = e.clientY - rect.top;

      if (!isDragging) return;
      const deltaX = e.clientX - lastMouseX;
      const deltaY = e.clientY - lastMouseY;
      rotY += deltaX * 0.005;
      rotX += deltaY * 0.005;
      lastMouseX = e.clientX;
      lastMouseY = e.clientY;
    };

    const handleMouseUp = () => {
      isDragging = false;
    };

    container.addEventListener("mouseenter", handleMouseEnter);
    container.addEventListener("mouseleave", handleMouseLeave);
    container.addEventListener("mousedown", handleMouseDown);
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);

    const render = () => {
      ctx.clearRect(0, 0, width, height);

      // Smooth zoom physics interpolation: Enlarges when hovered or interacted with!
      targetZoomScale = isHovered || isDragging ? 1.42 : 1.0;
      currentZoomScale += (targetZoomScale - currentZoomScale) * 0.09;

      // Auto-rotate along Y-axis continuously (slows slightly on hover for crisp interaction)
      if (!isDragging) {
        rotY += isHovered ? 0.002 : 0.004;
      }

      const centerX = width / 2;
      const centerY = height / 2;
      const R = Math.min(width, height) * 0.36 * currentZoomScale;

      // Transform 3D coordinates
      const transformed = points.map((p) => {
        // Rotate Y
        const cosY = Math.cos(rotY);
        const sinY = Math.sin(rotY);
        const x1 = p.x * cosY + p.z * sinY;
        const z1 = -p.x * sinY + p.z * cosY;
        const y1 = p.y;

        // Rotate X (tilt)
        const cosX = Math.cos(rotX);
        const sinX = Math.sin(rotX);
        const y2 = y1 * cosX - z1 * sinX;
        const z2 = y1 * sinX + z1 * cosX;
        const x2 = x1;

        // Perspective projection
        const focalLength = 550;
        const scale = focalLength / (focalLength - z2 * R * 0.55);
        const screenX = centerX + x2 * R * scale;
        const screenY = centerY + y2 * R * scale;

        // Check if mouse is hovering over this front face point
        const distToMouse = Math.hypot(screenX - mouseX, screenY - mouseY);
        const isTagHovered = z2 > 0.1 && distToMouse < 35;

        return {
          item: p.item,
          screenX,
          screenY,
          z: z2,
          scale,
          isTagHovered,
        };
      });

      // Sort points back-to-front for proper 3D depth rendering
      transformed.sort((a, b) => a.z - b.z);

      // Draw each text label
      transformed.forEach((p) => {
        const { item, screenX, screenY, z, scale, isTagHovered } = p;

        // Proportional font sizing: text length compensation prevents spherical bulging
        let baseFontSize = 13;
        if (item.weight === "large") baseFontSize = 18;
        else if (item.weight === "medium") baseFontSize = 14;
        else baseFontSize = 11;

        if (item.text.length > 20) baseFontSize *= 0.78;
        else if (item.text.length > 14) baseFontSize *= 0.86;

        // Boost font size when tag is directly hovered
        const hoverMultiplier = isTagHovered ? 1.35 : 1.0;
        const fontSize = Math.max(9, baseFontSize * scale * hoverMultiplier * 0.9);
        const fontWeight = isTagHovered ? "900" : item.weight === "large" ? "800" : item.weight === "medium" ? "600" : "500";

        ctx.font = `${fontWeight} ${fontSize}px Inter, system-ui, sans-serif`;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";

        const isLightMode = document.documentElement.classList.contains("light");

        if (isTagHovered) {
          // Hovered tag: Vibrant Cyan in Dark Mode, Electric Purple in Light Mode
          ctx.fillStyle = isLightMode ? "#6D28D9" : "#38BDF8";
          ctx.shadowColor = isLightMode ? "rgba(109, 40, 217, 0.6)" : "#38BDF8";
          ctx.shadowBlur = 24;
        } else if (z > 0) {
          // Front face: Crisp High-Contrast Deep Navy/Purple in Light Mode, Pure White in Dark Mode
          const alpha = Math.min(1.0, 0.75 + z * 0.35);
          if (isLightMode) {
            ctx.fillStyle = `rgba(30, 27, 75, ${alpha})`; // Deep Navy / Dark Purple (#1E1B4B)
            ctx.shadowColor = "rgba(124, 58, 237, 0.25)";
            ctx.shadowBlur = item.weight === "large" ? 10 * scale : 4 * scale;
          } else {
            ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`; // Pure White
            ctx.shadowColor = "rgba(255, 255, 255, 0.9)";
            ctx.shadowBlur = item.weight === "large" ? 14 * scale : 6 * scale;
          }
        } else {
          // Back face (hollow sphere interior): semi-transparent depth fade
          const alpha = Math.max(0.20, 0.50 + z * 0.35);
          if (isLightMode) {
            ctx.fillStyle = `rgba(109, 40, 217, ${alpha * 0.65})`; // Muted Soft Violet
            ctx.shadowColor = "transparent";
            ctx.shadowBlur = 0;
          } else {
            ctx.fillStyle = `rgba(167, 139, 250, ${alpha})`; // Soft Lavender
            ctx.shadowColor = "transparent";
            ctx.shadowBlur = 0;
          }
        }

        ctx.fillText(item.text, screenX, screenY);
      });

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener("resize", handleResize);
      container.removeEventListener("mouseenter", handleMouseEnter);
      container.removeEventListener("mouseleave", handleMouseLeave);
      container.removeEventListener("mousedown", handleMouseDown);
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <div ref={containerRef} className="relative w-full h-[520px] sm:h-[620px] select-none cursor-grab active:cursor-grabbing flex items-center justify-center">
      <canvas ref={canvasRef} className="w-full h-full" />
    </div>
  );
}

/* Interactive 3D Holographic Neon Rupee Component with Mouse Rotation Physics */
function Interactive3DHolographicRupee() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [rotX, setRotX] = useState(0);
  const [rotY, setRotY] = useState(0);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const dx = (e.clientX - centerX) / (window.innerWidth / 2);
      const dy = (e.clientY - centerY) / (window.innerHeight / 2);

      // Interactive 3D parallax tilt & rotation responding smoothly to mouse movement
      setRotX(-dy * 45); // Tilt pitch up/down up to 45 deg
      setRotY(dx * 45);  // Tilt yaw left/right up to 45 deg
    };

    window.addEventListener("mousemove", handleMouseMove);
    return () => window.removeEventListener("mousemove", handleMouseMove);
  }, []);

  return (
    <div
      ref={containerRef}
      className="flex items-center justify-center lg:col-span-5 py-6 select-none cursor-pointer"
      style={{ perspective: "1200px" }}
    >
      <motion.div
        animate={{
          rotateX: rotX,
          rotateY: rotY,
        }}
        transition={{ type: "spring", stiffness: 180, damping: 20 }}
        style={{ transformStyle: "preserve-3d" }}
        className="relative flex h-72 w-72 sm:h-80 sm:w-80 lg:h-96 lg:w-96 items-center justify-center"
      >
        {/* Outer 3D Holographic Orbit Ring 1 (Emerald Green Neon) */}
        <motion.div
          animate={{ rotateZ: 360 }}
          transition={{ duration: 16, repeat: Infinity, ease: "linear" }}
          className="absolute inset-0 rounded-full border-2 border-dashed border-emerald-400/45 shadow-[0_0_35px_rgba(52,211,153,0.35)]"
          style={{ transform: "translateZ(-40px)" }}
        />

        {/* Outer 3D Holographic Orbit Ring 2 (Cyan/Purple Neon) */}
        <motion.div
          animate={{ rotateZ: -360 }}
          transition={{ duration: 11, repeat: Infinity, ease: "linear" }}
          className="absolute inset-5 rounded-full border-2 border-purple-400/35 shadow-[0_0_25px_rgba(168,85,247,0.3)]"
          style={{ transform: "translateZ(-20px) rotateX(55deg)" }}
        />

        {/* Orbiting Satellite Node 1 (Emerald Glow) */}
        <motion.div
          animate={{ rotateZ: 360 }}
          transition={{ duration: 5.5, repeat: Infinity, ease: "linear" }}
          className="absolute inset-0 flex items-center justify-start p-1"
          style={{ transform: "translateZ(30px)" }}
        >
          <div className="h-5 w-5 rounded-full bg-emerald-400 shadow-[0_0_20px_#34D399]" />
        </motion.div>

        {/* Orbiting Satellite Node 2 (Cyan Glow) */}
        <motion.div
          animate={{ rotateZ: -360 }}
          transition={{ duration: 8.5, repeat: Infinity, ease: "linear" }}
          className="absolute inset-0 flex items-end justify-center p-2"
          style={{ transform: "translateZ(45px)" }}
        >
          <div className="h-4 w-4 rounded-full bg-cyan-400 shadow-[0_0_20px_#38BDF8]" />
        </motion.div>

        {/* Deep Volumetric Neon Green Glow Aura */}
        <div className="absolute inset-6 rounded-full bg-emerald-500/25 blur-[100px]" />

        {/* Extruded 3D Holographic Rupee Glyph with Depth Layers */}
        <div
          className="relative font-display text-[150px] sm:text-[200px] lg:text-[250px] font-black leading-none tracking-tighter"
          style={{ transform: "translateZ(60px)" }}
        >
          {/* 3D Depth Layer 3 (Deep Shadow) */}
          <span className="absolute inset-0 translate-x-4 translate-y-4 text-emerald-950/70 blur-xs select-none">
            ₹
          </span>
          {/* 3D Depth Layer 2 (Emerald Extrusion) */}
          <span className="absolute inset-0 translate-x-2 translate-y-2 text-emerald-600/90 select-none">
            ₹
          </span>
          {/* Front Glowing Original 3D Green Neon Gradient Glyph */}
          <span className="relative bg-gradient-to-br from-emerald-300 via-emerald-400 to-teal-300 bg-clip-text text-transparent drop-shadow-[0_0_40px_rgba(52,211,153,0.95)] drop-shadow-[0_0_85px_rgba(16,185,129,0.8)] select-none">
            ₹
          </span>
        </div>
      </motion.div>
    </div>
  );
}

// Sample demo expense messages for live hero interaction
const sampleExpenses = [
  {
    text: "Paid ₹4,000 for dinner at beach shack, split with David & Maya",
    title: "Beach Shack Dinner",
    total: "₹4,000.00",
    category: "Food & Drinks 🍽️",
    payer: "Sarah Menon",
    split: "Equal (3 members)",
    share: "₹1,333.33",
    confidence: 98,
  },
  {
    text: "Uber to Goa airport was 775, I paid, split equally with crew",
    title: "Goa Airport Cab",
    total: "₹775.00",
    category: "Transport 🚕",
    payer: "Sarah Menon",
    split: "Equal (3 members)",
    share: "₹258.33",
    confidence: 96,
  },
  {
    text: "Electricity bill 2,400 paid by David, split 40% Rahul, 35% me, 25% Maya",
    title: "August Electricity Bill",
    total: "₹2,400.00",
    category: "Home Bills 💡",
    payer: "David Rao",
    split: "Percentage (40/35/25)",
    share: "₹840.00 (your 35%)",
    confidence: 94,
  },
];

export function LandingPage({
  onGetStarted,
}: {
  onGetStarted: () => void;
  onLogin?: () => void;
}) {
  // Live Hero AI Demo State
  const [selectedDemo, setSelectedDemo] = useState(0);
  const [isParsing, setIsParsing] = useState(false);

  // FAQ Accordion State
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const handleSelectDemo = (index: number) => {
    setIsParsing(true);
    setSelectedDemo(index);
    setTimeout(() => {
      setIsParsing(false);
    }, 450);
  };

  const activeDemo = sampleExpenses[selectedDemo] || sampleExpenses[0]!;

  const faqs = [
    {
      q: "Do my friends need to create an account to view or approve expenses?",
      a: "No! SplitSmart generates instant web approval links and UPI intent QR codes. Anyone in your WhatsApp or Telegram group can view the breakdown and settle via UPI instantly without registering.",
    },
    {
      q: "How does the AI parse conversational chat messages?",
      a: "SplitSmart reads expense messages using intelligent natural language processing. It automatically identifies who paid, how much was spent, what category it belongs to, and how the bill should be split.",
    },
    {
      q: "What makes SplitSmart zero-dispute compared to spreadsheets?",
      a: "SplitSmart maintains a complete, transparent activity log of every expense added, edited, or settled. Everyone in the group can see exactly how balances were calculated.",
    },
    {
      q: "How does SplitSmart guarantee exact calculations?",
      a: "SplitSmart computes all amounts in exact paise integers rather than rounded decimals, guaranteeing that every single paise is accounted for without rounding discrepancies.",
    },
  ];

  return (
    <div className="relative space-y-16 py-4 sm:py-8">
      {/* 3D Cosmic Space Canvas Covering Entire Landing Page */}
      <CosmicSpace3DBackground />

      {/* HERO CONTAINER: FROSTED GLASSMORPHIC FRAME */}
      <motion.section
        whileHover={{ scale: 1.015, y: -4 }}
        transition={{ duration: 0.15, ease: "easeOut" }}
        className="group cursor-pointer relative overflow-hidden rounded-[32px] border border-purple-500/30 bg-card/65 dark:bg-[#0B0718]/45 backdrop-blur-xl p-6 sm:p-12 shadow-[0_0_60px_rgba(139,92,246,0.18)] transition-all duration-150 ease-out hover:border-purple-400/80 hover:shadow-[0_20px_60px_rgba(139,92,246,0.35)]"
      >
        {/* Top Volumetric Violet Glow Aura */}
        <div
          aria-hidden
          className="pointer-events-none absolute -top-40 left-1/2 h-96 w-[600px] -translate-x-1/2 rounded-full bg-purple-600/15 blur-[140px]"
        />

        <div className="relative z-10 grid gap-10 lg:grid-cols-12 lg:items-center pt-2">
          {/* Left Side: Interactive 3D Holographic Neon Rupee Element */}
          <Interactive3DHolographicRupee />

          {/* Right Side: Text & Primary Hero CTA */}
          <div className="space-y-6 lg:col-span-7 text-left">
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
              className="inline-flex items-center gap-2 rounded-full border border-cyan-500/30 bg-cyan-500/10 px-4 py-1.5 text-xs font-mono font-bold tracking-wider text-cyan-500 dark:text-cyan-300 uppercase shadow-[0_0_20px_rgba(56,189,248,0.25)]"
            >
              <Sparkles size={14} className="text-cyan-500 dark:text-cyan-400" />
              <span>Smart AI-Powered Expense Management</span>
            </motion.div>

            {/* Hero Headline */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.1 }}
            >
              <h1 className="font-display text-4xl font-extrabold tracking-tight text-foreground dark:text-white sm:text-6xl lg:text-7xl leading-[1.08] transition-transform duration-150 group-hover:scale-[1.02]">
                Reconcile{" "}
                <span className="bg-gradient-to-r from-purple-500 via-indigo-400 to-cyan-400 dark:from-purple-400 dark:via-indigo-300 dark:to-cyan-300 bg-clip-text text-transparent underline decoration-cyan-400/60 underline-offset-8">
                  every group
                </span>{" "}
                expense.
              </h1>
            </motion.div>

            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.15 }}
              className="max-w-xl text-base text-muted-foreground dark:text-slate-300 sm:text-lg leading-relaxed font-medium"
            >
              Effortlessly split group bills, track trip expenses, and settle balances via UPI with zero disputes. Simply drop your group chat messages and let AI handle the math.
            </motion.p>

            {/* High-Converting Pill CTA Button */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.2 }}
              className="flex flex-wrap items-center gap-4 pt-2"
            >
              <button
                type="button"
                onClick={onGetStarted}
                className="group/btn flex items-center gap-3 rounded-full bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 px-9 py-4 text-lg font-black text-white shadow-[0_0_30px_rgba(168,85,247,0.5)] transition-all duration-150 ease-out hover:scale-105 hover:shadow-[0_0_40px_rgba(56,189,248,0.7)] active:scale-98 cursor-pointer"
              >
                <span>Get Started Free</span>
                <ArrowRight size={20} className="transition-transform duration-150 group-hover/btn:translate-x-1.5" />
              </button>
            </motion.div>

            {/* Glowing Orb Status Indicators */}
            <div className="flex flex-wrap items-center gap-6 pt-4 text-xs font-semibold text-muted-foreground dark:text-slate-300">
              <span className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-400 shadow-[0_0_10px_#34D399]" />
                Instant Chat Parsing
              </span>
              <span className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-cyan-400 shadow-[0_0_10px_#38BDF8]" />
                1-Tap UPI Settlement
              </span>
              <span className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-purple-400 shadow-[0_0_10px_#C084FC]" />
                Conflict-Free Group Sync
              </span>
            </div>
          </div>
        </div>
      </motion.section>

      {/* 3D HOLLOW PLANET WORD CLOUD: FLOATING DIRECTLY ON 3D COSMIC BACKGROUND */}
      <section className="relative z-10 py-2 flex items-center justify-center pointer-events-auto">
        <RotatingHollowPlanetWordCloud />
      </section>

      {/* ASYMMETRIC SCI-FI SHOWCASE */}
      <motion.section
        whileHover={{ scale: 1.015, y: -4 }}
        transition={{ duration: 0.15, ease: "easeOut" }}
        className="group cursor-pointer relative overflow-hidden rounded-[32px] border border-purple-500/25 bg-card/65 dark:bg-[#0B0718]/45 backdrop-blur-xl p-6 sm:p-12 shadow-xl transition-all duration-150 ease-out hover:border-purple-400/80 hover:shadow-[0_20px_50px_rgba(139,92,246,0.3)]"
      >
        <div className="grid gap-8 lg:grid-cols-12 lg:items-center">
          {/* Left Column: Title & Text */}
          <div className="space-y-4 lg:col-span-6">
            <span className="font-mono text-xs font-bold uppercase tracking-widest text-purple-600 dark:text-cyan-400">
              Smart AI Debt Engine
            </span>
            <h2 className="font-display text-3xl font-extrabold text-foreground dark:text-white sm:text-5xl leading-tight transition-transform duration-150 group-hover:scale-[1.02]">
              Cut Group Debt Chaos by 70%
            </h2>
            <p className="text-sm text-muted-foreground dark:text-slate-300 leading-relaxed max-w-lg">
              Instead of everyone making multiple confusing transfers back and forth, SplitSmart automatically simplifies group debts into the absolute fewest payments needed.
            </p>

            {/* Interactive Sample Chips */}
            <div className="pt-3 space-y-2">
              <p className="text-xs font-mono text-purple-600 dark:text-cyan-400 uppercase tracking-wider">Try AI Parser Demos:</p>
              <div className="flex flex-wrap gap-2">
                {sampleExpenses.map((e, idx) => (
                  <button
                    key={e.title}
                    type="button"
                    onClick={() => handleSelectDemo(idx)}
                    className={`rounded-full border px-4 py-2 text-xs font-bold cursor-pointer transition-all duration-150 ${
                      selectedDemo === idx
                        ? "border-purple-400 bg-purple-600 text-white shadow-[0_0_15px_rgba(168,85,247,0.5)] scale-105"
                        : "border-border dark:border-purple-500/30 bg-secondary/80 dark:bg-slate-900/60 text-foreground dark:text-slate-300 hover:border-purple-400 hover:scale-105"
                    }`}
                  >
                    ΓÜí {e.title}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Right Column: Dark Sci-Fi Terminal Container */}
          <div className="lg:col-span-6">
            <div className="relative overflow-hidden rounded-3xl border border-purple-500/30 bg-card/85 dark:bg-[#0B061A]/85 p-6 shadow-[0_0_40px_rgba(139,92,246,0.15)]">
              {/* Terminal Header */}
              <div className="flex items-center justify-between border-b border-border/50 dark:border-purple-500/20 pb-4">
                <div className="flex items-center gap-2">
                  <div className="flex h-3.5 w-3.5 items-center justify-center rounded-full bg-purple-400 text-slate-950 shadow-[0_0_8px_#C084FC] transition-transform duration-150 hover:scale-110" title="Close">
                    <X size={8} strokeWidth={3.5} />
                  </div>
                  <div className="flex h-3.5 w-3.5 items-center justify-center rounded-full bg-cyan-400 text-slate-950 shadow-[0_0_8px_#38BDF8] transition-transform duration-150 hover:scale-110" title="Minimize">
                    <Minus size={8} strokeWidth={3.5} />
                  </div>
                  <div className="flex h-3.5 w-3.5 items-center justify-center rounded-full bg-emerald-400 text-slate-950 shadow-[0_0_8px_#34D399] transition-transform duration-150 hover:scale-110" title="Maximize">
                    <Plus size={8} strokeWidth={3.5} />
                  </div>
                </div>
                <span className="font-mono text-xs font-bold text-purple-600 dark:text-cyan-400 uppercase tracking-wider">
                  AI Parsing Engine
                </span>
              </div>

              {/* Terminal Content */}
              <div className="mt-4 space-y-3">
                <div className="rounded-2xl border border-border dark:border-purple-500/20 bg-secondary/60 dark:bg-[#060310]/90 p-4 text-xs font-mono text-foreground dark:text-slate-300">
                  <span className="text-purple-600 dark:text-cyan-400">&gt; Input:</span> "{activeDemo.text}"
                </div>

                <AnimatePresence mode="wait">
                  <motion.div
                    key={selectedDemo}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.15 }}
                    className="space-y-2.5"
                  >
                    <div className="flex items-center justify-between rounded-xl border border-border dark:border-purple-500/25 bg-secondary/40 dark:bg-purple-950/30 p-3">
                      <span className="text-xs font-bold text-muted-foreground dark:text-slate-300">Title</span>
                      <span className="font-mono text-xs font-bold text-purple-600 dark:text-cyan-300">{activeDemo.title}</span>
                    </div>

                    <div className="flex items-center justify-between rounded-xl border border-border dark:border-purple-500/25 bg-secondary/40 dark:bg-purple-950/30 p-3">
                      <span className="text-xs font-bold text-muted-foreground dark:text-slate-300">Parsed Total</span>
                      <span className="font-mono text-sm font-black text-foreground dark:text-white">{activeDemo.total}</span>
                    </div>

                    <div className="flex items-center justify-between rounded-xl border border-border dark:border-purple-500/25 bg-secondary/40 dark:bg-purple-950/30 p-3">
                      <span className="text-xs font-bold text-muted-foreground dark:text-slate-300">Your Share</span>
                      <span className="font-mono text-sm font-black text-purple-600 dark:text-cyan-300">{activeDemo.share}</span>
                    </div>
                  </motion.div>
                </AnimatePresence>

                <button
                  type="button"
                  onClick={onGetStarted}
                  className="mt-3 w-full rounded-2xl bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 py-3 text-xs font-black text-white shadow-[0_0_20px_rgba(168,85,247,0.5)] transition-all duration-150 hover:scale-102 cursor-pointer"
                >
                  Parse Expenses Free &rarr;
                </button>
              </div>
            </div>
          </div>
        </div>
      </motion.section>

      {/* HOW IT WORKS: 3 INTERACTIVE POPPING CARDS */}
      <section className="space-y-8">
        <div className="text-center space-y-2">
          <SectionLabel>Frictionless Group Expense Management</SectionLabel>
          <h2 className="font-display text-3xl font-extrabold text-foreground dark:text-white sm:text-4xl">
            How SplitSmart Works in 3 Steps
          </h2>
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          {[
            {
              step: "01",
              title: "Paste Chat or Message",
              desc: "Copy raw WhatsApp or Telegram group messages like 'Paid 3,500 for dinner, split with David & Sarah'. Smart AI extracts the details instantly.",
              icon: MessageSquare,
            },
            {
              step: "02",
              title: "Instant Draft & Consensus",
              desc: "SplitSmart creates an expense draft for group review. Members can quickly verify or adjust splits with zero calculation confusion.",
              icon: Layers,
            },
            {
              step: "03",
              title: "One-Tap UPI Settlement",
              desc: "Intelligent debt minimization reduces total group transfers to the absolute fewest payments needed. Settle up instantly via UPI QR.",
              icon: Wallet,
            },
          ].map((s) => {
            const Icon = s.icon;
            return (
              <motion.div
                key={s.step}
                whileHover={{ scale: 1.04, y: -6 }}
                transition={{ duration: 0.15, ease: "easeOut" }}
                className="group cursor-pointer rounded-[28px] border border-border dark:border-purple-500/25 bg-card/75 dark:bg-[#0B0718]/45 backdrop-blur-xl p-7 relative overflow-hidden transition-all duration-150 ease-out hover:border-purple-400 hover:shadow-[0_20px_40px_rgba(139,92,246,0.3)]"
              >
                <div className="flex items-center justify-between">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-purple-500/15 text-purple-600 dark:text-cyan-400 font-extrabold shadow-sm transition-transform duration-150 group-hover:scale-115">
                    <Icon size={24} />
                  </div>
                  <span className="font-display text-4xl font-black text-purple-500/20 dark:text-cyan-400/20 transition-transform duration-150 group-hover:scale-110 group-hover:text-purple-500/40">{s.step}</span>
                </div>
                <h3 className="font-display mt-5 text-xl font-bold text-foreground dark:text-white transition-colors duration-150 group-hover:text-purple-600 dark:group-hover:text-cyan-300">{s.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground dark:text-slate-300 leading-relaxed">{s.desc}</p>
              </motion.div>
            );
          })}
        </div>
      </section>

      {/* FEATURE HIGHLIGHT GRID WITH POPPING HOVER CARDS */}
      <section className="space-y-6">
        <div className="text-center space-y-2">
          <SectionLabel>Designed for Effortless Expense Sharing</SectionLabel>
          <h2 className="font-display text-3xl font-extrabold text-foreground dark:text-white sm:text-4xl">
            Why Groups Love SplitSmart
          </h2>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {[
            {
              icon: Cpu,
              title: "Smart AI Chat Parser",
              desc: "Parses raw WhatsApp and Telegram messages into structured group expense drafts in seconds.",
            },
            {
              icon: Layers,
              title: "Transparent Audit History",
              desc: "Every expense addition or edit is tracked in an explicit history, preventing accidental overwrites or confusion.",
            },
            {
              icon: BarChart3,
              title: "Smart Debt Minimization",
              desc: "Automatically combines group balances to minimize the total number of transfers needed between friends.",
            },
            {
              icon: Wallet,
              title: "Direct UPI QR Settlement",
              desc: "Generates instant, scannable UPI QR codes and payment links for quick 1-tap mobile settlements.",
            },
            {
              icon: ShieldCheck,
              title: "100% Accurate Paise Calculation",
              desc: "Calculates exact shares down to the paise, ensuring nobody pays a single fraction more than owed.",
            },
            {
              icon: Users,
              title: "Conflict-Free Group Sync",
              desc: "Version-tracked drafts ensure members never accidentally overwrite each other's edits during consensus.",
            },
          ].map((f) => {
            const Icon = f.icon;
            return (
              <motion.div
                key={f.title}
                whileHover={{ scale: 1.04, y: -6 }}
                transition={{ duration: 0.15, ease: "easeOut" }}
                className="group cursor-pointer rounded-[28px] border border-border dark:border-purple-500/25 bg-card/75 dark:bg-[#0B0718]/45 backdrop-blur-xl p-6 transition-all duration-150 ease-out hover:border-purple-400 hover:shadow-[0_20px_40px_rgba(139,92,246,0.3)]"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-purple-500/15 text-purple-600 dark:text-cyan-400 shadow-sm transition-transform duration-150 group-hover:scale-115">
                  <Icon size={24} />
                </div>
                <h3 className="font-display mt-4 text-xl font-bold text-foreground dark:text-white transition-colors duration-150 group-hover:text-purple-600 dark:group-hover:text-cyan-300">{f.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground dark:text-slate-300 leading-relaxed">{f.desc}</p>
              </motion.div>
            );
          })}
        </div>
      </section>

      {/* FREQUENTLY ASKED QUESTIONS */}
      <section className="space-y-6">
        <div className="text-center space-y-2">
          <SectionLabel>Have Questions?</SectionLabel>
          <h2 className="font-display text-3xl font-extrabold text-foreground dark:text-white sm:text-4xl">
            Frequently Asked Questions
          </h2>
        </div>

        <div className="max-w-3xl mx-auto space-y-3">
          {faqs.map((faq, idx) => {
            const isOpen = openFaq === idx;
            return (
              <motion.div
                key={faq.q}
                whileHover={{ scale: 1.01 }}
                transition={{ duration: 0.15, ease: "easeOut" }}
                className="rounded-2xl border border-border dark:border-purple-500/25 bg-card/75 dark:bg-[#0B0718]/45 backdrop-blur-xl overflow-hidden transition-all duration-150 hover:border-purple-400 cursor-pointer"
              >
                <button
                  type="button"
                  onClick={() => setOpenFaq(isOpen ? null : idx)}
                  className="w-full flex items-center justify-between p-5 text-left text-sm font-bold text-foreground dark:text-white hover:text-purple-600 dark:hover:text-cyan-300 transition-colors cursor-pointer"
                >
                  <span className="flex items-center gap-3">
                    <HelpCircle size={16} className="text-purple-600 dark:text-cyan-400 shrink-0" />
                    {faq.q}
                  </span>
                  <ChevronDown
                    size={16}
                    className={`transition-transform duration-150 text-muted-foreground dark:text-slate-400 ${
                      isOpen ? "rotate-180 text-purple-600 dark:text-cyan-300" : ""
                    }`}
                  />
                </button>
                {isOpen && (
                  <div className="px-5 pb-5 text-xs text-muted-foreground dark:text-slate-300 leading-relaxed border-t border-border/50 dark:border-purple-500/20 pt-3">
                    {faq.a}
                  </div>
                )}
              </motion.div>
            );
          })}
        </div>
      </section>

      {/* FINAL HIGH-CONVERTING CTA BANNER */}
      <motion.section
        whileHover={{ scale: 1.015, y: -4 }}
        transition={{ duration: 0.15, ease: "easeOut" }}
        className="group cursor-pointer rounded-[32px] p-10 sm:p-14 text-center border border-purple-500/30 bg-card/75 dark:bg-[#0B0718]/45 backdrop-blur-xl shadow-[0_0_60px_rgba(139,92,246,0.18)] transition-all duration-150 ease-out hover:border-purple-400 hover:shadow-[0_20px_50px_rgba(139,92,246,0.35)] relative overflow-hidden"
      >
        <div
          aria-hidden
          className="pointer-events-none absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-purple-600/20 blur-3xl"
        />
        <div className="relative z-10 space-y-4 max-w-2xl mx-auto">
          <h2 className="font-display text-3xl font-extrabold text-foreground dark:text-white sm:text-4xl transition-transform duration-150 group-hover:scale-102">
            Ready for frictionless group expense reconciliation?
          </h2>
          <p className="text-sm text-muted-foreground dark:text-slate-300 leading-relaxed">
            Start managing trip budgets, flatmate bills, and team payments with zero disputes and instant UPI payments.
          </p>
          <div className="pt-4">
            <button
              type="button"
              onClick={onGetStarted}
              className="inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 px-9 py-4 text-base font-extrabold text-white shadow-[0_0_30px_rgba(168,85,247,0.5)] transition-all duration-150 ease-out hover:scale-105 hover:shadow-[0_0_40px_rgba(56,189,248,0.7)] cursor-pointer"
            >
              <span>Get Started Free</span>
              <ArrowRight size={18} />
            </button>
          </div>
        </div>
      </motion.section>
    </div>
  );
}
