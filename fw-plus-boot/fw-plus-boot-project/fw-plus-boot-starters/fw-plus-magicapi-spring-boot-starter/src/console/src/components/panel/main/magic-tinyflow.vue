<template>
  <div ref="containerRef" class="magic-tinyflow-container">
    <!-- 顶部操作栏 -->
    <div class="tinyflow-header">
      <div class="header-left">
        <button class="tinyflow-btn" @click="toggleFullscreen" title="全屏">
          <svg v-if="!isFullscreen" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"/>
          </svg>
          {{ isFullscreen ? i18n('tinyflow.exitFullscreen') : i18n('tinyflow.fullscreen') }}
        </button>
      </div>
      <div class="header-right">
        <!-- 流程选择下拉树组件 -->
        <div class="workflow-selector-dropdown" ref="dropdownRef">
          <!-- 下拉触发按钮 -->
          <div class="dropdown-toggle" @click="toggleDropdown">
            <span class="selected-text">{{ selectedWorkflowText }}</span>
            <svg class="dropdown-arrow" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M7 10l5 5 5-5z"/>
            </svg>
          </div>
          
          <!-- 下拉内容 -->
          <div class="dropdown-content" v-show="isDropdownOpen">
            <div class="workflow-tree-container">
              <div class="workflow-select-list">
                <!-- 空选项 -->
                <div class="workflow-select-option" @click="onSelectEmpty">
                  <span class="empty-option-text">{{ i18n('tinyflow.selectWorkflow') }}</span>
                </div>
                
                <!-- 树形结构 -->
                <div class="workflow-tree">
                  <div 
                    v-for="item in workflows" 
                    :key="item.id"
                    class="workflow-node"
                  >
                    <!-- 文件夹节点 -->
                    <div 
                      v-if="item.folder"
                      class="node-item folder-node"
                      :class="{ 'opened': item.opened }"
                    >
                      <div class="node-content" @click.stop="toggleFolder(item)" :data-opened="item.opened">
                        <svg 
                          class="folder-toggle-icon" 
                          viewBox="0 0 24 24" 
                          width="16" 
                          height="16" 
                          fill="currentColor"
                          :style="{ transform: item.opened ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.2s ease' }"
                        >
                          <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6z"/>
                        </svg>
                        <svg 
                          class="folder-icon" 
                          viewBox="0 0 24 24" 
                          width="16" 
                          height="16" 
                          fill="currentColor"
                        >
                          <path d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z"/>
                        </svg>
                        <span class="node-name">{{ item.name }}</span>
                      </div>
                      
                      <!-- 子节点 -->
                      <div v-if="item.opened" class="child-nodes">
                        <div 
                          v-for="child in item.children" 
                          :key="child.id"
                          class="node-item file-node"
                          @click.stop="onSelectWorkflow(child)"
                        >
                          <div class="node-content">
                            <svg 
                              class="file-icon" 
                              viewBox="0 0 24 24" 
                              width="16" 
                              height="16" 
                              fill="currentColor"
                            >
                              <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
                            </svg>
                            <span class="node-name">{{ child.name }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                    
                    <!-- 文件节点（直接在根目录） -->
                    <div 
                      v-else
                      class="node-item file-node"
                      @click.stop="onSelectWorkflow(item)"
                    >
                      <div class="node-content">
                        <svg 
                          class="file-icon" 
                          viewBox="0 0 24 24" 
                          width="16" 
                          height="16" 
                          fill="currentColor"
                        >
                          <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
                        </svg>
                        <span class="node-name">{{ item.name }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <button class="tinyflow-btn" :disabled="showNoNodesTip" @click="saveWorkflow">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm2 16H5V5h11.17L19 7.83V19zm-7-7c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3zM6 6h9v4H6z"/>
          </svg>
          {{ i18n('tinyflow.save') }}
        </button>
        <button class="tinyflow-btn" :disabled="showNoNodesTip" @click="runWorkflow">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M8 5v14l11-7z"/>
          </svg>
          {{ i18n('tinyflow.run') }}
        </button>
        <button class="tinyflow-btn" :disabled="showNoNodesTip" @click="exportWorkflow">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
          </svg>
          {{ i18n('tinyflow.export') }}
        </button>
      </div>
    </div>
    
    <!-- 主内容区 -->
    <div class="tinyflow-main">
      <!-- 左侧：Tinyflow编辑器 -->
      <div class="editor-panel">
        <div ref="tinyflowRef" class="tinyflow-editor"></div>
      </div>
      
      <!-- 右侧：运行结果 -->
      <div class="results-panel" v-show="showResults">
        <div class="results-header">
          <h3>{{ i18n('tinyflow.results') }}</h3>
          <button class="close-btn" @click="showResults = false">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
        <div class="results-content">
          <div v-if="isRunning" class="running-indicator">
            <div class="spinner"></div>
            <span>{{ i18n('tinyflow.running') }}</span>
          </div>
          <div v-else-if="runResults" class="results-list">
            <div v-for="(result, index) in runResults" :key="index" class="result-item">
              <div class="result-title">{{ result.node }}</div>
              <div class="result-content" v-html="result.content"></div>
            </div>
          </div>
          <div v-else class="no-results">
            {{ i18n('tinyflow.noResults') }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { Tinyflow } from '@tinyflow-ai/ui';
import "@tinyflow-ai/ui/dist/index.css";
import $i from '../../../scripts/i18n.js';

// 组件名称
const name = 'MagicTinyflow';

// 引用
const containerRef = ref(null);
const tinyflowRef = ref(null);

// 状态
const tinyflow = ref(null);
const resizeObserver = ref(null);
const lastWidth = ref(0);
const lastHeight = ref(0);
const showResults = ref(false);
const isRunning = ref(false);
const runResults = ref(null);

// 工作流相关状态，包含树形结构的流程JSON数据
const workflows = ref([
  {
    id: 'group-1',
    name: '客户服务类',
    folder: true,
    opened: true,
    children: [
      {
        id: '1',
        name: '客户服务智能助手',
        folder: false,
        data: {
          nodes: [
            {
              id: 'start',
              type: 'start',
              label: '用户请求',
              position: { x: 100, y: 150 },
              config: {
                input: '用户输入的问题'
              }
            },
            {
              id: 'intent-recognition',
              type: 'llm',
              label: '意图识别',
              position: { x: 300, y: 100 },
              config: {
                model: 'gpt-3.5-turbo',
                temperature: 0.3,
                prompt: `请识别用户的意图和实体信息。
用户问题：{{input}}

请返回JSON格式，包含：
- intent: 意图类型（如：产品咨询、故障报修、投诉建议）
- entities: 实体列表（如：产品名称、订单号、时间）
- confidence: 置信度（0-1之间的数值）`
              }
            },
            {
              id: 'kb-query',
              type: 'rag',
              label: '知识库查询',
              position: { x: 300, y: 200 },
              config: {
                knowledge_base: 'customer_service',
                top_k: 3,
                similarity_threshold: 0.7
              }
            },
            {
              id: 'response-generation',
              type: 'llm',
              label: '回复生成',
              position: { x: 500, y: 150 },
              config: {
                model: 'gpt-4',
                temperature: 0.5,
                prompt: `根据用户意图、实体信息和知识库查询结果，生成友好、准确的回复。

用户问题：{{input}}
意图识别结果：{{intent_recognition_output}}
知识库查询结果：{{kb_query_output}}

请生成自然、专业的回复，避免使用技术术语，保持礼貌。`
              }
            },
            {
              id: 'feedback-collection',
              type: 'feedback',
              label: '收集反馈',
              position: { x: 700, y: 150 },
              config: {
                feedback_type: 'satisfaction',
                options: ['非常满意', '满意', '一般', '不满意', '非常不满意']
              }
            },
            {
              id: 'end',
              type: 'end',
              label: '结束',
              position: { x: 900, y: 150 },
              config: {
                output: '最终回复'
              }
            }
          ],
          edges: [
            {
              id: 'edge-1',
              source: 'start',
              target: 'intent-recognition'
            },
            {
              id: 'edge-2',
              source: 'start',
              target: 'kb-query'
            },
            {
              id: 'edge-3',
              source: 'intent-recognition',
              target: 'response-generation'
            },
            {
              id: 'edge-4',
              source: 'kb-query',
              target: 'response-generation'
            },
            {
              id: 'edge-5',
              source: 'response-generation',
              target: 'feedback-collection'
            },
            {
              id: 'edge-6',
              source: 'feedback-collection',
              target: 'end'
            }
          ]
        }
      }
    ]
  },
  {
    id: 'group-2',
    name: '内容创作类',
    folder: true,
    opened: true,
    children: [
      {
        id: '2',
        name: '内容生成与审核流程',
        folder: false,
        data: {
          nodes: [
            {
              id: 'start',
              type: 'start',
              label: '内容需求',
              position: { x: 100, y: 150 },
              config: {
                input: '内容主题和要求'
              }
            },
            {
              id: 'content-generation',
              type: 'llm',
              label: '内容生成',
              position: { x: 300, y: 100 },
              config: {
                model: 'gpt-4',
                temperature: 0.7,
                max_tokens: 1000,
                prompt: `请根据以下主题生成一篇高质量的内容。

主题：{{input}}

要求：
1. 结构清晰，包含标题、引言、正文和结论
2. 内容详实，有数据支持
3. 语言流畅，符合学术规范
4. 不少于800字`
              }
            },
            {
              id: 'image-generation',
              type: 'image',
              label: '配图生成',
              position: { x: 300, y: 200 },
              config: {
                model: 'dall-e-3',
                size: '1024x1024',
                style: 'photorealistic',
                prompt: `为以下内容生成一张相关的配图。

内容摘要：{{content_generation_output_summary}}

请生成高质量、相关性强的图片，确保符合内容主题。`
              }
            },
            {
              id: 'content-review',
              type: 'llm',
              label: '内容审核',
              position: { x: 500, y: 150 },
              config: {
                model: 'gpt-3.5-turbo',
                temperature: 0.1,
                prompt: `请审核以下内容，检查是否符合以下标准：

内容：{{content_generation_output}}

审核标准：
1. 内容准确性：无事实错误
2. 合规性：无违法违规内容
3. 道德性：无歧视、暴力等不良内容
4. 质量：结构清晰，语言流畅
5. 原创性：无抄袭嫌疑

请返回JSON格式，包含：
- passed: 布尔值，表示是否通过审核
- reason: 审核不通过的原因（如果passed为false）
- suggestions: 改进建议
- score: 0-100的评分`
              }
            },
            {
              id: 'content-approval',
              type: 'condition',
              label: '内容审批',
              position: { x: 700, y: 150 },
              config: {
                condition: '{{content_review_output.passed}} === true',
                true_branch: '发布',
                false_branch: '修改'
              }
            },
            {
              id: 'publish',
              type: 'action',
              label: '发布内容',
              position: { x: 900, y: 100 },
              config: {
                platform: ['website', 'wechat', 'weibo'],
                scheduled_time: 'immediate'
              }
            },
            {
              id: 'revise',
              type: 'human',
              label: '人工修改',
              position: { x: 900, y: 200 },
              config: {
                assignee: 'content-editor',
                deadline: '24h'
              }
            },
            {
              id: 'end',
              type: 'end',
              label: '结束',
              position: { x: 1100, y: 150 },
              config: {
                output: '最终处理结果'
              }
            }
          ],
          edges: [
            {
              id: 'edge-1',
              source: 'start',
              target: 'content-generation'
            },
            {
              id: 'edge-2',
              source: 'content-generation',
              target: 'image-generation'
            },
            {
              id: 'edge-3',
              source: 'content-generation',
              target: 'content-review'
            },
            {
              id: 'edge-4',
              source: 'image-generation',
              target: 'content-review'
            },
            {
              id: 'edge-5',
              source: 'content-review',
              target: 'content-approval'
            },
            {
              id: 'edge-6',
              source: 'content-approval',
              target: 'publish',
              condition: 'true'
            },
            {
              id: 'edge-7',
              source: 'content-approval',
              target: 'revise',
              condition: 'false'
            },
            {
              id: 'edge-8',
              source: 'publish',
              target: 'end'
            },
            {
              id: 'edge-9',
              source: 'revise',
              target: 'content-review'
            }
          ]
        }
      },
      {
        id: '3',
        name: '社交媒体内容生成',
        folder: false,
        data: {
          nodes: [
            {
              id: 'start',
              type: 'start',
              label: '内容需求',
              position: { x: 100, y: 150 },
              config: {
                input: '社交媒体内容主题'
              }
            },
            {
              id: 'social-content',
              type: 'llm',
              label: '社交媒体内容生成',
              position: { x: 300, y: 150 },
              config: {
                model: 'gpt-3.5-turbo',
                temperature: 0.8,
                prompt: `请根据以下主题生成社交媒体内容。

主题：{{input}}

要求：
1. 语言生动有趣，符合社交媒体风格
2. 包含相关话题标签
3. 长度适中，适合社交媒体发布
4. 有吸引力的开头和结尾`
              }
            },
            {
              id: 'end',
              type: 'end',
              label: '结束',
              position: { x: 500, y: 150 },
              config: {
                output: '社交媒体内容'
              }
            }
          ],
          edges: [
            {
              id: 'edge-1',
              source: 'start',
              target: 'social-content'
            },
            {
              id: 'edge-2',
              source: 'social-content',
              target: 'end'
            }
          ]
        }
      }
    ]
  }
]);
const selectedWorkflow = ref('');

// 下拉树相关状态
const isDropdownOpen = ref(false);
const dropdownRef = ref(null);

// 获取选中的工作流名称
const selectedWorkflowText = computed(() => {
  if (!selectedWorkflow.value) {
    return i18n('tinyflow.selectWorkflow');
  }
  
  // 查找选中的工作流 - 修复递归逻辑
  const findWorkflowName = (nodes) => {
    let result = null;
    for (const node of nodes) {
      if (node.id === selectedWorkflow.value) {
        return node.name;
      }
      if (node.folder && node.children) {
        // 递归查找子节点，只有找到有效结果才返回
        const found = findWorkflowName(node.children);
        if (found && found !== i18n('tinyflow.selectWorkflow')) {
          return found;
        }
      }
    }
    // 遍历完所有节点都没找到，返回默认文本
    return null;
  };
  
  const result = findWorkflowName(workflows.value);
  return result || i18n('tinyflow.selectWorkflow');
});

// 切换下拉框
const toggleDropdown = () => {
  isDropdownOpen.value = !isDropdownOpen.value;
};

// 点击外部关闭下拉框
const handleClickOutside = (event) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target)) {
    isDropdownOpen.value = false;
  }
};

