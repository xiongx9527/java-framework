import { readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { normalizePluginArtifact } from './helpers/plugin-contract.mjs'

const [inputPath, outputPath, sourceCommit, sourceBlob] = process.argv.slice(2)
if (!inputPath || !outputPath || !sourceCommit || !sourceBlob) {
    throw new Error('Usage: node test/generate-legacy-fixture.mjs <old-iife> <fixture> <commit> <blob>')
}

const source = await readFile(inputPath, 'utf8')
const contract = normalizePluginArtifact(source, path.basename(inputPath))
const fixture = {
    schemaVersion: 1,
    provenance: {
        sourceCommit,
        sourceBlob,
        sourcePath: 'src/main/resources/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js',
        sourceBytes: contract.artifact.bytes,
        sourceSha256: contract.artifact.sha256,
        normalization: 'test/helpers/plugin-contract.mjs'
    },
    legacyContract: {
        locales: contract.locales,
        registration: contract.registration,
        component: contract.component,
        services: contract.services,
        styles: contract.styles,
        icon: contract.icon
    },
    requiredAdditions: {
        statusBarItems: [{
            icon: '#magic-langchain4j-langchain4j',
            title: 'Langchain4j',
            displayKey: 'langchain4j',
            order: 800,
            opened: ['https://docs.langchain4j.dev/', '_blank']
        }]
    }
}

await writeFile(outputPath, `${JSON.stringify(fixture, null, 2)}\n`)
