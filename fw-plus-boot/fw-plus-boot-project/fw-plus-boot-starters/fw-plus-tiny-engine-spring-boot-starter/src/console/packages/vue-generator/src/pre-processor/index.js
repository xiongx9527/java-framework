/**
 * Copyright (c) 2023 - present TinyEngine Authors.
 * Copyright (c) 2023 - present Huawei Cloud Computing Technologies Co., Ltd.
 *
 * Use of this source code is governed by an MIT-style license.
 *
 * THE OPEN SOURCE SOFTWARE IN THIS PRODUCT IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE. SEE THE APPLICABLE LICENSES FOR MORE DETAILS.
 *
 */

import { BUILTIN_COMPONENT_NAME, TINY_ICON } from '../constant'

const TINY_DIALOG_BOX = 'TinyDialogBox'
const DIALOG_VISIBLE_STATE = 'dialogVisible'

const text2Span = (schema) => {
  const { componentName, props = {} } = schema

  if (componentName === BUILTIN_COMPONENT_NAME.TEXT) {
    schema.componentName = 'span'

    if (props.text) {
      schema.children = props.text
      delete props.text
    }
  }

  return schema
}

const tinyIcon = (schema) => {
  const { componentName, props = {} } = schema

  if (componentName === BUILTIN_COMPONENT_NAME.ICON && props.name) {
    // 图标组件名，统一前缀为 TinyIcon，与从组件库引入的方法名 iconXxx 区分开
    const iconName = props.name.startsWith(TINY_ICON) ? props.name : `Tiny${props.name}`
    schema.componentName = iconName
    delete props.name
  }

  return schema
}

const tinyGrid = (schema) => {
  const { componentName, props = {} } = schema

  if (componentName === 'TinyGrid' && props.data && props.fetchData) {
    delete props.data
  }

  return schema
}

const collection2Div = (schema) => {
  const { componentName } = schema

  if (componentName === BUILTIN_COMPONENT_NAME.COLLECTION) {
    schema.componentName = 'div'
  }

  return schema
}

const component2Block = (schema) => {
  const { componentType, componentName, fileName } = schema

  if (componentType === BUILTIN_COMPONENT_NAME.BLOCK) {
    schema.fileName = componentName
    schema.componentName = BUILTIN_COMPONENT_NAME.BLOCK
  }

  if (componentName && componentName === fileName) {
    schema.componentName = BUILTIN_COMPONENT_NAME.BLOCK
  }

  return schema
}

const getDialogVisibleStateKey = (state = {}) => {
  if (!Object.prototype.hasOwnProperty.call(state, DIALOG_VISIBLE_STATE)) {
    state[DIALOG_VISIBLE_STATE] = true

    return DIALOG_VISIBLE_STATE
  }

  let index = 1
  let stateKey = `${DIALOG_VISIBLE_STATE}${index}`

  while (Object.prototype.hasOwnProperty.call(state, stateKey)) {
    index++
    stateKey = `${DIALOG_VISIBLE_STATE}${index}`
  }

  state[stateKey] = true

  return stateKey
}

const dialogBoxVisibleModel = (schema, rootState) => {
  const { componentName, props = {} } = schema

  if (componentName !== TINY_DIALOG_BOX || props.visible !== true) {
    return schema
  }

  const stateKey = getDialogVisibleStateKey(rootState)
  props.visible = {
    type: 'JSExpression',
    value: `this.state.${stateKey}`,
    model: {
      prop: 'visible'
    }
  }

  return schema
}

const preProcess = (schema, rootState = schema.state || {}, isRoot = true) => {
  const { children } = schema

  if (isRoot) {
    schema.state = rootState
  }

  text2Span(schema)
  tinyIcon(schema)
  tinyGrid(schema)
  collection2Div(schema)
  component2Block(schema)
  dialogBoxVisibleModel(schema, rootState)

  if (Array.isArray(children)) {
    children.forEach((child) => preProcess(child, rootState, false))
  }

  return schema
}

export { preProcess, tinyIcon }
