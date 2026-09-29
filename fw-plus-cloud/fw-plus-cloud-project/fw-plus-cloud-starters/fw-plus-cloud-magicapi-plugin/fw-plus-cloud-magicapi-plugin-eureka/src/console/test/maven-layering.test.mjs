import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const repository = execFileSync('git', ['rev-parse', '--show-toplevel'], { encoding: 'utf8' }).trim()
const readPom = relativePath => readFileSync(path.join(repository, relativePath), 'utf8')

test('Eureka 插件只由 Cloud starters 聚合和 Cloud BOM 管理', () => {
    const bootAggregator = readPom('fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml')
    const bootBom = readPom('fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml')
    const cloudStarters = readPom('fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/pom.xml')
    const cloudPluginAggregator = readPom('fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml')
    const cloudBom = readPom('fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-dependencies/pom.xml')

    assert.doesNotMatch(bootAggregator, /<module>fw-plus-magicapi-plugin-eureka<\/module>/)
    assert.doesNotMatch(bootBom, /<artifactId>fw-plus-magicapi-plugin-eureka<\/artifactId>/)
    assert.match(cloudStarters, /<module>fw-plus-magicapi-plugin<\/module>/)
    assert.doesNotMatch(cloudStarters, /<module>fw-plus-magicapi-plugin-eureka<\/module>/)
    assert.match(cloudPluginAggregator, /<module>fw-plus-magicapi-plugin-eureka<\/module>/)
    assert.match(cloudBom, /<groupId>com\.xiongx9527\.fw\.plus\.cloud<\/groupId>\s*<artifactId>fw-plus-magicapi-plugin-eureka<\/artifactId>/)
})

test('Eureka 插件属于 Cloud 并由 Cloud Eureka server starter 引入', () => {
    const pluginPomPath = 'fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-eureka/pom.xml'
    assert.ok(existsSync(path.join(repository, pluginPomPath)), 'Cloud Magic-API 插件聚合器中缺少 Eureka 插件模块')
    const pluginPom = readPom(pluginPomPath)
    const eurekaServerStarter = readPom('fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-eureka-server-spring-cloud-starter/pom.xml')

    assert.match(pluginPom, /<artifactId>fw-plus-magicapi-plugin<\/artifactId>/)
    assert.match(eurekaServerStarter, /<groupId>com\.xiongx9527\.fw\.plus\.cloud<\/groupId>\s*<artifactId>fw-plus-magicapi-plugin-eureka<\/artifactId>/)
})
