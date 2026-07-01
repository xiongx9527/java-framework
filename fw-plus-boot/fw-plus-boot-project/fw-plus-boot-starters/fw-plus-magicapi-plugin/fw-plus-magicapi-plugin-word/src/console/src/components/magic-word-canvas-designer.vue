<template>
  <div class="canvas-designer">
    <!-- Toolbar -->
    <div class="menu">
      <div class="menu-item">
        <div class="menu-item__save" title="保存模板"><i></i></div>
        <div class="menu-item__upload" title="上传"><i></i></div>
        <div style="display:none">
          <input id="uploadDocxBtn" type="file" accept=".docx" @change="onUploadDocx">
        </div>
        <div class="menu-item__download" title="导出模板"><i></i></div>
        <div class="menu-item__preview" title="预览"><i></i></div>
        <div class="menu-item__undo"><i></i></div>
        <div class="menu-item__redo"><i></i></div>
        <div class="menu-item__painter" title="格式刷（双击可连续使用）"><i></i></div>
        <div class="menu-item__format" title="清除格式"><i></i></div>
      </div>
      <div class="menu-divider"></div>
      <div class="menu-item">
        <div class="menu-item__font">
          <span class="select" title="字体">微软雅黑</span>
          <div class="options"><ul>
            <li data-family="Microsoft YaHei" style="font-family:'Microsoft YaHei'">微软雅黑</li>
            <li data-family="宋体" style="font-family:'宋体'">宋体</li>
            <li data-family="黑体" style="font-family:'黑体'">黑体</li>
            <li data-family="仿宋" style="font-family:'仿宋'">仿宋</li>
            <li data-family="楷体" style="font-family:'楷体'">楷体</li>
            <li data-family="等线" style="font-family:'等线'">等线</li>
            <li data-family="华文琥珀" style="font-family:'华文琥珀'">华文琥珀</li>
            <li data-family="华文楷体" style="font-family:'华文楷体'">华文楷体</li>
            <li data-family="华文隶书" style="font-family:'华文隶书'">华文隶书</li>
            <li data-family="华文新魏" style="font-family:'华文新魏'">华文新魏</li>
            <li data-family="华文行楷" style="font-family:'华文行楷'">华文行楷</li>
            <li data-family="华文中宋" style="font-family:'华文中宋'">华文中宋</li>
            <li data-family="华文彩云" style="font-family:'华文彩云'">华文彩云</li>
            <li data-family="Arial" style="font-family:'Arial'">Arial</li>
            <li data-family="Segoe UI" style="font-family:'Segoe UI'">Segoe UI</li>
            <li data-family="Ink Free" style="font-family:'Ink Free'">Ink Free</li>
            <li data-family="Fantasy" style="font-family:'Fantasy'">Fantasy</li>
          </ul></div>
        </div>
        <div class="menu-item__size">
          <span class="select" title="字号">小四</span>
          <div class="options"><ul>
            <li data-size="56">初号</li><li data-size="48">小初</li><li data-size="34">一号</li>
            <li data-size="32">小一</li><li data-size="29">二号</li><li data-size="24">小二</li>
            <li data-size="21">三号</li><li data-size="20">小三</li><li data-size="18">四号</li>
            <li data-size="16">小四</li><li data-size="14">五号</li><li data-size="12">小五</li>
            <li data-size="10">六号</li><li data-size="8">小六</li><li data-size="7">七号</li><li data-size="6">八号</li>
          </ul></div>
        </div>
        <div class="menu-item__size-add" title="增大字号"><i></i></div>
        <div class="menu-item__size-minus" title="减小字号"><i></i></div>
        <div class="menu-item__bold" title="加粗"><i></i></div>
        <div class="menu-item__italic" title="斜体"><i></i></div>
        <div class="menu-item__underline" title="下划线">
          <i></i><span class="select"></span>
          <div class="options"><ul>
            <li data-decoration-style="solid"><i></i></li>
            <li data-decoration-style="double"><i></i></li>
            <li data-decoration-style="dashed"><i></i></li>
            <li data-decoration-style="dotted"><i></i></li>
            <li data-decoration-style="wavy"><i></i></li>
          </ul></div>
        </div>
        <div class="menu-item__strikeout" title="删除线"><i></i></div>
        <div class="menu-item__superscript" title="上标"><i></i></div>
        <div class="menu-item__subscript" title="下标"><i></i></div>
        <div class="menu-item__color" title="字体颜色">
          <i></i><span></span><input id="colorPicker" type="color">
        </div>
        <div class="menu-item__highlight" title="高亮">
          <i></i><span></span><input id="highlightPicker" type="color">
        </div>
        <div class="menu-item__cellcolor" title="单元格颜色">
          <i></i><input id="cellcolorPicker" type="color">
        </div>
      </div>
      <div class="menu-divider"></div>
      <div class="menu-item">
        <div class="menu-item__title">
          <i></i><span class="select" title="切换标题">正文</span>
          <div class="options"><ul>
            <li style="font-size:16px">正文</li>
            <li data-level="first" style="font-size:26px">标题1</li>
            <li data-level="second" style="font-size:24px">标题2</li>
            <li data-level="third" style="font-size:22px">标题3</li>
            <li data-level="fourth" style="font-size:20px">标题4</li>
            <li data-level="fifth" style="font-size:18px">标题5</li>
            <li data-level="sixth" style="font-size:16px">标题6</li>
          </ul></div>
        </div>
        <div class="menu-item__left" title="左对齐"><i></i></div>
        <div class="menu-item__center" title="居中对齐"><i></i></div>
        <div class="menu-item__right" title="右对齐"><i></i></div>
        <div class="menu-item__alignment" title="两端对齐"><i></i></div>
        <div class="menu-item__row-margin" title="行间距">
          <i></i>
          <div class="options"><ul>
            <li data-rowmargin="1">1</li><li data-rowmargin="1.25">1.25</li>
            <li data-rowmargin="1.5">1.5</li><li data-rowmargin="1.75">1.75</li>
            <li data-rowmargin="2">2</li><li data-rowmargin="2.5">2.5</li><li data-rowmargin="3">3</li>
          </ul></div>
        </div>
        <div class="menu-item__list" title="列表">
          <i></i>
          <div class="options"><ul>
            <li><label>取消列表</label></li>
            <li data-list-type="ol" data-list-style="decimal">
              <label>有序列表：</label><ol><li>________</li></ol>
            </li>
            <li data-list-type="ul" data-list-style="disc">
              <label>实心圆点列表：</label><ul style="list-style-type:disc"><li>________</li></ul>
            </li>
            <li data-list-type="ul" data-list-style="circle">
              <label>空心圆点列表：</label><ul style="list-style-type:circle"><li>________</li></ul>
            </li>
            <li data-list-type="ul" data-list-style="square">
              <label>空心方块列表：</label><ul style="list-style-type:square"><li>________</li></ul>
            </li>
          </ul></div>
        </div>
      </div>
      <div class="menu-divider"></div>
      <div class="menu-item">
        <div class="menu-item__chart" title="图表"><i></i></div>
        <div class="menu-item__table" title="表格"><i></i></div>
        <div class="menu-item__table__collapse">
          <div class="table-close">x</div>
          <div class="table-title"><span class="table-select">插入</span><span>表格</span></div>
          <div class="table-panel"></div>
        </div>
        <div class="menu-item__image" title="图片">
          <i></i>
          <input id="imageFileInput" type="file" accept=".png,.jpg,.jpeg,.svg,.gif">
        </div>
        <div class="menu-item__barcode" title="条形码"><i></i></div>
        <div class="menu-item__qrcode" title="二维码"><i></i></div>
        <div class="menu-item__hyperlink" title="超链接"><i></i></div>
        <div class="menu-item__separator" title="分割线">
          <i></i>
          <div class="options"><ul>
            <li data-separator="0,0"><i></i></li>
            <li data-separator="1,1"><i></i></li>
            <li data-separator="3,1"><i></i></li>
            <li data-separator="4,4"><i></i></li>
            <li data-separator="7,3,3,3"><i></i></li>
            <li data-separator="6,2,2,2,2,2"><i></i></li>
          </ul></div>
        </div>
        <div class="menu-item__watermark" title="水印">
          <i></i>
          <div class="options"><ul>
            <li data-menu="add">添加水印</li>
            <li data-menu="delete">删除水印</li>
          </ul></div>
        </div>
        <div class="menu-item__page-break" title="分页符"><i></i></div>
      </div>
      <div class="menu-divider"></div>
      <div class="menu-item">
        <div class="menu-item__search" title="搜索与替换"><i></i></div>
        <div class="menu-item__search__collapse">
          <div class="menu-item__search__collapse__search">
            <input type="text" placeholder="搜索">
            <label class="search-result"></label>
            <div class="arrow-left"><i></i></div>
            <div class="arrow-right"><i></i></div>
            <span class="search-close">x</span>
          </div>
          <div class="menu-item__search__collapse__replace">
            <input type="text" placeholder="替换为">
            <button>替换</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Section Tabs -->
    <div class="section-tabs">
      <span class="section-tab" :class="{active:currentSection==='main'}" @click="switchSection('main')">主体</span>
      <span class="section-tab" :class="{active:currentSection==='header'}" @click="switchSection('header')">页眉</span>
      <span class="section-tab" :class="{active:currentSection==='footer'}" @click="switchSection('footer')">页脚</span>
    </div>

    <!-- Editor Area -->
    <div class="main-area">
      <div class="editor"></div>
    </div>

    <!-- Footer / Status Bar -->
    <div class="footer">
      <div>
        <span>可见页码：<span class="page-no-list">1</span></span>
        <span>页面：<span class="page-no">1</span>/<span class="page-size">1</span></span>
        <span>字数：<span class="word-count">0</span></span>
      </div>
      <div>
        <div class="page-scale-minus" title="缩小"><i></i></div>
        <span class="page-scale-percentage" title="显示比例（点击可复原）">100%</span>
        <div class="page-scale-add" title="放大"><i></i></div>
        <div class="paper-size" title="纸张类型">
          <i></i>
          <div class="options"><ul>
            <li data-paper-size="794*1123" class="active">A4</li>
            <li data-paper-size="1593*2251">A2</li>
            <li data-paper-size="1125*1593">A3</li>
            <li data-paper-size="565*796">A5</li>
            <li data-paper-size="412*488">5号信封</li>
            <li data-paper-size="450*866">6号信封</li>
            <li data-paper-size="609*862">7号信封</li>
            <li data-paper-size="862*1221">9号信封</li>
            <li data-paper-size="813*1266">法律用纸</li>
            <li data-paper-size="813*1054">信纸</li>
          </ul></div>
        </div>
        <div class="paper-direction" title="纸张方向">
          <i></i>
          <div class="options"><ul>
            <li data-paper-direction="vertical" class="active">纵向</li>
            <li data-paper-direction="horizontal">横向</li>
          </ul></div>
        </div>
        <div class="paper-margin" title="页边距"><i></i></div>
        <div class="fullscreen" title="全屏显示"><i></i></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import Editor, { RowFlex } from '../assets/js/canvas-editor.es.js'