// 选择空选项
const onSelectEmpty = () => {
  selectedWorkflow.value = '';
  currentWorkflowData.value = null;
  isDropdownOpen.value = false;
  
  // 清空画布
  try {
    if (tinyflow.value) {
      if (typeof tinyflow.value.setData === 'function') {
        tinyflow.value.setData({ nodes: [], edges: [] });
      } else if (typeof tinyflow.value.clear === 'function') {
        tinyflow.value.clear();
      } else {
        // 降级方案：重新初始化编辑器
        tinyflow.value.destroy();
        tinyflow.value = new Tinyflow({
            element: tinyflowRef.value,
        });
      }
    }
    
    // 检查节点是否存在，更新按钮状态和提示
    checkNodesExist();
  } catch (error) {
    console.error('Failed to clear workflow:', error);
    showMessage(i18n('tinyflow.loadWorkflowError'), 'error');
  }
};

// 切换文件夹展开/折叠
const toggleFolder = (item) => {
  item.opened = !item.opened;
};

// 选择工作流
const onSelectWorkflow = (workflow) => {
  selectedWorkflow.value = workflow.id;
  currentWorkflowData.value = workflow.data;
  isDropdownOpen.value = false;
  
  // 加载工作流到编辑器
  try {
    if (tinyflow.value) {
      if (typeof tinyflow.value.setData === 'function') {
        tinyflow.value.setData(workflow.data);
      } else if (typeof tinyflow.value.load === 'function') {
        tinyflow.value.load(workflow.data);
      } else {
        throw new Error('Tinyflow instance does not have setData or load method');
      }
      
      showMessage(i18n('tinyflow.loadWorkflowSuccess'), 'success');
      
      // 检查节点是否存在，更新按钮状态和提示
      checkNodesExist();
    }
  } catch (error) {
    console.error('Failed to load workflow:', error);
    showMessage(i18n('tinyflow.loadWorkflowError'), 'error');
  }
};

