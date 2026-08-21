import { build } from 'vite'

const [, , configFile, entriesJson] = process.argv
const result = await build({
  configFile,
  logLevel: 'silent',
  build: {
    write: false,
    rollupOptions: {
      input: JSON.parse(entriesJson)
    }
  }
})
const outputs = (Array.isArray(result) ? result.flatMap((item) => item.output) : result.output).map((output) => {
  if (output.type === 'chunk') {
    return {
      type: output.type,
      fileName: output.fileName,
      code: output.code,
      map: output.map?.toString() || null
    }
  }

  const isString = typeof output.source === 'string'
  return {
    type: output.type,
    fileName: output.fileName,
    source: isString ? output.source : Buffer.from(output.source).toString('base64'),
    encoding: isString ? 'utf8' : 'base64'
  }
})

process.stdout.write(JSON.stringify(outputs))
