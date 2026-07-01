<template>
  <div ref="containerRef" class="magic-s3-container">
    <div v-if="!loading && !hasS3Plugin" class="magic-s3-no-plugin">
      <magic-icon icon="s3" size="48px" />
      <p>{{ $i('s3.noPlugin') }}</p>
      <p class="magic-s3-no-plugin-subtext">{{ $i('s3.noPluginSubtext') }}</p>
    </div>
    <div v-else class="magic-s3-wrapper">
      <!-- Toolbar -->
      <div class="magic-s3-toolbar">
        <div class="magic-s3-toolbar-left">
        </div>
        <div class="magic-s3-path">
          <div class="magic-s3-path-segments">
            <div class="magic-s3-path-button" style="cursor: default; pointer-events: none;">
              <span style="font-weight: bold;">/</span>
            </div>
            <span></span>
            <template v-for="(segment, index) in pathSegments" :key="index">
              <span v-if="index > 0" class="magic-s3-path-separator">/</span>
              <button class="magic-s3-path-button" @click="changePath(segment.path)">
                {{ segment.name }}
              </button>
            </template>
          </div>
        </div>
        <div class="magic-s3-toolbar-actions">
          <div class="magic-s3-search-container">
            <magic-input 
              v-model:value="searchQuery" 
              :placeholder="$i('s3.search')" 
              width="250px"
              @input="handleSearch"
              :disabled="loading || isLoadingMore"
            />
            <button v-if="searchQuery && !loading && !isLoadingMore" class="magic-s3-search-clear" @click="clearSearch">
              <magic-icon icon="close" size="12px" />
            </button>
          </div>

          <div class="magic-s3-storage-selector">
            <button class="magic-s3-button" @click="toggleStorageMenu" :disabled="loading || isLoadingMore">
              <magic-icon icon="s3" size="16px" />
              <span class="magic-s3-button-text">{{ activeStorage ? activeStorage.name : $i('s3.loading') }}</span>
              <magic-icon icon="arrow-bottom" size="12px" />
            </button>
            <div v-if="showStorageMenu && storagesArray.length > 0 && !loading && !isLoadingMore" class="magic-s3-dropdown">
              <div 
                v-for="storage in storagesArray" 
                :key="storage.key"
                class="magic-s3-dropdown-item"
                :class="{ active: activeStorage && storage.key === activeStorage.key }"
                @click="changeStorage(storage)"
              >
                {{ storage.name }}
              </div>
            </div>
          </div>
          
          <!-- BasePath Edit -->
          <div v-if="activeStorage && activeStorage.key" class="magic-s3-basepath-edit">
            <div v-if="!showBasePathEdit" class="magic-s3-basepath-display">
              <span class="magic-s3-basepath-label">{{ $i('s3.basePath') }}:</span>
              <span class="magic-s3-basepath-value">{{ activeStorage.basePath || '/' }}</span>
              <button class="magic-s3-basepath-edit-button" @click="openBasePathEdit" :disabled="loading || isLoadingMore">
                <magic-icon icon="close" size="12px" />
              </button>
            </div>
            <div v-else class="magic-s3-basepath-input-container">
              <magic-input 
                v-model:value="editBasePath" 
                placeholder="{{ $i('s3.enterBasePath') }}"
                width="120px"
                @keyup.enter="saveBasePath"
                :disabled="loading || isLoadingMore"
              />
              <button class="magic-s3-basepath-save" @click="saveBasePath" :disabled="loading || isLoadingMore">
                {{ $i('s3.save') }}
              </button>
              <button class="magic-s3-basepath-cancel" @click="cancelBasePathEdit" :disabled="loading || isLoadingMore">
                {{ $i('s3.cancel') }}
              </button>
            </div>
          </div>

          <input type="file" ref="fileInput" multiple style="display: none" @change="handleFileUpload" />
        </div>
      </div>

      <!-- Main Content -->
      <div class="magic-s3-main">
        <!-- File List -->
        <div 
          class="magic-s3-list"
          @dragover.prevent
          @dragenter.prevent
          @drop.prevent="handleDrop"
        >
          <div v-if="loading" class="magic-s3-loading">
            <magic-icon icon="loading" size="24px" />
            <span>{{ $i('s3.loading') }}</span>
          </div>
          <div v-else-if="files.length === 0" class="magic-s3-empty">
            <magic-icon icon="empty" size="48px" />
            <p>{{ $i('s3.noFiles') }}</p>
          </div>
          <div v-else class="magic-s3-files">
            <div v-for="item in combinedItems" :key="item.path" class="magic-s3-file-item" @click="handleItemClick(item)">
              <div class="magic-s3-file-icon">
                <magic-icon :icon="getFileIcon(item.name, item.type)" size="24px" />
              </div>
              <div class="magic-s3-file-info">
                <div class="magic-s3-file-name" v-html="highlightText(item.name, searchQuery)"></div>
              </div>
            </div>
            <!-- Loading more indicator -->
            <div v-if="isLoadingMore" class="magic-s3-loading-more">
              <span>{{ $i('s3.loadingMore') }}</span>
            </div>
            <!-- Load more button -->
            <div v-else-if="nextMarker" class="magic-s3-load-more-button-container">
              <button class="magic-s3-load-more-button" @click="loadMoreFiles">
                {{ $i('s3.loadMore') }}
              </button>
            </div>
          </div>
        </div>
        
        <!-- File Details Panel -->
        <div v-if="selectedFile" class="magic-s3-details-panel">
          <div class="magic-s3-details-header">
            <h3>{{ selectedFile.name }}</h3>
            <div class="magic-s3-details-header-actions">
              <button class="magic-s3-details-button" @click="generatePresignedUrl(selectedFile)" :title="$i('s3.preview')">
                <magic-icon icon="eye" size="16px" />
              </button>
              <button class="magic-s3-details-button" @click="confirmDeleteFile(selectedFile)" :title="$i('s3.delete')">
                <magic-icon icon="delete" size="16px" />
              </button>
              <button class="magic-s3-details-close" @click="selectedFile = null">
                <magic-icon icon="close" size="16px" />
              </button>
            </div>
          </div>
          <div class="magic-s3-details-content">
            <MagicMonacoEditor :value="formatJson(selectedFile)" language="json" :readonly="true" />
          </div>
        </div>
      </div>

      <!-- Delete Confirmation Dialog -->
      <div v-if="showDeleteDialog" class="magic-s3-dialog-overlay" @click="showDeleteDialog = false">
        <div class="magic-s3-dialog" @click.stop>
          <div class="magic-s3-dialog-header">
            <h3>{{ fileToDelete.type === 'folder' ? $i('s3.deleteFolder') : $i('s3.deleteFile') }}</h3>
            <button class="magic-s3-dialog-close" @click="showDeleteDialog = false">
              <magic-icon icon="close" size="16px" />
            </button>
          </div>
          <div class="magic-s3-dialog-body">
            <p>{{ $i('s3.confirmDelete', fileToDelete.name) }}</p>
            <p v-if="fileToDelete.type === 'folder'" class="magic-s3-dialog-warning">{{ $i('s3.confirmDeleteFolder') }}</p>
            <p class="magic-s3-dialog-warning">{{ $i('s3.deleteWarning') }}</p>
          </div>
          <div class="magic-s3-dialog-footer">
            <button class="magic-s3-dialog-button" @click="showDeleteDialog = false">{{ $i('s3.cancel') }}</button>
            <button 
              class="magic-s3-dialog-button magic-s3-dialog-button-danger" 
              @click="confirmDelete"
            >
              {{ $i('s3.delete') }}
            </button>
          </div>
        </div>
      </div>

      <!-- File Preview Dialog -->
      <div v-if="showPreviewDialog" class="magic-s3-preview-overlay" @click="showPreviewDialog = false">
        <div class="magic-s3-preview-container" @click.stop>
          <button class="magic-s3-preview-close" @click="showPreviewDialog = false">
            <magic-icon icon="close" size="16px" />
          </button>
          <div class="magic-s3-preview-body">
            <div class="magic-s3-preview-jit">
              <div ref="viewerRef" style="width: 100%; height: 500px;"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Rename Dialog -->
      <div v-if="showRenameDialog" class="magic-s3-dialog-overlay" @click="showRenameDialog = false">
        <div class="magic-s3-dialog" @click.stop>
          <div class="magic-s3-dialog-header">
            <h3>{{ fileToRename.type === 'folder' ? $i('s3.renameFolder') : $i('s3.renameFile') }}</h3>
            <button class="magic-s3-dialog-close" @click="showRenameDialog = false">
              <magic-icon icon="close" size="16px" />
            </button>
          </div>
          <div class="magic-s3-dialog-body">
            <input 
              type="text" 
              v-model="newFileName" 
              class="magic-s3-dialog-input"
              :placeholder="fileToRename.type === 'folder' ? $i('s3.enterFolderName') : $i('s3.enterFileName')"
              @keyup.enter="confirmRename"
            />
          </div>
          <div class="magic-s3-dialog-footer">
            <button class="magic-s3-dialog-button" @click="showRenameDialog = false">{{ $i('s3.cancel') }}</button>
            <button 
              class="magic-s3-dialog-button magic-s3-dialog-button-primary" 
              @click="confirmRename"
              :disabled="!newFileName.trim()"
            >
              {{ $i('s3.rename') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// Constants
let availableStorages = []

const endpoints = {
  list: { url: "/storage/{storage}/list?path={path}", method: "get" },
  upload: { url: "/storage/{storage}/upload?path={path}", method: "post" },
  mkdir: { url: "/storage/{storage}/mkdir?path={path}", method: "post" },
  delete: { url: "/storage/{storage}/delete?path={path}", method: "post" },
  download: { url: "/storage/{storage}/download?path={path}", method: "get" }
}

const fileIcons = {
  zip: "folder",
  rar: "folder",
  htm: "script",
  html: "script",
  js: "script",
  json: "script",
  md: "script",
  pdf: "file",
  png: "file",
  jpg: "file",
  jpeg: "file",
  mp4: "file",
  mkv: "file",
  avi: "file",
  wmv: "file",
  mov: "file",
  txt: "file",
  xls: "file",
  other: "file"
}

export default {
  props: {
    // comma-separated list of active storage keys
    storages: {
      type: String,
      default: () => availableStorages.map(item => item.key).join(",")
    },
    // key of default storage
    storage: { type: String, default: "s3" },
    // show tree view
    tree: { type: Boolean, default: true },
    // custom backend endpoints
    endpoints: { type: Object, default: () => endpoints },
    // custom axios instance
    axios: { type: Function },
    // custom configuration for internal axios instance
    axiosConfig: { type: Object, default: () => {} }
  },
  components: {
    MagicMonacoEditor
  },
  emits: ['change']
}
</script>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import $i from '../../../scripts/i18n.js'
import MagicIcon from '../../common/icon/magic-icon.vue'
import MagicMonacoEditor from '../../common/magic-monaco-editor.vue'
import request from '../../../scripts/request.js'
import constants from '../../../scripts/constants.js'
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'
import { createViewer } from 'jit-viewer'
import 'jit-viewer/style.css'

// Props
const props = defineProps()

// Emits
const emit = defineEmits(['change'])

// Constants (imported from the script block above)
// availableStorages will be fetched from API

const endpoints = {
  list: { url: "/storage/{storage}/list?path={path}", method: "get" },
  upload: { url: "/storage/{storage}/upload?path={path}", method: "post" },
  mkdir: { url: "/storage/{storage}/mkdir?path={path}", method: "post" },
  delete: { url: "/storage/{storage}/delete?path={path}", method: "post" },
  download: { url: "/storage/{storage}/download?path={path}", method: "get" }
}

const fileIcons = {
  zip: "folder",
  rar: "folder",
  htm: "script",
  html: "script",
  js: "script",
  json: "script",
  md: "script",
  pdf: "file",
  png: "file",
  jpg: "file",
  jpeg: "file",
  mp4: "file",
  mkv: "file",
  avi: "file",
  wmv: "file",
  mov: "file",
  txt: "file",
  xls: "file",
  other: "file"
}

// State
const loading = ref(false)
const hasS3Plugin = ref(true)
const path = ref("")
const activeStorage = ref(null) // Will be set after fetching data
const showStorageMenu = ref(false)
const showDeleteDialog = ref(false)
const showPreviewDialog = ref(false)
const showRenameDialog = ref(false)
const fileToDelete = ref(null)
const fileToPreview = ref(null)
const fileToRename = ref(null)
const newFileName = ref("")
const previewContent = ref("")
const containerRef = ref(null)
const searchQuery = ref("")
const files = ref([])
const nextMarker = ref(null)
const isLoadingMore = ref(false)
const selectedFile = ref(null)
const showBasePathEdit = ref(false)
const editBasePath = ref("")
const viewerRef = ref(null)
let viewerInstance = null

// Computed
const storagesArray = computed(() => {
  // If availableStorages is not yet loaded, return empty array
  if (!availableStorages || availableStorages.length === 0) {
    return []
  }
  
  let storageKeys = (props.storages || "").split(","),
      result = []
  
  // Always include the first item ("Please select" option)
  if (availableStorages.length > 0) {
    result.push(availableStorages[0])
  }
  
  // If no specific storages are specified, return all available storages including "Please select"
  if (!storageKeys || storageKeys.length === 0 || (storageKeys.length === 1 && storageKeys[0] === "")) {
    return availableStorages
  }
  
  // Add other storages that match the keys
  storageKeys.forEach(key => {
    const storage = availableStorages.find(item => item.key == key)
    if (storage && storage.key) {
      result.push(storage)
    }
  })
  
  return result.length > 0 ? result : availableStorages
})

const pathSegments = computed(() => {
  const currentPath = path.value || ""
  let resultPath = "/",
      isFolder = currentPath && currentPath[currentPath.length - 1] === "/",
      segments = currentPath ? currentPath.split("/").filter(item => item) : []

  segments = segments.map((item, index) => {
    resultPath += item + (index < segments.length - 1 || isFolder ? "/" : "")
    return {
      name: item,
      path: resultPath
    }
  })

  return segments
})

const combinedItems = computed(() => {
  // Use files.value directly since it contains both folders and files
  // Search is now handled by the API, so no need for client-side filtering
  return files.value
})

// Mock data for demonstration
const treeItems = ref([
  { name: "documents", path: "/documents/" },
  { name: "images", path: "/images/" },
  { name: "videos", path: "/videos/" }
])

// Methods
const toggleStorageMenu = () => {
  showStorageMenu.value = !showStorageMenu.value
}

const changeStorage = (storage) => {
  activeStorage.value = storage
  showStorageMenu.value = false
  // Close details panel when storage changes
  selectedFile.value = null
  // Set path to basePath from storage if available, otherwise use /
  let basePath = storage.basePath || "/"
  if (!basePath.endsWith('/')) {
    basePath = basePath + '/'
  }
  path.value = basePath
  emit('change', basePath)
  // Only load files if a storage is selected (not "Please select")
  if (storage.key && storage.key !== '') {
    // Always load files when storage changes
    loadFiles(basePath)
  } else {
    // Clear files when "Please select" is selected
    files.value = []
  }
}

const changePath = (newPath) => {
  // Check if newPath is a full path (starts with /)
  let updatedPath
  if (newPath.startsWith('/')) {
    // If it's a full path (like when clicking path segments), use it directly
    updatedPath = newPath
  } else {
    // If it's just a folder name (like when clicking folders), append to current path
    updatedPath = path.value + newPath
  }
  // Ensure path starts with /
  if (!updatedPath.startsWith('/')) {
    updatedPath = '/' + updatedPath
  }
  // Remove duplicate slashes
  updatedPath = updatedPath.replace(/\/+/g, '/')
  // Ensure path ends with / for folders
  if (!updatedPath.endsWith('/')) {
    updatedPath = updatedPath + '/'
  }
  
  // Check if the path is the same as current path
  if (updatedPath !== path.value) {
    path.value = updatedPath
    emit('change', updatedPath)
    // Only load files if activeStorage is initialized
    if (activeStorage.value && activeStorage.value.key) {
      // Load files for the new path
      loadFiles(updatedPath)
    }
    // Update last clicked type to folder since we're navigating to a directory
    lastClickedType.value = 'folder'
  }
}



const deleteFile = (file) => {
  fileToDelete.value = file
  showDeleteDialog.value = true
}

const confirmDelete = () => {
  if (fileToDelete.value) {
    // Here you would typically call the API to delete the file
    console.log('Deleting file:', fileToDelete.value.name)
    // Refresh file list
    loadFiles(path.value)
    showDeleteDialog.value = false
    fileToDelete.value = null
  }
}

// Preview file
const previewFile = (file) => {
  fileToPreview.value = file
  
  showPreviewDialog.value = true
  
  // Wait for the dialog to be rendered, then create jit-viewer instance
  setTimeout(() => {
    console.log('=== jit-viewer initialization start ===')
    console.log('File:', file)
    console.log('File URL:', file.url)
    console.log('Viewer ref:', viewerRef.value)
    console.log('Is image file:', isImageFile(file.name))
    console.log('Is text file:', isTextFile(file.name))
    console.log('createViewer available:', typeof createViewer === 'function')
    
    // Use jit-viewer for all file types
    console.log('Creating jit-viewer for file:', file.name)
    
    // Ensure viewerRef.value exists
    if (!viewerRef.value) {
      console.error('Viewer ref is null or undefined')
      // Try again after a short delay
      setTimeout(() => {
        console.log('Retrying jit-viewer initialization...')
        if (viewerRef.value) {
          initializeJitViewer(file)
        } else {
          console.error('Still no viewer ref available')
        }
      }, 200)
      return
    }
    
    initializeJitViewer(file)
    
    console.log('=== jit-viewer initialization end ===')
  }, 500)
}

// Initialize jit-viewer instance
const initializeJitViewer = (file) => {
  console.log('Initializing jit-viewer with file:', file.name)
  
  // Destroy existing instance if it exists
  if (viewerInstance) {
    console.log('Destroying existing viewer instance')
    try {
      viewerInstance.destroy()
    } catch (e) {
      console.error('Error destroying viewer instance:', e)
    }
    viewerInstance = null
  }
  
  // Create new jit-viewer instance
  console.log('Creating new jit-viewer instance with file:', file.url)
  try {
    // Clear the container first
    viewerRef.value.innerHTML = ''
    console.log('Container cleared')
    
    // Set dimensions for the container
    viewerRef.value.style.width = '100%'
    viewerRef.value.style.height = '500px'
    viewerRef.value.style.border = '1px solid #ccc'
    
    // Verify container dimensions
    console.log('Container dimensions:', viewerRef.value.offsetWidth, 'x', viewerRef.value.offsetHeight)
    
    // Use imported createViewer function
    if (typeof createViewer === 'function') {
      console.log('Using imported createViewer')
      try {
        // Create jit-viewer instance
        console.log('Creating jit-viewer with file URL:', file.url)
        
        // Check if viewerRef.value is a valid DOM element
        if (!(viewerRef.value instanceof HTMLElement)) {
          throw new Error('viewerRef.value is not a valid DOM element')
        }
        
        viewerInstance = createViewer({
          file: file.url,
          target: viewerRef.value,
          width: '100%',
          height: '500px',
          theme: 'light',
          toolbar: true,
          fullscreen: true,
          onReady: () => {
            console.log('jit-viewer ready')
            console.log('Viewer instance after ready:', viewerInstance)
          },
          onLoad: () => {
            console.log('jit-viewer file loaded')
          },
          onError: (err) => {
            console.error('jit-viewer error:', err)
            // Show error message in the container
            viewerRef.value.innerHTML = `<div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: red;">
              <p>Error loading file:</p>
              <p>${err.message || 'Unknown error'}</p>
            </div>`
          }
        })
        console.log('Viewer instance created successfully:', viewerInstance)
        
        // Check if the instance has a mount method
        if (viewerInstance && typeof viewerInstance.mount === 'function') {
          console.log('Calling mount method')
          viewerInstance.mount()
          console.log('Mount method called')
        } else {
          console.log('No mount method found on viewer instance')
        }
      } catch (createError) {
        console.error('Error creating jit-viewer instance:', createError)
        console.error('Error stack:', createError.stack)
        throw createError
      }
    } else {
      throw new Error('createViewer is not available')
    }
  } catch (e) {
    console.error('Error creating jit-viewer:', e)
    // Fallback: show unsupported message
    const fallbackDiv = document.createElement('div')
    fallbackDiv.style.display = 'flex'
    fallbackDiv.style.flexDirection = 'column'
    fallbackDiv.style.alignItems = 'center'
    fallbackDiv.style.justifyContent = 'center'
    fallbackDiv.style.height = '100%'
    
    const p = document.createElement('p')
    p.textContent = $i('s3.previewUnsupported')
    fallbackDiv.appendChild(p)
    
    const errorP = document.createElement('p')
    errorP.textContent = 'Error: ' + e.message
    errorP.style.color = 'red'
    errorP.style.fontSize = '12px'
    fallbackDiv.appendChild(errorP)
    
    const button = document.createElement('button')
    button.className = 'magic-s3-dialog-button'
    button.textContent = $i('s3.downloadFile')
    button.onclick = () => downloadFile(fileToPreview.value)
    fallbackDiv.appendChild(button)
    
    // Clear the container and add fallback
    if (viewerRef.value) {
      viewerRef.value.innerHTML = ''
      viewerRef.value.appendChild(fallbackDiv)
    }
  }
}

// Handle search
const handleSearch = () => {
  // Call loadFiles with searchQuery as filenamePrefix
  if (activeStorage.value && activeStorage.value.key) {
    loadFiles(path.value, false, searchQuery.value)
  }
}

// Clear search
const clearSearch = () => {
  searchQuery.value = ""
  // Reload files without search query
  if (activeStorage.value && activeStorage.value.key) {
    loadFiles(path.value, false, "")
  }
}

// Highlight text in search results
const highlightText = (text, query) => {
  if (!query || !text) return text
  
  const lowerText = text.toLowerCase()
  const lowerQuery = query.toLowerCase()
  const index = lowerText.indexOf(lowerQuery)
  
  if (index === -1) return text
  
  const start = text.substring(0, index)
  const match = text.substring(index, index + lowerQuery.length)
  const end = text.substring(index + lowerQuery.length)
  
  return start + '<span style="background-color: #FFDE7B; color: #000000; font-weight: bold;">' + match + '</span>' + end
}



// Check if file is an image
const isImageFile = (fileName) => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp']
  const extension = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return imageExtensions.includes(extension)
}

// Check if file is a text file
const isTextFile = (fileName) => {
  const textExtensions = ['.txt', '.js', '.json', '.html', '.css', '.md', '.xml', '.csv']
  const extension = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return textExtensions.includes(extension)
}

// Get file URL for preview
const getFileUrl = (file) => {
  // In a real implementation, this would return a signed URL or similar
  return `https://via.placeholder.com/300x200?text=${encodeURIComponent(file.name)}`
}

// Get icon based on file type
const getFileIcon = (fileName, type) => {
  if (type === 'folder') {
    return 'list'
  }
  
  if (!fileName) {
    return 'save'
  }
  
  const extension = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  
  const iconMap = {
    '.jpg': 'save',
    '.jpeg': 'save',
    '.png': 'save',
    '.gif': 'save',
    '.webp': 'save',
    '.bmp': 'save',
    '.svg': 'save',
    '.pdf': 'save',
    '.doc': 'save',
    '.docx': 'save',
    '.xls': 'save',
    '.xlsx': 'save',
    '.ppt': 'save',
    '.pptx': 'save',
    '.txt': 'save',
    '.js': 'script',
    '.json': 'json',
    '.html': 'save',
    '.css': 'save',
    '.md': 'save',
    '.xml': 'save',
    '.csv': 'save',
    '.zip': 'save',
    '.rar': 'save',
    '.7z': 'save',
    '.tar': 'save',
    '.gz': 'save',
    '.mp4': 'save',
    '.avi': 'save',
    '.mkv': 'save',
    '.mov': 'save',
    '.mp3': 'save',
    '.wav': 'save',
    '.flac': 'save'
  }
  
  return iconMap[extension] || 'save'
}

// Rename file or folder
const renameFile = (file) => {
  fileToRename.value = file
  newFileName.value = file.name
  showRenameDialog.value = true
}

const confirmRename = () => {
  if (fileToRename.value && newFileName.value.trim()) {
    // Here you would typically call the API to rename the file
    console.log('Renaming', fileToRename.value.name, 'to', newFileName.value)
    // Refresh file list
    loadFiles(path.value)
    showRenameDialog.value = false
    fileToRename.value = null
    newFileName.value = ''
  }
}

const generatePresignedUrl = (file) => {
  if (!activeStorage.value || !activeStorage.value.key) {
    console.error('No active storage selected')
    return
  }
  
  const storageKey = activeStorage.value.key
  
  console.log('Generating presigned URL for:', file.name, 'with url:', file.url, 'basePath:', file.basePath, 'path:', file.path, 'fileName:', file.filename || file.name)
  
  request.sendGet(`/s3/datasource/${storageKey}/generateUploadPresignedUrl`, {
    url: file.url,
    basePath: file.basePath,
    path: file.path,
    fileName: file.filename || file.name
  })
    .success(data => {
      console.log('Presigned URL generated:', data)
      
      let urlToUse = null
      
      // Check if data is a JSON object with data field
      if (data && data.data) {
        // Clean the URL by removing backticks and spaces
        urlToUse = data.data.trim().replace(/`/g, '')
      } else if (typeof data === 'string') {
        // API returned directly the URL string
        urlToUse = data.trim().replace(/`/g, '')
      }
      
      console.log('URL to use for preview:', urlToUse)
      
      if (urlToUse) {
        // Create a temporary file object with the presigned URL
        const tempFile = {
          ...file,
          url: urlToUse
        }
        
        // Use the previewFile function to show the preview dialog with jit-viewer
        previewFile(tempFile)
      }
    })
    .error((data, response, error) => {
      console.error('Failed to generate presigned URL:', error)
    })
    .exception((code, message) => {
      console.warn('API returned error:', code, message)
    })
}

const confirmDeleteFile = (file) => {
  if (!activeStorage.value || !activeStorage.value.key) {
    console.error('No active storage selected')
    return
  }
  
  if (!file) {
    console.error('No file selected for deletion')
    return
  }
  
  if (confirm('确定要删除文件 ' + file.name + ' 吗？')) {
    const storageKey = activeStorage.value.key
    
    console.log('Deleting file:', file.name, 'with url:', file.url)
    
    request.sendGet(`/s3/datasource/${storageKey}/delete`, {
      url: file.url
    })
      .success(data => {
        console.log('File deleted successfully:', data)
        // Close details panel
        selectedFile.value = null
        // Refresh file list
        loadFiles(path.value)
      })
      .error((data, response, error) => {
        console.error('Failed to delete file:', error)
      })
      .exception((code, message) => {
        console.warn('API returned error:', code, message)
      })
  }
}

const downloadFile = (file) => {
  console.log('Download file function called with:', file)
  
  if (!activeStorage.value || !activeStorage.value.key) {
    console.error('No active storage selected')
    return
  }
  
  const storageKey = activeStorage.value.key
  
  console.log('Downloading file:', file.name, 'with url:', file.url, 'basePath:', file.basePath, 'path:', file.path, 'fileName:', file.filename || file.name)
  
  // Generate presigned URL first
  request.sendGet(`/s3/datasource/${storageKey}/generateUploadPresignedUrl`, {
    url: file.url,
    basePath: file.basePath,
    path: file.path,
    fileName: file.filename || file.name
  })
    .success(data => {
      console.log('Presigned URL generated for download:', data)
      
      let urlToUse = null
      
      // Check if data is a JSON object with data field
      if (data && data.data) {
        // Clean the URL by removing backticks and spaces
        urlToUse = data.data.trim().replace(/`/g, '')
      } else if (typeof data === 'string') {
        // API returned directly the URL string
        urlToUse = data.trim().replace(/`/g, '')
      }
      
      console.log('URL to use for download:', urlToUse)
      
      // Check if URL is valid
      if (!urlToUse || urlToUse === '') {
        console.error('Invalid URL after cleaning')
        return
      }
      
      // Use a more reliable method to trigger download
      try {
        const a = document.createElement('a')
        a.href = urlToUse
        a.target = '_blank'
        a.download = file.name || 'download'
        console.log('Creating download link with filename:', file.name || 'download')
        document.body.appendChild(a)
        console.log('Clicking download link')
        a.click()
        setTimeout(() => {
          document.body.removeChild(a)
          console.log('Removed download link')
        }, 100)
      } catch (e) {
        console.error('Error triggering download:', e)
      }
    })
    .error((data, response, error) => {
      console.error('Failed to generate presigned URL for download:', error)
      console.error('Error response:', data, 'Response:', response)
    })
    .exception((code, message) => {
      console.warn('API returned error:', code, message)
    })
}

const loadFiles = (path, isLoadMore = false, filenamePrefix = '') => {
  if (isLoadMore) {
    // Check if there's a nextMarker before loading more
    if (!nextMarker.value) {
      console.log('No more data to load')
      isLoadingMore.value = false
      return
    }
    isLoadingMore.value = true
  } else {
    loading.value = true
    // Reset nextMarker when loading new path
    nextMarker.value = null
  }
  
  // Call the API to get files
  if (!activeStorage.value) {
    console.error('No active storage selected')
    loading.value = false
    isLoadingMore.value = false
    return
  }
  
  const storageKey = activeStorage.value.key
  if (!storageKey || storageKey === '') {
    console.error('Invalid storage key')
    loading.value = false
    isLoadingMore.value = false
    return
  }
  
  // Build the API URL
  let apiUrl = `/s3/datasource/${storageKey}/listFilesResult`
  const params = {}
  
  // basePath 使用存储的 basePath（最右边的基础路径）
  let storageBasePath = activeStorage.value.basePath || '/'
  if (!storageBasePath.endsWith('/')) {
    storageBasePath = storageBasePath + '/'
  }
  params.basePath = storageBasePath

  // path 为当前路径去除 basePath 前缀后的部分
  let currentPath = path || '/'
  if (!currentPath.startsWith('/')) {
    currentPath = '/' + currentPath
  }
  if (currentPath.startsWith(storageBasePath)) {
    params.path = currentPath.substring(storageBasePath.length)
  } else {
    params.path = currentPath
  }

  // Add filenamePrefix if provided
  if (filenamePrefix) {
    params.filenamePrefix = filenamePrefix
  }
  
  // Add marker if nextMarker exists
  if (nextMarker.value) {
    params.marker = nextMarker.value
  }
  
  console.log('Loading files from:', apiUrl, 'with params:', params)
  
  request.sendGet(apiUrl, params)
    .success(data => {
      loading.value = false
      isLoadingMore.value = false
      console.log('API response data:', data)
      console.log('API response data type:', typeof data)
      console.log('API response data keys:', Object.keys(data))
      if (data) {
        // Combine directories and files into a single list
        const combined = []
        
        // Add directories
        if (data.dirList && Array.isArray(data.dirList)) {
          console.log('Found directories:', data.dirList.length)
          data.dirList.forEach(dir => {
            combined.push({
              name: dir.name,
              path: dir.path || '',
              type: 'folder',
              size: 0,
              modified: dir.lastModified || new Date()
            })
          })
        } else if (data.directories && Array.isArray(data.directories)) {
          console.log('Found directories (alternative structure):', data.directories.length)
          data.directories.forEach(dir => {
            combined.push({
              name: dir.name,
              path: dir.path || '',
              type: 'folder',
              size: 0,
              modified: dir.lastModified || new Date()
            })
          })
        } else if (data.folders && Array.isArray(data.folders)) {
          console.log('Found directories (folders structure):', data.folders.length)
          data.folders.forEach(dir => {
            combined.push({
              name: dir.name,
              path: dir.path || '',
              type: 'folder',
              size: 0,
              modified: dir.lastModified || new Date()
            })
          })
        } else {
          console.log('No directories found')
        }
        
        // Add files
              if (data.fileList && Array.isArray(data.fileList)) {
                console.log('Found files:', data.fileList.length)
                data.fileList.forEach(file => {
                  combined.push({
                    name: file.filename || file.name,
                    path: file.path || '',
                    type: 'file',
                    size: file.size || 0,
                    modified: file.lastModified || new Date(),
                    url: file.url || '',
                    platform: file.platform || '',
                    basePath: file.basePath || '',
                    filename: file.filename || file.name,
                    ext: file.ext || '',
                    contentDisposition: file.contentDisposition,
                    contentType: file.contentType,
                    contentMd5: file.contentMd5,
                    metadata: file.metadata || {},
                    userMetadata: file.userMetadata || {},
                    original: file.original,
                    etag: file.etag
                  })
                })
              } else if (data.files && Array.isArray(data.files)) {
                console.log('Found files (alternative structure):', data.files.length)
                data.files.forEach(file => {
                  combined.push({
                    name: file.filename || file.name,
                    path: file.path || '',
                    type: 'file',
                    size: file.size || 0,
                    modified: file.lastModified || new Date(),
                    url: file.url || '',
                    platform: file.platform || '',
                    basePath: file.basePath || '',
                    filename: file.filename || file.name,
                    ext: file.ext || '',
                    contentDisposition: file.contentDisposition,
                    contentType: file.contentType,
                    contentMd5: file.contentMd5,
                    metadata: file.metadata || {},
                    userMetadata: file.userMetadata || {},
                    original: file.original,
                    etag: file.etag
                  })
                })
              } else {
                console.log('No files found')
              }
        
        console.log('Combined items:', combined.length)
        
        // Update files and nextMarker
        if (isLoadMore) {
          // Append to existing files
          files.value = [...files.value, ...combined]
        } else {
          // Replace existing files
          files.value = combined
        }
        
        // Update nextMarker
        nextMarker.value = data.nextMarker !== undefined ? data.nextMarker : (data.marker !== undefined ? data.marker : null)
        console.log('Next marker:', nextMarker.value)
        console.log('Is load more:', isLoadMore)
      } else {
        console.log('No data returned from API')
        if (!isLoadMore) {
          files.value = []
        }
      }
    })
    .error((data, response, error) => {
      loading.value = false
      isLoadingMore.value = false
      console.error('Failed to load files:', error)
      console.error('Error response:', response)
      console.error('Error data:', data)
      console.error('API URL:', apiUrl)
      console.error('Params:', params)
      if (!isLoadMore) {
        files.value = []
      }
    })
    .exception((code, message) => {
      loading.value = false
      isLoadingMore.value = false
      console.warn('API returned error:', code, message)
      console.warn('API URL:', apiUrl)
      console.warn('Params:', params)
      if (!isLoadMore) {
        files.value = []
      }
    })
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString()
}

// Format JSON for display
const formatJson = (obj) => {
  return JSON.stringify(obj, null, 2)
}

// Track last clicked item type
const lastClickedType = ref(null)

// Handle item click
const handleItemClick = (item) => {
  if (item.type === 'folder') {
    // path.value always ends with / for directories, so just append the folder name
    const updatedPath = path.value + item.name + '/'
    
    // Update path
    path.value = updatedPath
    emit('change', updatedPath)
    
    // Update last clicked type
    lastClickedType.value = 'folder'
    
    // Load files for the new folder
    loadFiles(updatedPath)
  } else if (item.type === 'file') {
    // Handle file click - show details in right panel
    selectedFile.value = item
  }
}

// BasePath Edit Methods
const openBasePathEdit = () => {
  if (activeStorage.value) {
    editBasePath.value = activeStorage.value.basePath || '/'
    showBasePathEdit.value = true
  }
}

const saveBasePath = () => {
  if (activeStorage.value) {
    // Update the basePath in activeStorage, ensure trailing /
    let basePath = editBasePath.value || '/'
    if (!basePath.endsWith('/')) {
      basePath = basePath + '/'
    }
    activeStorage.value.basePath = basePath
    // Update the current path to use the new basePath
    path.value = basePath
    emit('change', basePath)
    // Reload files with the new basePath
    if (activeStorage.value.key) {
      loadFiles(basePath)
    }
    // Close the edit mode
    showBasePathEdit.value = false
  }
}

const cancelBasePathEdit = () => {
  showBasePathEdit.value = false
  editBasePath.value = ''
}

// Fetch available storages from API
const fetchAvailableStorages = () => {
  // Check if s3 plugin is available by calling API
  request.sendGet('/s3/datasource/availableStorages')
    .success(data => {
      if (data && Array.isArray(data)) {
        // Add "Please select" option at the beginning
        availableStorages = [
          {
            name: $i('s3.pleaseSelect'),
            key: ""
          },
          ...data
        ]
        hasS3Plugin.value = true
        // Initialize activeStorage with the prop value if provided, otherwise use the first storage ("Please select")
        const storageFromProps = availableStorages.find(item => item.key == props.storage && item.key)
        if (storageFromProps) {
          activeStorage.value = storageFromProps
          // Set path to basePath from storage if available, otherwise use /
          let basePath = storageFromProps.basePath || "/"
          if (!basePath.endsWith('/')) {
            basePath = basePath + '/'
          }
          path.value = basePath
          emit('change', basePath)
          // Load files for the current path
          loadFiles(basePath)
        } else if (availableStorages.length > 0) {
          // Use the first storage ("Please select") by default
          activeStorage.value = availableStorages[0]
        }
      } else {
        // If response is not an array, no s3 plugin
        console.warn('API returned invalid response, no s3 plugin')
        hasS3Plugin.value = false
      }
    })
    .error((data, response, error) => {
      console.error('Failed to fetch available storages:', error)
      // If API call fails, no s3 plugin
      hasS3Plugin.value = false
    })
    .exception((code, message) => {
      console.warn('API returned error:', code, message)
      // If API returns error, no s3 plugin
      hasS3Plugin.value = false
    })
}

// Use fallback storages
const useFallbackStorages = () => {
  availableStorages = [
    {
      name: $i('s3.pleaseSelect'),
      key: ""
    },
    {
      name: "Local",
      key: "local",
      basePath: "/"
    },
    {
      name: "Amazon S3",
      key: "s3",
      basePath: "/"
    }
  ]
  // Use the first storage ("Please select") by default
  if (availableStorages.length > 0) {
    activeStorage.value = availableStorages[0]
  }
}

// Lifecycle
onMounted(() => {
  // 延迟加载：等登录完成后再调用 availableStorages 接口，避免 token 无效
  bus.$on(Message.LOGINED, fetchAvailableStorages)

  if (!path.value) {
    changePath("/")
  }
  // Add click outside event listener
  document.addEventListener('click', handleClickOutside)

  // Add scroll event listener for infinite scroll
  setTimeout(() => {
    const fileListContainer = document.querySelector('.magic-s3-list')
    if (fileListContainer) {
      fileListContainer.addEventListener('scroll', handleScroll)
    }
  }, 100)
})

onUnmounted(() => {
  // Remove LOGINED event listener
  bus.$off(Message.LOGINED, fetchAvailableStorages)
  // Remove click outside event listener
  document.removeEventListener('click', handleClickOutside)
  
  // Destroy jit-viewer instance
  if (viewerInstance) {
    viewerInstance.destroy()
    viewerInstance = null
  }
  
  // Remove scroll event listener
  const fileListContainer = document.querySelector('.magic-s3-list')
  if (fileListContainer) {
    fileListContainer.removeEventListener('scroll', handleScroll)
  }
})
// Handle scroll event for infinite scroll
const handleScroll = (event) => {
  const target = event.target
  // Check if we've reached the bottom of the scrollable area
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 100) {
    // Load more files if there's a next marker and we're not already loading
    if (nextMarker.value && !isLoadingMore.value && activeStorage.value && activeStorage.value.key) {
      loadFiles(path.value, true)
    }
  }
}
// Handle click outside to close dropdown
const handleClickOutside = (event) => {
  const storageSelector = document.querySelector('.magic-s3-storage-selector')
  if (storageSelector && !storageSelector.contains(event.target)) {
    showStorageMenu.value = false
  }
}

