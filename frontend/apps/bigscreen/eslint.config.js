import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import globals from 'globals'

// ESLint 9 扁平配置（apps/bigscreen），对齐 web：禁 any、_ 前缀忽略、允许空 catch。
export default tseslint.config(
  {
    ignores: ['dist/**', 'coverage/**', 'node_modules/**', '**/*.config.{js,ts,cjs,mjs}', '**/*.js', '**/*.jsx'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['src/**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.browser, ...globals.es2021 },
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'no-empty': ['error', { allowEmptyCatch: true }],
    },
  },
)
