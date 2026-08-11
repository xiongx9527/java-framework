import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Login from '../src/login/Login.vue'
import Register from '../src/login/Register.vue'

const mocks = vi.hoisted(() => ({
  post: vi.fn(() => new Promise(() => {})),
  fetchUserInfo: vi.fn(),
  setUserInfo: vi.fn(),
  setNeedToLogin: vi.fn()
}))

vi.mock('@opentiny/vue', () => ({
  TinyForm: { template: '<form><slot /></form>' },
  TinyFormItem: { template: '<div><slot /></div>' },
  TinyInput: {
    emits: ['keyup'],
    template: '<input @keyup.stop="$emit(\'keyup\', $event)" />'
  },
  TinyButton: { template: '<button><slot /></button>' },
  TinyTooltip: { template: '<div><slot /></div>' }
}))

vi.mock('@opentiny/tiny-engine-meta-register', () => ({
  META_SERVICE: {
    GlobalService: 'global-service',
    Http: 'http'
  },
  getMetaApi: (service: string) =>
    service === 'global-service'
      ? {
          fetchUserInfo: mocks.fetchUserInfo,
          setUserInfo: mocks.setUserInfo,
          setNeedToLogin: mocks.setNeedToLogin
        }
      : { post: mocks.post }
}))

const stubs = {
  TinyForm: { template: '<form><slot /></form>' },
  TinyFormItem: { template: '<div><slot /></div>' },
  TinyInput: {
    emits: ['keyup'],
    template: '<input @keyup.stop="$emit(\'keyup\', $event)" />'
  },
  TinyButton: { template: '<button><slot /></button>' },
  TinyTooltip: { template: '<div><slot /></div>' },
  SvgIcon: { template: '<span />' }
}

describe('TinyEngine account forms Enter submission', () => {
  beforeEach(() => {
    mocks.post.mockClear()
  })

  it('submits login with Enter only when both credentials are present', async () => {
    const wrapper = mount(Login, { global: { stubs } })

    const inputs = wrapper.findAll('input')
    await inputs[0].trigger('keyup', { key: 'Enter' })
    expect(mocks.post).not.toHaveBeenCalled()

    wrapper.vm.state.loginData.username = 'admin'
    wrapper.vm.state.loginData.password = 'TinyEngine@123456'
    await nextTick()
    for (const input of inputs) {
      await input.trigger('keyup', { key: 'Enter' })
      expect(mocks.post).toHaveBeenLastCalledWith('/platform-center/api/user/login', {
        username: 'admin',
        password: 'TinyEngine@123456'
      })
    }
    expect(mocks.post).toHaveBeenCalledTimes(inputs.length)
  })

  it('submits registration with Enter only when the existing validation passes', async () => {
    const wrapper = mount(Register, { global: { stubs } })

    const inputs = wrapper.findAll('input')
    await inputs[0].trigger('keyup', { key: 'Enter' })
    expect(mocks.post).not.toHaveBeenCalled()

    wrapper.vm.state.registerData.username = 'enter-user'
    wrapper.vm.state.registerData.password = 'TinyEngine@123456'
    wrapper.vm.state.registerData.confirmPassword = 'TinyEngine@123456'
    await nextTick()
    for (const input of inputs) {
      await input.trigger('keyup', { key: 'Enter' })
      expect(mocks.post).toHaveBeenLastCalledWith('/platform-center/api/user/register', {
        username: 'enter-user',
        password: 'TinyEngine@123456'
      })
    }
    expect(mocks.post).toHaveBeenCalledTimes(inputs.length)
  })
})