// Load more files
const loadMoreFiles = () => {
  console.log('loadMoreFiles called, nextMarker:', nextMarker.value, 'isLoadingMore:', isLoadingMore.value)
  if (nextMarker.value && !isLoadingMore.value) {
    loadFiles(path.value, true)
  } else {
    console.log('loadMoreFiles skipped, nextMarker:', nextMarker.value, 'isLoadingMore:', isLoadingMore.value)
  }
}

// Handle file upload
const handleFileUpload = (event) => {
  const files = event.target.files
  if (files && files.length > 0) {
    console.log('Files to upload:', files)
    // Here you would typically call the API to upload the files
  }
}

</script>

<style scoped>
.magic-s3-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: var(--main-background-color);
}

.magic-s3-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.magic-s3-no-plugin {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  color: var(--text-secondary-color);
}

.magic-s3-no-plugin icon {
  margin-bottom: 16px;
  color: var(--primary-color);
}

.magic-s3-no-plugin p {
  margin: 8px 0;
  font-size: 16px;
}

.magic-s3-no-plugin-subtext {
  font-size: 14px !important;
  opacity: 0.7;
}

.magic-s3-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: var(--main-background-color);
}



.magic-s3-content {
  flex: 1;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* Toolbar */
.magic-s3-toolbar {
  padding: 4px 6px;
  border-bottom: 1px solid var(--main-border-color);
  display: flex;
  align-items: center;
  background-color: var(--main-background-color);
  position: sticky;
  top: 0;
  z-index: 100;
}

.magic-s3-toolbar-left {
  display: flex;
  align-items: center;
  margin-right: 12px;
}

.magic-s3-path {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  overflow: hidden;
  justify-content: center;
}

.magic-s3-storage-selector {
  position: relative;
  white-space: nowrap;
  width: 200px;
  margin-left: 12px;
}

.magic-s3-button {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 6px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  font-size: 11px;
  cursor: pointer;
  width: 100%;
  box-sizing: border-box;
  justify-content: space-between;
}

.magic-s3-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-s3-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: var(--select-background-color);
}

