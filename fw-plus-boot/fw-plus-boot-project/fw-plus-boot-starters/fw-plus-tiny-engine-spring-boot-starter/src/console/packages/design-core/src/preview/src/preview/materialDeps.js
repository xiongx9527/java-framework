const getComponentName = (component) => (Array.isArray(component) ? component[0] : component)

// The Vue DSL generator already appends its official built-in TinyVue maps.
// Re-adding the same packages from material bundle npm fields can create duplicate local imports.
const GENERATOR_BUILT_IN_PACKAGES = new Set(['@opentiny/vue', '@opentiny/vue-icon', '@opentiny/vue-huicharts'])

export const addScriptAndStyle = (scripts, styles, pkg = {}) => {
  if (pkg.package && pkg.script) {
    scripts.set(pkg.package, pkg.script)
  }

  if (Array.isArray(pkg.css)) {
    pkg.css.forEach((style) => {
      if (style) {
        styles.add(style)
      }
    })
  } else if (pkg.css) {
    styles.add(pkg.css)
  }
}

export const collectMaterialComponentsMap = (materials = []) => {
  const componentMap = new Map()

  materials.forEach((material) => {
    const components = material?.components

    if (!Array.isArray(components)) {
      return
    }

    components.forEach((component) => {
      const componentName = getComponentName(component?.component)
      const npm = component?.npm || {}

      if (
        !componentName ||
        !npm.package ||
        GENERATOR_BUILT_IN_PACKAGES.has(npm.package) ||
        componentMap.has(componentName)
      ) {
        return
      }

      componentMap.set(componentName, {
        componentName,
        package: npm.package,
        exportName: npm.exportName || componentName,
        version: npm.version,
        destructuring: npm.destructuring !== false
      })
    })
  })

  return Array.from(componentMap.values())
}

export const mergeComponentsMap = (...maps) => {
  const componentMap = new Map()

  maps.flat().forEach((item = {}) => {
    if (!item.componentName || componentMap.has(item.componentName)) {
      return
    }

    if (!item.package && typeof item.main !== 'string') {
      return
    }

    componentMap.set(item.componentName, item)
  })

  return Array.from(componentMap.values())
}