// 加载工作流（保留用于兼容性）
const loadWorkflow = () => {
  // 这个函数现在主要作为兼容性保留，实际选择逻辑在onSelectWorkflow中处理
  if (selectedWorkflow.value) {
    // 查找选中的工作流
    const findWorkflow = (nodes) => {
      for (const node of nodes) {
        if (node.id === selectedWorkflow.value) {
          return node;
        }
        if (node.folder && node.children) {
          const found = findWorkflow(node.children);
          if (found) {
            return found;
          }
        }
      }
      return null;
    };
    
    const workflow = findWorkflow(workflows.value);
    if (workflow) {
      currentWorkflowData.value = workflow.data;
      
      try {
        if (tinyflow.value) {
          if (typeof tinyflow.value.setData === 'function') {
            tinyflow.value.setData(workflow.data);
          } else if (typeof tinyflow.value.load === 'function') {
            tinyflow.value.load(workflow.data);
          }
        }
        checkNodesExist();
      } catch (error) {
        console.error('Failed to load workflow:', error);
      }
    }
  }
};

// 添加点击外部关闭事件监听
onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

// 移除点击外部关闭事件监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});

// 国际化函数别名，用于模板中使用
const i18n = $i;

// 显示提示状态
const showNoNodesTip = ref(true);

