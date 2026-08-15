/**
 * Copyright (c) 2023 - present TinyEngine Authors.
 * Copyright (c) 2023 - present Huawei Cloud Computing Technologies Co., Ltd.
 *
 * Use of this source code is governed by an MIT-style license.
 */

/* metaService: engine.plugins.robot.useRobot */
import { reactive, readonly } from 'vue'
import { getMetaApi, META_SERVICE } from '@opentiny/tiny-engine-meta-register'
import {
  isValidJsonPatchObjectString,
  getRobotServiceOptions,
  addSystemPrompt,
  jsonPatchAutoFix,
  isValidFastJsonPatch,
  getJsonObjectString,
  fixMethods,
  schemaAutoFix
} from '../../utils'
import { formatComponents, getAgentSystemPrompt, getJsonFixPrompt } from '../../constants/prompts'
import { ChatMode } from '../../types/mode.types'
import type { AppRole, ModelConfig, ModelService, ModelServiceMutation, RobotSettings, SelectedModelInfo } from '../../types/setting.types'
import modelConfigApi from '../../services/modelConfigApi'
import { createSharedModelConfig } from './sharedModelConfig'
import { updatePageSchema } from './pageUpdater'

const robotSettingState = reactive<RobotSettings>({
  version: 3,
  appId: 0,
  currentRole: '',
  canManage: false,
  defaultModel: { serviceId: '', modelName: '' },
  quickModel: { serviceId: '', modelName: '' },
  services: [],
  chatMode: ChatMode.Agent,
  enableThinking: false
})

const applyRemoteState = (shared: ReturnType<typeof createSharedModelConfig>) => {
  robotSettingState.appId = shared.state.appId
  robotSettingState.currentRole = shared.state.role
  robotSettingState.canManage = shared.state.canManage
  robotSettingState.services = [...shared.state.services]
  robotSettingState.defaultModel = { ...shared.state.defaultModel }
  robotSettingState.quickModel = { ...shared.state.quickModel }
  robotSettingState.chatMode = shared.state.chatMode
  robotSettingState.enableThinking = shared.state.enableThinking
}

let sharedConfig: ReturnType<typeof createSharedModelConfig> | null = null

const reload = async () => {
  if (!sharedConfig) return
  await sharedConfig.reload()
  applyRemoteState(sharedConfig)
}

export const init = async () => {
  const baseInfo = getMetaApi(META_SERVICE.GlobalService).getBaseInfo()
  const appId = Number(baseInfo?.id)
  if (!appId) return
  const app = await getMetaApi(META_SERVICE.Http).get(`/app-center/api/apps/detail/${appId}`)
  const role = (app?.currentRole || '') as AppRole
  sharedConfig = createSharedModelConfig(modelConfigApi)
  await sharedConfig.init(appId, role)
  applyRemoteState(sharedConfig)
}

const saveRobotSettingState = async (state: Partial<RobotSettings>) => {
  if (!sharedConfig) return
  await sharedConfig.updateSettings({
    ...(state.defaultModel
      ? { defaultModel: `${state.defaultModel.serviceId || ''}::${state.defaultModel.modelName || ''}` }
      : {}),
    ...(state.quickModel ? { quickModel: `${state.quickModel.serviceId || ''}::${state.quickModel.modelName || ''}` } : {}),
    ...(state.chatMode !== undefined ? { chatMode: state.chatMode } : {}),
    ...(state.enableThinking !== undefined ? { enableThinking: state.enableThinking } : {})
  })
  applyRemoteState(sharedConfig)
}

const loadRobotSettingState = () => ({
  chatMode: robotSettingState.chatMode,
  enableThinking: robotSettingState.enableThinking
})

const getModelCapabilities = (serviceId: string, modelName: string) => {
  const service = robotSettingState.services.find((item) => String(item.id) === String(serviceId))
  return service?.models.find((model) => model.name === modelName)?.capabilities as any
}

const getAllAvailableModels = () =>
  robotSettingState.services
    .filter((service) => service.enabled !== false)
    .flatMap((service) =>
      service.models.filter((model) => model.enabled !== false).map((model) => ({
        serviceId: String(service.id),
        serviceName: service.label,
        modelName: model.name,
        modelLabel: model.label,
        capabilities: model.capabilities || {},
        displayLabel: `${service.label} - ${model.label}`,
        value: `${service.id}::${model.name}`
      }))
    )

const getCompactModels = () => getAllAvailableModels().filter((model) => (model.capabilities as any)?.compact)

const updateThinkingState = (value: boolean) => {
  robotSettingState.enableThinking = value
  void saveRobotSettingState({ enableThinking: value }).catch(() => reload())
}
const updateChatModeState = (value: string) => {
  robotSettingState.chatMode = value
  void saveRobotSettingState({ chatMode: value }).catch(() => reload())
}

