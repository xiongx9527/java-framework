import { getMetaApi, META_SERVICE } from '@opentiny/tiny-engine-meta-register'
import type {
  AppModelSettings,
  ModelConfig,
  ModelService,
  ModelServiceMutation,
  ModelImportRequest
} from '../types/setting.types'

const http = () => getMetaApi(META_SERVICE.Http)
const root = (appId: number | string) => `app-center/api/apps/${appId}/ai-model-services`

export const modelConfigApi = {
  listUsable: (appId: number | string): Promise<ModelService[]> => http().get(root(appId)),
  listManageable: (appId: number | string): Promise<ModelService[]> => http().get(`${root(appId)}/manage`),
  getSettings: (appId: number | string): Promise<AppModelSettings> => http().get(`${root(appId)}/settings`),
  updateSettings: (appId: number | string, settings: AppModelSettings): Promise<AppModelSettings> =>
    http().put(`${root(appId)}/settings`, settings),
  createService: (appId: number | string, service: ModelServiceMutation): Promise<ModelService> =>
    http().post(root(appId), service),
  updateService: (
    appId: number | string,
    serviceId: number | string,
    service: ModelServiceMutation
  ): Promise<ModelService> => http().put(`${root(appId)}/${serviceId}`, service),
  deleteService: (appId: number | string, serviceId: number | string): Promise<unknown> =>
    http().delete(`${root(appId)}/${serviceId}`),
  addModel: (appId: number | string, serviceId: number | string, model: Partial<ModelConfig>): Promise<ModelService> =>
    http().post(`${root(appId)}/${serviceId}/models`, model),
  updateModel: (
    appId: number | string,
    serviceId: number | string,
    modelId: number | string,
    model: Partial<ModelConfig>
  ): Promise<ModelService> => http().put(`${root(appId)}/${serviceId}/models/${modelId}`, model),
  deleteModel: (appId: number | string, serviceId: number | string, modelId: number | string): Promise<unknown> =>
    http().delete(`${root(appId)}/${serviceId}/models/${modelId}`),
  importLocal: (appId: number | string, request: ModelImportRequest): Promise<ModelService[]> =>
    http().post(`${root(appId)}/import-local`, request)
}

export default modelConfigApi
