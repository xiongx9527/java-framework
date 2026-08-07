import { expect, test } from 'vitest'

import { generateCode } from '../src/generator/page.js'

const componentsMap = [
  {
    componentName: 'TinyDialogBox',
    package: '@opentiny/vue',
    exportName: 'DialogBox',
    destructuring: true
  }
]

test('static visible TinyDialogBox generates a closeable visible model', () => {
  const result = generateCode({
    pageInfo: {
      name: 'DialogPage',
      schema: {
        componentName: 'Page',
        fileName: 'DialogPage',
        state: {},
        children: [
          {
            componentName: 'TinyDialogBox',
            id: 'dialog_a',
            props: {
              visible: true,
              'show-close': true,
              title: 'dialogBox title'
            },
            children: [{ componentName: 'div' }]
          }
        ]
      }
    },
    componentsMap
  })

  const vueFile = result.find((item) => item.panelName === 'DialogPage.vue')?.panelValue || ''

  expect(vueFile).toMatch(/v-model:visible="state\.dialogVisible"/)
  expect(vueFile).toMatch(/"?dialogVisible"?:\s*true/)
  expect(vueFile).not.toMatch(/:visible="true"/)
})
