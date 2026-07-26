import assert from 'node:assert/strict'
import test from 'node:test'
import { isValidStatusBarItem, stableSortByOrder } from '../src/scripts/plugin-extension.js'

test('stableSortByOrder 按有限 number order 稳定排序且不修改输入', () => {
    const items = [
        { id: 'late' },
        { id: 'b', order: 20 },
        { id: 'a', order: 10 },
        { id: 'b2', order: 20 },
        { id: 'bad', order: '1' }
    ]

    assert.deepEqual(
        stableSortByOrder(items).map(it => it.id),
        ['a', 'b', 'b2', 'late', 'bad']
    )
    assert.deepEqual(items.map(it => it.id), ['late', 'b', 'a', 'b2', 'bad'])
})

test('isValidStatusBarItem 要求完整且类型正确的状态栏项', () => {
    assert.equal(isValidStatusBarItem({ icon: 'x', title: 'x', displayKey: 'x', onClick() {} }), true)
    assert.equal(isValidStatusBarItem({ icon: 'x', title: 'x' }), false)
    assert.equal(isValidStatusBarItem(null), false)
    assert.equal(isValidStatusBarItem({ icon: '', title: 'x', displayKey: 'x', onClick() {} }), false)
    assert.equal(isValidStatusBarItem({ icon: 'x', title: 'x', displayKey: 'x', onClick: true }), false)
})
