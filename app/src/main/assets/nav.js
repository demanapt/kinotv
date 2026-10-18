// Навигация по сайту стрелками пульта, как в Netflix: подсветка выбранного элемента,
// переход к ближайшему элементу в нужную сторону, OK — нажать.
(function () {
  if (window.__ktv) return;

  var SEL = 'a[href],button,input:not([type=hidden]),select,textarea,[onclick],[role=button],' +
    '.player-tabs li,iframe';

  var style = document.createElement('style');
  style.textContent =
    '.ktv-focus{outline:4px solid #E53935!important;outline-offset:3px!important;' +
    'box-shadow:0 0 0 9px rgba(229,57,53,.35)!important;border-radius:6px;position:relative;z-index:5}' +
    '.ktv-focus.ktv-poster{transform:scale(1.06);transition:transform .12s ease-out}';
  (document.head || document.documentElement).appendChild(style);

  var cur = null;

  function rectOf(el) {
    var r = el.getBoundingClientRect();
    return (r.width >= 4 && r.height >= 4) ? r : null;
  }

  function alive(el) {
    return el && document.documentElement.contains(el) && rectOf(el);
  }

  function setFocus(el) {
    if (cur) cur.classList.remove('ktv-focus', 'ktv-poster');
    cur = el;
    el.classList.add('ktv-focus');
    if (el.tagName !== 'IFRAME' && el.querySelector('img')) el.classList.add('ktv-poster');
    // Раскрываем меню, которые открываются при наведении мыши
    try { el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true })); } catch (e) {}
    var r = el.getBoundingClientRect(), m = 80;
    if (r.top < m || r.bottom > innerHeight - m || r.left < 0 || r.right > innerWidth) {
      el.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'smooth' });
    }
  }

  function candidates() {
    var list = document.querySelectorAll(SEL), out = [];
    for (var i = 0; i < list.length; i++) {
      var el = list[i];
      if (el === cur) continue;
      if (cur && (el.contains(cur) || cur.contains(el))) continue;
      var r = rectOf(el);
      if (r) out.push({ el: el, r: r });
    }
    return out;
  }

  function gap(a1, a2, b1, b2) {
    if (b2 < a1) return a1 - b2;
    if (b1 > a2) return b1 - a2;
    return 0;
  }

  function firstVisible() {
    var c = candidates(), best = null, bestScore = Infinity;
    for (var i = 0; i < c.length; i++) {
      var r = c[i].r;
      if (r.bottom < 0 || r.top > innerHeight) continue;
      var s = Math.max(r.top, 0) * 2 + Math.max(r.left, 0);
      if (s < bestScore) { bestScore = s; best = c[i].el; }
    }
    return best;
  }

  function move(dir) {
    if (!alive(cur)) {
      var f = firstVisible();
      if (f) { setFocus(f); return 'ok'; }
      scrollBy(0, dir === 'up' ? -innerHeight * 0.6 : innerHeight * 0.6);
      return 'scroll';
    }
    var fr = cur.getBoundingClientRect(), c = candidates(), best = null, bestScore = Infinity;
    for (var i = 0; i < c.length; i++) {
      var r = c[i].r, primary, secondary;
      if (dir === 'down') {
        if (r.top < fr.bottom - Math.min(fr.height, r.height) * 0.5) continue;
        primary = Math.max(0, r.top - fr.bottom);
        secondary = gap(fr.left, fr.right, r.left, r.right);
      } else if (dir === 'up') {
        if (r.bottom > fr.top + Math.min(fr.height, r.height) * 0.5) continue;
        primary = Math.max(0, fr.top - r.bottom);
        secondary = gap(fr.left, fr.right, r.left, r.right);
      } else if (dir === 'right') {
        if (r.left < fr.right - Math.min(fr.width, r.width) * 0.5) continue;
        primary = Math.max(0, r.left - fr.right);
        secondary = gap(fr.top, fr.bottom, r.top, r.bottom);
      } else {
        if (r.right > fr.left + Math.min(fr.width, r.width) * 0.5) continue;
        primary = Math.max(0, fr.left - r.right);
        secondary = gap(fr.top, fr.bottom, r.top, r.bottom);
      }
      var score = primary + secondary * 3;
      if (score < bestScore) { bestScore = score; best = c[i]; }
    }
    var vertical = dir === 'up' || dir === 'down';
    // Ближайший элемент слишком далеко (большой текстовый блок) — просто прокручиваем страницу
    if (vertical && (!best || bestScore > innerHeight * 0.9)) {
      scrollBy({ top: (dir === 'up' ? -1 : 1) * innerHeight * 0.5, behavior: 'smooth' });
      return best ? 'scroll' : 'edge';
    }
    if (!best) return 'edge';
    setFocus(best.el);
    return 'ok';
  }

  // Возвращает, куда приложению нажать настоящим касанием (так сайт считает нажатие пользовательским)
  function click() {
    if (!alive(cur)) { move('down'); return 'none'; }
    var r = cur.getBoundingClientRect();
    var kind = cur.tagName === 'IFRAME' ? 'player' : 'tap';
    return kind + ':' + (r.left + r.width / 2) + ',' + (r.top + r.height / 2) + ',' + innerWidth;
  }

  function current() {
    if (!alive(cur)) return 'none';
    var r = cur.getBoundingClientRect();
    return (r.left + r.width / 2) + ',' + (r.top + r.height / 2) + ',' + innerWidth;
  }

  window.__ktv = { move: move, click: click, current: current };
})();
