import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import globals from 'globals'

// ESLint 9 扁平配置（apps/web）。解析 TS/TSX 需 typescript-eslint parser。
// 基线规则：JS 推荐 + TS 推荐（非类型检查模式，快）；对齐 CLAUDE.md 关键约束（禁 any）。
export default tseslint.config(
  {
    // src 仅 TS/TSX；忽略 dist、配置文件、以及任何残留的编译产物 .js
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
      // CLAUDE.md §6.2：禁止 any
      '@typescript-eslint/no-explicit-any': 'error',
      // 允许有意忽略的参数/变量用 _ 前缀（如 mutation onSuccess 的 _data）
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      // 空 catch 是本项目刻意的“静默降级”惯用法（已带注释说明）
      'no-empty': ['error', { allowEmptyCatch: true }],
    },
  },
)