// 当前加载的工作流数据
const currentWorkflowData = ref(null);

// 全屏状态
const isFullscreen = ref(false);

// 检查是否有节点（直接使用当前工作流数据）
const checkNodesExist = () => {
  if (!currentWorkflowData.value) {
    showNoNodesTip.value = true;
    return false;
  }
  
  // 直接使用当前工作流数据检查节点
  const hasNodes = currentWorkflowData.value.nodes && 
                  Array.isArray(currentWorkflowData.value.nodes) && 
                  currentWorkflowData.value.nodes.length > 0;
  
  showNoNodesTip.value = !hasNodes;
  return hasNodes;
};

// 生命周期 - 挂载
onMounted(() => {
  try {
    // 初始化 Tinyflow
    tinyflow.value = new Tinyflow({
        element: tinyflowRef.value,
    });
    
    // 设置 Tinyflow 数据变化事件监听（如果支持）
    if (tinyflow.value && typeof tinyflow.value.on === 'function') {
      tinyflow.value.on('dataChange', (data) => {
        currentWorkflowData.value = data;
        checkNodesExist();
      });
    }
    
    // 定期检查节点是否存在，使用setInterval持续检查
    const checkInterval = setInterval(() => {
      checkNodesExist();
    }, 1000);
    
    // 保存定时器ID，用于卸载时清除
    window.tinyflowCheckInterval = checkInterval;
    
    // 使用 ResizeObserver 监听容器大小变化
    resizeObserver.value = new ResizeObserver((entries) => {
      const entry = entries[0];
      const { width, height } = entry.contentRect;
      
      // 只在尺寸变化超过10px时才调整，避免频繁触发
      if (Math.abs(lastWidth.value - width) > 10 || Math.abs(lastHeight.value - height) > 10) {
        smartResize();
        lastWidth.value = width;
        lastHeight.value = height;
      }
    });
    
    // 保存初始尺寸
    const containerRect = containerRef.value.getBoundingClientRect();
    lastWidth.value = containerRect.width;
    lastHeight.value = containerRect.height;
    
    resizeObserver.value.observe(containerRef.value);
    
    // 初始检查一次
    checkNodesExist();
    
    // 添加全屏变化事件监听
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.addEventListener('msfullscreenchange', handleFullscreenChange);
    document.addEventListener('mozfullscreenchange', handleFullscreenChange);
    
  } catch (error) {
    console.error('Failed to initialize tinyflow:', error);
    showNoNodesTip.value = true;
  }
});

