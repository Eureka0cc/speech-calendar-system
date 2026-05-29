<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import RecordButton from './components/RecordButton.vue'
import { processText, getEvents, type ProcessResult, type EventVO } from './api/event'

const voiceText = ref('')
const manualText = ref('')
const aiResult = ref<ProcessResult | null>(null)
const events = ref<EventVO[]>([])
const loading = ref(false)
const isRecording = ref(false)

interface OpEntry {
  type: string
  message: string
  time: string
}
const opLog = ref<OpEntry[]>([])

const statusText = computed(() => {
  if (loading.value) return 'Creating Schedule...'
  if (isRecording.value) return 'Listening...'
  if (aiResult.value) {
    if (aiResult.value.success) {
      if (aiResult.value.intent === 'create_event') return 'Schedule Created'
      if (aiResult.value.intent === 'delete_event') return 'Schedule Deleted'
      return 'Query Completed'
    }
    return 'Understanding...'
  }
  return 'Tap to speak'
})

const displayDate = computed(() => {
  const d = new Date()
  const w = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return {
    day: d.getDate(),
    month: months[d.getMonth()],
    weekday: w[d.getDay()],
    fullDate: `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`
  }
})

onMounted(() => {
  loadEvents()
})

function todayStr(): string {
  const d = new Date()
  return d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0')
}

async function loadEvents() {
  try {
    events.value = await getEvents(todayStr())
  } catch {
    events.value = []
  }
}

function onVoiceResult(text: string) {
  voiceText.value = text
  handleText(text)
}

function onRecordingChange(v: boolean) {
  isRecording.value = v
}

async function handleText(text: string) {
  if (!text.trim()) return
  loading.value = true
  aiResult.value = null
  try {
    const result = await processText(text.trim())
    aiResult.value = result
    opLog.value.unshift({
      type: result.intent,
      message: result.message,
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
    })
    if (opLog.value.length > 10) opLog.value.pop()
    if (result.success) speak(result.message)
    await loadEvents()
  } catch (e: any) {
    ElMessage.error('Request failed: ' + (e.message || 'Network error'))
  } finally {
    loading.value = false
  }
}

function sendManual() {
  if (!manualText.value.trim()) return
  handleText(manualText.value)
  manualText.value = ''
}

function speak(text: string) {
  if (!window.speechSynthesis) return
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'zh-CN'
  u.rate = 1.0
  window.speechSynthesis.speak(u)
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return d.getHours().toString().padStart(2, '0') + ':' +
    d.getMinutes().toString().padStart(2, '0')
}

function formatDate(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return months[d.getMonth()] + ' ' + d.getDate()
}

function intentLabel(intent: string) {
  if (intent === 'create_event') return 'Create'
  if (intent === 'delete_event') return 'Delete'
  return 'Query'
}

function intentColor(intent: string) {
  if (intent === 'create_event') return '#22c55e'
  if (intent === 'delete_event') return '#ef4444'
  return '#436ad3'
}
</script>

