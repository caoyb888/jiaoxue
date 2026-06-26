/**
 * 大屏配置。院系活跃排行无全院系聚合接口，前端对此处列出的院系并行查询并排名；
 * 真实院系名/ID 待 S7-16 教务联调后由后端提供，当前用 seed 院系占位。
 */
export const RANK_DEPT_IDS = [1, 2, 3, 4, 5]

export const DEPT_NAMES: Record<number, string> = {
  1: '信息工程学院',
  2: '工商学院',
  3: '会计学院',
  4: '人文学院',
  5: '艺术学院',
}