.magic-s3-button-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0 4px;
}

.magic-s3-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 1px;
  border: 1px solid var(--main-border-color);
  border-radius: 3px;
  background-color: var(--main-background-color);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 250px;
}

.magic-s3-dropdown-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 11px;
  color: var(--main-color);
}

.magic-s3-dropdown-item:hover {
  background-color: var(--main-hover-background-color);
}

.magic-s3-dropdown-item.active {
  background-color: var(--main-hover-background-color);
  font-weight: bold;
}

.magic-s3-path-segments {
  display: flex;
  align-items: center;
  gap: 3px;
  flex: 1;
  overflow: hidden;
}

.magic-s3-path-button {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 1px 3px;
  border: none;
  background: none;
  color: var(--main-color);
  font-size: 11px;
  white-space: nowrap;
  cursor: pointer;
}

.magic-s3-path-button magic-icon {
  font-size: 16px;
}

.magic-s3-path-button:hover {
  background-color: var(--main-hover-background-color);
  border-radius: 2px;
}

.magic-s3-path-separator {
  color: var(--resource-span-color);
  font-size: 11px;
}

.magic-s3-toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* BasePath Edit */
.magic-s3-basepath-edit {
  display: flex;
  align-items: center;
  gap: 6px;
}

.magic-s3-basepath-display {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  height: 24px;
  box-sizing: border-box;
}

