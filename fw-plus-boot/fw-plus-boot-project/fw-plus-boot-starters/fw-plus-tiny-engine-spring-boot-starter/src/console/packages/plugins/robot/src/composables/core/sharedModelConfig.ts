import { reactive, readonly } from 'vue'
import type {
  AppModelSettings,
  AppRole,
  ModelConfig,
  ModelImportRequest,
  ModelService,
  ModelSettingsStorage,
  ModelSelection
} from '../../types/setting.types'

const SETTING_STORAGE_KEY = 'tiny-engine-robot-settings'
const MANAGER_ROLES: AppRole[] = ['app_owner', 'app_admin']
const MIGRATED_FIELDS = ['services', 'defaultModel', 'quickModel']

interface ModelConfigApi {
  listUsable(appId: number): Promise<ModelService[]>
  listManageable(appId: number): Promise<ModelService[]>
  getSettings(appId: number): Promise<AppModelSettings>
  updateSettings(appId: number, settings: AppModelSettings): Promise<AppModelSettings>
  importLocal(appId: number, request: ModelImportRequest): Promise<ModelService[]>
}

const parseCapabilities = (value: unknown) => {
  if (!value || typeof value !== 'string') return value || {}
  try {
    return JSON.parse(value)
  } catch (_error) {
    return {}
  }
}

const normalizeServices = (services: ModelService[] = [], includeDisabled = false): ModelService[] =>
  services
    .filter((service) => includeDisabled || service?.enabled !== false)
    .map((service) => ({
      ...service,
      models: (service.models || [])
        .filter((model) => includeDisabled || model?.enabled !== false)
        .map((model) => ({
          ...model,
          capabilities: parseCapabilities(model.capabilities),
          settings: parseCapabilities(model.settings)
        }))
    }))

const parseSelection = (value: unknown): ModelSelection => {
  if (typeof value !== 'string') return { serviceId: '', modelName: '' }
  const separator = value.includes('::') ? '::' : ':'
  const [serviceId = '', modelName = ''] = value.split(separator)
  return { serviceId, modelName }
}

const resolveSelection = (value: unknown, services: ModelService[]): ModelSelection => {
  const parsed = parseSelection(value)
  const service = services.find(
    (item) => String(item.id) === String(parsed.serviceId) || item.serviceKey === parsed.serviceId
  )
  return {
    serviceId: service ? String(service.id) : parsed.serviceId,
    modelName: parsed.modelName
  }
}

const hasRemoteSettings = (settings: AppModelSettings | null | undefined) =>
  Object.entries(settings || {}).some(
    ([key, value]) => key !== 'services' && value !== undefined && value !== null
  )

const parseLocal = (storage: ModelSettingsStorage): Record<string, any> | null => {
  const value = storage.getItem(SETTING_STORAGE_KEY)
  if (!value) return null
  try {
    return JSON.parse(value)
  } catch (_error) {
    return null
  }
}

const localServiceKey = (service: Record<string, any>) => String(service.serviceKey || service.id || service.provider || '')

