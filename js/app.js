/**
 * LUNE WEB - JavaScript
 * Theme switcher (Warm Coffee / Espresso), GitHub API release loader,
 * Smooth Animated Canvas Waves, and Donation Clipboard handler.
 */

(function () {
  'use strict';

  /* ==========================================================================
     1. THEME SWITCHER (Light / Dark)
     ========================================================================== */
  const themeToggleBtn = document.getElementById('theme-toggle');
  const htmlRoot = document.documentElement;

  function getPreferredTheme() {
    const savedTheme = localStorage.getItem('lune_theme');
    if (savedTheme) {
      return savedTheme;
    }
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches
      ? 'light'
      : 'dark';
  }

  function setTheme(theme) {
    htmlRoot.setAttribute('data-theme', theme);
    localStorage.setItem('lune_theme', theme);
    if (window.updateWaveColors) {
      window.updateWaveColors();
    }
  }

  // Initialize theme
  const initialTheme = getPreferredTheme();
  setTheme(initialTheme);

  // Toggle theme button
  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      const currentTheme = htmlRoot.getAttribute('data-theme') || 'dark';
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      setTheme(newTheme);
    });
  }

  // Listen to system changes if user hasn't overridden
  if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (!localStorage.getItem('lune_theme')) {
        setTheme(e.matches ? 'dark' : 'light');
      }
    });
  }

  /* ==========================================================================
     2. GITHUB RELEASES DYNAMIC FETCHER (English format)
     ========================================================================== */
  const GITHUB_REPO = 'MrDemonc/Lune';
  const API_URL = `https://api.github.com/repos/${GITHUB_REPO}/releases/latest`;

  const versionEl = document.getElementById('release-version');
  const dateEl = document.getElementById('release-date');
  const notesListEl = document.getElementById('release-notes-list');
  const downloadBtn = document.getElementById('download-apk-btn');
  const apkSizeLabel = document.getElementById('apk-size-label');

  function formatDate(isoString) {
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch (e) {
      return isoString;
    }
  }

  function formatBytes(bytes) {
    if (!bytes || bytes === 0) return '';
    const mb = bytes / (1024 * 1024);
    return `(${mb.toFixed(1)} MB)`;
  }

  async function fetchLatestRelease() {
    try {
      const response = await fetch(API_URL);
      if (!response.ok) return;

      const data = await response.json();

      // 1. Version tag
      if (data.tag_name && versionEl) {
        versionEl.textContent = data.tag_name.startsWith('v') ? data.tag_name : `v${data.tag_name}`;
      }

      // 2. Published Date
      const publishedDate = data.published_at || data.created_at;
      if (publishedDate && dateEl) {
        dateEl.textContent = formatDate(publishedDate);
        dateEl.setAttribute('datetime', publishedDate);
      }

      // 3. Release Notes bullets
      if (data.body && notesListEl) {
        const lines = data.body
          .split('\n')
          .map(line => line.trim())
          .filter(line => line.length > 0)
          .map(line => line.replace(/^[-*•]\s*/, '').trim());

        if (lines.length > 0) {
          notesListEl.innerHTML = '';
          lines.forEach(item => {
            if (item) {
              const li = document.createElement('li');
              li.textContent = item;
              notesListEl.appendChild(li);
            }
          });
        }
      }

      // 4. Direct APK Download link and size
      if (data.assets && data.assets.length > 0 && downloadBtn) {
        const apkAsset = data.assets.find(asset => asset.name.endsWith('.apk')) || data.assets[0];
        if (apkAsset) {
          downloadBtn.href = apkAsset.browser_download_url;
          if (apkSizeLabel && apkAsset.size) {
            apkSizeLabel.textContent = `Direct download ${formatBytes(apkAsset.size)}`;
          }
        }
      }
    } catch (err) {
      console.warn('Could not load latest release dynamically, using static fallback.', err);
    }
  }

  fetchLatestRelease();

  /* Dynamic GitHub Contributors Fetcher */
  async function fetchContributors() {
    const contributorsListEl = document.getElementById('contributors-list');
    if (!contributorsListEl) return;

    try {
      const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/contributors`);
      if (!response.ok) return;

      const contributors = await response.json();
      if (!Array.isArray(contributors) || contributors.length === 0) return;

      // Clear static list to render fresh data from API + Desukia UX credit
      contributorsListEl.innerHTML = '';

      // 1. MrDemonc (Creator)
      const creator = contributors.find(c => c.login.toLowerCase() === 'mrdemonc') || {
        login: 'MrDemonc',
        html_url: 'https://github.com/MrDemonc',
        avatar_url: 'https://avatars.githubusercontent.com/u/182368138?v=4'
      };

      const creatorCard = document.createElement('a');
      creatorCard.href = creator.html_url;
      creatorCard.target = '_blank';
      creatorCard.rel = 'noopener noreferrer';
      creatorCard.className = 'contributor-card';
      creatorCard.innerHTML = `
        <img src="${creator.avatar_url}" alt="${creator.login}" class="contributor-avatar" loading="lazy">
        <div class="contributor-details">
          <span class="contributor-name">${creator.login}</span>
          <span class="contributor-role-badge badge-creator">Creator & Lead Dev</span>
        </div>
      `;
      contributorsListEl.appendChild(creatorCard);

      // 2. Desukia (Design & UX Testing)
      const desukiaCard = document.createElement('div');
      desukiaCard.className = 'contributor-card';
      desukiaCard.innerHTML = `
        <div class="contributor-avatar avatar-text">D</div>
        <div class="contributor-details">
          <span class="contributor-name">Desukia</span>
          <span class="contributor-role-badge badge-design">Design & UX Testing</span>
        </div>
      `;
      contributorsListEl.appendChild(desukiaCard);

      // 3. All other contributors
      contributors
        .filter(c => c.login.toLowerCase() !== 'mrdemonc')
        .forEach(contributor => {
          const card = document.createElement('a');
          card.href = contributor.html_url;
          card.target = '_blank';
          card.rel = 'noopener noreferrer';
          card.className = 'contributor-card';
          card.innerHTML = `
            <img src="${contributor.avatar_url}" alt="${contributor.login}" class="contributor-avatar" loading="lazy">
            <div class="contributor-details">
              <span class="contributor-name">${contributor.login}</span>
              <span class="contributor-role-badge">Contributor</span>
            </div>
          `;
          contributorsListEl.appendChild(card);
        });
    } catch (err) {
      console.warn('Using static contributors list.', err);
    }
  }

  fetchContributors();

  /* ==========================================================================
     3. MONERO COPY TO CLIPBOARD & TOAST
     ========================================================================== */
  const copyMoneroBtn = document.getElementById('copy-monero-btn');
  const moneroAddressInput = document.getElementById('monero-address');
  const toast = document.getElementById('toast');
  let toastTimer = null;

  function showToast(message) {
    if (!toast) return;
    if (message) {
      const textSpan = toast.querySelector('span');
      if (textSpan) textSpan.textContent = message;
    }
    toast.classList.add('show');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toast.classList.remove('show');
    }, 3200);
  }

  if (copyMoneroBtn && moneroAddressInput) {
    copyMoneroBtn.addEventListener('click', async () => {
      const address = moneroAddressInput.value;
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(address);
        } else {
          // Fallback for older browsers
          moneroAddressInput.type = 'text';
          moneroAddressInput.select();
          document.execCommand('copy');
          moneroAddressInput.type = 'hidden';
        }
        showToast('Monero address copied to clipboard!');
      } catch (err) {
        prompt('Monero Address (Ctrl+C to copy):', address);
      }
    });
  }

  /* ==========================================================================
     4. ANIMATED WAVE CANVAS (Warm Coffee / Cream sinusoidal waves)
     ========================================================================== */
  const canvas = document.getElementById('wave-canvas');
  if (canvas) {
    const ctx = canvas.getContext('2d');
    let width = 0;
    let height = 0;
    let step = 0;

    function resizeCanvas() {
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    }

    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();

    function getThemeWaveColors() {
      const computedStyle = getComputedStyle(document.documentElement);
      return [
        computedStyle.getPropertyValue('--wave-color-1').trim() || 'rgba(217, 153, 98, 0.14)',
        computedStyle.getPropertyValue('--wave-color-2').trim() || 'rgba(184, 117, 58, 0.10)',
        computedStyle.getPropertyValue('--wave-color-3').trim() || 'rgba(142, 87, 39, 0.08)'
      ];
    }

    let waveColors = getThemeWaveColors();

    window.updateWaveColors = function () {
      waveColors = getThemeWaveColors();
    };

    // Wave configurations [frequency, amplitude, speed, yOffsetPercent]
    const waves = [
      { freq: 0.0035, amp: 45, speed: 0.015, offset: 0.68 },
      { freq: 0.0055, amp: 35, speed: -0.02, offset: 0.72 },
      { freq: 0.0025, amp: 55, speed: 0.012, offset: 0.76 }
    ];

    function drawWave(freq, amp, speed, offsetRatio, color) {
      ctx.fillStyle = color;
      ctx.beginPath();
      
      const baseY = height * offsetRatio;
      ctx.moveTo(0, height);

      for (let x = 0; x <= width; x += 10) {
        const y = baseY + Math.sin(x * freq + step * speed * 2) * amp + Math.cos(x * freq * 0.5 + step * speed) * (amp * 0.4);
        ctx.lineTo(x, y);
      }

      ctx.lineTo(width, height);
      ctx.closePath();
      ctx.fill();
    }

    function animate() {
      ctx.clearRect(0, 0, width, height);
      step += 1;

      for (let i = 0; i < waves.length; i++) {
        const w = waves[i];
        const color = waveColors[i] || waveColors[0];
        drawWave(w.freq, w.amp, w.speed, w.offset, color);
      }

      requestAnimationFrame(animate);
    }

    animate();
  }

})();
