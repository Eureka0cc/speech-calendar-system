<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  processing?: boolean
}>()

const emit = defineEmits<{
  result: [text: string]
  'update:recording': [boolean]
}>()

const isRecording = ref(false)
const interimText = ref('')

const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
let recognition: any = null

function startRecording() {
  if (!SpeechRecognition) {
    emit('result', '[Browser not supported. Please use Chrome or Edge.]')
    return
  }
  if (props.processing) return

  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.continuous = true

  recognition.onresult = (event: any) => {
    let final = ''
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      if (event.results[i].isFinal) {
        final += event.results[i][0].transcript
      } else {
        interim += event.results[i][0].transcript
      }
    }
    interimText.value = interim
    if (final) {
      emit('result', final)
    }
  }

  recognition.onerror = (event: any) => {
    if (event.error === 'not-allowed') {
      emit('result', '[Microphone permission denied]')
    }
    stopRecording()
  }

  recognition.onend = () => {
    if (isRecording.value) {
      const t = interimText.value
      interimText.value = ''
      if (t) emit('result', t)
    }
    isRecording.value = false
    emit('update:recording', false)
  }

  isRecording.value = true
  emit('update:recording', true)
  recognition.start()
}

function stopRecording() {
  isRecording.value = false
  emit('update:recording', false)
  if (recognition) {
    recognition.stop()
    recognition = null
  }
}
</script>

<template>
  <div class="voice-button-wrapper" :class="{ recording: isRecording, processing: processing && !isRecording }">
    <!-- Ripple Effects -->
    <div class="ripple-container">
      <div class="ripple ripple-1"></div>
      <div class="ripple ripple-2"></div>
      <div class="ripple ripple-3"></div>
    </div>

    <!-- Glow Effect -->
    <div class="glow-effect"></div>

    <!-- Main Button -->
    <button
      class="voice-button"
      :class="{ recording: isRecording, processing: processing && !isRecording }"
      @mousedown.prevent="startRecording"
      @mouseup.prevent="stopRecording"
      @mouseleave.prevent="stopRecording"
      @touchstart.prevent="startRecording"
      @touchend.prevent="stopRecording"
      :disabled="processing && !isRecording"
    >
      <!-- Idle: Microphone -->
      <svg v-if="!isRecording && !processing" class="icon-microphone" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
        <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
      </svg>

      <!-- Processing: Spinner -->
      <svg v-else-if="processing && !isRecording" class="icon-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <circle cx="12" cy="12" r="10" stroke-dasharray="60" stroke-dashoffset="20"/>
      </svg>

      <!-- Recording: Square -->
      <svg v-else class="icon-recording" viewBox="0 0 24 24" fill="currentColor">
        <rect x="6" y="6" width="12" height="12" rx="3"/>
      </svg>
    </button>

    <!-- Recording Label -->
    <span class="button-label" v-if="isRecording">Release to send</span>
  </div>
</template>

<style scoped>
.voice-button-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

/* Ripple Container */
.ripple-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 280px;
  height: 280px;
  pointer-events: none;
}

.ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 280px;
  height: 280px;
  margin-top: -140px;
  margin-left: -140px;
  border-radius: 50%;
  border: 2px solid rgba(67, 106, 211, 0.3);
  opacity: 0;
  animation: ripple-animation 3s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}

.ripple-1 {
  animation-delay: 0s;
}

.ripple-2 {
  animation-delay: 1s;
}

.ripple-3 {
  animation-delay: 2s;
}

@keyframes ripple-animation {
  0% {
    transform: scale(0.8);
    opacity: 0.6;
  }
  100% {
    transform: scale(1.6);
    opacity: 0;
  }
}

/* Recording State Ripples */
.voice-button-wrapper.recording .ripple {
  border-color: rgba(239, 68, 68, 0.4);
  animation-duration: 1.5s;
}

.voice-button-wrapper.recording .ripple-1 {
  animation-delay: 0s;
}

.voice-button-wrapper.recording .ripple-2 {
  animation-delay: 0.5s;
}

.voice-button-wrapper.recording .ripple-3 {
  animation-delay: 1s;
}

/* Processing State Ripples */
.voice-button-wrapper.processing .ripple {
  border-color: rgba(167, 139, 250, 0.3);
  animation-duration: 2s;
}

/* Glow Effect */
.glow-effect {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(67, 106, 211, 0.15) 0%, transparent 70%);
  pointer-events: none;
  transition: all 0.5s ease;
}

.voice-button-wrapper.recording .glow-effect {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(239, 68, 68, 0.2) 0%, transparent 70%);
}

/* Voice Button */
.voice-button {
  position: relative;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  border: 2px solid rgba(67, 106, 211, 0.4);
  background: linear-gradient(145deg, rgba(67, 106, 211, 0.2) 0%, rgba(34, 31, 89, 0.4) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #436ad3;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
  box-shadow:
    0 0 60px rgba(67, 106, 211, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.voice-button:hover:not(:disabled) {
  border-color: rgba(67, 106, 211, 0.8);
  color: #5a7fd8;
  transform: scale(1.05);
  box-shadow:
    0 0 80px rgba(67, 106, 211, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.voice-button:active:not(:disabled) {
  transform: scale(0.95);
}

.voice-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* Recording State */
.voice-button.recording {
  border-color: rgba(239, 68, 68, 0.6);
  color: #ef4444;
  background: linear-gradient(145deg, rgba(239, 68, 68, 0.2) 0%, rgba(60, 18, 18, 0.4) 100%);
  box-shadow:
    0 0 80px rgba(239, 68, 68, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% {
    box-shadow:
      0 0 60px rgba(239, 68, 68, 0.2),
      inset 0 1px 0 rgba(255, 255, 255, 0.1);
    transform: scale(1);
  }
  50% {
    box-shadow:
      0 0 100px rgba(239, 68, 68, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.15);
    transform: scale(1.03);
  }
}

/* Processing State */
.voice-button.processing {
  border-color: rgba(167, 139, 250, 0.5);
  color: #a78bfa;
  background: linear-gradient(145deg, rgba(167, 139, 250, 0.15) 0%, rgba(45, 35, 70, 0.4) 100%);
}

/* Icons */
.icon-microphone {
  width: 56px;
  height: 56px;
  opacity: 0.9;
}

.icon-spinner {
  width: 44px;
  height: 44px;
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

.icon-recording {
  width: 40px;
  height: 40px;
}

/* Button Label */
.button-label {
  font-size: 13px;
  color: #ef4444;
  font-weight: 500;
  letter-spacing: 0.5px;
  animation: fade-in 0.3s ease;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