<template>
  <div class="app-container">
    <!-- Left Sidebar -->
    <aside class="sidebar-left">
      <div class="sidebar-content">
        <!-- Logo Card -->
        <div class="glass-card logo-section">
          <div class="logo-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
          </div>
          <div class="logo-text">
            <h1>SpeechCalendar</h1>
            <span>AI Calendar Assistant</span>
          </div>
        </div>

        <!-- Date Card -->
        <div class="glass-card date-section">
          <div class="date-display">
            <span class="date-day">{{ displayDate.day }}</span>
            <div class="date-info">
              <span class="date-month">{{ displayDate.month }}</span>
              <span class="date-weekday">{{ displayDate.weekday }}</span>
            </div>
          </div>
        </div>

        <!-- Events Card -->
        <div class="glass-card events-section">
          <div class="section-header">
            <h3>Today's Schedule</h3>
            <span class="badge">{{ events.length }}</span>
          </div>
          <div class="events-list">
            <div v-if="events.length === 0" class="empty-state">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="8" y1="12" x2="16" y2="12"/>
              </svg>
              <p>No events scheduled</p>
              <span>Speak or type to add</span>
            </div>
            <div v-else class="event-items">
              <div v-for="ev in events" :key="ev.id" class="event-item">
                <div class="event-time">
                  <span>{{ formatTime(ev.startTime) }}</span>
                  <span v-if="ev.endTime" class="time-separator">→</span>
                  <span v-if="ev.endTime">{{ formatTime(ev.endTime) }}</span>
                </div>
                <span class="event-title">{{ ev.title }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Center Main Area -->
    <main class="main-center">
      <div class="center-content">
        <div class="voice-container">
          <RecordButton
            @result="onVoiceResult"
            @update:recording="onRecordingChange"
            :processing="loading"
          />
          <p class="status-text" :class="{ active: isRecording || loading }">{{ statusText }}</p>
        </div>
      </div>
    </main>

    <!-- Right Sidebar -->
    <aside class="sidebar-right">
      <div class="sidebar-content">
        <!-- Recognition Result -->
        <div class="glass-card" v-if="voiceText || aiResult">
          <div class="section-header">
            <h3>Recognition</h3>
          </div>
          <p class="recognition-text" v-if="voiceText">{{ voiceText }}</p>
        </div>

        <!-- AI Result -->
        <div class="glass-card ai-result" v-if="aiResult">
          <div class="section-header">
            <h3>AI Analysis</h3>
            <span class="intent-tag" :style="{ backgroundColor: intentColor(aiResult.intent) + '20', color: intentColor(aiResult.intent) }">
              {{ intentLabel(aiResult.intent) }}
            </span>
          </div>
          <div class="ai-details" v-if="aiResult.parsed">
            <div class="detail-row" v-if="aiResult.parsed.title">
              <span class="label">Title</span>
              <span class="value">{{ aiResult.parsed.title }}</span>
            </div>
            <div class="detail-row" v-if="aiResult.parsed.startTime">
              <span class="label">When</span>
              <span class="value">
                {{ formatDate(aiResult.parsed.startTime) }} {{ formatTime(aiResult.parsed.startTime) }}
                <template v-if="aiResult.parsed.endTime">
                  - {{ formatTime(aiResult.parsed.endTime) }}
                </template>
              </span>
            </div>
          </div>
          <div class="result-message" :class="{ success: aiResult.success, error: !aiResult.success }">
            {{ aiResult.message }}
          </div>
        </div>

        <!-- Operation Log -->
        <div class="glass-card">
          <div class="section-header">
            <h3>Recent Activity</h3>
          </div>
          <div class="log-list">
            <div v-if="opLog.length === 0" class="empty-log">
              No recent activity
            </div>
            <div v-else class="log-items">
              <div v-for="(entry, i) in opLog" :key="i" class="log-item">
                <span class="log-time">{{ entry.time }}</span>
                <span class="log-message">{{ entry.message }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Bottom Input Area -->
    <div class="floating-input">
      <div class="input-container">
        <input
          v-model="manualText"
          type="text"
          placeholder="Type a command, e.g., 'Meeting at 3pm tomorrow'..."
          @keyup.enter="sendManual"
          :disabled="loading"
        />
        <button
          class="send-btn"
          @click="sendManual"
          :disabled="!manualText.trim() || loading"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"/>
            <polygon points="22 2 15 22 11 13 2 9"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

:root {
  --bg-primary: #000206;
  --card-bg: #221f59;
  --border-color: #33416b;
  --primary: #436ad3;
  --primary-hover: #3458a6;
  --text-primary: #E5E7EB;
  --text-secondary: #9CA3AF;
  --text-muted: #6B7280;
  --success: #22c55e;
  --error: #ef4444;
}

html, body {
  height: 100%;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
}

#app {
  height: 100%;
}

.app-container {
  display: flex;
  height: 100vh;
  background: var(--bg-primary);
  position: relative;
}

/* Glass Card Base */
.glass-card {
  background: rgba(34, 31, 89, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
}

/* Left Sidebar */
.sidebar-left {
  width: 320px;
  flex-shrink: 0;
  padding: 24px;
  border-right: 1px solid rgba(51, 65, 107, 0.5);
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

/* Logo Section */
.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, var(--primary), var(--primary-hover));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.logo-text h1 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.logo-text span {
  font-size: 12px;
  color: var(--text-secondary);
}

/* Date Section */
.date-section {
  padding: 24px;
}

.date-display {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.date-day {
  font-size: 48px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.date-info {
  display: flex;
  flex-direction: column;
}

.date-month {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.date-weekday {
  font-size: 14px;
  color: var(--text-secondary);
}

/* Events Section */
.events-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge {
  background: var(--primary);
  color: white;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.events-list {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-muted);
  text-align: center;
}

.empty-state svg {
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.empty-state span {
  font-size: 12px;
  color: var(--text-muted);
}

.event-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.event-item {
  background: rgba(67, 106, 211, 0.1);
  border: 1px solid rgba(67, 106, 211, 0.2);
  border-radius: 10px;
  padding: 12px 14px;
}

.event-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary);
  font-weight: 500;
  margin-bottom: 4px;
}

.time-separator {
  color: var(--text-muted);
}

.event-title {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

/* Main Center Area */
.main-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

.center-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.voice-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.status-text {
  font-size: 18px;
  color: var(--text-muted);
  font-weight: 500;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}

.status-text.active {
  color: var(--primary);
}

/* Right Sidebar */
.sidebar-right {
  width: 360px;
  flex-shrink: 0;
  padding: 24px;
  border-left: 1px solid rgba(51, 65, 107, 0.5);
}

.sidebar-right .sidebar-content {
  overflow-y: auto;
}

.recognition-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
  padding: 8px 0;
}

.ai-result .ai-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-row .label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-row .value {
  font-size: 14px;
  color: var(--text-primary);
}

.intent-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  text-transform: uppercase;
}

.result-message {
  font-size: 13px;
  padding: 12px;
  border-radius: 8px;
  margin-top: 12px;
}

.result-message.success {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.result-message.error {
  background: rgba(239, 68, 68, 0.1);
  color: var(--error);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

/* Log List */
.log-list {
  max-height: 200px;
  overflow-y: auto;
}

.empty-log {
  font-size: 13px;
  color: var(--text-muted);
  padding: 20px 0;
  text-align: center;
}

.log-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
}

.log-time {
  font-size: 11px;
  color: var(--text-muted);
}

.log-message {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Floating Input */
.floating-input {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 720px;
  padding: 0 24px;
  z-index: 100;
}

.input-container {
  display: flex;
  align-items: center;
  background: rgba(34, 31, 89, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 50px;
  padding: 6px 6px 6px 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(67, 106, 211, 0.1);
  transition: all 0.3s ease;
}

.input-container:focus-within {
  border-color: var(--primary);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(67, 106, 211, 0.3);
}

.input-container input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 15px;
  padding: 12px 0;
}

.input-container input::placeholder {
  color: var(--text-muted);
}

.input-container input:disabled {
  opacity: 0.5;
}

.send-btn {
  width: 40px;
  height: 40px;
  background: var(--primary);
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: scale(1.05);
}

.send-btn:disabled {
  background: var(--border-color);
  cursor: not-allowed;
  opacity: 0.5;
}

/* Scrollbar Styling */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--primary);
}

/* Responsive */
@media (max-width: 1200px) {
  .sidebar-right {
    display: none;
  }
}

@media (max-width: 768px) {
  .sidebar-left {
    display: none;
  }
}
</style>
