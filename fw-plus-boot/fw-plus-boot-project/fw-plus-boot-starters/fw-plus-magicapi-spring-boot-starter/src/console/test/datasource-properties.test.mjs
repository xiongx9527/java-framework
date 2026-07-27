import assert from 'node:assert/strict'
import test from 'node:test'
import {
    normalizeDatasourceProperties,
    prepareDatasourceProperties,
    usesDatasourceProperties
} from '../src/scripts/service/datasource-properties.js'

test('目标 datasource 打开时把 properties 格式化为编辑文本', () => {
    const properties = {
        timeout: 3000,
        ssl: true,
        nested: { modes: ['read', 'write'] }
    }

    const prepared = prepareDatasourceProperties('redis-datasource', { properties })

    assert.deepEqual(JSON.parse(prepared.propertiesText), properties)
    assert.equal(prepared.properties, properties)
})

test('保存时解析对象并移除临时 propertiesText', () => {
    const normalized = normalizeDatasourceProperties('kafka-datasource', {
        key: 'orders',
        propertiesText: '{"timeout":3000,"ssl":true,"nested":{"values":[1,null]}}'
    })

    assert.deepEqual(normalized.properties, {
        timeout: 3000,
        ssl: true,
        nested: { values: [1, null] }
    })
    assert.equal(Object.hasOwn(normalized, 'propertiesText'), false)
})

test('空编辑文本按空对象处理', () => {
    assert.deepEqual(
        normalizeDatasourceProperties('ws-datasource', { propertiesText: '   ' }).properties,
        {}
    )
})

test('数组、标量和非法 JSON 均被拒绝', () => {
    for (const propertiesText of ['[]', '"text"', '1']) {
        assert.throws(
            () => normalizeDatasourceProperties('s3-datasource', { propertiesText }),
            error => error.message === 'datasource.properties.objectRequired'
        )
    }
    assert.throws(
        () => normalizeDatasourceProperties('s3-datasource', { propertiesText: '{' }),
        error => error.message === 'datasource.properties.invalidJson'
    )
})

test('非目标资源保持原结构，subscribe 不受影响', () => {
    const subscribe = { other: '{"manualAck":true}' }

    assert.equal(usesDatasourceProperties('kafka-subscribe'), false)
    assert.deepEqual(normalizeDatasourceProperties('kafka-subscribe', subscribe), subscribe)
})
