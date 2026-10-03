// ===================================================
// PENCIL 2D - DRAW & ANIMATIONS STUDIO JAVASCRIPT
// Comprehensive 2D frame-by-frame animation engine
// ===================================================

(function() {
  'use strict';

  // State
  const State = {
    projects: [],
    currentProject: null,
    currentFrameIndex: 0,
    currentTool: 'brush', // 'brush', 'pencil', 'eraser', 'bucket'
    currentColor: '#1F1F1F',
    brushSize: 10,
    onionSkinEnabled: true,
    showGrid: false,
    isPlaying: false,
    isLooping: true,
    zoomLevel: 1.0,
    activeView: 'gallery', // 'gallery' | 'studio'
    activeFilter: 'all',   // 'all' | 'image' | 'video'
    
    // Playback
    playbackTimer: null,
    playbackIndex: 0,

    // Drawing
    isDrawing: false,
    points: [],
    undoStack: [],
    redoStack: [],

    // Hover preview timers
    hoverTimers: {}
  };

  const CanvasRatios = {
    '1:1': { w: 720, h: 720, css: 'ratio-1-1', label: '1:1 Square' },
    '9:16': { w: 450, h: 800, css: 'ratio-9-16', label: '9:16 Vertical' },
    '16:9': { w: 800, h: 450, css: 'ratio-16-9', label: '16:9 Cinema' },
    '4:3': { w: 720, h: 540, css: 'ratio-4-3', label: '4:3 Classic' },
    '4:5': { w: 600, h: 750, css: 'ratio-4-5', label: '4:5 Portrait' }
  };

  // DOM Elements
  const DOM = {
    // Navigation & Header
    navTabGallery: document.getElementById('nav-tab-gallery'),
    navTabStudio: document.getElementById('nav-tab-studio'),
    viewGallery: document.getElementById('view-gallery'),
    viewStudio: document.getElementById('view-studio'),
    headerStudioControls: document.getElementById('header-studio-controls'),
    headerProjectName: document.getElementById('header-project-name'),
    headerProjectTitleText: document.getElementById('header-project-title-text'),
    btnBrandHome: document.getElementById('btn-brand-home'),
    
    // Quick Tools
    btnHeaderUndo: document.getElementById('btn-header-undo'),
    btnHeaderRedo: document.getElementById('btn-header-redo'),
    btnHeaderOnion: document.getElementById('btn-header-onion'),
    btnHeaderGrid: document.getElementById('btn-header-grid'),
    btnModeDesktop: document.getElementById('btn-mode-desktop'),
    btnModeMobile: document.getElementById('btn-mode-mobile'),
    btnHeaderNewProject: document.getElementById('btn-header-new-project'),
    btnHeaderExport: document.getElementById('btn-header-export'),
    btnHeaderSavePhoto: document.getElementById('btn-header-save-photo'),

    // Gallery
    projectsGrid: document.getElementById('projects-grid'),
    statProjectCount: document.getElementById('stat-project-count'),
    filterBtns: document.querySelectorAll('.filter-tab-btn'),

    // Import Photo
    btnImportPhoto: document.getElementById('btn-import-photo'),
    inputImportPhotoFile: document.getElementById('input-import-photo-file'),

    // Studio Tools
    toolButtons: document.querySelectorAll('.tool-btn'),
    brushSizeSlider: document.getElementById('brush-size-slider'),
    brushSizeText: document.getElementById('brush-size-text'),
    brushPreviewDot: document.getElementById('brush-preview-dot'),
    nativeColorInput: document.getElementById('native-color-input'),
    customColorPreview: document.getElementById('custom-color-preview'),
    swatchButtons: document.querySelectorAll('.swatch-btn'),
    btnClearCanvas: document.getElementById('btn-clear-canvas'),
    btnStudioImportPhoto: document.getElementById('btn-studio-import-photo'),
    inputStudioImportPhotoFile: document.getElementById('input-studio-import-photo-file'),

    // Canvas Stage
    canvasPaperCard: document.getElementById('canvas-paper-card'),
    canvasGridOverlay: document.getElementById('canvas-grid-overlay'),
    onionCanvas: document.getElementById('onion-canvas'),
    bgCanvas: document.getElementById('bg-canvas'),
    drawingCanvas: document.getElementById('drawing-canvas'),
    canvasRatioBadge: document.getElementById('canvas-ratio-badge'),
    frameCounterBadge: document.getElementById('frame-counter-badge'),
    zoomLevelText: document.getElementById('zoom-level-text'),
    btnZoomIn: document.getElementById('btn-zoom-in'),
    btnZoomOut: document.getElementById('btn-zoom-out'),
    btnZoomReset: document.getElementById('btn-zoom-reset'),
    centerPlayOverlay: document.getElementById('center-play-overlay'),
    btnCenterPlay: document.getElementById('btn-center-play'),
    centerPlayIcon: document.getElementById('center-play-icon'),
    centerPauseIcon: document.getElementById('center-pause-icon'),

    // Right Sidebar
    ratioOptionButtons: document.querySelectorAll('.ratio-option-btn'),
    toggleOnionCheckbox: document.getElementById('toggle-onion-checkbox'),
    btnExportTriggerSide: document.getElementById('btn-export-trigger-side'),
    btnSavePhotoSide: document.getElementById('btn-save-photo-side'),

    // Timeline Dock & Photo Dock
    studioPhotoDock: document.getElementById('studio-photo-dock'),
    studioTimelineDock: document.getElementById('studio-timeline-dock'),
    btnPhotoDockImport: document.getElementById('btn-photo-dock-import'),
    btnPhotoDockSave: document.getElementById('btn-photo-dock-save'),
    btnPhotoConvertAnim: document.getElementById('btn-photo-convert-anim'),
    timelinePlayBtn: document.getElementById('timeline-play-btn'),
    timePlayIcon: document.getElementById('time-play-icon'),
    timePauseIcon: document.getElementById('time-pause-icon'),
    btnLoopToggle: document.getElementById('btn-loop-toggle'),
    fpsChips: document.querySelectorAll('.fps-chip'),
    btnDuplicateFrame: document.getElementById('btn-duplicate-frame'),
    btnDeleteFrame: document.getElementById('btn-delete-frame'),
    timelineFramesScroll: document.getElementById('timeline-frames-scroll'),
    btnAddFramePlus: document.getElementById('btn-add-frame-plus'),

    // Modals
    exportModal: document.getElementById('export-modal'),
    btnCloseExport: document.getElementById('btn-close-export'),
    btnCancelExportModal: document.getElementById('btn-cancel-export-modal'),
    btnConfirmExport: document.getElementById('btn-confirm-export'),
    btnExportMp4: document.getElementById('btn-export-mp4'),
    btnExportPng: document.getElementById('btn-export-png'),
    btnExportGif: document.getElementById('btn-export-gif'),
    exportProgressWrap: document.getElementById('export-progress-wrap'),
    exportProgressBar: document.getElementById('export-progress-bar'),
    exportStatusLabel: document.getElementById('export-status-label'),

    newProjectModal: document.getElementById('new-project-modal'),
    btnCloseNewProject: document.getElementById('btn-close-new-project'),
    btnCancelNewProject: document.getElementById('btn-cancel-new-project'),
    btnSubmitNewProject: document.getElementById('btn-submit-new-project'),
    inputNewProjectTitle: document.getElementById('input-new-project-title'),

    toastNotification: document.getElementById('toast-notification')
  };

  // Canvas Contexts
  const ctx = {
    onion: DOM.onionCanvas.getContext('2d'),
    bg: DOM.bgCanvas.getContext('2d'),
    drawing: DOM.drawingCanvas.getContext('2d')
  };

  // ==================== INITIALIZATION ====================
  function init() {
    createDefaultNotebookProjects();
    renderGallery();
    setupEventListeners();
    setupShortcuts();

    // Default open Project 1
    if (State.projects.length > 0) {
      State.currentProject = State.projects[0];
      State.currentFrameIndex = 0;
    }
  }

  // Toast Notification Helper
  function showToast(message) {
    DOM.toastNotification.textContent = message;
    DOM.toastNotification.classList.add('show');
    setTimeout(() => {
      DOM.toastNotification.classList.remove('show');
    }, 2400);
  }

  // ==================== SWITCH VIEWS ====================
  function switchView(viewName) {
    State.activeView = viewName;
    if (viewName === 'gallery') {
      DOM.viewGallery.classList.add('active');
      DOM.viewStudio.classList.remove('active');
      DOM.navTabGallery.classList.add('active');
      DOM.navTabStudio.classList.remove('active');
      DOM.headerStudioControls.style.display = 'none';
      DOM.headerProjectName.style.display = 'none';
      DOM.btnHeaderExport.style.display = 'none';
      if (DOM.btnHeaderSavePhoto) DOM.btnHeaderSavePhoto.style.display = 'none';
      stopPlayback();
      renderGallery();
    } else {
      DOM.viewGallery.classList.remove('active');
      DOM.viewStudio.classList.add('active');
      DOM.navTabGallery.classList.remove('active');
      DOM.navTabStudio.classList.add('active');
      DOM.headerStudioControls.style.display = 'flex';
      DOM.headerProjectName.style.display = 'flex';
      const isPhoto = State.currentProject && State.currentProject.type === 'image';
      DOM.btnHeaderExport.style.display = isPhoto ? 'none' : 'flex';
      if (DOM.btnHeaderSavePhoto) DOM.btnHeaderSavePhoto.style.display = isPhoto ? 'inline-flex' : 'none';
      if (DOM.btnHeaderOnion) DOM.btnHeaderOnion.style.display = isPhoto ? 'none' : 'flex';
      if (State.currentProject) {
        DOM.headerProjectTitleText.textContent = State.currentProject.title;
        applyProjectToStudio();
      }
    }
  }

  // ==================== SAMPLE PROJECTS (1:1 with App Store) ====================
  function createDefaultNotebookProjects() {
    State.projects = [
      createProject1StickmanSoccer(),
      createProject2ChartPresentation(),
      createProject3BoyWithHair(),
      createProject4BoyAndDog(),
      createProject5HeartBalloon(),
      createProject6Campfire(),
      createProject7CuteBug(),
      createProject8PeekingStickman()
    ];
    DOM.statProjectCount.textContent = State.projects.length;
  }

  // Project 1: Stickman Soccer (4 Frames)
  function createProject1StickmanSoccer() {
    const p = { id: 'p1', title: 'Stickman Soccer', ratio: '1:1', fps: 8, type: 'animation', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 4; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.lineJoin = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 14;
      // Head
      g.beginPath(); g.arc(360, 220, 60, 0, Math.PI * 2); g.stroke();
      g.lineWidth = 7;
      g.beginPath(); g.arc(360, 220, 32, 0.1 * Math.PI, 0.9 * Math.PI); g.stroke();
      g.fillStyle = '#1F1F1F';
      g.beginPath(); g.arc(340, 205, 6, 0, Math.PI * 2); g.arc(380, 205, 6, 0, Math.PI * 2); g.fill();
      // Body
      g.lineWidth = 14;
      g.beginPath(); g.moveTo(360, 280); g.lineTo(360, 440); g.stroke();
      // Arms
      g.beginPath(); g.moveTo(360, 310); g.lineTo(290, 370);
      g.moveTo(360, 310); g.lineTo(430, 350 - f * 8); g.stroke();
      // Stand Leg
      g.beginPath(); g.moveTo(360, 440); g.lineTo(315, 580); g.stroke();
      // Kick Leg
      const kickX = 360 + 60 + f * 35;
      const kickY = 440 + 140 - f * 45;
      g.beginPath(); g.moveTo(360, 440); g.lineTo(kickX, kickY); g.stroke();
      // Soccer Ball
      const ballX = 460 + f * 28;
      const ballY = 560 - f * 38 + (f * f * 2.5);
      g.fillStyle = '#FFFFFF';
      g.beginPath(); g.arc(ballX, ballY, 30, 0, Math.PI * 2); g.fill();
      g.lineWidth = 6; g.stroke();
      g.fillStyle = '#1F1F1F';
      g.beginPath(); g.arc(ballX, ballY, 12, 0, Math.PI * 2); g.fill();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // Project 2: Business Chart (3 Frames)
  function createProject2ChartPresentation() {
    const p = { id: 'p2', title: 'Chart Presentation', ratio: '1:1', fps: 8, type: 'animation', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 3; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.lineJoin = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 10;
      g.strokeRect(140, 160, 260, 200);
      g.beginPath(); g.moveTo(180, 320); g.lineTo(220, 280); g.lineTo(260, 300); g.lineTo(340, 200 - f * 10); g.stroke();
      g.beginPath(); g.moveTo(270, 360); g.lineTo(220, 520); g.moveTo(270, 360); g.lineTo(320, 520); g.stroke();
      g.beginPath(); g.arc(520, 240, 50, 0, Math.PI * 2); g.stroke();
      g.beginPath(); g.moveTo(520, 290); g.lineTo(520, 460); g.stroke();
      g.beginPath(); g.moveTo(520, 330); g.lineTo(440, 320); g.lineTo(350, 200 + f * 15); g.stroke();
      g.beginPath(); g.moveTo(520, 460); g.lineTo(480, 580); g.moveTo(520, 460); g.lineTo(560, 580); g.stroke();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // Project 3: Boy with messy hair (Photo/Drawing Project)
  function createProject3BoyWithHair() {
    const p = { id: 'p3', title: 'Boy Portrait', ratio: '1:1', fps: 1, type: 'image', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 2; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 12;
      g.beginPath(); g.arc(360, 340, 90, 0, Math.PI * 2); g.stroke();
      g.fillStyle = '#1F1F1F';
      g.beginPath(); g.moveTo(260, 300); g.quadraticCurveTo(360, 180 + f * 8, 460, 300); g.fill();
      g.beginPath(); g.arc(330, 330, 8, 0, Math.PI * 2); g.arc(390, 330, 8, 0, Math.PI * 2); g.fill();
      g.lineWidth = 6;
      g.beginPath(); g.arc(360, 350, 40, 0.1 * Math.PI, 0.9 * Math.PI); g.stroke();
      g.lineWidth = 12;
      g.beginPath(); g.moveTo(360, 430); g.lineTo(360, 520); g.moveTo(240, 560); g.quadraticCurveTo(360, 500, 480, 560); g.stroke();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // Project 4: Boy throwing ball to dog
  function createProject4BoyAndDog() {
    const p = { id: 'p4', title: 'Boy and Puppy', ratio: '1:1', fps: 8, type: 'animation', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 3; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 10;
      g.beginPath(); g.arc(260, 260, 50, 0, Math.PI * 2); g.stroke();
      g.beginPath(); g.moveTo(260, 310); g.lineTo(260, 460); g.stroke();
      g.beginPath(); g.moveTo(260, 350); g.lineTo(340 + f * 10, 310 - f * 8); g.stroke();
      g.beginPath(); g.arc(360 + f * 45, 290 - f * 15, 14, 0, Math.PI * 2); g.fillStyle = '#FB8C00'; g.fill(); g.stroke();
      g.fillStyle = '#1F1F1F';
      g.beginPath(); g.arc(520, 430 - (f === 2 ? 20 : 0), 25, 0, Math.PI * 2); g.stroke();
      g.strokeRect(440, 420 - (f === 2 ? 20 : 0), 80, 40);
      g.beginPath(); g.moveTo(450, 460); g.lineTo(450, 520); g.moveTo(510, 460); g.lineTo(510, 520); g.stroke();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // Project 5: Heart Balloon
  function createProject5HeartBalloon() {
    const p = { id: 'p5', title: 'Heart Balloon', ratio: '1:1', fps: 8, type: 'animation', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 4; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 10;
      g.beginPath(); g.arc(280, 380, 45, 0, Math.PI * 2); g.stroke();
      g.beginPath(); g.moveTo(280, 425); g.lineTo(280, 560); g.stroke();
      g.beginPath(); g.moveTo(280, 460); g.lineTo(360, 420); g.stroke();
      // Heart
      const hy = 240 - f * 18;
      g.fillStyle = '#FF2D55';
      g.beginPath();
      g.moveTo(380, hy);
      g.bezierCurveTo(380, hy - 40, 320, hy - 40, 320, hy);
      g.bezierCurveTo(320, hy + 40, 380, hy + 80, 380, hy + 100);
      g.bezierCurveTo(380, hy + 80, 440, hy + 40, 440, hy);
      g.bezierCurveTo(440, hy - 40, 380, hy - 40, 380, hy);
      g.fill(); g.stroke();
      // String
      g.beginPath(); g.moveTo(380, hy + 100); g.quadraticCurveTo(370, 360, 360, 420); g.stroke();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // Project 6: Campfire
  function createProject6Campfire() {
    const p = { id: 'p6', title: 'Campfire Night', ratio: '1:1', fps: 8, type: 'animation', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 3; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 12;
      // Logs
      g.beginPath(); g.moveTo(260, 520); g.lineTo(460, 480); g.moveTo(280, 480); g.lineTo(440, 520); g.stroke();
      // Fire
      g.fillStyle = f % 2 === 0 ? '#FB8C00' : '#FF2D55';
      g.beginPath();
      g.moveTo(360, 320 + f * 10);
      g.quadraticCurveTo(320 - f * 8, 420, 300, 480);
      g.quadraticCurveTo(360, 460, 420, 480);
      g.quadraticCurveTo(400 + f * 8, 420, 360, 320 + f * 10);
      g.fill(); g.stroke();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // Project 7: Cute Bug (Photo / Drawing)
  function createProject7CuteBug() {
    const p = { id: 'p7', title: 'Little Ladybug', ratio: '1:1', fps: 1, type: 'image', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 1; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 10;
      // Oval body
      g.fillStyle = '#26A69A';
      g.beginPath(); g.ellipse(360, 360, 80, 110, (f - 1) * 0.1, 0, Math.PI * 2); g.fill(); g.stroke();
      // Head
      g.fillStyle = '#1F1F1F';
      g.beginPath(); g.arc(360, 240, 40, 0, Math.PI * 2); g.fill(); g.stroke();
      // Antennae
      g.beginPath(); g.moveTo(340, 210); g.quadraticCurveTo(310, 160, 290, 170); g.moveTo(380, 210); g.quadraticCurveTo(410, 160, 430, 170); g.stroke();
      // Legs
      g.beginPath();
      g.moveTo(280, 320); g.lineTo(220, 300);
      g.moveTo(280, 380); g.lineTo(210, 380);
      g.moveTo(440, 320); g.lineTo(500, 300);
      g.moveTo(440, 380); g.lineTo(510, 380);
      g.stroke();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // Project 8: Peeking Stickman (Photo / Drawing)
  function createProject8PeekingStickman() {
    const p = { id: 'p8', title: 'Peeking Friend', ratio: '1:1', fps: 1, type: 'image', frames: [] };
    const w = 720, h = 720;
    for (let f = 0; f < 3; f++) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const g = c.getContext('2d');
      g.lineCap = 'round'; g.strokeStyle = '#1F1F1F'; g.lineWidth = 12;
      // Wall
      g.strokeRect(180, 220, 180, 360);
      // Peeking Head
      const px = 370 + f * 15;
      g.beginPath(); g.arc(px, 340, 50, -0.4 * Math.PI, 0.4 * Math.PI); g.stroke();
      g.fillStyle = '#1F1F1F';
      g.beginPath(); g.arc(px - 15, 330, 6, 0, Math.PI * 2); g.fill();
      p.frames.push({ id: 'f_' + f, canvas: c });
    }
    return p;
  }

  // ==================== GALLERY RENDERING ====================
  function renderGallery() {
    DOM.projectsGrid.innerHTML = '';

    // "+" New Project card first
    const newCard = document.createElement('div');
    newCard.className = 'project-card-new';
    newCard.innerHTML = `
      <div class="new-card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
      </div>
      <div class="new-card-label">Tạo Dự Án Mới</div>
      <div class="new-card-sub">Hoạt hình chuyển động hoặc Tranh vẽ</div>
    `;
    newCard.addEventListener('click', () => {
      DOM.newProjectModal.style.display = 'flex';
    });
    DOM.projectsGrid.appendChild(newCard);

    // Filter projects by current tab
    const filtered = State.projects.filter(proj => {
      if (State.activeFilter === 'all') return true;
      if (State.activeFilter === 'image') return proj.type === 'image';
      if (State.activeFilter === 'video') return proj.type !== 'image';
      return true;
    });

    // Render cards for visible projects
    filtered.forEach(proj => {
      const isPhoto = proj.type === 'image';
      const card = document.createElement('div');
      card.className = 'project-card';

      // Create preview canvas
      const previewCanvas = document.createElement('canvas');
      previewCanvas.className = 'card-canvas';
      previewCanvas.width = 320;
      previewCanvas.height = 320;
      const pctx = previewCanvas.getContext('2d');

      if (proj.frames.length > 0) {
        pctx.fillStyle = '#FFF';
        pctx.fillRect(0, 0, 320, 320);
        pctx.drawImage(proj.frames[0].canvas, 0, 0, 320, 320);
      }

      const typeLabel = isPhoto ? '🖼️ Photo' : '🎬 Animation';
      const typeBadgeCls = isPhoto ? 'photo-badge' : 'anim-badge';
      const subtitleText = isPhoto
        ? `1 frame • ${proj.ratio}`
        : `${proj.frames.length} frames • ${proj.ratio}`;
      const actionHoverBtn = isPhoto
        ? `<div class="card-edit-hover-btn" title="Mở bức vẽ để chỉnh sửa & vẽ thêm">
             <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>
           </div>`
        : `<div class="card-play-hover-btn" title="Xem trước chuyển động">
             <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="6 3 20 12 6 21 6 3"></polygon></svg>
           </div>`;
      const footerTag = isPhoto ? 'PNG' : `MP4 : ${proj.fps} fps`;

      card.innerHTML = `
        <div class="card-paper-wrapper">
          <span class="card-type-badge ${typeBadgeCls}">${typeLabel}</span>
          ${actionHoverBtn}
        </div>
        <div class="card-info">
          <div class="card-title-group">
            <div class="card-title">${proj.title}</div>
            <div class="card-subtitle">${subtitleText}</div>
          </div>
          <div class="card-fps-tag">${footerTag}</div>
        </div>
      `;

      card.querySelector('.card-paper-wrapper').prepend(previewCanvas);

      // Hover animation playback on multi-frame projects
      let fIndex = 0;
      let timer = null;
      card.addEventListener('mouseenter', () => {
        if (isPhoto || proj.frames.length <= 1) return;
        timer = setInterval(() => {
          fIndex = (fIndex + 1) % proj.frames.length;
          pctx.fillStyle = '#FFF';
          pctx.fillRect(0, 0, 320, 320);
          pctx.drawImage(proj.frames[fIndex].canvas, 0, 0, 320, 320);
        }, 1000 / proj.fps);
      });

      card.addEventListener('mouseleave', () => {
        if (timer) clearInterval(timer);
        fIndex = 0;
        pctx.fillStyle = '#FFF';
        pctx.fillRect(0, 0, 320, 320);
        if (proj.frames.length > 0) {
          pctx.drawImage(proj.frames[0].canvas, 0, 0, 320, 320);
        }
      });

      card.addEventListener('click', () => {
        if (timer) clearInterval(timer);
        openProjectInStudio(proj);
      });

      DOM.projectsGrid.appendChild(card);
    });

    // Show empty state if no match
    if (filtered.length === 0) {
      const emptyEl = document.createElement('div');
      emptyEl.style.cssText = 'grid-column: 1/-1; text-align: center; padding: 60px 20px; color: var(--text-dim);';
      emptyEl.innerHTML = '<div style="font-size:40px; margin-bottom:12px;">📂</div><div style="font-size:15px; font-weight:600;">Không tìm thấy dự án phù hợp</div>';
      DOM.projectsGrid.appendChild(emptyEl);
    }

    DOM.statProjectCount.textContent = State.projects.length;
  }


  // ==================== STUDIO OPEN & SETUP ====================
  function openProjectInStudio(proj) {
    State.currentProject = proj;
    State.currentFrameIndex = 0;
    State.undoStack = [];
    State.redoStack = [];
    switchView('studio');
  }

  function applyProjectToStudio() {
    const proj = State.currentProject;
    if (!proj) return;

    const isPhoto = proj.type === 'image';
    DOM.viewStudio.classList.toggle('photo-mode', isPhoto);

    // Apply ratio dimensions
    const ratioData = CanvasRatios[proj.ratio] || CanvasRatios['1:1'];
    DOM.canvasPaperCard.className = `canvas-paper-shadow ${ratioData.css}`;
    DOM.canvasRatioBadge.textContent = ratioData.label;

    [DOM.drawingCanvas, DOM.onionCanvas, DOM.bgCanvas].forEach(c => {
      c.width = ratioData.w;
      c.height = ratioData.h;
    });

    // Update ratio sidebar buttons
    DOM.ratioOptionButtons.forEach(btn => {
      btn.classList.toggle('active', btn.dataset.ratio === proj.ratio);
    });

    // Update FPS chips
    DOM.fpsChips.forEach(chip => {
      chip.classList.toggle('active', parseInt(chip.dataset.fps) === proj.fps);
    });

    // Toggle photo dock vs timeline dock
    if (DOM.studioPhotoDock) DOM.studioPhotoDock.style.display = isPhoto ? 'flex' : 'none';
    if (DOM.studioTimelineDock) DOM.studioTimelineDock.style.display = isPhoto ? 'none' : 'block';
    if (DOM.btnHeaderExport) DOM.btnHeaderExport.style.display = isPhoto ? 'none' : 'flex';
    if (DOM.btnHeaderSavePhoto) DOM.btnHeaderSavePhoto.style.display = isPhoto ? 'inline-flex' : 'none';
    if (DOM.btnExportTriggerSide) DOM.btnExportTriggerSide.style.display = isPhoto ? 'none' : 'flex';
    if (DOM.btnSavePhotoSide) DOM.btnSavePhotoSide.style.display = isPhoto ? 'flex' : 'none';
    if (DOM.btnHeaderOnion) DOM.btnHeaderOnion.style.display = isPhoto ? 'none' : 'flex';
    if (DOM.centerPlayOverlay) DOM.centerPlayOverlay.style.display = isPhoto ? 'none' : '';

    if (!isPhoto) {
      renderTimelineStrip();
    }
    loadFrameToCanvas();
  }

  // Load current frame & onion skin into canvas
  function loadFrameToCanvas() {
    const proj = State.currentProject;
    if (!proj || proj.frames.length === 0) return;

    const isPhoto = proj.type === 'image';
    const frame = proj.frames[State.currentFrameIndex];
    if (!frame) return;

    // Clear all layers
    ctx.bg.clearRect(0, 0, DOM.bgCanvas.width, DOM.bgCanvas.height);
    ctx.onion.clearRect(0, 0, DOM.onionCanvas.width, DOM.onionCanvas.height);
    ctx.drawing.clearRect(0, 0, DOM.drawingCanvas.width, DOM.drawingCanvas.height);

    // Render Onion Skinning (only for animations)
    if (State.onionSkinEnabled && !isPhoto) {
      // Previous frame in Red tint
      if (State.currentFrameIndex > 0) {
        const prevFrame = proj.frames[State.currentFrameIndex - 1];
        ctx.onion.save();
        ctx.onion.globalAlpha = 0.35;
        ctx.onion.drawImage(prevFrame.canvas, 0, 0, DOM.onionCanvas.width, DOM.onionCanvas.height);
        ctx.onion.globalCompositeOperation = 'source-in';
        ctx.onion.fillStyle = '#FF2D55';
        ctx.onion.fillRect(0, 0, DOM.onionCanvas.width, DOM.onionCanvas.height);
        ctx.onion.restore();
      }

      // Next frame in Green tint
      if (State.currentFrameIndex < proj.frames.length - 1) {
        const nextFrame = proj.frames[State.currentFrameIndex + 1];
        ctx.onion.save();
        ctx.onion.globalAlpha = 0.3;
        ctx.onion.drawImage(nextFrame.canvas, 0, 0, DOM.onionCanvas.width, DOM.onionCanvas.height);
        ctx.onion.globalCompositeOperation = 'source-in';
        ctx.onion.fillStyle = '#10B981';
        ctx.onion.fillRect(0, 0, DOM.onionCanvas.width, DOM.onionCanvas.height);
        ctx.onion.restore();
      }
    }

    // Render Active Frame
    ctx.drawing.drawImage(frame.canvas, 0, 0, DOM.drawingCanvas.width, DOM.drawingCanvas.height);

    // Update Frame Counter / Badge
    if (isPhoto) {
      DOM.frameCounterBadge.textContent = '🖼️ Photo & Art • 1 Frame';
    } else {
      DOM.frameCounterBadge.textContent = `Frame ${State.currentFrameIndex + 1} / ${proj.frames.length}`;
    }

    // Highlight active timeline frame
    if (!isPhoto) {
      updateActiveTimelineFrame();
    }
  }

  function saveCanvasToFrame() {
    const proj = State.currentProject;
    if (!proj) return;
    const frame = proj.frames[State.currentFrameIndex];
    if (!frame) return;

    const fctx = frame.canvas.getContext('2d');
    fctx.clearRect(0, 0, frame.canvas.width, frame.canvas.height);
    fctx.drawImage(DOM.drawingCanvas, 0, 0);

    // Update thumbnail in timeline
    updateTimelineThumbnail(State.currentFrameIndex);
  }

  // ==================== TIMELINE STRIP ====================
  function renderTimelineStrip() {
    const proj = State.currentProject;
    if (!proj) return;

    DOM.timelineFramesScroll.innerHTML = '';
    proj.frames.forEach((frame, idx) => {
      const item = document.createElement('div');
      item.className = 'timeline-frame-item' + (idx === State.currentFrameIndex ? ' active' : '');
      item.dataset.index = idx;

      const thumb = document.createElement('canvas');
      thumb.className = 'frame-thumb-canvas';
      thumb.width = 120; thumb.height = 120;
      const tctx = thumb.getContext('2d');
      tctx.drawImage(frame.canvas, 0, 0, 120, 120);

      const num = document.createElement('div');
      num.className = 'frame-badge-num';
      num.textContent = idx + 1;

      item.appendChild(thumb);
      item.appendChild(num);

      item.addEventListener('click', () => {
        saveCanvasToFrame();
        State.currentFrameIndex = idx;
        loadFrameToCanvas();
      });

      DOM.timelineFramesScroll.appendChild(item);
    });
  }

  function updateActiveTimelineFrame() {
    const items = DOM.timelineFramesScroll.querySelectorAll('.timeline-frame-item');
    items.forEach((item, idx) => {
      item.classList.toggle('active', idx === State.currentFrameIndex);
      if (idx === State.currentFrameIndex) {
        item.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
      }
    });
  }

  function updateTimelineThumbnail(idx) {
    const items = DOM.timelineFramesScroll.querySelectorAll('.timeline-frame-item');
    const item = items[idx];
    if (item && State.currentProject.frames[idx]) {
      const thumb = item.querySelector('.frame-thumb-canvas');
      if (thumb) {
        const tctx = thumb.getContext('2d');
        tctx.clearRect(0, 0, 120, 120);
        tctx.drawImage(State.currentProject.frames[idx].canvas, 0, 0, 120, 120);
      }
    }
  }

  // ==================== PLAYBACK ENGINE ====================
  function togglePlayback() {
    if (State.isPlaying) {
      stopPlayback();
    } else {
      startPlayback();
    }
  }

  function startPlayback() {
    const proj = State.currentProject;
    if (!proj || proj.frames.length <= 1) return;

    saveCanvasToFrame();
    State.isPlaying = true;
    State.playbackIndex = State.currentFrameIndex;

    DOM.timePlayIcon.style.display = 'none';
    DOM.timePauseIcon.style.display = 'block';
    DOM.centerPlayIcon.style.display = 'none';
    DOM.centerPauseIcon.style.display = 'block';
    DOM.timelinePlayBtn.classList.add('playing');
    DOM.centerPlayOverlay.classList.remove('hidden');

    // Hide onion skin during playback
    ctx.onion.clearRect(0, 0, DOM.onionCanvas.width, DOM.onionCanvas.height);

    State.playbackTimer = setInterval(() => {
      State.playbackIndex++;
      if (State.playbackIndex >= proj.frames.length) {
        if (State.isLooping) {
          State.playbackIndex = 0;
        } else {
          stopPlayback();
          return;
        }
      }

      ctx.drawing.clearRect(0, 0, DOM.drawingCanvas.width, DOM.drawingCanvas.height);
      ctx.drawing.drawImage(proj.frames[State.playbackIndex].canvas, 0, 0);
      DOM.frameCounterBadge.textContent = `Frame ${State.playbackIndex + 1} / ${proj.frames.length}`;
      
      const items = DOM.timelineFramesScroll.querySelectorAll('.timeline-frame-item');
      items.forEach((item, idx) => item.classList.toggle('active', idx === State.playbackIndex));
    }, 1000 / proj.fps);
  }

  function stopPlayback() {
    if (!State.isPlaying) return;
    State.isPlaying = false;
    clearInterval(State.playbackTimer);

    DOM.timePlayIcon.style.display = 'block';
    DOM.timePauseIcon.style.display = 'none';
    DOM.centerPlayIcon.style.display = 'block';
    DOM.centerPauseIcon.style.display = 'none';
    DOM.timelinePlayBtn.classList.remove('playing');

    // Reload active frame and onion skin
    loadFrameToCanvas();
  }

  // ==================== DRAWING ENGINE ====================
  function getCanvasCoords(e) {
    const rect = DOM.drawingCanvas.getBoundingClientRect();
    const scaleX = DOM.drawingCanvas.width / rect.width;
    const scaleY = DOM.drawingCanvas.height / rect.height;

    let clientX = e.clientX;
    let clientY = e.clientY;

    if (e.touches && e.touches.length > 0) {
      clientX = e.touches[0].clientX;
      clientY = e.touches[0].clientY;
    }

    return {
      x: (clientX - rect.left) * scaleX,
      y: (clientY - rect.top) * scaleY
    };
  }

  function saveUndoState() {
    const imgData = ctx.drawing.getImageData(0, 0, DOM.drawingCanvas.width, DOM.drawingCanvas.height);
    State.undoStack.push(imgData);
    if (State.undoStack.length > 25) State.undoStack.shift();
    State.redoStack = [];
  }

  function undo() {
    if (State.undoStack.length === 0) return;
    const currentImg = ctx.drawing.getImageData(0, 0, DOM.drawingCanvas.width, DOM.drawingCanvas.height);
    State.redoStack.push(currentImg);
    const prevImg = State.undoStack.pop();
    ctx.drawing.putImageData(prevImg, 0, 0);
    saveCanvasToFrame();
    showToast('Hoàn tác nét vẽ (Undo)');
  }

  function redo() {
    if (State.redoStack.length === 0) return;
    const currentImg = ctx.drawing.getImageData(0, 0, DOM.drawingCanvas.width, DOM.drawingCanvas.height);
    State.undoStack.push(currentImg);
    const nextImg = State.redoStack.pop();
    ctx.drawing.putImageData(nextImg, 0, 0);
    saveCanvasToFrame();
    showToast('Làm lại nét vẽ (Redo)');
  }

  function startDraw(e) {
    if (State.isPlaying) stopPlayback();
    e.preventDefault();

    const pt = getCanvasCoords(e);

    // Flood fill tool check
    if (State.currentTool === 'bucket') {
      saveUndoState();
      floodFill(Math.round(pt.x), Math.round(pt.y), State.currentColor);
      saveCanvasToFrame();
      return;
    }

    saveUndoState();
    State.isDrawing = true;
    State.points = [pt];

    DOM.centerPlayOverlay.classList.add('hidden');

    ctx.drawing.lineCap = 'round';
    ctx.drawing.lineJoin = 'round';

    if (State.currentTool === 'eraser') {
      ctx.drawing.globalCompositeOperation = 'destination-out';
      ctx.drawing.lineWidth = State.brushSize * 1.5;
    } else if (State.currentTool === 'pencil') {
      ctx.drawing.globalCompositeOperation = 'source-over';
      ctx.drawing.strokeStyle = State.currentColor;
      ctx.drawing.lineWidth = Math.max(2, State.brushSize * 0.6);
      ctx.drawing.globalAlpha = 0.85;
    } else { // brush
      ctx.drawing.globalCompositeOperation = 'source-over';
      ctx.drawing.strokeStyle = State.currentColor;
      ctx.drawing.lineWidth = State.brushSize;
      ctx.drawing.globalAlpha = 1.0;
    }

    ctx.drawing.beginPath();
    ctx.drawing.arc(pt.x, pt.y, ctx.drawing.lineWidth / 2, 0, Math.PI * 2);
    ctx.drawing.fillStyle = ctx.drawing.strokeStyle;
    ctx.drawing.fill();
  }

  function moveDraw(e) {
    if (!State.isDrawing) return;
    e.preventDefault();

    const pt = getCanvasCoords(e);
    State.points.push(pt);

    if (State.points.length > 2) {
      const p1 = State.points[State.points.length - 2];
      const p2 = State.points[State.points.length - 1];
      const mid = { x: (p1.x + p2.x) / 2, y: (p1.y + p2.y) / 2 };

      ctx.drawing.beginPath();
      ctx.drawing.moveTo(p1.x, p1.y);
      ctx.drawing.quadraticCurveTo(p1.x, p1.y, mid.x, mid.y);
      ctx.drawing.stroke();
    }
  }

  function endDraw() {
    if (!State.isDrawing) return;
    State.isDrawing = false;
    State.points = [];
    ctx.drawing.globalAlpha = 1.0;
    ctx.drawing.globalCompositeOperation = 'source-over';
    saveCanvasToFrame();
    DOM.centerPlayOverlay.classList.remove('hidden');
  }

  // BFS Flood Fill Engine
  function floodFill(startX, startY, fillHex) {
    const w = DOM.drawingCanvas.width;
    const h = DOM.drawingCanvas.height;
    if (startX < 0 || startX >= w || startY < 0 || startY >= h) return;

    const imgData = ctx.drawing.getImageData(0, 0, w, h);
    const data = imgData.data;

    // Convert hex to rgb
    const fillR = parseInt(fillHex.slice(1, 3), 16);
    const fillG = parseInt(fillHex.slice(3, 5), 16);
    const fillB = parseInt(fillHex.slice(5, 7), 16);
    const fillA = 255;

    const startIndex = (startY * w + startX) * 4;
    const startR = data[startIndex];
    const startG = data[startIndex + 1];
    const startB = data[startIndex + 2];
    const startA = data[startIndex + 3];

    // If already same color, return
    if (startR === fillR && startG === fillG && startB === fillB && startA === fillA) return;

    function matchColor(idx) {
      return Math.abs(data[idx] - startR) < 30 &&
             Math.abs(data[idx + 1] - startG) < 30 &&
             Math.abs(data[idx + 2] - startB) < 30 &&
             Math.abs(data[idx + 3] - startA) < 30;
    }

    const queue = [[startX, startY]];
    const visited = new Uint8Array(w * h);

    while (queue.length > 0) {
      const [x, y] = queue.pop();
      const pos = y * w + x;
      if (visited[pos]) continue;
      visited[pos] = 1;

      const idx = pos * 4;
      if (matchColor(idx)) {
        data[idx] = fillR;
        data[idx + 1] = fillG;
        data[idx + 2] = fillB;
        data[idx + 3] = fillA;

        if (x > 0) queue.push([x - 1, y]);
        if (x < w - 1) queue.push([x + 1, y]);
        if (y > 0) queue.push([x, y - 1]);
        if (y < h - 1) queue.push([x, y + 1]);
      }
    }

    ctx.drawing.putImageData(imgData, 0, 0);
  }

  // ==================== PHOTO DRAWING FUNCTIONS ====================
  function saveCurrentPhotoPng() {
    const proj = State.currentProject;
    if (!proj) return;

    saveCanvasToFrame();

    const exportCanvas = document.createElement('canvas');
    exportCanvas.width = DOM.drawingCanvas.width;
    exportCanvas.height = DOM.drawingCanvas.height;
    const ectx = exportCanvas.getContext('2d');

    // White background
    ectx.fillStyle = '#FFFFFF';
    ectx.fillRect(0, 0, exportCanvas.width, exportCanvas.height);

    // Draw background (photo if any)
    ectx.drawImage(DOM.bgCanvas, 0, 0);

    // Draw active drawing layer
    ectx.drawImage(DOM.drawingCanvas, 0, 0);

    exportCanvas.toBlob(blob => {
      if (!blob) return;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Pencil2D_Art_${(proj.title || 'Photo').replace(/\s+/g, '_')}_${Date.now()}.png`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      showToast('Đã lưu bức vẽ PNG thành công!');
    }, 'image/png');
  }

  function importPhotoToStudioCanvas(file) {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = e => {
      const img = new Image();
      img.onload = () => {
        saveUndoState();
        const w = DOM.drawingCanvas.width;
        const h = DOM.drawingCanvas.height;

        ctx.bg.clearRect(0, 0, w, h);
        ctx.bg.fillStyle = '#FFFFFF';
        ctx.bg.fillRect(0, 0, w, h);

        const scale = Math.min(w / img.width, h / img.height);
        const nw = img.width * scale;
        const nh = img.height * scale;
        const nx = (w - nw) / 2;
        const ny = (h - nh) / 2;

        ctx.bg.drawImage(img, nx, ny, nw, nh);
        saveCanvasToFrame();
        showToast('Đã chèn ảnh nền vào khung vẽ để đồ nét & vẽ thêm!');
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  function convertPhotoToAnimation() {
    const proj = State.currentProject;
    if (!proj) return;
    proj.type = 'animation';
    proj.fps = 8;
    const f2 = document.createElement('canvas');
    f2.width = DOM.drawingCanvas.width;
    f2.height = DOM.drawingCanvas.height;
    proj.frames.push({ id: 'f_' + Date.now(), canvas: f2 });
    applyProjectToStudio();
    showToast('Đã chuyển sang chế độ Hoạt Hình đa khung hình!');
  }

  // ==================== EVENT LISTENERS ====================
  function setupEventListeners() {
    // Navigation Tabs
    DOM.navTabGallery.addEventListener('click', () => switchView('gallery'));
    DOM.navTabStudio.addEventListener('click', () => switchView('studio'));
    DOM.btnBrandHome.addEventListener('click', () => switchView('gallery'));

    // Photo Mode Actions
    if (DOM.btnHeaderSavePhoto) DOM.btnHeaderSavePhoto.addEventListener('click', saveCurrentPhotoPng);
    if (DOM.btnPhotoDockSave) DOM.btnPhotoDockSave.addEventListener('click', saveCurrentPhotoPng);
    if (DOM.btnSavePhotoSide) DOM.btnSavePhotoSide.addEventListener('click', saveCurrentPhotoPng);

    if (DOM.btnPhotoDockImport) {
      DOM.btnPhotoDockImport.addEventListener('click', () => {
        if (DOM.inputStudioImportPhotoFile) DOM.inputStudioImportPhotoFile.click();
      });
    }
    if (DOM.btnStudioImportPhoto) {
      DOM.btnStudioImportPhoto.addEventListener('click', () => {
        if (DOM.inputStudioImportPhotoFile) DOM.inputStudioImportPhotoFile.click();
      });
    }
    if (DOM.inputStudioImportPhotoFile) {
      DOM.inputStudioImportPhotoFile.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) importPhotoToStudioCanvas(file);
        e.target.value = '';
      });
    }
    if (DOM.btnPhotoConvertAnim) {
      DOM.btnPhotoConvertAnim.addEventListener('click', convertPhotoToAnimation);
    }

    // Gallery Filter Tabs
    DOM.filterBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        DOM.filterBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        State.activeFilter = btn.dataset.filter;
        renderGallery();
      });
    });

    // Photo Import (from gallery header)
    if (DOM.btnImportPhoto) {
      DOM.btnImportPhoto.addEventListener('click', () => {
        if (DOM.inputImportPhotoFile) DOM.inputImportPhotoFile.click();
      });
    }
    if (DOM.inputImportPhotoFile) {
      DOM.inputImportPhotoFile.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const url = URL.createObjectURL(file);
        const img = new Image();
        img.onload = () => {
          const ratio = '1:1';
          const ratioData = CanvasRatios[ratio];
          const c = document.createElement('canvas');
          c.width = ratioData.w; c.height = ratioData.h;
          const gctx = c.getContext('2d');
          const scale = Math.max(ratioData.w / img.width, ratioData.h / img.height);
          const dx = (ratioData.w - img.width * scale) / 2;
          const dy = (ratioData.h - img.height * scale) / 2;
          gctx.fillStyle = '#FFFFFF';
          gctx.fillRect(0, 0, ratioData.w, ratioData.h);
          gctx.drawImage(img, dx, dy, img.width * scale, img.height * scale);
          const newProj = {
            id: 'p_' + Date.now(),
            title: file.name.replace(/\.[^.]+$/, '') || 'Imported Photo',
            ratio: ratio,
            fps: 1,
            type: 'image',
            frames: [{ id: 'f_0', canvas: c }]
          };
          State.projects.unshift(newProj);
          renderGallery();
          openProjectInStudio(newProj);
          showToast(`Opened photo: "${newProj.title}"`);
          URL.revokeObjectURL(url);
        };
        img.src = url;
        e.target.value = '';
      });
    }

    // Quick Pill Controls
    DOM.btnHeaderUndo.addEventListener('click', undo);
    DOM.btnHeaderRedo.addEventListener('click', redo);

    DOM.btnHeaderOnion.addEventListener('click', () => {
      State.onionSkinEnabled = !State.onionSkinEnabled;
      DOM.btnHeaderOnion.classList.toggle('active', State.onionSkinEnabled);
      DOM.toggleOnionCheckbox.checked = State.onionSkinEnabled;
      loadFrameToCanvas();
      showToast(State.onionSkinEnabled ? 'Bật Onion Skin (Đỏ/Xanh lá)' : 'Tắt Onion Skin');
    });

    DOM.toggleOnionCheckbox.addEventListener('change', (e) => {
      State.onionSkinEnabled = e.target.checked;
      DOM.btnHeaderOnion.classList.toggle('active', State.onionSkinEnabled);
      loadFrameToCanvas();
    });

    DOM.btnHeaderGrid.addEventListener('click', () => {
      State.showGrid = !State.showGrid;
      DOM.btnHeaderGrid.classList.toggle('active', State.showGrid);
      DOM.canvasGridOverlay.classList.toggle('active', State.showGrid);
      showToast(State.showGrid ? 'Bật lưới ô ly' : 'Tắt lưới ô ly');
    });

    // View Mode Switcher (Desktop Full App vs Mobile Phone)
    DOM.btnModeDesktop.addEventListener('click', () => {
      document.body.className = 'mode-desktop';
      DOM.btnModeDesktop.classList.add('active');
      DOM.btnModeMobile.classList.remove('active');
      showToast('Chế độ Web App toàn màn hình');
    });

    DOM.btnModeMobile.addEventListener('click', () => {
      document.body.className = 'mode-mobile';
      DOM.btnModeDesktop.classList.remove('active');
      DOM.btnModeMobile.classList.add('active');
      showToast('Chế độ mô phỏng điện thoại iPhone');
    });

    // Tool Buttons
    DOM.toolButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        DOM.toolButtons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        State.currentTool = btn.dataset.tool;
        showToast(`Công cụ: ${btn.dataset.tool.toUpperCase()}`);
      });
    });

    // Brush Size Slider
    DOM.brushSizeSlider.addEventListener('input', (e) => {
      State.brushSize = parseInt(e.target.value);
      DOM.brushSizeText.textContent = `${State.brushSize}px`;
      DOM.brushPreviewDot.style.width = `${State.brushSize}px`;
      DOM.brushPreviewDot.style.height = `${State.brushSize}px`;
    });

    // Color Swatches
    DOM.swatchButtons.forEach(swatch => {
      swatch.addEventListener('click', () => {
        DOM.swatchButtons.forEach(s => s.classList.remove('active'));
        swatch.classList.add('active');
        State.currentColor = swatch.dataset.color;
        DOM.customColorPreview.style.background = State.currentColor;
        DOM.brushPreviewDot.style.backgroundColor = State.currentColor;
        if (State.currentTool === 'eraser') {
          State.currentTool = 'brush';
          DOM.toolButtons.forEach(b => b.classList.toggle('active', b.dataset.tool === 'brush'));
        }
      });
    });

    // Native Color Picker
    DOM.nativeColorInput.addEventListener('input', (e) => {
      State.currentColor = e.target.value;
      DOM.swatchButtons.forEach(s => s.classList.remove('active'));
      DOM.customColorPreview.style.background = State.currentColor;
      DOM.brushPreviewDot.style.backgroundColor = State.currentColor;
      if (State.currentTool === 'eraser') {
        State.currentTool = 'brush';
        DOM.toolButtons.forEach(b => b.classList.toggle('active', b.dataset.tool === 'brush'));
      }
    });

    // Clear Canvas
    DOM.btnClearCanvas.addEventListener('click', () => {
      saveUndoState();
      ctx.drawing.clearRect(0, 0, DOM.drawingCanvas.width, DOM.drawingCanvas.height);
      saveCanvasToFrame();
      showToast('Đã xóa trắng khung hình hiện tại');
    });

    // Zoom Controls
    DOM.btnZoomIn.addEventListener('click', () => {
      State.zoomLevel = Math.min(2.5, State.zoomLevel + 0.15);
      applyZoom();
    });
    DOM.btnZoomOut.addEventListener('click', () => {
      State.zoomLevel = Math.max(0.5, State.zoomLevel - 0.15);
      applyZoom();
    });
    DOM.btnZoomReset.addEventListener('click', () => {
      State.zoomLevel = 1.0;
      applyZoom();
    });

    function applyZoom() {
      DOM.canvasPaperCard.style.transform = `scale(${State.zoomLevel})`;
      DOM.zoomLevelText.textContent = `${Math.round(State.zoomLevel * 100)}%`;
    }

    // Playback buttons
    DOM.timelinePlayBtn.addEventListener('click', togglePlayback);
    DOM.btnCenterPlay.addEventListener('click', togglePlayback);

    DOM.btnLoopToggle.addEventListener('click', () => {
      State.isLooping = !State.isLooping;
      DOM.btnLoopToggle.classList.toggle('active', State.isLooping);
      showToast(State.isLooping ? 'Chế độ lặp vô tận (Loop ON)' : 'Chạy một lần (Loop OFF)');
    });

    // FPS Chips
    DOM.fpsChips.forEach(chip => {
      chip.addEventListener('click', () => {
        DOM.fpsChips.forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        const fps = parseInt(chip.dataset.fps);
        State.currentProject.fps = fps;
        if (State.isPlaying) {
          stopPlayback();
          startPlayback();
        }
        showToast(`Tốc độ hoạt hình: ${fps} FPS`);
      });
    });

    // Aspect Ratio options
    DOM.ratioOptionButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const ratio = btn.dataset.ratio;
        State.currentProject.ratio = ratio;
        applyProjectToStudio();
        showToast(`Tỷ lệ khung vẽ: ${ratio}`);
      });
    });

    // Frame Duplicate & Delete
    DOM.btnDuplicateFrame.addEventListener('click', () => {
      const proj = State.currentProject;
      if (!proj) return;
      saveCanvasToFrame();

      const cur = proj.frames[State.currentFrameIndex];
      const newCanvas = document.createElement('canvas');
      newCanvas.width = cur.canvas.width;
      newCanvas.height = cur.canvas.height;
      newCanvas.getContext('2d').drawImage(cur.canvas, 0, 0);

      proj.frames.splice(State.currentFrameIndex + 1, 0, {
        id: 'f_' + Date.now(),
        canvas: newCanvas
      });

      State.currentFrameIndex++;
      renderTimelineStrip();
      loadFrameToCanvas();
      showToast('Đã nhân bản khung hình');
    });

    DOM.btnDeleteFrame.addEventListener('click', () => {
      const proj = State.currentProject;
      if (!proj) return;
      if (proj.frames.length <= 1) {
        showToast('Không thể xóa khung duy nhất!');
        return;
      }
      proj.frames.splice(State.currentFrameIndex, 1);
      if (State.currentFrameIndex >= proj.frames.length) {
        State.currentFrameIndex = proj.frames.length - 1;
      }
      renderTimelineStrip();
      loadFrameToCanvas();
      showToast('Đã xóa khung hình');
    });

    // Add Frame (+)
    DOM.btnAddFramePlus.addEventListener('click', () => {
      const proj = State.currentProject;
      if (!proj) return;
      saveCanvasToFrame();

      const ratioData = CanvasRatios[proj.ratio] || CanvasRatios['1:1'];
      const newCanvas = document.createElement('canvas');
      newCanvas.width = ratioData.w;
      newCanvas.height = ratioData.h;

      proj.frames.splice(State.currentFrameIndex + 1, 0, {
        id: 'f_' + Date.now(),
        canvas: newCanvas
      });

      State.currentFrameIndex++;
      renderTimelineStrip();
      loadFrameToCanvas();
      showToast('New blank frame added');
    });

    // Export Modals Trigger
    DOM.btnHeaderExport.addEventListener('click', () => {
      DOM.exportModal.style.display = 'flex';
    });
    DOM.btnExportTriggerSide.addEventListener('click', () => {
      DOM.exportModal.style.display = 'flex';
    });
    DOM.btnCloseExport.addEventListener('click', () => {
      DOM.exportModal.style.display = 'none';
    });
    DOM.btnCancelExportModal.addEventListener('click', () => {
      DOM.exportModal.style.display = 'none';
    });

    // Export Card selection
    let exportFormat = 'mp4';
    DOM.btnExportMp4.addEventListener('click', () => {
      exportFormat = 'mp4';
      DOM.btnExportMp4.classList.add('active');
      DOM.btnExportPng.classList.remove('active');
      if (DOM.btnExportGif) DOM.btnExportGif.classList.remove('active');
    });
    DOM.btnExportPng.addEventListener('click', () => {
      exportFormat = 'png';
      DOM.btnExportPng.classList.add('active');
      DOM.btnExportMp4.classList.remove('active');
      if (DOM.btnExportGif) DOM.btnExportGif.classList.remove('active');
    });
    if (DOM.btnExportGif) {
      DOM.btnExportGif.addEventListener('click', () => {
        exportFormat = 'gif';
        DOM.btnExportGif.classList.add('active');
        DOM.btnExportMp4.classList.remove('active');
        DOM.btnExportPng.classList.remove('active');
      });
    }

    // Execute Export
    DOM.btnConfirmExport.addEventListener('click', () => {
      if (exportFormat === 'png') {
        const a = document.createElement('a');
        a.href = DOM.drawingCanvas.toDataURL('image/png');
        a.download = `${State.currentProject.title}_frame_${State.currentFrameIndex + 1}.png`;
        a.click();
        DOM.exportModal.style.display = 'none';
        showToast('Đã tải xuống ảnh PNG khung hình');
      } else if (exportFormat === 'gif') {
        exportGifAnimation();
      } else {
        exportVideoAnimation();
      }
    });

    // Video Export via MediaRecorder
    function exportVideoAnimation() {
      saveCanvasToFrame();
      const proj = State.currentProject;
      DOM.exportProgressWrap.style.display = 'flex';
      DOM.exportProgressBar.style.width = '10%';
      DOM.exportStatusLabel.textContent = 'Đang chuẩn bị khung hình video...';

      const recCanvas = document.createElement('canvas');
      recCanvas.width = DOM.drawingCanvas.width;
      recCanvas.height = DOM.drawingCanvas.height;
      const recCtx = recCanvas.getContext('2d');

      const stream = recCanvas.captureStream(proj.fps);
      const mime = MediaRecorder.isTypeSupported('video/mp4') ? 'video/mp4' : 'video/webm';
      const recorder = new MediaRecorder(stream, { mimeType: mime });
      const chunks = [];

      recorder.ondataavailable = e => chunks.push(e.data);
      recorder.onstop = () => {
        DOM.exportProgressBar.style.width = '100%';
        DOM.exportStatusLabel.textContent = 'Đang hoàn tất đóng gói...';

        const blob = new Blob(chunks, { type: mime });
        const ext = mime.includes('mp4') ? 'mp4' : 'webm';
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${proj.title.replace(/\s+/g, '_')}_animation.${ext}`;
        a.click();

        setTimeout(() => {
          DOM.exportProgressWrap.style.display = 'none';
          DOM.exportModal.style.display = 'none';
          showToast(`Đã xuất video hoạt hình .${ext} thành công!`);
        }, 800);
      };

      recorder.start();

      let f = 0, loops = 0;
      const totalFrames = proj.frames.length * 4; // 4 loops
      let framesRendered = 0;

      const timer = setInterval(() => {
        recCtx.fillStyle = '#FFFFFF';
        recCtx.fillRect(0, 0, recCanvas.width, recCanvas.height);
        recCtx.drawImage(proj.frames[f].canvas, 0, 0);

        framesRendered++;
        const pct = Math.min(95, Math.round((framesRendered / totalFrames) * 100));
        DOM.exportProgressBar.style.width = `${pct}%`;
        DOM.exportStatusLabel.textContent = `Đang render khung ${f + 1}/${proj.frames.length} (${pct}%)...`;

        f++;
        if (f >= proj.frames.length) {
          f = 0; loops++;
          if (loops >= 4) {
            clearInterval(timer);
            setTimeout(() => recorder.stop(), 200);
          }
        }
      }, 1000 / proj.fps);
    }

    // GIF Export via frame-by-frame PNG sprite simulation (animated GIF via canvas)
    function exportGifAnimation() {
      saveCanvasToFrame();
      const proj = State.currentProject;
      DOM.exportProgressWrap.style.display = 'flex';
      DOM.exportStatusLabel.textContent = 'Đang tạo GIF từ các khung hình...';
      DOM.exportProgressBar.style.width = '0%';

      // We export each frame as PNG and create a webp animation (browser approach)
      // For true GIF we'll export a sprite sheet PNG as fallback
      const totalF = proj.frames.length;
      const cols = Math.ceil(Math.sqrt(totalF));
      const rows = Math.ceil(totalF / cols);
      const fw = DOM.drawingCanvas.width;
      const fh = DOM.drawingCanvas.height;

      const sprite = document.createElement('canvas');
      sprite.width = fw * cols;
      sprite.height = fh * rows;
      const sctx = sprite.getContext('2d');
      sctx.fillStyle = '#FFFFFF';
      sctx.fillRect(0, 0, sprite.width, sprite.height);

      proj.frames.forEach((frame, idx) => {
        const col = idx % cols;
        const row = Math.floor(idx / cols);
        sctx.drawImage(frame.canvas, col * fw, row * fh, fw, fh);
        const pct = Math.round(((idx + 1) / totalF) * 80);
        DOM.exportProgressBar.style.width = `${pct}%`;
      });

      setTimeout(() => {
        DOM.exportProgressBar.style.width = '100%';
        DOM.exportStatusLabel.textContent = 'Đang đóng gói sprite sheet...';
        const a = document.createElement('a');
        a.href = sprite.toDataURL('image/png');
        a.download = `${proj.title.replace(/\s+/g, '_')}_spritesheet.png`;
        a.click();
        setTimeout(() => {
          DOM.exportProgressWrap.style.display = 'none';
          DOM.exportModal.style.display = 'none';
          showToast(`Đã xuất sprite sheet PNG (${cols}x${rows}) - dùng tool ngoài chuyển GIF`);
        }, 600);
      }, 300);
    }

    // New Project Modal
    DOM.btnHeaderNewProject.addEventListener('click', () => {
      DOM.newProjectModal.style.display = 'flex';
    });
    DOM.btnCloseNewProject.addEventListener('click', () => {
      DOM.newProjectModal.style.display = 'none';
    });
    DOM.btnCancelNewProject.addEventListener('click', () => {
      DOM.newProjectModal.style.display = 'none';
    });

    const newTypeCards = document.querySelectorAll('.type-radio-card');
    newTypeCards.forEach(card => {
      card.addEventListener('click', () => {
        newTypeCards.forEach(c => c.classList.remove('active'));
        card.classList.add('active');
        const inp = card.querySelector('input');
        if (inp) inp.checked = true;
      });
    });

    const newRatioRadios = document.querySelectorAll('.ratio-radio-card');
    newRatioRadios.forEach(card => {
      card.addEventListener('click', () => {
        newRatioRadios.forEach(c => c.classList.remove('active'));
        card.classList.add('active');
        card.querySelector('input').checked = true;
      });
    });

    DOM.btnSubmitNewProject.addEventListener('click', () => {
      const title = DOM.inputNewProjectTitle.value.trim() || `Hoạt hình ${State.projects.length + 1}`;
      const checkedRatio = document.querySelector('input[name="new-ratio"]:checked');
      const checkedType = document.querySelector('input[name="new-type"]:checked');
      const ratio = checkedRatio ? checkedRatio.value : '1:1';
      const projType = checkedType ? checkedType.value : 'animation';
      const ratioData = CanvasRatios[ratio] || CanvasRatios['1:1'];

      const newProj = {
        id: 'p_' + Date.now(),
        title: title,
        ratio: ratio,
        fps: 8,
        type: projType,
        frames: [
          {
            id: 'f_0',
            canvas: (() => {
              const c = document.createElement('canvas');
              c.width = ratioData.w; c.height = ratioData.h;
              return c;
            })()
          }
        ]
      };

      State.projects.unshift(newProj);
      DOM.newProjectModal.style.display = 'none';
      openProjectInStudio(newProj);
      showToast(`Đã tạo dự án mới: "${title}"`);
    });

    // Canvas Mouse & Touch Drawing Events
    DOM.drawingCanvas.addEventListener('mousedown', startDraw);
    DOM.drawingCanvas.addEventListener('mousemove', moveDraw);
    window.addEventListener('mouseup', endDraw);

    DOM.drawingCanvas.addEventListener('touchstart', startDraw, { passive: false });
    DOM.drawingCanvas.addEventListener('touchmove', moveDraw, { passive: false });
    window.addEventListener('touchend', endDraw);
  }

  // ==================== KEYBOARD SHORTCUTS ====================
  function setupShortcuts() {
    window.addEventListener('keydown', (e) => {
      // Ignore if typing in text input
      if (e.target.tagName === 'INPUT') return;

      // Spacebar: Play / Pause
      if (e.code === 'Space') {
        e.preventDefault();
        togglePlayback();
      }
      // Undo: Ctrl+Z / Cmd+Z
      else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z' && !e.shiftKey) {
        e.preventDefault();
        undo();
      }
      // Redo: Ctrl+Y or Ctrl+Shift+Z
      else if (((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') ||
               ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'z')) {
        e.preventDefault();
        redo();
      }
      // B: Brush
      else if (e.key.toLowerCase() === 'b') {
        DOM.toolButtons.forEach(b => b.classList.toggle('active', b.dataset.tool === 'brush'));
        State.currentTool = 'brush';
        showToast('Công cụ: BRUSH (B)');
      }
      // P: Pencil
      else if (e.key.toLowerCase() === 'p') {
        DOM.toolButtons.forEach(b => b.classList.toggle('active', b.dataset.tool === 'pencil'));
        State.currentTool = 'pencil';
        showToast('Công cụ: PENCIL (P)');
      }
      // E: Eraser
      else if (e.key.toLowerCase() === 'e') {
        DOM.toolButtons.forEach(b => b.classList.toggle('active', b.dataset.tool === 'eraser'));
        State.currentTool = 'eraser';
        showToast('Công cụ: ERASER (E)');
      }
      // F: Flood Fill Bucket
      else if (e.key.toLowerCase() === 'f') {
        DOM.toolButtons.forEach(b => b.classList.toggle('active', b.dataset.tool === 'bucket'));
        State.currentTool = 'bucket';
        showToast('Công cụ: FILL BUCKET (F)');
      }
      // O: Toggle Onion Skin
      else if (e.key.toLowerCase() === 'o') {
        State.onionSkinEnabled = !State.onionSkinEnabled;
        DOM.btnHeaderOnion.classList.toggle('active', State.onionSkinEnabled);
        DOM.toggleOnionCheckbox.checked = State.onionSkinEnabled;
        loadFrameToCanvas();
        showToast(State.onionSkinEnabled ? 'Bật Onion Skin (O)' : 'Tắt Onion Skin (O)');
      }
      // G: Toggle Grid
      else if (e.key.toLowerCase() === 'g') {
        State.showGrid = !State.showGrid;
        DOM.btnHeaderGrid.classList.toggle('active', State.showGrid);
        DOM.canvasGridOverlay.classList.toggle('active', State.showGrid);
        showToast(State.showGrid ? 'Bật lưới ô ly (G)' : 'Tắt lưới ô ly (G)');
      }
      // Left Arrow: Previous Frame
      else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        if (State.currentProject && State.currentFrameIndex > 0) {
          saveCanvasToFrame();
          State.currentFrameIndex--;
          loadFrameToCanvas();
        }
      }
      // Right Arrow: Next Frame
      else if (e.code === 'ArrowRight') {
        e.preventDefault();
        if (State.currentProject && State.currentFrameIndex < State.currentProject.frames.length - 1) {
          saveCanvasToFrame();
          State.currentFrameIndex++;
          loadFrameToCanvas();
        }
      }
    });
  }

  // Run App
  init();
})();