const buildImportRequest = (local: Record<string, any>): ModelImportRequest | null => {
  if (!Array.isArray(local.services) || !local.services.length) return null
  const apiKeys: Record<string, string> = {}
  const services = local.services.map((service: Record<string, any>, serviceIndex: number) => {
    const serviceKey = localServiceKey(service)
    if (service.apiKey) apiKeys[serviceKey] = service.apiKey
    return {
      serviceKey,
      provider: service.provider || 'openai',
      label: service.label || serviceKey,
      baseUrl: service.baseUrl,
      allowEmptyApiKey: Boolean(service.allowEmptyApiKey),
      enabled: service.enabled !== false,
      sortOrder: service.sortOrder ?? serviceIndex,
      models: (service.models || []).map((model: Record<string, any>, modelIndex: number) => ({
        name: model.name,
        label: model.label || model.name,
        capabilities: typeof model.capabilities === 'string' ? model.capabilities : JSON.stringify(model.capabilities || {}),
        settings: typeof model.settings === 'string' ? model.settings : JSON.stringify(model.settings || {}),
        enabled: model.enabled !== false,
        sortOrder: model.sortOrder ?? modelIndex
      }))
    }
  })
  return {
    settings: {
      services,
      defaultModel:
        typeof local.defaultModel === 'string'
          ? local.defaultModel
          : local.defaultModel
            ? `${local.defaultModel.serviceId || ''}::${local.defaultModel.modelName || ''}`
            : undefined,
      quickModel:
        typeof local.quickModel === 'string'
          ? local.quickModel
          : local.quickModel
            ? `${local.quickModel.serviceId || ''}::${local.quickModel.modelName || ''}`
            : undefined,
      chatMode: local.chatMode,
      enableThinking: local.enableThinking
    },
    apiKeys
  }
}

const cleanupMigratedLocalFields = (storage: ModelSettingsStorage, local: Record<string, any>) => {
  const retained = { ...local }
  MIGRATED_FIELDS.forEach((key) => delete retained[key])
  storage.setItem(SETTING_STORAGE_KEY, JSON.stringify(retained))
}

export const createSharedModelConfig = (api: ModelConfigApi, storage: ModelSettingsStorage = localStorage) => {
  const state = reactive({
    appId: 0,
    role: '' as AppRole | '',
    canManage: false,
    services: [] as ModelService[],
    defaultModel: { serviceId: '', modelName: '' } as ModelSelection,
    quickModel: { serviceId: '', modelName: '' } as ModelSelection,
    chatMode: 'agent',
    enableThinking: false,
    loading: false
  })

  const load = async () => {
    const [services, settings] = await Promise.all([
      state.canManage ? api.listManageable(state.appId) : api.listUsable(state.appId),
      api.getSettings(state.appId)
    ])
    state.services = normalizeServices(services, state.canManage)
    state.defaultModel = resolveSelection(settings?.defaultModel, state.services)
    state.quickModel = resolveSelection(settings?.quickModel, state.services)
    state.chatMode = settings?.chatMode || 'agent'
    state.enableThinking = Boolean(settings?.enableThinking)
    return { services: state.services, settings }
  }

  const init = async (
    appId: number,
    role: AppRole | string,
    _settings: AppModelSettings = {}
  ) => {
    state.appId = Number(appId)
    state.role = role as AppRole
    state.canManage = MANAGER_ROLES.includes(role as AppRole)
    state.loading = true
    try {
      const remote = await load()
      const local = parseLocal(storage)
      const request =
        local && state.canManage && remote.services.length === 0 && !hasRemoteSettings(remote.settings)
          ? buildImportRequest(local)
          : null
      if (request) {
        await api.importLocal(state.appId, request)
        await load()
        cleanupMigratedLocalFields(storage, local as Record<string, any>)
      }
    } finally {
      state.loading = false
    }
  }

  const updateSettings = async (settings: AppModelSettings) => {
    const updated = await api.updateSettings(state.appId, settings)
    state.defaultModel = resolveSelection(updated?.defaultModel, state.services)
    state.quickModel = resolveSelection(updated?.quickModel, state.services)
    state.chatMode = updated?.chatMode || state.chatMode
    state.enableThinking = updated?.enableThinking === undefined ? state.enableThinking : Boolean(updated.enableThinking)
    return updated
  }

  const buildChatSelection = () => {
    const serviceId = Number(state.defaultModel.serviceId)
    return {
      appId: state.appId,
      serviceId: Number.isFinite(serviceId) ? serviceId : 0,
      modelName: state.defaultModel.modelName,
      chatMode: state.chatMode,
      enableThinking: state.enableThinking
    }
  }

  return {
    state: readonly(state),
    init,
    reload: load,
    updateSettings,
    buildChatSelection
  }
}

export default createSharedModelConfig