// 生命周期 - 卸载
onUnmounted(() => {
  // 清理资源
  if (resizeObserver.value) {
    resizeObserver.value.disconnect();
  }
  if (tinyflow.value && typeof tinyflow.value.destroy === 'function') {
    tinyflow.value.destroy();
  }
  
  // 清除检查节点的定时器
  if (window.tinyflowCheckInterval) {
    clearInterval(window.tinyflowCheckInterval);
    delete window.tinyflowCheckInterval;
  }
  
  // 移除全屏变化事件监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
  document.removeEventListener('msfullscreenchange', handleFullscreenChange);
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
});

// 智能调整大小
const smartResize = () => {
  if (!containerRef.value || !tinyflowRef.value || !tinyflow.value) return;
  
  try {
    // 让tinyflow自己处理尺寸调整
    if (typeof tinyflow.value.resize === 'function') {
      const containerRect = containerRef.value.getBoundingClientRect();
      tinyflow.value.resize(containerRect.width, containerRect.height);
    }
  } catch (error) {
    console.error('Failed to resize tinyflow:', error);
  }
};

// 保存工作流
const saveWorkflow = () => {
  try {
    // 使用统一的节点检查函数
    if (!checkNodesExist()) {
      showMessage(i18n('tinyflow.noNodes'), 'error');
      return;
    }
    
    // 优先使用Tinyflow API获取最新的工作流数据
    let workflowData;
    if (tinyflow.value && typeof tinyflow.value.getData === 'function') {
      workflowData = tinyflow.value.getData();
    } else {
      workflowData = currentWorkflowData.value;
    }
    
    localStorage.setItem('tinyflow-workflow', JSON.stringify(workflowData));
    console.log('Workflow saved:', workflowData);
    showMessage(i18n('tinyflow.success'), 'success');
  } catch (error) {
    console.error('Failed to save workflow:', error);
    showMessage(i18n('tinyflow.error'), 'error');
  }
};

// 运行工作流
const runWorkflow = () => {
  try {
    // 使用统一的节点检查函数
    if (!checkNodesExist()) {
      showMessage(i18n('tinyflow.noNodes'), 'error');
      return;
    }
    
    // 优先使用Tinyflow API获取最新的工作流数据
    let workflowData;
    if (tinyflow.value && typeof tinyflow.value.getData === 'function') {
      workflowData = tinyflow.value.getData();
    } else {
      workflowData = currentWorkflowData.value;
    }
    
    isRunning.value = true;
    showResults.value = true;
    runResults.value = [];
    
    // 模拟SSE流式运行结果，使用实际的工作流节点
    simulateSSEWorkflow(workflowData.nodes);
  } catch (error) {
    console.error('Failed to run workflow:', error);
    isRunning.value = false;
    showMessage(i18n('tinyflow.error'), 'error');
  }
};