.magic-s3-basepath-label {
  font-size: 11px;
  color: var(--main-color);
  white-space: nowrap;
}

.magic-s3-basepath-value {
  font-size: 11px;
  color: var(--main-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.magic-s3-basepath-edit-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  cursor: pointer;
  font-size: 11px;
  padding: 0;
  transition: all 0.2s ease;
}

.magic-s3-basepath-edit-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-s3-basepath-edit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.magic-s3-basepath-input-container {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  height: 24px;
  box-sizing: border-box;
}

.magic-s3-basepath-save,
.magic-s3-basepath-cancel {
  padding: 3px 8px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 20px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}

.magic-s3-basepath-save:hover {
  background-color: var(--tab-selected-border-color);
  color: white;
  border-color: var(--tab-selected-border-color);
}

.magic-s3-basepath-cancel:hover {
  background-color: var(--main-hover-background-color);
}

.magic-s3-basepath-save:disabled,
.magic-s3-basepath-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.magic-s3-search-container {
  position: relative;
  display: inline-block;
}

.magic-s3-search-clear {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--main-color);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  border-radius: 2px;
}

.magic-s3-search-clear:hover {
  background-color: var(--main-hover-background-color);
}

.magic-s3-action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  cursor: pointer;
}

.magic-s3-action-button:hover {
  background-color: var(--select-hover-background-color);
}