const requireManage = () => {
  if (!robotSettingState.canManage) throw new Error('TinyEngine application permission denied')
}

const modelMutation = (model: ModelConfig) => ({
  name: model.name,
  label: model.label,
  capabilities: typeof model.capabilities === 'string' ? model.capabilities : JSON.stringify(model.capabilities || {}),
  settings: typeof model.settings === 'string' ? model.settings : JSON.stringify(model.settings || {}),
  enabled: model.enabled !== false,
  sortOrder: model.sortOrder || 0
})

const serviceMutation = (service: Partial<ModelService> & ModelServiceMutation): ModelServiceMutation => ({
  serviceKey: service.serviceKey || service.provider,
  provider: service.provider,
  label: service.label,
  baseUrl: service.baseUrl,
  apiKey: service.apiKey,
  clearApiKey: service.clearApiKey,
  allowEmptyApiKey: service.allowEmptyApiKey,
  enabled: service.enabled !== false,
  sortOrder: service.sortOrder || 0
})

const addCustomService = async (service: Partial<ModelService> & ModelServiceMutation) => {
  requireManage()
  const created = await modelConfigApi.createService(
    robotSettingState.appId,
    serviceMutation({ ...service, serviceKey: service.serviceKey || `custom-${Date.now()}` })
  )
  for (const model of service.models || []) await modelConfigApi.addModel(robotSettingState.appId, created.id, modelMutation(model))
  await reload()
  return created.id
}

const updateService = async (serviceId: number | string, updates: Partial<ModelService> & ModelServiceMutation) => {
  requireManage()
  const previous = getServiceById(serviceId)
  await modelConfigApi.updateService(robotSettingState.appId, serviceId, serviceMutation({ ...previous, ...updates }))
  const nextModels = updates.models || []
  const nextIds = new Set(nextModels.filter((model) => model.id).map((model) => model.id))
  for (const model of previous?.models || []) {
    if (model.id && !nextIds.has(model.id)) await modelConfigApi.deleteModel(robotSettingState.appId, serviceId, model.id)
  }
  for (const model of nextModels) {
    if (model.id) await modelConfigApi.updateModel(robotSettingState.appId, serviceId, model.id, modelMutation(model))
    else await modelConfigApi.addModel(robotSettingState.appId, serviceId, modelMutation(model))
  }
  await reload()
}

const deleteService = async (serviceId: number | string) => {
  requireManage()
  await modelConfigApi.deleteService(robotSettingState.appId, serviceId)
  await reload()
}

const getServiceById = (serviceId: number | string) =>
  robotSettingState.services.find((service) => String(service.id) === String(serviceId))

const selectedInfo = (quick = false): SelectedModelInfo => {
  const selection = quick ? robotSettingState.quickModel : robotSettingState.defaultModel
  const selectedService = getServiceById(selection.serviceId)
  const currentService = selectedService?.enabled === false ? undefined : selectedService
  const selectedModel = currentService?.models.find((model) => model.name === selection.modelName)
  const currentModel = selectedModel?.enabled === false ? undefined : selectedModel
  const { models, ...service } = currentService || ({ models: [] } as ModelService)
  return {
    ...(currentModel || { name: '', label: '', capabilities: {} }),
    service: currentService && currentModel ? service : null,
    config: { chatMode: robotSettingState.chatMode, enableThinking: robotSettingState.enableThinking },
    appId: robotSettingState.appId,
    serviceId: currentService && currentModel ? Number(currentService.id) : 0,
    modelName: currentModel ? selection.modelName : '',
    model: currentModel ? selection.modelName : '',
    completeModel: quick && currentModel ? selection.modelName : undefined
  }
}

const getSelectedModelInfo = () => selectedInfo(false)
const getSelectedQuickModelInfo = () => selectedInfo(true)

export default () => ({
  robotSettingState: readonly(robotSettingState),
  updateThinkingState,
  updateChatModeState,
  saveRobotSettingState,
  loadRobotSettingState,
  getAIModelOptions: () => robotSettingState.services,
  getModelCapabilities,
  getAllAvailableModels,
  getCompactModels,
  getSelectedModelInfo,
  getSelectedQuickModelInfo,
  addCustomService,
  updateService,
  deleteService,
  getServiceById,
  reload,
  formatComponents,
  getAgentSystemPrompt,
  getJsonFixPrompt,
  isValidJsonPatchObjectString,
  getRobotServiceOptions,
  addSystemPrompt,
  jsonPatchAutoFix,
  isValidFastJsonPatch,
  getJsonObjectString,
  fixMethods,
  schemaAutoFix,
  updatePageSchema
})
