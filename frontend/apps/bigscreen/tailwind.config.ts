import type { Config } from 'tailwindcss'

export default {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // 大屏深色科技风配色
        screen: {
          bg: '#04122b',
          panel: '#0a1f44',
          border: '#1b3a6b',
          accent: '#22d3ee',
          accent2: '#38bdf8',
        },
      },
    },
  },
  plugins: [],
} satisfies Config
