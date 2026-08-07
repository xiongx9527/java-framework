import assert from 'node:assert/strict'
import test from 'node:test'

import { collectMaterialComponentsMap, mergeComponentsMap } from '../src/preview/src/preview/materialDeps.js'

test('preview generation uses material npm declarations as component maps', () => {
  const materialComponents = collectMaterialComponentsMap([
    {
      components: [
        {
          component: 'ElInput',
          npm: {
            package: 'element-plus',
            exportName: 'ElInput',
            version: '^2.4.2',
            destructuring: true
          }
        },
        {
          component: ['TinyButton'],
          npm: {
            package: '@opentiny/vue',
            exportName: 'Button',
            version: '^3.20.0',
            destructuring: true
          }
        },
        {
          component: 'h1',
          npm: {}
        }
      ]
    }
  ])

  assert.deepEqual(materialComponents, [
    {
      componentName: 'ElInput',
      package: 'element-plus',
      exportName: 'ElInput',
      version: '^2.4.2',
      destructuring: true
    }
  ])

  assert.deepEqual(
    mergeComponentsMap(
      [
        {
          componentName: 'ElInput',
          package: 'custom-element-plus',
          exportName: 'CustomElInput',
          destructuring: true
        }
      ],
      materialComponents
    ),
    [
      {
        componentName: 'ElInput',
        package: 'custom-element-plus',
        exportName: 'CustomElInput',
        destructuring: true
      },
    ]
  )
})