/* Main Content */
.magic-s3-main {
  flex: 1;
  display: flex;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.magic-s3-list {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  position: relative;
}

/* File Details Panel */
.magic-s3-details-panel {
  width: 500px;
  height: 100%;
  border-left: 1px solid var(--main-border-color);
  background-color: var(--main-background-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
}

.magic-s3-details-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--main-background-color);
}

.magic-s3-details-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-s3-details-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.magic-s3-details-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  color: var(--main-color);
  cursor: pointer;
  border-radius: 4px;
  padding: 0;
  transition: background-color 0.3s ease;
}

.magic-s3-details-close:hover {
  background-color: red;
  color: white;
}

.magic-s3-details-content {
  flex: 1;
  padding: 0;
  overflow: hidden;
  background-color: var(--main-background-color);
  display: flex;
  flex-direction: column;
}

.magic-s3-details-json {
  margin: 0;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  line-height: 1.4;
  color: var(--main-color);
  background-color: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.magic-s3-details-content :deep(.magic-monaco-editor) {
  width: 100%;
  height: 100%;
  flex: 1;
}

.magic-s3-details-button {
  padding: 6px;
  border: none;
  border-radius: 3px;
  background: none;
  color: var(--main-color);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.magic-s3-details-button:hover {
  background-color: var(--main-hover-background-color);
}

.magic-s3-details-button-primary {
  background: none;
  color: var(--tab-selected-border-color);
  border: none;
}

.magic-s3-details-button-primary:hover {
  background-color: var(--main-hover-background-color);
  border: none;
}

/* Animation keyframes */
@keyframes s3-spin {
  from {
    transform: rotate(0deg) scale(1);
  }
  50% {
    transform: rotate(180deg) scale(1.1);
  }
  to {
    transform: rotate(360deg) scale(1);
  }
}

@keyframes s3-pulse {
  0% {
    opacity: 1;
    transform: scale(1);
    color: var(--resource-span-color);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.05);
    color: var(--tab-selected-border-color);
  }
  100% {
    opacity: 1;
    transform: scale(1);
    color: var(--resource-span-color);
  }
}

@keyframes s3-glow {
  0% {
    box-shadow: 0 0 5px rgba(52, 152, 219, 0.3);
  }
  50% {
    box-shadow: 0 0 20px rgba(52, 152, 219, 0.6);
  }
  100% {
    box-shadow: 0 0 5px rgba(52, 152, 219, 0.3);
  }
}

.magic-s3-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 16px;
  color: var(--resource-span-color);
  padding: 20px;
  border-radius: 8px;
  background-color: rgba(255, 255, 255, 0.05);
  animation: s3-glow 2s ease-in-out infinite;
}