// 模拟SSE流式运行结果
const simulateSSEWorkflow = (nodes) => {
  let currentIndex = 0;
  
  // 格式化时间
  const formatTime = () => {
    const now = new Date();
    return now.toLocaleTimeString();
  };
  
  const simulateNodeResult = () => {
    if (currentIndex >= nodes.length) {
      // 添加工作流完成结果
      runResults.value.push({
        node: '工作流',
        content: `<div class="result-success">\n  <div class="result-item-header">\n    <span class="result-status">✓</span>\n    <span class="result-time">${formatTime()}</span>\n  </div>\n  <div class="result-item-content">\n    <div>工作流执行完成</div>\n    <div class="result-meta">共执行 ${nodes.length} 个节点</div>\n  </div>\n</div>`
      });
      
      isRunning.value = false;
      showMessage(i18n('tinyflow.success'), 'success');
      return;
    }
    
    const node = nodes[currentIndex];
    let status = '';
    let statusClass = '';
    let nodeInfo = '';
    
    // 根据节点类型生成不同的内容
    switch (node.type) {
      case 'start':
        status = '▶';
        statusClass = 'result-info';
        nodeInfo = '<div class="result-meta">工作流启动节点</div>';
        break;
      case 'end':
        status = '✓';
        statusClass = 'result-success';
        nodeInfo = '<div class="result-meta">工作流结束节点</div>';
        break;
      case 'llm':
        status = '🤖';
        statusClass = 'result-info';
        nodeInfo = '<div class="result-meta">大语言模型节点</div>';
        break;
      case 'prompt':
        status = '💬';
        statusClass = 'result-info';
        nodeInfo = '<div class="result-meta">提示词节点</div>';
        break;
      case 'condition':
        status = '🔀';
        statusClass = 'result-info';
        nodeInfo = '<div class="result-meta">条件判断节点</div>';
        break;
      case 'code':
        status = '💻';
        statusClass = 'result-info';
        nodeInfo = '<div class="result-meta">代码执行节点</div>';
        break;
      default:
        status = '▶';
        statusClass = 'result-info';
        nodeInfo = `<div class="result-meta">未知节点类型: ${node.type}</div>`;
        break;
    }
    
    // 生成富文本结果内容
    const content = `
      <div class="${statusClass}">
        <div class="result-item-header">
          <span class="result-status">${status}</span>
          <span class="result-time">${formatTime()}</span>
        </div>
        <div class="result-item-content">
          <div>执行节点: <strong>${node.label || node.id}</strong></div>
          ${nodeInfo}
          ${node.config ? `<div class="result-config">配置: ${JSON.stringify(node.config).substring(0, 100)}${node.config.length > 100 ? '...' : ''}</div>` : ''}
        </div>
      </div>
    `;
    
    runResults.value.push({
      node: node.label || node.id,
      content: content
    });
    
    currentIndex++;
    
    // 模拟流式延迟
    setTimeout(simulateNodeResult, 800);
  };
  
  // 添加工作流开始信息
  runResults.value.push({
    node: '工作流',
    content: `<div class="result-info">\n  <div class="result-item-header">\n    <span class="result-status">▶</span>\n    <span class="result-time">${formatTime()}</span>\n  </div>\n  <div class="result-item-content">\n    <div>开始执行工作流</div>\n    <div class="result-meta">包含 ${nodes.length} 个节点</div>\n  </div>\n</div>`
  });
  
  // 开始模拟
  setTimeout(simulateNodeResult, 500);
};

// 导出工作流
const exportWorkflow = () => {
  try {
    // 使用统一的节点检查函数
    if (!checkNodesExist()) {
      showMessage(i18n('tinyflow.noNodes'), 'error');
      return;
    }
    
    // 优先使用Tinyflow API获取最新的工作流数据
    let workflowData;
    if (tinyflow.value && typeof tinyflow.value.getData === 'function') {
      workflowData = tinyflow.value.getData();
    } else {
      workflowData = currentWorkflowData.value;
    }
    
    const dataStr = JSON.stringify(workflowData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'workflow.json';
    link.click();
    URL.revokeObjectURL(url);
    console.log('Workflow exported:', workflowData);
    showMessage(i18n('tinyflow.success'), 'success');
  } catch (error) {
    console.error('Failed to export workflow:', error);
    showMessage(i18n('tinyflow.error'), 'error');
  }
};

// 显示消息
const showMessage = (message, type) => {
  // 这里可以集成现有的消息提示组件
  console.log(`${type}: ${message}`);
  // 示例：如果有全局消息组件，可以这样调用
  // this.$message({
  //   message: message,
  //   type: type
  // });
};

// 切换全屏
const toggleFullscreen = () => {
  const container = containerRef.value;
  if (!container) return;
  
  try {
    if (!isFullscreen.value) {
      // 进入全屏
      if (container.requestFullscreen) {
        container.requestFullscreen();
      } else if (container.webkitRequestFullscreen) {
        container.webkitRequestFullscreen();
      } else if (container.msRequestFullscreen) {
        container.msRequestFullscreen();
      } else if (container.mozRequestFullScreen) {
        container.mozRequestFullScreen();
      }
    } else {
      // 退出全屏
      if (document.exitFullscreen) {
        document.exitFullscreen();
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      } else if (document.msExitFullscreen) {
        document.msExitFullscreen();
      } else if (document.mozCancelFullScreen) {
        document.mozCancelFullScreen();
      }
    }
  } catch (error) {
    console.error('Failed to toggle fullscreen:', error);
    showMessage('切换全屏失败', 'error');
  }
};

// 监听全屏状态变化
const handleFullscreenChange = () => {
  isFullscreen.value = !!(document.fullscreenElement || 
    document.webkitFullscreenElement || 
    document.msFullscreenElement || 
    document.mozFullScreenElement);
};

// 将 $i 函数暴露给模板
defineExpose({
  $i
});

// 导出名称
// 注意：在Composition API中，组件名称不需要显式导出
</script>

<style scoped>
/* 基础样式 */
.magic-tinyflow-container {
  flex: 1;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--main-background-color);
  overflow: hidden;
}

/* 顶部操作栏 */
.tinyflow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background-color: var(--header-background-color);
  border-bottom: 1px solid var(--border-color);
  height: 40px;
}

