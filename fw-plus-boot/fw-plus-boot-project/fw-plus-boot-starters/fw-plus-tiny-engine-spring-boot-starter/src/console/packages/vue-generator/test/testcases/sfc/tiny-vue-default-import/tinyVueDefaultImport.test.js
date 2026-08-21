import { expect, test } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { DEFAULT_COMPONENTS_MAP } from '@/constant'
import { getImportMap } from '@/generator/vue/sfc/parseImport'
import { generateImportByPkgName } from '@/utils/generateImportStatement'

const getTinyMaterialComponents = () => {
  const bundlePath = resolve(process.cwd(), '../../designer-demo/public/mock/bundle.json')
  const bundle = JSON.parse(readFileSync(bundlePath, 'utf8'))
  const components = bundle?.data?.materials?.components || []

  return [
    ...new Map(
      components
        .map((item) => {
          const componentName = Array.isArray(item.component) ? item.component[0] : item.component
          const npm = item.npm

          if (!componentName?.startsWith('Tiny') || !npm?.package || !npm?.destructuring) {
            return null
          }

          return [
            componentName,
            {
              componentName,
              exportName: npm.exportName || componentName,
              package: npm.package
            }
          ]
        })
        .filter(Boolean)
    ).values()
  ].sort((a, b) => a.componentName.localeCompare(b.componentName))
}

test('should generate imports for all official Tiny material components in preview SFC', () => {
  const missing = []

  getTinyMaterialComponents().forEach((material) => {
    const { pkgMap } = getImportMap(
      {
        children: [
          {
            componentName: material.componentName,
            props: {},
            children: []
          }
        ]
      },
      DEFAULT_COMPONENTS_MAP,
      {}
    )

    const imports = pkgMap[material.package] || []
    const importStatement = generateImportByPkgName({
      pkgName: material.package,
      imports
    })
    const expected =
      material.exportName === material.componentName
        ? material.componentName
        : `${material.exportName} as ${material.componentName}`

    if (!importStatement.includes(expected)) {
      missing.push({
        componentName: material.componentName,
        expected,
        actual: importStatement
      })
    }
  })

  expect(missing).toEqual([])
})