.magic-s3-loading magic-icon {
  animation: s3-spin 1.2s ease-in-out infinite;
  font-size: 32px;
  margin-bottom: 8px;
  color: var(--tab-selected-border-color);
}

.magic-s3-loading span {
  animation: s3-pulse 1.5s ease-in-out infinite;
  font-size: 14px;
  font-weight: 500;
}

.magic-s3-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 12px;
  color: var(--resource-span-color);
}

.magic-s3-loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: var(--resource-span-color);
  font-size: 13px;
  font-weight: 500;
  animation: s3-pulse 1.5s ease-in-out infinite;
  border-radius: 8px;
  background-color: rgba(255, 255, 255, 0.05);
  margin: 16px 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  flex-basis: 100%;
  order: 999;
}

.magic-s3-load-more-button-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  width: 100%;
  margin-top: 16px;
  flex-basis: 100%;
  order: 999;
}

.magic-s3-load-more-button {
  padding: 8px 20px;
  border: 1px solid var(--tab-selected-border-color);
  border-radius: 20px;
  background-color: var(--select-background-color);
  color: var(--tab-selected-border-color);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.magic-s3-load-more-button:hover {
  background-color: var(--tab-selected-border-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}



.magic-s3-files {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  padding-bottom: 6px;
  overflow: visible;
  position: relative;
  justify-content: flex-start;
}

.magic-s3-file-item {
  display: flex;
  flex-direction: column;
  padding: 3px;
  border: none;
  border-radius: 2px;
  background-color: var(--main-background-color);
  transition: all 0.2s ease;
  box-sizing: border-box;
  width: 80px;
  height: 80px;
  align-items: center;
  text-align: center;
}

.magic-s3-file-item:hover {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.magic-s3-file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 55px;
  width: 100%;
  color: var(--main-icon-color);
  margin-bottom: 2px;
}

.magic-s3-file-icon magic-icon {
  font-size: 35px;
}

.magic-s3-file-info {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.magic-s3-file-name {
  font-size: 9px;
  font-weight: 500;
  color: var(--main-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.magic-s3-search-highlight {
  background-color: #FFDE7B;
  color: #000000;
  font-weight: bold;
}

.magic-s3-file-meta {
  font-size: 6px;
  color: var(--resource-span-color);
}

.magic-s3-file-actions {
  display: flex;
  gap: 1px;
  justify-content: flex-end;
}

.magic-s3-file-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border: 1px solid var(--input-border-color);
  border-radius: 2px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  cursor: pointer;
  font-size: 7px;
}

.magic-s3-file-action:hover {
  background-color: var(--select-hover-background-color);
}

/* Dialog */
.magic-s3-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.magic-s3-dialog {
  width: 400px;
  max-width: 90%;
  border: 1px solid var(--main-border-color);
  border-radius: 8px;
  background-color: var(--main-background-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.magic-s3-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
}

.magic-s3-dialog-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-s3-dialog-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  color: var(--main-color);
  cursor: pointer;
  border-radius: 4px;
}

.magic-s3-dialog-close:hover {
  background-color: var(--main-hover-background-color);
}

.magic-s3-dialog-body {
  padding: 16px;
}

.magic-s3-dialog-input {
  width: 100%;
  padding: 8px;
  border: 1px solid var(--input-border-color);
  border-radius: 4px;
  background-color: var(--select-inputable-background-color);
  color: var(--main-color);
  font-size: 12px;
  box-sizing: border-box;
}

.magic-s3-dialog-input:focus {
  outline: none;
  border-color: var(--main-hover-background-color);
}

.magic-s3-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--main-border-color);
}

.magic-s3-dialog-button {
  padding: 6px 12px;
  border: 1px solid var(--input-border-color);
  border-radius: 4px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  font-size: 12px;
  cursor: pointer;
}

.magic-s3-dialog-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-s3-dialog-button-primary {
  background-color: var(--tab-selected-background-color);
  color: white;
  border-color: var(--tab-selected-border-color);
}

.magic-s3-dialog-button-primary:hover {
  opacity: 0.8;
}

.magic-s3-dialog-button-danger {
  background-color: #e74c3c;
  color: white;
  border-color: #c0392b;
}

.magic-s3-dialog-button-danger:hover {
  opacity: 0.8;
  background-color: #c0392b;
}

.magic-s3-dialog-button-primary {
  background-color: #3498db;
  color: white;
  border-color: #2980b9;
}

.magic-s3-dialog-button-primary:hover {
  opacity: 0.8;
  background-color: #2980b9;
}

.magic-s3-dialog-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--input-border-color);
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  margin-top: 8px;
}

