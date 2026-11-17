import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'

// 規則 CA-9：畫面只顯示後端整理好的 ViewModel，不做格式轉換。
// 在畫面元件裡用日期、數字格式化的 API，就代表前端在自己整理資料——lint 直接擋下。
// 每個業務情境的 web-ui 都適用。
const noFormattingInViews = {
  files: ['*/web-ui/src/views/**/*.vue', '*/web-ui/src/components/**/*.vue'],
  rules: {
    'no-restricted-globals': [
      'error',
      { name: 'Date', message: '時間由後端 Presenter 整理成文字，畫面不自己處理（CA-9）' },
      { name: 'Intl', message: '格式化由後端 Presenter 負責，畫面不自己處理（CA-9）' },
    ],
    'no-restricted-properties': [
      'error',
      { property: 'toLocaleString', message: '格式化由後端 Presenter 負責（CA-9）' },
      { property: 'toLocaleDateString', message: '格式化由後端 Presenter 負責（CA-9）' },
      { property: 'toLocaleTimeString', message: '格式化由後端 Presenter 負責（CA-9）' },
      { property: 'toFixed', message: '格式化由後端 Presenter 負責（CA-9）' },
    ],
  },
}

// 設計第 8 節：各業務情境的畫面互不引用，也不引用前端的 Main；只由 main/web-ui 的路由接起來。
function contextScreensOnlyKnowThemselves(context, others) {
  return {
    files: [`${context}/web-ui/src/**/*.{ts,vue}`],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: [
                '@kanban/main-web-ui',
                '@kanban/main-web-ui/*',
                '**/main/web-ui/**',
                ...others.flatMap((other) => [`@kanban/${other}-web-ui`, `@kanban/${other}-web-ui/*`, `**/${other}/**`]),
              ],
              message: '各業務情境的畫面互不引用，只由前端的 Main（main/web-ui）的路由接起來（設計第 8 節）',
            },
          ],
        },
      ],
    },
  }
}

export default tseslint.config(
  { ignores: ['**/dist/**'] },
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['**/*.vue'],
    languageOptions: { parserOptions: { parser: tseslint.parser } },
  },
  noFormattingInViews,
  contextScreensOnlyKnowThemselves('board-management', ['flow-analysis']),
  contextScreensOnlyKnowThemselves('flow-analysis', ['board-management']),
  {
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-self-closing': 'off',
    },
  },
)
