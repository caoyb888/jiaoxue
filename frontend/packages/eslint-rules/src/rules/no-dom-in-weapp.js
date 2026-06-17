/**
 * no-dom-in-weapp
 *
 * C8 约束：weapp/ 目录下禁止直接调用 DOM API
 * 触发条件：document.*、window.*、localStorage、sessionStorage、IndexedDB、navigator
 *
 * CI 检查：前端所有 PR 通过此规则拦截 weapp/ 下的 DOM 调用
 */

'use strict'

const DOM_OBJECTS = ['document', 'window', 'localStorage', 'sessionStorage', 'indexedDB', 'navigator']
const DOM_EXCEPTIONS = ['window.__TARO_ENV', 'process.env.TARO_ENV'] // 允许的例外

/** @type {import('eslint').Rule.RuleModule} */
module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Disallow direct DOM API usage in Taro weapp directory (C8 constraint)',
      category: 'Architecture',
      recommended: true,
    },
    messages: {
      noDomInWeapp:
        "'{{name}}' is a browser DOM API and must not be used directly in weapp/ code. " +
        "Use Taro APIs or conditional compilation with isWeb guard. (C8 constraint)",
    },
    schema: [],
  },

  create(context) {
    const filename = context.getFilename()

    // 只检查 weapp/ 目录下的文件
    const isWeappFile = filename.includes('/weapp/') || filename.includes('\\weapp\\')
    if (!isWeappFile) return {}

    return {
      MemberExpression(node) {
        const objName = node.object.type === 'Identifier' ? node.object.name : null
        if (!objName || !DOM_OBJECTS.includes(objName)) return

        // 允许 process.env.TARO_ENV 例外
        const code = context.getSourceCode().getText(node)
        if (DOM_EXCEPTIONS.some(ex => code.startsWith(ex))) return

        context.report({
          node,
          messageId: 'noDomInWeapp',
          data: { name: objName },
        })
      },

      Identifier(node) {
        // 检查裸调用如 localStorage.getItem（已被 MemberExpression 覆盖，此处补充独立引用）
        if (!DOM_OBJECTS.includes(node.name)) return
        if (node.parent.type === 'MemberExpression' && node.parent.object === node) return
        if (node.parent.type === 'Property' || node.parent.type === 'VariableDeclarator') return

        context.report({
          node,
          messageId: 'noDomInWeapp',
          data: { name: node.name },
        })
      },
    }
  },
}