.magic-s3-dialog-input:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.magic-s3-dialog-warning {
  color: #e74c3c;
  font-size: 12px;
  margin-top: 8px;
}

.magic-s3-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.magic-s3-preview-container {
  background-color: var(--main-background-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 1000px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.magic-s3-preview-close {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--main-color);
  transition: all 0.2s ease;
  z-index: 1000;
}

.magic-s3-preview-close:hover {
  background-color: red;
  color: white;
}

.magic-s3-preview-body {
  flex: 1;
  padding: 16px;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.magic-s3-preview-image {
  max-width: 100%;
  max-height: 60vh;
  overflow: hidden;
}

.magic-s3-preview-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.magic-s3-preview-text {
  width: 100%;
  max-height: 60vh;
  overflow: auto;
  background-color: white;
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  padding: 12px;
}

.magic-s3-preview-text pre {
  margin: 0;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.magic-s3-preview-unsupported {
  text-align: center;
  padding: 40px 20px;
  color: var(--main-color);
}

.magic-s3-preview-unsupported p {
  margin-bottom: 20px;
}

.magic-s3-preview-footer {
  padding: 16px;
  border-top: 1px solid var(--main-border-color);
  background-color: var(--main-background-color);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.magic-s3-dialog-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Upload */
.magic-s3-upload-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.magic-s3-upload-container {
  width: 500px;
  max-width: 90%;
  border: 1px solid var(--main-border-color);
  border-radius: 8px;
  background-color: var(--main-background-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.magic-s3-upload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
}

.magic-s3-upload-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-s3-upload-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  color: var(--main-color);
  cursor: pointer;
  border-radius: 4px;
}

.magic-s3-upload-close:hover {
  background-color: var(--main-hover-background-color);
}

.magic-s3-upload-list {
  padding: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.magic-s3-upload-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--main-border-color);
}

.magic-s3-upload-item:last-child {
  border-bottom: none;
}

.magic-s3-upload-name {
  flex: 1;
  font-size: 12px;
  color: var(--main-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.magic-s3-upload-progress {
  flex: 2;
  height: 8px;
  background-color: var(--main-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.magic-s3-upload-progress-bar {
  height: 100%;
  background-color: var(--tab-selected-border-color);
  transition: width 0.3s ease;
}

.magic-s3-upload-status {
  width: 40px;
  font-size: 10px;
  color: var(--resource-span-color);
  text-align: right;
}

/* Selected Files Dialog */
.magic-s3-files-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.magic-s3-files-container {
  width: 350px;
  max-width: 90%;
  border: 1px solid var(--main-border-color);
  border-radius: 6px;
  background-color: var(--main-background-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
}

.magic-s3-files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--main-border-color);
}

.magic-s3-files-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  background: none;
  color: var(--main-color);
  cursor: pointer;
  border-radius: 2px;
  padding: 0;
}

.magic-s3-files-close:hover {
  background-color: var(--main-hover-background-color);
}

.magic-s3-files-header h3 {
  margin: 0;
  font-size: 12px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-s3-files-list {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  max-height: 200px;
}

.magic-s3-files-item {
  display: flex;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid var(--main-border-color);
}

.magic-s3-files-item:last-child {
  border-bottom: none;
}

.magic-s3-files-info {
  flex: 1;
}

.magic-s3-files-name {
  font-size: 11px;
  color: var(--main-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 1px;
}

.magic-s3-files-size {
  font-size: 9px;
  color: var(--resource-span-color);
}

.magic-s3-upload-progress-container {
  padding: 12px;
  border-top: 1px solid var(--main-border-color);
}

.magic-s3-upload-progress-item {
  margin-bottom: 8px;
}

.magic-s3-upload-progress-item:last-child {
  margin-bottom: 0;
}

.magic-s3-upload-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3px;
}

.magic-s3-upload-progress-name {
  font-size: 11px;
  color: var(--main-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.magic-s3-upload-progress-status {
  font-size: 9px;
  color: var(--resource-span-color);
  margin-left: 6px;
}

.magic-s3-upload-progress-bar-container {
  height: 6px;
  background-color: var(--main-border-color);
  border-radius: 3px;
  overflow: hidden;
}

.magic-s3-upload-progress-bar {
  height: 100%;
  background-color: var(--tab-selected-border-color);
  transition: width 0.3s ease;
}

.magic-s3-files-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--main-border-color);
}

.magic-s3-files-button {
  padding: 6px 12px;
  border: 1px solid var(--input-border-color);
  border-radius: 4px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  font-size: 12px;
  cursor: pointer;
}

.magic-s3-files-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-s3-files-button-primary {
  background-color: var(--tab-selected-background-color);
  color: white;
  border-color: var(--tab-selected-border-color);
}

.magic-s3-files-button-primary:hover {
  opacity: 0.8;
}
</style>