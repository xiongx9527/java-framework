import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateDialog from '../../workspace/template-center/src/TemplateDialog.vue'
import TemplateDetail from '../../workspace/template-center/src/TemplateDetail.vue'

const mocks = vi.hoisted(() => ({
  createAppFromTemplate: vi.fn(),
  getPageList: vi.fn(),
  notify: vi.fn(),
  modalConfirm: vi.fn(),
  getMergeMeta: vi.fn()
}))

vi.mock('@opentiny/vue', () => ({
  Input: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  Button: { template: '<button><slot /></button>' },
  DialogBox: { template: '<div><slot /><slot name="footer" /></div>' },
  Form: {
    props: ['model', 'rules'],
    data: () => ({ fields: new Set<string>() }),
    provide() {
      return {
        registerFormField: (field: string) => this.fields.add(field)
      }
    },
    methods: {
      async validateRule(rule: any, value: unknown) {
        if (rule.required && (value === undefined || value === null || value === '')) {
          return false
        }
        if (rule.max && String(value ?? '').length > rule.max) {
          return false
        }
        if (!rule.validator) {
          return true
        }

        return new Promise<boolean>((resolve) => {
          try {
            rule.validator(rule, value, (error?: Error) => resolve(!error))
          } catch {
            resolve(false)
          }
        })
      },
      async validate(callback: (valid: boolean) => void) {
        if (!this.rules) {
          callback(true)
          return
        }

        for (const field of this.fields) {
          for (const rule of this.rules[field] || []) {
            if (!(await this.validateRule(rule, this.model[field]))) {
              callback(false)
              return
            }
          }
        }
        callback(true)
      }
    },
    template: '<form><slot /></form>'
  },
  FormItem: {
    props: ['prop'],
    inject: ['registerFormField'],
    created() {
      if (this.prop) {
        this.registerFormField(this.prop)
      }
    },
    template: '<div><slot /></div>'
  },
  Drawer: { template: '<div><slot /></div>' },
  Notify: mocks.notify,
  Modal: { confirm: mocks.modalConfirm }
}))

vi.mock('@opentiny/tiny-engine-meta-register', () => ({
  getMergeMeta: mocks.getMergeMeta
}))

vi.mock('../../workspace/template-center/src/js/http', () => ({
  isDevelopEnv: false,
  createAppFromTemplate: mocks.createAppFromTemplate,
  getPageList: mocks.getPageList
}))

const dialogStubs = {
  SvgIcon: { template: '<span />' }
}

const detailStubs = {
  SvgIcon: { template: '<span />' },
  TemplateDialog: { template: '<div />' }
}

describe('模板创建', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getPageList.mockResolvedValue([])
  })

  const mountDialog = () =>
    mount(TemplateDialog, {
      props: {
        template: { code: 'template-code' }
      },
      global: { stubs: dialogStubs }
    })

  const submitDialog = async (code: string) => {
    const wrapper = mountDialog()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('new-app')
    await inputs[1].setValue(code)
    await wrapper.findAll('button')[0].trigger('click')
    await flushPromises()
    return wrapper
  }

  it.each([
    ['空白编码', ''],
    ['包含非法字符的编码', 'new app'],
    ['超过 20 个字符的编码', 'a'.repeat(21)]
  ])('%s 不会提交表单', async (_caseName, code) => {
    const wrapper = await submitDialog(code)

    expect(wrapper.emitted('confirm')).toBeUndefined()
  })

  it('提交用户输入的合法应用编码而不是模板编码', async () => {
    const wrapper = await submitDialog('new-app-code')

    expect(wrapper.emitted('confirm')?.[0]?.[0]).toMatchObject({ code: 'new-app-code' })
    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })

  it('全局 HTTP 层拒绝创建时不显示第二个通知', async () => {
    mocks.createAppFromTemplate.mockRejectedValueOnce(new Error('request failed'))
    const wrapper = mount(TemplateDetail, {
      props: { template: {} },
      global: { stubs: detailStubs }
    })

    wrapper.vm.confirmCreateApp({ code: 'new-app-code' })
    await flushPromises()

    expect(mocks.notify).not.toHaveBeenCalled()
  })
})
