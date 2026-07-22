<template>
  <div class="dashboard-container">
    <section class="welcome-row"><div><p class="eyebrow">系统概览</p><h1>工作台</h1><p>欢迎回来，以下是当前系统的运行概况。</p></div><el-button type="primary" :icon="Refresh" plain>刷新数据</el-button></section>
    <section class="stat-grid"><el-card v-for="item in statistics" :key="item.title" shadow="never" class="stat-card"><div class="stat-icon"><el-icon><component :is="item.icon" /></el-icon></div><div><p>{{ item.title }}</p><count-to :start-val="0" :end-val="item.value" :duration="900" class="stat-value" /><span>{{ item.detail }}</span></div></el-card></section>
    <el-card shadow="never" class="contribution-card"><template #header><div class="card-title"><span>活跃度</span><small>近一年系统操作分布</small></div></template><ContributionGraph :data="contributionData" /></el-card>
    <section class="chart-grid"><el-card shadow="never"><template #header><div class="card-title"><span>访问趋势</span><small>近七日</small></div></template><div ref="lineChartRef" class="chart"></div></el-card><el-card shadow="never"><template #header><div class="card-title"><span>分类统计</span><small>当前内容分布</small></div></template><div ref="pieChartRef" class="chart"></div></el-card></section>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import { Collection, Document, Refresh, View, ChatLineRound } from '@element-plus/icons-vue'
import CountTo from '@/views/dashboard/components/CountTo.vue'
import ContributionGraph from './components/ContributionGraph.vue'

const statistics = ref([{ title: '文章总数', value: 156, detail: '内容持续沉淀', icon: markRaw(Document) }, { title: '用户总数', value: 2854, detail: '较上月稳步增长', icon: markRaw(Collection) }, { title: '留言总数', value: 1264, detail: '等待及时响应', icon: markRaw(ChatLineRound) }, { title: '访问量', value: 12574, detail: '近 7 日累计', icon: markRaw(View) }])
const contributionData = ref([{ date: '2024-08-01', count: 4 }, { date: '2024-09-02', count: 2 }, { date: '2024-12-05', count: 3 }])
const lineChartRef = ref<HTMLElement>(); const pieChartRef = ref<HTMLElement>(); const lineChart = shallowRef<echarts.ECharts>(); const pieChart = shallowRef<echarts.ECharts>()
const textColor = () => getComputedStyle(document.documentElement).getPropertyValue('--el-text-color-secondary').trim()
const primaryColor = () => getComputedStyle(document.documentElement).getPropertyValue('--el-color-primary').trim()
const chartOption = (): EChartsOption => ({ tooltip: { trigger: 'axis' }, grid: { left: 8, right: 12, top: 18, bottom: 8, containLabel: true }, xAxis: { type: 'category', boundaryGap: false, data: ['周一','周二','周三','周四','周五','周六','周日'], axisLine: { lineStyle: { color: '#e5e7eb' } }, axisLabel: { color: textColor() } }, yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef0f4' } }, axisLabel: { color: textColor() } }, series: [{ name: '访问量', type: 'line', smooth: true, showSymbol: false, data: [820,932,901,934,1290,1330,1320], lineStyle: { color: primaryColor(), width: 3 }, itemStyle: { color: primaryColor() }, areaStyle: { color: primaryColor(), opacity: .12 } }, { name: '浏览量', type: 'line', smooth: true, showSymbol: false, data: [620,732,701,734,1090,1130,1120], lineStyle: { color: '#14b8a6', width: 2 }, itemStyle: { color: '#14b8a6' } }] })
const pieOption = (): EChartsOption => ({ tooltip: { trigger: 'item' }, legend: { bottom: 0, icon: 'circle', textStyle: { color: textColor() } }, series: [{ name: '分类统计', type: 'pie', radius: ['52%', '72%'], center: ['50%', '45%'], itemStyle: { borderRadius: 6, borderColor: getComputedStyle(document.documentElement).getPropertyValue('--el-bg-color').trim(), borderWidth: 3 }, label: { show: false }, data: [{ value: 1048, name: '技术文章' }, { value: 735, name: '生活随笔' }, { value: 580, name: '项目分享' }, { value: 484, name: '学习笔记' }, { value: 300, name: '其他' }] }] })
const initCharts = () => { if (lineChartRef.value) { lineChart.value = echarts.init(lineChartRef.value); lineChart.value.setOption(chartOption()) } if (pieChartRef.value) { pieChart.value = echarts.init(pieChartRef.value); pieChart.value.setOption(pieOption()) } }
const resize = () => { lineChart.value?.resize(); pieChart.value?.resize() }
onMounted(() => { initCharts(); window.addEventListener('resize', resize) }); onUnmounted(() => { window.removeEventListener('resize', resize); lineChart.value?.dispose(); pieChart.value?.dispose() })
</script>

<style scoped>
.dashboard-container { max-width: 1440px; margin: 0 auto; }.welcome-row { display: flex; justify-content: space-between; align-items: end; gap: 16px; margin-bottom: 24px; }.eyebrow { margin: 0 0 6px; color: var(--el-color-primary); font-size: 12px; font-weight: 700; letter-spacing: .08em; }.welcome-row h1 { margin: 0; color: var(--el-text-color-primary); font-size: 28px; letter-spacing: -.03em; }.welcome-row p:not(.eyebrow) { margin: 8px 0 0; color: var(--el-text-color-secondary); }.stat-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 16px; }.stat-card { border-radius: 12px; }.stat-card :deep(.el-card__body) { display: flex; gap: 14px; align-items: center; padding: 20px; }.stat-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; color: var(--el-color-primary); border-radius: 10px; background: var(--el-color-primary-light-9); font-size: 20px; }.stat-card p { margin: 0 0 6px; color: var(--el-text-color-secondary); font-size: 13px; }.stat-value { display: block; color: var(--el-text-color-primary); font-size: 25px; font-weight: 650; line-height: 1.1; }.stat-card span { display: block; margin-top: 5px; color: var(--el-text-color-placeholder); font-size: 12px; }.contribution-card, .chart-grid :deep(.el-card) { border-radius: 12px; }.contribution-card { margin-bottom: 16px; }.card-title { display: flex; align-items: baseline; justify-content: space-between; }.card-title span { color: var(--el-text-color-primary); font-weight: 600; }.card-title small { color: var(--el-text-color-secondary); }.chart-grid { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(300px, 1fr); gap: 16px; }.chart { height: 310px; } :deep(.el-card__header) { padding: 17px 20px; border-bottom-color: var(--el-border-color-lighter); }
@media (max-width: 1080px) { .stat-grid { grid-template-columns: repeat(2, 1fr); }.chart-grid { grid-template-columns: 1fr; } }
@media (max-width: 560px) { .welcome-row { align-items: start; flex-direction: column; }.stat-grid { grid-template-columns: 1fr; }.chart { height: 270px; } }
</style>