.tinyflow-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color);
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tinyflow-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background-color: #fff3cd;
  color: #856404;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #ffeeba;
  border-radius: 4px;
  animation: fadeIn 0.3s ease-in-out;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.tinyflow-tip:hover {
  background-color: #ffeeba;
  border-color: #ffc107;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.tinyflow-tip svg {
  width: 18px;
  height: 18px;
  fill: #856404;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.tinyflow-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background-color: var(--button-background-color);
  color: var(--button-text-color);
  border: 2px solid var(--border-color);
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
  font-weight: 500;
}

.tinyflow-btn:hover {
  background-color: var(--button-hover-background-color);
  border-color: var(--button-hover-border-color);
}

/* 下拉树选择器样式 */
.workflow-selector-dropdown {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 下拉触发器 */
.dropdown-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background-color: var(--input-background-color);
  border: 2px solid var(--border-color);
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-color);
  width: 250px;
  transition: all 0.2s ease;
}

.dropdown-toggle:hover {
  background-color: var(--hover-background-color);
  border-color: var(--button-hover-border-color);
}

.selected-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.workflow-selector-dropdown .dropdown-arrow {
  transform: rotate(0deg);
  transition: transform 0.2s ease;
}

.workflow-selector-dropdown .dropdown-arrow {
  transform: rotate(0deg);
}

.workflow-selector-dropdown[open] .dropdown-arrow {
  transform: rotate(180deg);
}

.isDropdownOpen .dropdown-arrow {
  transform: rotate(180deg);
}

/* 下拉内容 */
.dropdown-content {
  position: absolute;
  top: 100%;
  left: 0;
  width: 250px;
  max-height: 300px;
  overflow-y: auto;
  border: 2px solid var(--border-color);
  border-radius: 3px;
  background-color: var(--input-background-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  margin-top: 4px;
}

.workflow-tree-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background-color: var(--input-background-color);
}


/* 工作流选择列表 */
.workflow-select-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 空选项 */
.workflow-select-option {
  padding: 6px 8px;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-color);
  background-color: var(--input-background-color);
  border-radius: 2px;
  transition: background-color 0.2s ease;
  margin-bottom: 2px;
}

.workflow-select-option:hover {
  background-color: var(--hover-background-color);
}

.empty-option-text {
  font-style: italic;
  color: var(--text-muted-color);
}

/* 树形结构 */
.workflow-tree {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 工作流节点 */
.workflow-node {
  display: flex;
  flex-direction: column;
}

/* 节点基础样式 */
.node-item {
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-radius: 2px;
}

/* 节点内容 */
.node-content {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
}

/* 文件夹节点 */
.folder-node .node-content {
  font-weight: 600;
}

.folder-node:hover .node-content {
  background-color: var(--hover-background-color);
}

/* 文件节点 */
.file-node:hover .node-content {
  background-color: var(--hover-background-color);
}

/* 选中节点 */
.node-item.selected .node-content {
  background-color: var(--active-background-color);
  color: var(--active-text-color);
}

/* 图标样式 */
.folder-toggle-icon,
.folder-icon,
.file-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--icon-color);
  transition: transform 0.2s ease;
}

/* 文件夹图标 */
.folder-icon {
  color: #6c757d;
}

/* 文件图标 */
.file-icon {
  color: #20c997;
}

/* 展开/折叠图标 */
.folder-toggle-icon {
  cursor: pointer;
  transition: transform 0.2s ease;
}

/* 展开状态 - 修复选择器 */
.folder-node .folder-toggle-icon {
  transform: rotate(0deg);
  transition: transform 0.2s ease;
}

/* 基于数据属性的展开状态 */
.folder-node:has(.node-content[data-opened="true"]) .folder-toggle-icon {
  transform: rotate(90deg);
}

/* 基于父元素class的展开状态 */
.folder-node.opened .folder-toggle-icon {
  transform: rotate(90deg);
}

/* 确保在Vue模板中正确应用展开状态 */
.folder-node .folder-toggle-icon {
  transform-origin: center center;
}

/* 优化文件夹图标样式 */
.folder-icon {
  color: #6c757d;
  transition: color 0.2s ease;
}

.folder-node:hover .folder-icon {
  color: var(--button-hover-text-color);
}

/* 节点名称 */
.node-name {
  font-size: 12px;
  color: var(--resource-label-color);
  flex: 1;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

/* 子节点容器 */
.child-nodes {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid var(--border-color);
}

/* 子节点 */
.child-nodes .node-item {
  margin: 0;
}

/* 滚动条样式 */
.workflow-tree-container::-webkit-scrollbar {
  width: 8px;
}

.workflow-tree-container::-webkit-scrollbar-track {
  background: var(--scrollbar-track-color);
  border-radius: 4px;
}

.workflow-tree-container::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb-color);
  border-radius: 4px;
}

.workflow-tree-container::-webkit-scrollbar-thumb:hover {
  background: var(--scrollbar-thumb-hover-color);
}

/* 确保树形组件内容完整显示 */
.workflow-tree-container {
  overflow-y: auto;
  overflow-x: hidden;
}