const $i = inject('i18n.format')
const info = inject('info')

let instance = null

const currentSection = ref('main')
let codeSync = false

const DEFAULT_DESIGN = {
  header: [], footer: [], main: [{value: ''}],
  paperDirection: 'vertical', width: 794, height: 1123,
  margins: [1440, 1440, 1440, 1440]
}

function parseDesign() {
  try {
    if (!info.value.script) return JSON.parse(JSON.stringify(DEFAULT_DESIGN))
    const obj = JSON.parse(info.value.script)
    return {
      header: obj.header || [], footer: obj.footer || [], main: obj.main || [{value: ''}],
      paperDirection: obj.paperDirection || 'vertical', width: obj.width || 794,
      height: obj.height || 1123, margins: obj.margins || [1440, 1440, 1440, 1440]
    }
  } catch (e) { return JSON.parse(JSON.stringify(DEFAULT_DESIGN)) }
}

const design = reactive(parseDesign())

// --- Editor Lifecycle ---

function debounce(fn, delay) {
  let timer
  return function (...args) {
    clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

function postProcessElements(elements) {
  if (!Array.isArray(elements)) return elements
  return elements.map(el => {
    if (el.type === 'image' && el._placeholderType) {
      return { type: el._placeholderType, value: el.value, width: el.width, height: el.height }
    }
    if (el.trList) el.trList = el.trList.map(row => row.map(cell => {
      if (cell.elements) cell.elements = postProcessElements(cell.elements)
      return cell
    }))
    if (el.children) el.children = postProcessElements(el.children)
    if (el.valueList) el.valueList = postProcessElements(el.valueList)
    return el
  })
}

function syncFromEditor() {
  try {
    const val = instance.command.getValue()
    codeSync = true
    design.header = postProcessElements(val.header || [])
    design.main = postProcessElements(val.main || [])
    design.footer = postProcessElements(val.footer || [])
    saveDesign()
  } catch (e) { /* ignore */ }
}

function saveDesign() {
  info.value.script = JSON.stringify({
    header: design.header, footer: design.footer, main: design.main,
    paperDirection: design.paperDirection, width: design.width,
    height: design.height, margins: design.margins
  }, null, 2)
}

function setDropdownContentPosition(parentNode, dropdownNode) {
  const rect = parentNode.getBoundingClientRect()
  dropdownNode.style.top = rect.bottom + 'px'
  dropdownNode.style.left = rect.left + 'px'
  if (rect.left + dropdownNode.offsetWidth > window.innerWidth) {
    dropdownNode.style.left = (rect.right - dropdownNode.offsetWidth) + 'px'
  }
}

function makePlaceholderSvg(label, w, h) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">
    <rect width="${w}" height="${h}" fill="#f0f2f5" stroke="#ccc" stroke-width="1"/>
    <text x="${w/2}" y="${h/2}" text-anchor="middle" dominant-baseline="central"
      font-family="sans-serif" font-size="20" fill="#999">${label}</text>
  </svg>`
  return 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svg)))
}

function initEditor() {
  const editorEl = document.querySelector('.editor')
  if (!editorEl) return
  if (instance) { instance.destroy(); instance = null }

  const data = {
    header: JSON.parse(JSON.stringify(design.header)),
    main: JSON.parse(JSON.stringify(design.main)),
    footer: JSON.parse(JSON.stringify(design.footer))
  }
  instance = new Editor(editorEl, data)

  instance.command.executePaperSize(design.width, design.height)
  instance.command.executePaperDirection(design.paperDirection)
  instance.command.executeSetPaperMargin(design.margins)

  const isApple = /Mac OS X/.test(navigator.userAgent)

  // Lock scroll position during click→focus cycle to prevent browser auto-scroll
  const scrollArea = editorEl.parentElement
  let lockedScrollTop = null
  let lockTimer = null
  scrollArea.addEventListener('mousedown', () => {
    lockedScrollTop = scrollArea.scrollTop
    if (lockTimer) clearTimeout(lockTimer)
  }, true)
  scrollArea.addEventListener('scroll', () => {
    if (lockedScrollTop !== null && scrollArea.scrollTop !== lockedScrollTop) {
      scrollArea.scrollTop = lockedScrollTop
    }
  }, true)
  document.addEventListener('mouseup', () => {
    // Keep lock for 300ms after mouseup to catch async focus() from setTimeout
    if (lockTimer) clearTimeout(lockTimer)
    lockTimer = setTimeout(() => { lockedScrollTop = null }, 300)
  }, true)

  // --- Toolbar Event Wiring ---

  // Save
  document.querySelector('.menu-item__save').onclick = () => { syncFromEditor() }

  // Upload
  document.querySelector('.menu-item__upload').onclick = () => { document.getElementById('uploadDocxBtn').click() }

  // Download
  document.querySelector('.menu-item__download').onclick = () => { alert('导出功能：请通过后端接口导出 docx') }

  // Preview
  document.querySelector('.menu-item__preview').onclick = () => { alert('预览功能：请通过后端接口预览') }

  // Undo
  const undoDom = document.querySelector('.menu-item__undo')
  undoDom.title = `撤销(${isApple ? '⌘' : 'Ctrl'}+Z)`
  undoDom.onclick = () => { instance.command.executeUndo() }

  // Redo
  const redoDom = document.querySelector('.menu-item__redo')
  redoDom.title = `重做(${isApple ? '⌘' : 'Ctrl'}+Y)`
  redoDom.onclick = () => { instance.command.executeRedo() }

  // Format Painter
  const painterDom = document.querySelector('.menu-item__painter')
  let isFirstPainterClick = true
  let painterTimeout = 0
  painterDom.onclick = () => {
    if (isFirstPainterClick) {
      isFirstPainterClick = false
      painterTimeout = window.setTimeout(() => {
        isFirstPainterClick = true
        instance.command.executePainter({ isDblclick: false })
      }, 200)
    } else {
      window.clearTimeout(painterTimeout)
    }
  }
  painterDom.ondblclick = () => {
    isFirstPainterClick = true
    window.clearTimeout(painterTimeout)
    instance.command.executePainter({ isDblclick: true })
  }

  // Clear Format
  document.querySelector('.menu-item__format').onclick = () => { instance.command.executeFormat() }

  // Font dropdown
  const fontDom = document.querySelector('.menu-item__font')
  const fontSelectDom = fontDom.querySelector('.select')
  const fontOptionDom = fontDom.querySelector('.options')
  fontDom.onclick = () => {
    fontOptionDom.classList.toggle('visible')
    setDropdownContentPosition(fontDom, fontOptionDom)
  }
  fontOptionDom.onclick = (evt) => {
    const family = evt.target.dataset.family
    if (family) instance.command.executeFont(family)
  }

  // Size dropdown
  const sizeDom = document.querySelector('.menu-item__size')
  const sizeSelectDom = sizeDom.querySelector('.select')
  const sizeOptionDom = sizeDom.querySelector('.options')
  sizeDom.onclick = () => {
    sizeOptionDom.classList.toggle('visible')
    setDropdownContentPosition(sizeDom, sizeOptionDom)
  }
  sizeOptionDom.onclick = (evt) => {
    const size = evt.target.dataset.size
    if (size) instance.command.executeSize(Number(size))
  }

  // Size add/minus
  document.querySelector('.menu-item__size-add').onclick = () => { instance.command.executeSizeAdd() }
  document.querySelector('.menu-item__size-minus').onclick = () => { instance.command.executeSizeMinus() }

  // Bold
  const boldDom = document.querySelector('.menu-item__bold')
  boldDom.title = `加粗(${isApple ? '⌘' : 'Ctrl'}+B)`
  boldDom.onclick = () => { instance.command.executeBold() }

  // Italic
  const italicDom = document.querySelector('.menu-item__italic')
  italicDom.title = `斜体(${isApple ? '⌘' : 'Ctrl'}+I)`
  italicDom.onclick = () => { instance.command.executeItalic() }

  // Underline
  const underlineDom = document.querySelector('.menu-item__underline')
  underlineDom.title = `下划线(${isApple ? '⌘' : 'Ctrl'}+U)`
  const underlineOptionDom = underlineDom.querySelector('.options')
  underlineDom.querySelector('.select').onclick = () => {
    underlineOptionDom.classList.toggle('visible')
  }
  underlineDom.querySelector('i').onclick = () => {
    instance.command.executeUnderline()
    underlineOptionDom.classList.remove('visible')
  }
  underlineDom.querySelector('ul').onmousedown = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    const decorationStyle = li.dataset.decorationStyle
    instance.command.executeUnderline({ style: decorationStyle })
    underlineOptionDom.classList.remove('visible')
  }

  // Strikeout
  const strikeoutDom = document.querySelector('.menu-item__strikeout')
  strikeoutDom.onclick = () => { instance.command.executeStrikeout() }

  // Superscript / Subscript
  document.querySelector('.menu-item__superscript').onclick = () => { instance.command.executeSuperscript() }
  document.querySelector('.menu-item__subscript').onclick = () => { instance.command.executeSubscript() }

  // Font Color
  const colorControlDom = document.getElementById('colorPicker')
  const colorDom = document.querySelector('.menu-item__color')
  const colorSpanDom = colorDom.querySelector('span')
  colorControlDom.oninput = () => { instance.command.executeColor(colorControlDom.value) }
  colorDom.onclick = () => { colorControlDom.click() }

  // Highlight
  const highlightControlDom = document.getElementById('highlightPicker')
  const highlightDom = document.querySelector('.menu-item__highlight')
  const highlightSpanDom = highlightDom.querySelector('span')
  highlightControlDom.oninput = () => { instance.command.executeHighlight(highlightControlDom.value) }
  highlightDom.onclick = () => { highlightControlDom.click() }

  // Cell Color
  const cellcolorControlDom = document.getElementById('cellcolorPicker')
  const cellcolorDom = document.querySelector('.menu-item__cellcolor')
  cellcolorControlDom.oninput = () => { instance.command.executeTableTdBackgroundColor(cellcolorControlDom.value) }
  cellcolorDom.onclick = () => { cellcolorControlDom.click() }

  // Title
  const titleDom = document.querySelector('.menu-item__title')
  const titleSelectDom = titleDom.querySelector('.select')
  const titleOptionDom = titleDom.querySelector('.options')
  titleDom.onclick = () => {
    titleOptionDom.classList.toggle('visible')
    setDropdownContentPosition(titleDom, titleOptionDom)
  }
  titleOptionDom.onclick = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    const level = li.dataset.level || null
    instance.command.executeTitle(level)
  }

  // Row Flex
  document.querySelector('.menu-item__left').onclick = () => { instance.command.executeRowFlex(RowFlex.LEFT) }
  document.querySelector('.menu-item__center').onclick = () => { instance.command.executeRowFlex(RowFlex.CENTER) }
  document.querySelector('.menu-item__right').onclick = () => { instance.command.executeRowFlex(RowFlex.RIGHT) }
  document.querySelector('.menu-item__alignment').onclick = () => { instance.command.executeRowFlex(RowFlex.ALIGNMENT) }

  // Row Margin
  const rowMarginDom = document.querySelector('.menu-item__row-margin')
  const rowOptionDom = rowMarginDom.querySelector('.options')
  rowMarginDom.onclick = () => {
    rowOptionDom.classList.toggle('visible')
    setDropdownContentPosition(rowMarginDom, rowOptionDom)
  }
  rowOptionDom.onclick = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    instance.command.executeRowMargin(Number(li.dataset.rowmargin))
  }

  // List
  const listDom = document.querySelector('.menu-item__list')
  const listOptionDom = listDom.querySelector('.options')
  listDom.onclick = () => {
    listOptionDom.classList.toggle('visible')
    setDropdownContentPosition(listDom, listOptionDom)
  }
  listOptionDom.onclick = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    const listType = li.dataset.listType || null
    const listStyle = li.dataset.listStyle
    instance.command.executeList(listType, listStyle)
  }

  // Table
  const tableDom = document.querySelector('.menu-item__table')
  const tablePanelContainer = document.querySelector('.menu-item__table__collapse')
  const tableCloseDom = tablePanelContainer.querySelector('.table-close')
  const tableTitleDom = tablePanelContainer.querySelector('.table-select')
  const tablePanelDom = tablePanelContainer.querySelector('.table-panel')

  const tableCellList = []
  tablePanelDom.innerHTML = ''
  for (let i = 0; i < 10; i++) {
    const tr = document.createElement('div')
    tr.classList.add('table-row')
    const trCellList = []
    for (let j = 0; j < 10; j++) {
      const td = document.createElement('div')
      td.classList.add('table-cel')
      tr.append(td)
      trCellList.push(td)
    }
    tablePanelDom.append(tr)
    tableCellList.push(trCellList)
  }

  let colIndex = 0, rowIndex = 0

  function removeAllTableCellSelect() {
    tableCellList.forEach(tr => tr.forEach(td => td.classList.remove('active')))
  }

  function recoveryTable() {
    removeAllTableCellSelect()
    tableTitleDom.innerText = '插入'
    colIndex = 0
    rowIndex = 0
    tablePanelContainer.style.display = 'none'
  }

  tableDom.onclick = () => {
    tablePanelContainer.style.display = 'block'
    setDropdownContentPosition(tableDom, tablePanelContainer)
  }

  tablePanelDom.onmousemove = (evt) => {
    const celSize = 16, rowMarginTop = 10, celMarginRight = 6
    const { offsetX, offsetY } = evt
    removeAllTableCellSelect()
    colIndex = Math.ceil(offsetX / (celSize + celMarginRight)) || 1
    rowIndex = Math.ceil(offsetY / (celSize + rowMarginTop)) || 1
    tableCellList.forEach((tr, trIndex) => {
      tr.forEach((td, tdIndex) => {
        if (tdIndex < colIndex && trIndex < rowIndex) td.classList.add('active')
      })
    })
    tableTitleDom.innerText = `${rowIndex}x${colIndex}`
  }

  tableCloseDom.onclick = () => { recoveryTable() }

  tablePanelDom.onclick = () => {
    instance.command.executeInsertTable(rowIndex, colIndex)
    recoveryTable()
  }

  // Image
  const imageDom = document.querySelector('.menu-item__image')
  const imageFileDom = document.getElementById('imageFileInput')
  imageDom.onclick = () => { imageFileDom.click() }
  imageFileDom.onchange = () => {
    const file = imageFileDom.files[0]
    if (!file || !instance) return
    const reader = new FileReader()
    reader.onload = (ev) => {
      const dataUrl = ev.target.result
      const img = new Image()
      img.onload = () => {
        let w = img.width, h = img.height
        if (w > design.width) { h = h * (design.width / w); w = design.width }
        if (h > design.height) { h = design.height; w = w * (design.height / h) }
        instance.command.executeImage({ value: dataUrl, width: w, height: h })
      }
      img.src = dataUrl
    }
    reader.readAsDataURL(file)
    imageFileDom.value = ''
  }

  // Hyperlink
  document.querySelector('.menu-item__hyperlink').onclick = () => {
    const selectedText = instance.command.getRangeText ? instance.command.getRangeText() : ''
    const url = prompt('请输入链接URL', 'https://')
    if (!url) return
    instance.command.executeHyperlink({
      type: 'hyperlink', value: '',
      url: url,
      valueList: (selectedText || url).split('').map(ch => ({ value: ch, size: 16 }))
    })
  }

  // Separator
  const separatorDom = document.querySelector('.menu-item__separator')
  const separatorOptionDom = separatorDom.querySelector('.options')
  separatorDom.onclick = () => { separatorOptionDom.classList.toggle('visible') }
  separatorOptionDom.onmousedown = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    let payload = []
    const separatorDash = li.dataset.separator ? li.dataset.separator.split(',').map(Number) : null
    if (separatorDash) {
      const isSingleLine = separatorDash.every(d => d === 0)
      if (!isSingleLine) payload = separatorDash
    }
    instance.command.executeSeparator(payload)
  }

  // Watermark
  const watermarkDom = document.querySelector('.menu-item__watermark')
  const watermarkOptionDom = watermarkDom.querySelector('.options')
  watermarkDom.onclick = () => {
    watermarkOptionDom.classList.toggle('visible')
    setDropdownContentPosition(watermarkDom, watermarkOptionDom)
  }
  watermarkOptionDom.onmousedown = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    const menu = li.dataset.menu
    watermarkOptionDom.classList.remove('visible')
    if (menu === 'add') {
      const data = prompt('请输入水印内容', 'CONFIDENTIAL')
      if (data) instance.command.executeAddWatermark({ data, color: '#C0C0C0', size: 80, font: '微软雅黑' })
    } else {
      instance.command.executeDeleteWatermark()
    }
  }

  // Page Break
  document.querySelector('.menu-item__page-break').onclick = () => { instance.command.executePageBreak() }

  // Chart / Barcode / QRCode placeholders
  document.querySelector('.menu-item__chart').onclick = () => {
    const w = 520, h = 250
    instance.command.executeImage({ value: makePlaceholderSvg('Chart 图表', w, h), width: w, height: h, _placeholderType: 'chart' })
  }

  document.querySelector('.menu-item__barcode').onclick = () => {
    const w = 354, h = 120
    instance.command.executeImage({ value: makePlaceholderSvg('Barcode 条形码', w, h), width: w, height: h, _placeholderType: 'barcode' })
  }

  document.querySelector('.menu-item__qrcode').onclick = () => {
    const w = 200, h = 200
    instance.command.executeImage({ value: makePlaceholderSvg('QR Code 二维码', w, h), width: w, height: h, _placeholderType: 'qrcode' })
  }

  // Search & Replace
  const searchCollapseDom = document.querySelector('.menu-item__search__collapse')
  const searchInputDom = searchCollapseDom.querySelector('.menu-item__search__collapse__search input')
  const replaceInputDom = searchCollapseDom.querySelector('.menu-item__search__collapse__replace input')
  const searchDom = document.querySelector('.menu-item__search')
  const searchResultDom = searchCollapseDom.querySelector('.search-result')

  function setSearchResult() {
    const result = instance.command.getSearchNavigateInfo ? instance.command.getSearchNavigateInfo() : null
    if (result) {
      searchResultDom.innerText = `${result.index}/${result.count}`
    } else {
      searchResultDom.innerText = ''
    }
  }

  searchDom.onclick = () => {
    searchCollapseDom.style.display = 'block'
    const bodyRect = document.body.getBoundingClientRect()
    const searchRect = searchDom.getBoundingClientRect()
    const searchCollapseRect = searchCollapseDom.getBoundingClientRect()
    if (searchRect.left + searchCollapseRect.width > bodyRect.width) {
      searchCollapseDom.style.right = '0px'
      searchCollapseDom.style.left = 'unset'
    } else {
      searchCollapseDom.style.right = 'unset'
    }
    searchInputDom.focus()
  }

  searchCollapseDom.querySelector('.search-close').onclick = () => {
    searchCollapseDom.style.display = 'none'
    searchInputDom.value = ''
    replaceInputDom.value = ''
    instance.command.executeSearch(null)
    setSearchResult()
  }

  searchInputDom.oninput = () => {
    instance.command.executeSearch(searchInputDom.value || null)
    setSearchResult()
  }
  searchInputDom.onkeydown = (evt) => {
    if (evt.key === 'Enter') {
      instance.command.executeSearch(searchInputDom.value || null)
      setSearchResult()
    }
  }

  searchCollapseDom.querySelector('button').onclick = () => {
    const searchValue = searchInputDom.value
    const replaceValue = replaceInputDom.value
    if (searchValue && replaceValue && searchValue !== replaceValue) {
      instance.command.executeReplace(replaceValue)
    }
  }

  searchCollapseDom.querySelector('.arrow-left').onclick = () => {
    instance.command.executeSearchNavigatePre()
    setSearchResult()
  }
  searchCollapseDom.querySelector('.arrow-right').onclick = () => {
    instance.command.executeSearchNavigateNext()
    setSearchResult()
  }

  // --- Footer Controls ---

  document.querySelector('.page-scale-percentage').onclick = () => { instance.command.executePageScaleRecovery() }
  document.querySelector('.page-scale-minus').onclick = () => { instance.command.executePageScaleMinus() }
  document.querySelector('.page-scale-add').onclick = () => { instance.command.executePageScaleAdd() }

  // Paper Size (footer)
  const paperSizeDom = document.querySelector('.paper-size')
  const paperSizeOptionDom = paperSizeDom.querySelector('.options')
  paperSizeDom.onclick = () => { paperSizeOptionDom.classList.toggle('visible') }
  paperSizeOptionDom.onclick = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    const paperType = li.dataset.paperSize
    const [width, height] = paperType.split('*').map(Number)
    design.width = width; design.height = height
    instance.command.executePaperSize(width, height)
    saveDesign()
    paperSizeOptionDom.querySelectorAll('li').forEach(c => c.classList.remove('active'))
    li.classList.add('active')
  }

  // Paper Direction (footer)
  const paperDirectionDom = document.querySelector('.paper-direction')
  const paperDirectionOptionDom = paperDirectionDom.querySelector('.options')
  paperDirectionDom.onclick = () => { paperDirectionOptionDom.classList.toggle('visible') }
  paperDirectionOptionDom.onclick = (evt) => {
    const li = evt.target.closest('li')
    if (!li) return
    const paperDirection = li.dataset.paperDirection
    design.paperDirection = paperDirection
    instance.command.executePaperDirection(paperDirection)
    saveDesign()
    paperDirectionOptionDom.querySelectorAll('li').forEach(c => c.classList.remove('active'))
    li.classList.add('active')
  }

  // Paper Margin
  document.querySelector('.paper-margin').onclick = () => {
    const margins = instance.command.getPaperMargin()
    const topVal = prompt('上边距', margins ? String(margins[0]) : '1440')
    if (topVal === null) return
    const rightVal = prompt('右边距', margins ? String(margins[1]) : '1440')
    if (rightVal === null) return
    const bottomVal = prompt('下边距', margins ? String(margins[2]) : '1440')
    if (bottomVal === null) return
    const leftVal = prompt('左边距', margins ? String(margins[3]) : '1440')
    if (leftVal === null) return
    const newMargins = [Number(topVal), Number(rightVal), Number(bottomVal), Number(leftVal)]
    design.margins = newMargins
    instance.command.executeSetPaperMargin(newMargins)
    saveDesign()
  }

  // Fullscreen
  const fullscreenDom = document.querySelector('.fullscreen')
  fullscreenDom.onclick = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen()
    } else {
      document.exitFullscreen()
    }
  }
  document.addEventListener('fullscreenchange', () => {
    fullscreenDom.classList.toggle('exist')
  })

  // --- Editor Listeners ---

  instance.listener.rangeStyleChange = (payload) => {
    if (!payload) return

    const subscriptDom = document.querySelector('.menu-item__subscript')
    const superscriptDom = document.querySelector('.menu-item__superscript')
    payload.type === 'subscript' ? subscriptDom.classList.add('active') : subscriptDom.classList.remove('active')
    payload.type === 'superscript' ? superscriptDom.classList.add('active') : superscriptDom.classList.remove('active')

    payload.type === 'separator' ? separatorDom.classList.add('active') : separatorDom.classList.remove('active')
    separatorOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
    if (payload.type === 'separator') {
      const sep = payload.dashArray ? payload.dashArray.join(',') : '0,0'
      const curSepDom = separatorOptionDom.querySelector(`[data-separator='${sep}']`)
      if (curSepDom) curSepDom.classList.add('active')
    }

    fontOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
    const curFontLi = fontOptionDom.querySelector(`[data-family='${payload.font}']`)
    if (curFontLi) {
      fontSelectDom.innerText = curFontLi.innerText
      fontSelectDom.style.fontFamily = payload.font
      curFontLi.classList.add('active')
    }

    sizeOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
    const curSizeLi = sizeOptionDom.querySelector(`[data-size='${payload.size}']`)
    if (curSizeLi) {
      sizeSelectDom.innerText = curSizeLi.innerText
      curSizeLi.classList.add('active')
    } else {
      sizeSelectDom.innerText = String(payload.size)
    }

    payload.bold ? boldDom.classList.add('active') : boldDom.classList.remove('active')
    payload.italic ? italicDom.classList.add('active') : italicDom.classList.remove('active')
    payload.underline ? underlineDom.classList.add('active') : underlineDom.classList.remove('active')
    payload.strikeout ? strikeoutDom.classList.add('active') : strikeoutDom.classList.remove('active')

    if (payload.color) {
      colorDom.classList.add('active')
      colorControlDom.value = payload.color
      colorSpanDom.style.backgroundColor = payload.color
    } else {
      colorDom.classList.remove('active')
      colorControlDom.value = '#000000'
      colorSpanDom.style.backgroundColor = '#000000'
    }

    if (payload.highlight) {
      highlightDom.classList.add('active')
      highlightControlDom.value = payload.highlight
      highlightSpanDom.style.backgroundColor = payload.highlight
    } else {
      highlightDom.classList.remove('active')
      highlightControlDom.value = '#ffff00'
      highlightSpanDom.style.backgroundColor = '#ffff00'
    }

    const leftDom = document.querySelector('.menu-item__left')
    const centerDom = document.querySelector('.menu-item__center')
    const rightDom = document.querySelector('.menu-item__right')
    const alignmentDom = document.querySelector('.menu-item__alignment')
    leftDom.classList.remove('active'); centerDom.classList.remove('active')
    rightDom.classList.remove('active'); alignmentDom.classList.remove('active')
    if (payload.rowFlex === 'right') rightDom.classList.add('active')
    else if (payload.rowFlex === 'center') centerDom.classList.add('active')
    else if (payload.rowFlex === 'alignment') alignmentDom.classList.add('active')
    else leftDom.classList.add('active')

    rowOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
    const curRowLi = rowOptionDom.querySelector(`[data-rowmargin='${payload.rowMargin}']`)
    if (curRowLi) curRowLi.classList.add('active')

    payload.undo ? undoDom.classList.remove('no-allow') : undoDom.classList.add('no-allow')
    payload.redo ? redoDom.classList.remove('no-allow') : redoDom.classList.add('no-allow')
    payload.painter ? painterDom.classList.add('active') : painterDom.classList.remove('active')

    titleOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
    if (payload.level) {
      const curTitleLi = titleOptionDom.querySelector(`[data-level='${payload.level}']`)
      if (curTitleLi) {
        titleSelectDom.innerText = curTitleLi.innerText
        curTitleLi.classList.add('active')
      }
    } else {
      titleSelectDom.innerText = '正文'
      const firstLi = titleOptionDom.querySelector('li:first-child')
      if (firstLi) firstLi.classList.add('active')
    }

    listOptionDom.querySelectorAll('li').forEach(li => li.classList.remove('active'))
    if (payload.listType) {
      listDom.classList.add('active')
      const curListLi = listOptionDom.querySelector(`[data-list-type='${payload.listType}'][data-list-style='${payload.listStyle || 'decimal'}']`)
      if (curListLi) curListLi.classList.add('active')
    } else {
      listDom.classList.remove('active')
    }
  }

  instance.listener.visiblePageNoListChange = (payload) => {
    const text = payload.map(i => i + 1).join('、')
    const el = document.querySelector('.page-no-list')
    if (el) el.innerText = text
  }

  instance.listener.pageSizeChange = (payload) => {
    const el = document.querySelector('.page-size')
    if (el) el.innerText = String(payload)
  }

  instance.listener.intersectionPageNoChange = (payload) => {
    const el = document.querySelector('.page-no')
    if (el) el.innerText = String(payload + 1)
  }

  instance.listener.pageScaleChange = (payload) => {
    const el = document.querySelector('.page-scale-percentage')
    if (el) el.innerText = Math.floor(payload * 10 * 10) + '%'
  }

  instance.listener.contentChange = debounce(async () => {
    try {
      const wordCount = await instance.command.getWordCount()
      const el = document.querySelector('.word-count')
      if (el) el.innerText = String(wordCount || 0)
    } catch (e) { /* ignore */ }
  }, 200)
}

// --- Upload Docx ---
function onUploadDocx(e) {
  const file = e.target.files[0]
  if (!file || !instance) return
  alert('DOCX 导入功能需要通过后端接口实现')
  e.target.value = ''
}

function switchSection(section) {
  currentSection.value = section
}

// Close dropdowns on outside click
function onDocClick(e) {
  const menu = document.querySelector('.menu')
  if (menu && !menu.contains(e.target)) {
    menu.querySelectorAll('.options.visible').forEach(el => el.classList.remove('visible'))
    const tablePanel = document.querySelector('.menu-item__table__collapse')
    if (tablePanel) tablePanel.style.display = 'none'
  }
}

// --- Watchers ---

watch(() => info.value.script, (v) => {
  if (codeSync) { codeSync = false; return }
  try {
    const obj = JSON.parse(v || '{}')
    if (obj.main !== undefined) {
      codeSync = true
      Object.assign(design, {
        header: obj.header || [], footer: obj.footer || [], main: obj.main || [{value: ''}],
        paperDirection: obj.paperDirection || 'vertical', width: obj.width || 794,
        height: obj.height || 1123, margins: obj.margins || [1440, 1440, 1440, 1440]
      })
      if (instance) {
        instance.command.setValue({
          header: JSON.parse(JSON.stringify(design.header)),
          main: JSON.parse(JSON.stringify(design.main)),
          footer: JSON.parse(JSON.stringify(design.footer))
        })
      }
    }
  } catch (e) { /* ignore */ }
})

watch(() => [design.paperDirection, design.width, design.height, design.margins], () => {
  if (codeSync) return
  if (instance) initEditor()
})

// --- Lifecycle ---

onMounted(() => {
  document.addEventListener('click', onDocClick)
  nextTick(() => { initEditor() })
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  if (instance) { instance.destroy(); instance = null }
})
</script>

<style scoped>
/* ===== Scrollbar ===== */
:deep(::-webkit-scrollbar) { height: 16px; width: 16px; overflow: visible }
:deep(::-webkit-scrollbar-button) { width: 0; height: 0 }
:deep(::-webkit-scrollbar-corner) { background: transparent }
:deep(::-webkit-scrollbar-thumb) {
  background-color: #ddd; background-clip: padding-box;
  border: 4px solid #f2f4f7; border-radius: 8px; min-height: 24px
}
:deep(::-webkit-scrollbar-thumb:hover) { background-color: #c9c9c9 }
:deep(::-webkit-scrollbar-track) { background: #f2f4f7; background-clip: padding-box }

:deep(ul) { list-style: none; }

/* ===== Toolbar / Menu ===== */
:deep(.menu) {
  width: fit-content; max-width: 100%; height: 60px; margin: 0 auto; top: 0;
  z-index: 9; overflow-x: auto; display: flex; align-items: center;
  background: #F2F4F7; box-shadow: 0 2px 4px 0 transparent;
}
:deep(.menu-divider) { width: 1px; height: 16px; margin: 0 8px; display: inline-block; background-color: #cfd2d8; }
:deep(.menu-item) { height: 24px; display: flex; align-items: center; position: relative; }
:deep(.menu-item > div) {
  width: 24px; height: 24px; cursor: pointer; display: flex;
  align-items: center; justify-content: center; margin: 0 2px;
}
:deep(.menu-item > div:hover) { background: rgba(25, 55, 88, .04); }
:deep(.menu-item > div.active) { background: rgba(25, 55, 88, .08); }
:deep(.menu-item i) { width: 16px; height: 16px; display: inline-block; background-repeat: no-repeat; background-size: 100% 100%; }
:deep(.menu-item > div > span) { width: 16px; height: 3px; display: inline-block; border: 1px solid #e2e6ed; }

:deep(.menu-item .select) { border: none; font-size: 12px; line-height: 24px; user-select: none; }
:deep(.menu-item .select::after) {
  position: absolute; content: ""; top: 11px; width: 0; height: 0; right: 2px;
  border-color: #767c85 transparent transparent; border-style: solid solid none; border-width: 3px 3px 0;
}

:deep(.menu-item .options) {
  position: fixed; padding: 10px; background: #fff; font-size: 14px;
  box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%); border: 1px solid #e2e6ed;
  border-radius: 2px; display: none; z-index: 99;
}
:deep(.menu-item .options.visible) { display: block; }
:deep(.menu-item .options li) { padding: 5px; margin: 5px 0; user-select: none; transition: all .3s; }
:deep(.menu-item .options li:hover) { background-color: #ebecef; }
:deep(.menu-item .options li.active) { background-color: #e2e6ed; }

:deep(.menu-item__undo.no-allow), :deep(.menu-item__redo.no-allow), :deep(.menu-item > div.disable) {
  color: #c0c4cc; cursor: not-allowed; opacity: 0.4; pointer-events: none;
}

/* ===== Toolbar Icon Definitions ===== */
:deep(.menu-item__save i) { background-image: url('../assets/images/save.svg'); }
:deep(.menu-item__upload i) { background-image: url('../assets/images/upload.svg'); }
:deep(.menu-item__download i) { background-image: url('../assets/images/download.svg'); }
:deep(.menu-item__preview i) { background-image: url('../assets/images/preview.svg'); }
:deep(.menu-item__undo i) { background-image: url('../assets/images/undo.svg'); }
:deep(.menu-item__redo i) { background-image: url('../assets/images/redo.svg'); }
:deep(.menu-item__painter i) { background-image: url('../assets/images/painter.svg'); }
:deep(.menu-item__format i) { background-image: url('../assets/images/format.svg'); }
:deep(.menu-item__size-add i) { background-image: url('../assets/images/size-add.svg'); }
:deep(.menu-item__size-minus i) { background-image: url('../assets/images/size-minus.svg'); }
:deep(.menu-item__bold i) { background-image: url('../assets/images/bold.svg'); }
:deep(.menu-item__italic i) { background-image: url('../assets/images/italic.svg'); }
:deep(.menu-item__strikeout i) { background-image: url('../assets/images/strikeout.svg'); }
:deep(.menu-item__superscript i) { background-image: url('../assets/images/superscript.svg'); }
:deep(.menu-item__subscript i) { background-image: url('../assets/images/subscript.svg'); }
:deep(.menu-item__color i) { background-image: url('../assets/images/color.svg'); }
:deep(.menu-item__highlight i) { background-image: url('../assets/images/highlight.svg'); }
:deep(.menu-item__cellcolor i) { background-image: url('../assets/images/cellcolor.svg'); }
:deep(.menu-item__title i) { transform: translateX(-5px); background-image: url('../assets/images/title.svg'); }
:deep(.menu-item__left i) { background-image: url('../assets/images/left.svg'); }
:deep(.menu-item__center i) { background-image: url('../assets/images/center.svg'); }
:deep(.menu-item__right i) { background-image: url('../assets/images/right.svg'); }
:deep(.menu-item__alignment i) { background-image: url('../assets/images/alignment.svg'); }
:deep(.menu-item__row-margin i) { background-image: url('../assets/images/row-margin.svg'); }
:deep(.menu-item__list i) { background-image: url('../assets/images/list.svg'); }
:deep(.menu-item__image i) { background-image: url('../assets/images/image.svg'); }
:deep(.menu-item__barcode i) { background-image: url('../assets/images/barcode.svg'); }
:deep(.menu-item__qrcode i) { background-image: url('../assets/images/qrcode.svg'); }
:deep(.menu-item__chart i) { background-image: url('../assets/images/chart.svg'); }
:deep(.menu-item__table i) { background-image: url('../assets/images/table.svg'); }
:deep(.menu-item__hyperlink i) { background-image: url('../assets/images/hyperlink.svg'); }
:deep(.menu-item__separator > i) { background-image: url('../assets/images/separator.svg'); }
:deep(.menu-item__watermark > i) { background-image: url('../assets/images/watermark.svg'); }
:deep(.menu-item__page-break i) { background-image: url('../assets/images/page-break.svg'); }
:deep(.menu-item__search i) { background-image: url('../assets/images/search.svg'); }

/* ===== Font / Size Selectors ===== */
:deep(.menu-item .menu-item__font) { width: 65px; position: relative; }
:deep(.menu-item .menu-item__size) { width: 50px; text-align: center; position: relative; }
:deep(.menu-item__font .select), :deep(.menu-item__size .select) { width: 100%; height: 100%; }

/* ===== Underline ===== */
:deep(.menu-item .menu-item__underline) { width: 30px; position: relative; }
:deep(.menu-item__underline > i) { flex-shrink: 0; background-image: url('../assets/images/underline.svg'); }
:deep(.menu-item__underline .select) { width: 100%; height: 100%; }
:deep(.menu-item .menu-item__underline .options) { width: 128px; }
:deep(.menu-item .menu-item__underline li) { padding: 1px 5px; }
:deep(.menu-item__underline li i) { pointer-events: none; }
:deep(.menu-item__underline li[data-decoration-style="solid"]) { background-image: url('../assets/images/line-single.svg'); }
:deep(.menu-item__underline li[data-decoration-style="double"]) { background-image: url('../assets/images/line-double.svg'); }
:deep(.menu-item__underline li[data-decoration-style="dashed"]) { background-image: url('../assets/images/line-dash-small-gap.svg'); }
:deep(.menu-item__underline li[data-decoration-style="dotted"]) { background-image: url('../assets/images/line-dot.svg'); }
:deep(.menu-item__underline li[data-decoration-style="wavy"]) { background-image: url('../assets/images/line-wavy.svg'); }

/* ===== Color / Highlight / Cellcolor ===== */
:deep(.menu-item__color), :deep(.menu-item__highlight), :deep(.menu-item__cellcolor) { display: flex; flex-direction: column; }
:deep(.menu-item__color input), :deep(.menu-item__highlight input), :deep(.menu-item__cellcolor input) {
  width: 1px; height: 1px; visibility: hidden; outline: none; appearance: none;
}
:deep(.menu-item__color span) { background-color: #000000; }
:deep(.menu-item__highlight span) { background-color: #ffff00; }

/* ===== Title ===== */
:deep(.menu-item .menu-item__title) { width: 60px; position: relative; }
:deep(.menu-item__title .select) { width: calc(100% - 20px); height: 100%; }
:deep(.menu-item__title .options) { width: 80px; }

/* ===== Row Margin / List ===== */
:deep(.menu-item__row-margin) { position: relative; }
:deep(.menu-item__list) { position: relative; }
:deep(.menu-item__list .options) { width: 140px; }
:deep(.menu-item__list .options > ul > li *) { pointer-events: none; }
:deep(.menu-item__list .options > ul > li li) { margin-left: 18px; }

/* ===== Table Panel ===== */
:deep(.menu-item .menu-item__table__collapse) {
  width: 270px; height: 310px; background: #fff;
  box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%); border: 1px solid #e2e6ed;
  box-sizing: border-box; border-radius: 2px; position: fixed; display: none;
  z-index: 99; padding: 14px 27px; cursor: auto;
}
:deep(.menu-item .menu-item__table__collapse .table-close) {
  position: absolute; right: 10px; top: 5px; cursor: pointer;
}
:deep(.menu-item .menu-item__table__collapse .table-close:hover) { color: #7d7e80; }
:deep(.menu-item .menu-item__table__collapse:hover) { background: #fff; }
:deep(.menu-item .menu-item__table__collapse .table-title) {
  display: flex; justify-content: flex-start; padding-bottom: 5px; border-bottom: 1px solid #e2e6ed;
}
:deep(.table-title span) { font-size: 12px; color: #3d4757; display: inline; margin: 0; }
:deep(.table-panel) { cursor: pointer; }
:deep(.table-panel .table-row) { display: flex; flex-wrap: nowrap; margin-top: 10px; pointer-events: none; }
:deep(.table-panel .table-cel) {
  width: 16px; height: 16px; box-sizing: border-box; border: 1px solid #e2e6ed;
  background: #fff; position: relative; margin-right: 6px; pointer-events: none;
}
:deep(.table-panel .table-cel.active) { border: 1px solid rgba(73, 145, 242, .2); background: rgba(73, 145, 242, .15); }
:deep(.table-panel .table-row .table-cel:last-child) { margin-right: 0; }

/* ===== Image ===== */
:deep(.menu-item__image input) { display: none; }

/* ===== Separator ===== */
:deep(.menu-item__separator) { position: relative; }
:deep(.menu-item .menu-item__separator .options) { width: 128px; }
:deep(.menu-item .menu-item__separator li) { padding: 1px 5px; }
:deep(.menu-item__separator li i) { pointer-events: none; }
:deep(.menu-item__separator li[data-separator="0,0"]) { background-image: url('../assets/images/line-single.svg'); }
:deep(.menu-item__separator li[data-separator="1,1"]) { background-image: url('../assets/images/line-dot.svg'); }
:deep(.menu-item__separator li[data-separator="3,1"]) { background-image: url('../assets/images/line-dash-small-gap.svg'); }
:deep(.menu-item__separator li[data-separator="4,4"]) { background-image: url('../assets/images/line-dash-large-gap.svg'); }
:deep(.menu-item__separator li[data-separator="7,3,3,3"]) { background-image: url('../assets/images/line-dash-dot.svg'); }
:deep(.menu-item__separator li[data-separator="6,2,2,2,2,2"]) { background-image: url('../assets/images/line-dash-dot-dot.svg'); }

/* ===== Watermark ===== */
:deep(.menu-item__watermark) { position: relative; }

/* ===== Search ===== */
:deep(.menu-item__search) { position: relative; }
:deep(.menu-item .menu-item__search__collapse) {
  width: 260px; height: 72px; box-sizing: border-box; position: fixed; display: none;
  z-index: 99; top: 113px; left: 0; background: #ffffff; box-shadow: 0px 5px 5px #e3dfdf;
}
:deep(.menu-item .menu-item__search__collapse:hover) { background: #ffffff; }
:deep(.menu-item .menu-item__search__collapse > div) {
  width: 250px; height: 36px; padding: 0 5px; line-height: 36px;
  display: flex; align-items: center; justify-content: space-between; border-radius: 4px;
}
:deep(.menu-item .menu-item__search__collapse > div input) {
  width: 205px; height: 27px; appearance: none; background-color: #fff;
  background-image: none; border-radius: 4px; border: 1px solid #ebebeb;
  box-sizing: border-box; color: #606266; display: inline-block;
  line-height: 27px; outline: none; padding: 0 5px;
}
:deep(.menu-item .menu-item__search__collapse > div span.search-close) {
  height: 100%; color: #dcdfe6; font-size: 25px; display: inline-block; border: 0; padding: 0 10px; cursor: pointer;
}
:deep(.menu-item .menu-item__search__collapse__replace button) {
  display: inline-block; border: 1px solid #e2e6ed; border-radius: 2px; background: #fff;
  line-height: 22px; padding: 0 6px; white-space: nowrap; margin-left: 4px; cursor: pointer; font-size: 12px;
}
:deep(.menu-item .menu-item__search__collapse__replace button:hover) { background: rgba(25, 55, 88, .04); }
:deep(.menu-item .menu-item__search__collapse__search) { position: relative; }
:deep(.menu-item .menu-item__search__collapse__search label) {
  right: 110px; font-size: 12px; color: #3d4757; position: absolute;
}
:deep(.menu-item .menu-item__search__collapse__search > input) { padding: 5px 90px 5px 5px !important; }
:deep(.menu-item .menu-item__search__collapse__search > div) {
  width: 28px; height: 27px; display: flex; justify-content: center; align-items: center;
  position: absolute; border-left: 1px solid #e2e6ed; transition: all .5s;
}
:deep(.menu-item .menu-item__search__collapse__search > div:hover) { background-color: rgba(25, 55, 88, .04); }
:deep(.menu-item .menu-item__search__collapse__search i) { width: 6px; height: 8px; transform: translateY(1px); }
:deep(.menu-item .menu-item__search__collapse__search .arrow-left) { right: 76px; }
:deep(.menu-item .menu-item__search__collapse__search .arrow-left i) { background: url('../assets/images/arrow-left.svg') no-repeat; }
:deep(.menu-item .menu-item__search__collapse__search .arrow-right) { right: 48px; }
:deep(.menu-item .menu-item__search__collapse__search .arrow-right i) { background: url('../assets/images/arrow-right.svg') no-repeat; }

/* ===== Section Tabs ===== */
.section-tabs { display: flex; justify-content: center; gap: 0; background: #F2F4F7; padding: 4px 0; }
.section-tab {
  padding: 4px 16px; cursor: pointer; font-size: 12px; color: #666;
  border: 1px solid #cfd2d8; border-right: none; user-select: none; background: #fff;
}
.section-tab:last-child { border-right: 1px solid #cfd2d8; }
.section-tab.active { background: #4991f2; color: #fff; border-color: #4991f2; }
.section-tab:hover:not(.active) { background: rgba(25, 55, 88, .04); }

/* ===== Editor Area ===== */
.main-area { display: flex; flex: 1; justify-content: center; overflow-y: auto; overflow-anchor: none; background: #F2F4F7; }
:deep(.editor) { position: relative !important; flex-shrink: 0; overflow-anchor: none; }
:deep(.editor>div) { margin: 80px auto; }
:deep(.editor *) { overflow-anchor: none; }
:deep(.ce-page-container canvas) { box-shadow: rgb(158 161 165 / 40%) 0px 2px 12px 0px; }

/* ===== Footer ===== */
:deep(.footer) {
  width: 100%; height: 30px; display: flex; align-items: center; justify-content: space-between;
  background: #f2f4f7; z-index: 9; font-size: 12px; padding: 0 4px 0 20px;
  box-sizing: border-box; flex-shrink: 0;
}
:deep(.footer > div:first-child) { display: flex; align-items: center; }
:deep(.footer > div:first-child > span) { display: inline-block; margin-right: 5px; letter-spacing: 1px; }
:deep(.footer > div:last-child) { display: flex; align-items: center; justify-content: space-between; }
:deep(.footer > div:last-child > div) {
  width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
}
:deep(.footer > div:last-child > div:hover) { background: rgba(25, 55, 88, .04); }
:deep(.footer > div:last-child i) { width: 16px; height: 16px; display: inline-block; cursor: pointer; }
:deep(.footer .page-scale-minus i) { background-image: url('../assets/images/page-scale-minus.svg'); }
:deep(.footer .page-scale-add i) { background-image: url('../assets/images/page-scale-add.svg'); }
:deep(.footer .page-scale-percentage) { cursor: pointer; user-select: none; }
:deep(.footer .paper-size) { position: relative; }
:deep(.footer .paper-size i) { background-image: url('../assets/images/paper-size.svg'); }
:deep(.footer .paper-direction) { position: relative; }
:deep(.footer .paper-direction i) { background-image: url('../assets/images/paper-direction.svg'); }
:deep(.footer .paper-margin i) { background-image: url('../assets/images/paper-margin.svg'); }
:deep(.footer .fullscreen i) { background-image: url('../assets/images/request-fullscreen.svg'); }
:deep(.footer .fullscreen.exist i) { background-image: url('../assets/images/exit-fullscreen.svg'); }

:deep(.footer .options) {
  position: absolute; left: 0; bottom: 25px; padding: 10px; background: #fff;
  font-size: 14px; box-shadow: 0 2px 12px 0 rgb(56 56 56 / 20%);
  border: 1px solid #e2e6ed; border-radius: 2px; display: none;
}
:deep(.footer .options.visible) { display: block; }
:deep(.footer .options li) { padding: 5px; margin: 5px 0; user-select: none; transition: all .3s; text-align: center; cursor: pointer; }
:deep(.footer .options li:hover) { background-color: #ebecef; }
:deep(.footer .options li.active) { background-color: #e2e6ed; }
:deep(.footer .paper-size .options) { right: 0; left: unset; }
:deep(.footer .paper-direction .options) { right: 0; left: unset; }

/* ===== Layout ===== */
.canvas-designer {
  width: 100%; height: 100%; display: flex; flex-direction: column; overflow: hidden; background-color: #F2F4F7;
}
</style>
