import { execFile } from 'node:child_process'
import path from 'node:path'
import { promisify } from 'node:util'

const buildRE = /(?:\?|&)build=(?<name>.*?)(?:&|$)/
const queryRE = /[?#].*$/

const bundleCache = new WeakMap()
const execFileAsync = promisify(execFile)

function cleanUrl(url: string) {
  return url.replace(queryRE, '')
}
function saveEmitBundleAssets(config, asset) {
  const bundleMap = bundleCache.get(config)
  bundleMap.assets.set(asset.fileName, asset)
}

async function bundleBuildEntry(config, options) {
  const viteConfigFile = options.customBuildConfig[options.buildConfig]
  const workerFile = path.resolve(path.dirname(viteConfigFile), 'vite-build-entry-worker.mjs')
  const { stdout } = await execFileAsync(process.execPath, [workerFile, viteConfigFile, JSON.stringify(options.entries)], {
    cwd: config.root,
    maxBuffer: 64 * 1024 * 1024
  })
  const [outputChunk, ...outputChunks] = JSON.parse(stdout).map((output) => {
    if (output.type === 'asset' && output.encoding === 'base64') {
      return { ...output, source: Buffer.from(output.source, 'base64') }
    }
    return output
  })

  // handle sourceMap
  const { map: sourcemap, fileName: chunkFileName } = outputChunk
  if (sourcemap) {
    if (config.build.sourcemap === 'hidden' || config.build.sourcemap === true) {
      saveEmitBundleAssets(config, {
        fileName: chunkFileName + '.map',
        source: sourcemap.toString()
      })
    }
  }

  // handle assets
  outputChunks.forEach((assets) => {
    if (assets.type === 'assets') {
      saveEmitBundleAssets(config, assets)
    } else if (assets.type === 'chunk') {
      saveEmitBundleAssets(config, {
        fileName: assets.fileName,
        source: assets.code
      })
    }
  })
  return outputChunk
}

function isSameContent(a, b) {
  if (typeof a === 'string') {
    if (typeof b === 'string') {
      return a === b
    }
    return Buffer.from(a).equals(b)
  }
  return Buffer.from(b).equals(a)
}

export async function vitePluginBuildEntry(customBuildConfig) {
  let config
  return {
    name: 'vite-plugin-build-entry',
    apply: 'build',
    enforce: 'pre',
    configResolved(resolveConfig) {
      config = resolveConfig
    },
    buildStart() {
      bundleCache.set(config, {
        assets: new Map(),
        bundle: new Map(),
        fileNameHash: new Map()
      })
    },
    async load(id) {
      const match = buildRE.exec(id)
      if (!match) {
        return
      }
      const file = cleanUrl(id)
      const { code } = await bundleBuildEntry(config, {
        customBuildConfig,
        buildConfig: match.groups.name,
        entries: [file]
      })

      return `export default ${JSON.stringify(code)}`
    },
    generateBundle(opts, bundle) {
      if (opts.__vite_skip_assets_emit__) {
        return
      }
      const bundleMap = bundleCache.get(config)
      bundleMap.assets.forEach((asset) => {
        const duplicateAsset = bundle[asset.fileName]
        if (duplicateAsset) {
          const content = duplicateAsset.type === 'asset' ? duplicateAsset.source : duplicateAsset.code
          if (isSameContent(content, asset.source)) {
            return
          }
        }
        this.emitFile({
          type: 'asset',
          fileName: asset.fileName,
          source: asset.source
        })
        bundleMap.assets.clear()
      })
    }
  }
}

export default vitePluginBuildEntry
