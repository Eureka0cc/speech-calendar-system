const BASE = '/api/events'

export interface ProcessResult {
  success: boolean
  message: string
  intent: string
  parsed: {
    intent: string
    title: string
    startTime: string
    endTime: string | null
    date: string
  }
  events: EventVO[]
}

export interface EventVO {
  id: number
  title: string
  startTime: string
  endTime: string | null
}

export async function processText(text: string, userId: number = 1): Promise<ProcessResult> {
  const res = await fetch(`${BASE}/process`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, userId }),
  })
  return res.json()
}

export async function getEvents(date: string): Promise<EventVO[]> {
  const res = await fetch(`${BASE}?date=${date}`)
  return res.json()
}
