import assert from 'node:assert/strict'
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

import { extraBundleCdnLinks } from '../../packages/build/vite-config/src/localCdnFile/copyBundleDeps.js'

test('bundle dependency localization discovers both unpkg and npmmirror resources', () => {
  const workDir = mkdtempSync(path.join(tmpdir(), 'tiny-engine-local-cdn-'))
  const bundleFile = path.join(workDir, 'bundle.json')

  try {
    writeFileSync(
      bundleFile,
      JSON.stringify({
        data: {
          materials: {
            packages: [
              {
                package: '@opentiny/vue',
                script: 'https://unpkg.com/@opentiny/vue-runtime@3.20/dist3/tiny-vue-pc.mjs',
                css: 'https://unpkg.com/@opentiny/vue-theme@3.20/index.css'
              },
              {
                package: 'element-plus',
                script: 'https://registry.npmmirror.com/element-plus/2.4.2/files/dist/index.full.mjs',
                css: 'https://registry.npmmirror.com/element-plus/2.4.2/files/dist/index.css'
              }
            ]
          }
        }
      })
    )

    const urls = extraBundleCdnLinks(bundleFile, 'https://unpkg.com').map((item) => item.url)

    assert.equal(urls.length, 4)
    assert.equal(urls.some((url) => url.includes('registry.npmmirror.com/element-plus')), true)
    assert.equal(urls.some((url) => url.includes('unpkg.com/@opentiny/vue-runtime')), true)
  } finally {
    rmSync(workDir, { recursive: true, force: true })
  }
})
