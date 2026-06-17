'use strict'

const noDomInWeapp = require('./rules/no-dom-in-weapp')

module.exports = {
  rules: {
    'no-dom-in-weapp': noDomInWeapp,
  },
  configs: {
    recommended: {
      plugins: ['@edu/eslint-rules'],
      rules: {
        '@edu/eslint-rules/no-dom-in-weapp': 'error',
      },
    },
  },
}
