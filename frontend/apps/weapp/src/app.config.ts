export default defineAppConfig({
  pages: [
    'pages/course/index',
    'pages/classroom/index',
    'pages/attend/index',
    'pages/exam/index',
    'pages/exam/answer',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#1d4ed8',
    navigationBarTitleText: '智慧教学',
    navigationBarTextStyle: 'white',
  },
  tabBar: {
    color: '#9ca3af',
    selectedColor: '#1d4ed8',
    list: [
      { pagePath: 'pages/course/index', text: '课程' },
      { pagePath: 'pages/classroom/index', text: '课堂' },
    ],
  },
})