/* 优化节点间距 */
.node-item {
  margin: 1px 0;
}

/* 优化嵌套层级显示 */
.child-nodes .child-nodes {
  border-left: 1px solid var(--border-color);
}

/* 选中状态优化 */
.workflow-select-option.selected {
  background-color: var(--active-background-color);
  color: var(--active-text-color);
}

/* 禁用状态 */
.node-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.node-item.disabled:hover .node-content {
  background-color: transparent;
}

/* 优化图标间距 */
.node-content svg {
  flex-shrink: 0;
}

/* 优化点击区域 */
.node-content {
  user-select: none;
}

/* 优化文字显示 */
.node-name {
  font-family: var(--font-family);
}

/* 确保根节点没有左边框 */
.workflow-tree > .workflow-node .node-content {
  padding-left: 8px;
}

/* 确保子节点有适当的缩进 */
.child-nodes .node-content {
  padding-left: 8px;
}

/* 禁用状态样式 - 确保优先级 */
.tinyflow-btn:disabled,
.tinyflow-btn[disabled] {
  opacity: 0.5 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
  background-color: var(--button-background-color) !important;
  color: var(--button-text-color) !important;
  border: 2px solid var(--border-color) !important;
}

/* 确保悬停样式不影响禁用状态 */
.tinyflow-btn:disabled:hover,
.tinyflow-btn[disabled]:hover {
  background-color: var(--button-background-color) !important;
  color: var(--button-text-color) !important;
  border-color: var(--border-color) !important;
}

/* 主内容区 */
.tinyflow-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 编辑器面板 */
.editor-panel {
  flex: 1;
  height: 100%;
  overflow: hidden;
  position: relative;
}

/* Tinyflow编辑器 */
.tinyflow-editor {
  width: 100%;
  height: 100%;
  background-color: white;
}

/* 结果面板 */
.results-panel {
  width: 350px;
  background-color: #f8f9fa;
  border-left: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #e9ecef;
  background-color: white;
  height: 48px;
}

.results-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #212529;
  letter-spacing: 0.5px;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  color: #6c757d;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  width: 28px;
  height: 28px;
}

.close-btn:hover {
  background-color: #e9ecef;
  color: #495057;
}

.results-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #f8f9fa;
}

/* 运行指示器 */
.running-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0d6efd;
  padding: 16px 0;
  background-color: white;
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e9ecef;
  border-top-color: #0d6efd;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 结果列表 */
.results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  background-color: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.result-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #dee2e6;
}

.result-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #212529;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-content {
  font-size: 12px;
  color: #495057;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-content pre {
  background-color: #f1f3f5;
  padding: 10px;
  border-radius: 6px;
  overflow-x: auto;
  font-family: 'JetBrainsMono', 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  margin: 8px 0;
  border: 1px solid #e9ecef;
}

.result-content code {
  background-color: #f1f3f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrainsMono', 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  color: #dc3545;
}

/* 无结果状态 */
.no-results {
  color: #6c757d;
  text-align: center;
  padding: 40px 20px;
  font-size: 13px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* 状态样式 */
.result-success {
  color: #198754;
  font-weight: 600;
}

.result-info {
  color: #0d6efd;
  font-weight: 600;
}

.result-error {
  color: #dc3545;
  font-weight: 600;
}

/* 为结果标题添加状态图标 */
.result-title::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #6c757d;
  display: inline-block;
}

.result-item:nth-child(odd) .result-title::before {
  background-color: #0d6efd;
}

.result-item:nth-child(even) .result-title::before {
  background-color: #198754;
}

/* 优化滚动条样式 */
.results-content::-webkit-scrollbar {
  width: 6px;
}

.results-content::-webkit-scrollbar-track {
  background: #f1f3f5;
  border-radius: 3px;
}

.results-content::-webkit-scrollbar-thumb {
  background: #adb5bd;
  border-radius: 3px;
}

.results-content::-webkit-scrollbar-thumb:hover {
  background: #6c757d;
}

/* 结果项内部样式 */
.result-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px dashed #e9ecef;
}

.result-status {
  font-size: 14px;
  font-weight: bold;
}

.result-time {
  font-size: 11px;
  color: #6c757d;
  font-family: 'JetBrainsMono', monospace;
}

.result-item-content {
  font-size: 12px;
  line-height: 1.6;
}

.result-meta {
  color: #6c757d;
  margin-top: 4px;
  font-size: 11px;
}

.result-config {
  margin-top: 6px;
  padding: 6px 8px;
  background-color: #f8f9fa;
  border-left: 3px solid #0d6efd;
  border-radius: 0 4px 4px 0;
  font-family: 'JetBrainsMono', monospace;
  font-size: 11px;
  color: #495057;
  overflow-x: auto;
  white-space: nowrap;
}

/* 确保组件能正确占满resizer容器 */
:deep(.magic-toolbar .magic-resizer-y) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

:deep(.magic-toolbar .magic-resizer-y .magic-toolbar-header + div) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>