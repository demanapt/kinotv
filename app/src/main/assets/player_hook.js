// Встраивается в страницу плеера (evaluateJavascript после загрузки).
// Сообщает приложению время/длительность, нажимает «Продолжить с …» и выполняет команды пульта.
// Работает только со своим <video> и кнопками плеера — ссылки на видео не трогает.
(function () {
  if (window.__kinoHook) return;
  window.__kinoHook = true;
  var bridge = window.KinoNative;
  if (!bridge) return;

  function send(o) {
    try { bridge.post(JSON.stringify(o)); } catch (e) {}
  }

  function video() {
    var list = document.querySelectorAll('video');
    for (var i = 0; i < list.length; i++) {
      if (list[i].duration > 0) return list[i];
    }
    return list[0] || null;
  }

  function visible(el) {
    var r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  }

  // Кнопка плеера по надписи («Следующая», «Предыдущая»)
  function clickByText(words) {
    var els = document.querySelectorAll('button, [role=button], div, span, a');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      if (el.children.length > 2) continue;
      var t = (el.textContent || '').trim();
      for (var j = 0; j < words.length; j++) {
        if (t === words[j] && visible(el)) { el.click(); return true; }
      }
    }
    return false;
  }

  // Подпись текущей серии: из заголовка «Продолжить» или из выпадающих списков плеера
  var resumeLabel = null;
  function currentLabel() {
    var parts = [];
    var sel = document.querySelectorAll('select');
    for (var i = 0; i < sel.length; i++) {
      var o = sel[i].options[sel[i].selectedIndex];
      if (o && o.text) parts.push(o.text.trim());
    }
    if (parts.length) return parts.join(' · ');
    return resumeLabel;
  }

  window.__kinoCmd = function (c) {
    var v = video();
    if (c.cmd === 'toggle' && v) {
      if (v.paused) v.play(); else v.pause();
    } else if (c.cmd === 'seekTo' && v && v.duration > 0) {
      v.currentTime = Math.max(0, Math.min(v.duration - 5, c.time));
      if (v.paused) v.play();
    } else if (c.cmd === 'seek' && v && v.duration > 0) {
      v.currentTime = Math.max(0, Math.min(v.duration - 1, v.currentTime + c.delta));
    } else if (c.cmd === 'next') {
      send({ type: 'clicked', ok: clickByText(['Следующая', 'Следующая серия', 'Далее']) });
    } else if (c.cmd === 'prev') {
      send({ type: 'clicked', ok: clickByText(['Предыдущая', 'Предыдущая серия', 'Назад']) });
    } else if (c.cmd === 'dump') {
      // Отладка: какие кнопки и списки есть в плеере
      var out = [];
      var els = document.querySelectorAll('button, select, [class]');
      for (var i = 0; i < els.length && out.length < 120; i++) {
        var el = els[i];
        if (!visible(el)) continue;
        var t = (el.textContent || '').trim().slice(0, 40);
        if (!t && el.tagName !== 'SELECT' && el.tagName !== 'VIDEO') continue;
        out.push(el.tagName.toLowerCase() + '.' + (el.className && el.className.baseVal === undefined ? el.className : '') + ' «' + t + '»');
      }
      send({ type: 'dump', items: out });
    }
  };

  // «Продолжить с …» — нажимаем сами, чтобы просмотр шёл с того же места, серии и озвучки
  var resumed = false;
  function tryResume() {
    if (resumed) return;
    var b = document.querySelector('button.continue');
    if (b && visible(b)) {
      resumed = true;
      resumeLabel = (b.textContent || '').replace(/\s+с\s+[\d:]+\s*$/, '').trim();
      send({ type: 'resume', text: (b.textContent || '').trim() });
      setTimeout(function () { b.click(); }, 300);
    }
  }

  setInterval(function () {
    tryResume();
    var v = video();
    if (!v || !(v.duration > 0) || !isFinite(v.duration)) return;
    send({
      type: 'progress',
      time: v.currentTime,
      duration: v.duration,
      paused: v.paused,
      label: currentLabel()
    });
  }, 1000);

  send({
    type: 'hello',
    host: location.hostname,
    videos: document.querySelectorAll('video').length,
    iframes: document.querySelectorAll('iframe').length
  });

})();
