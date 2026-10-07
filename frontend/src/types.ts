export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface EventView {
  eventId: string
  eventType: string
  source: string | null
  sourceType: string
  occurredAt: string
  severity: string
  parkId: string
  location: string | null
  payload: Record<string, unknown>
  rawPayload: Record<string, unknown> | null
  traceId: string | null
  status: string
  receivedAt: string
  updatedAt: string
}

export interface TraceView {
  stage: string
  status: string | null
  detail: string | null
  occurredAt: string
}

export interface TransformRule {
  from: string
  to: string
  defaultValue?: unknown
}

export interface SubscriptionView {
  id: string
  name: string
  parkId: string
  eventType: string
  enabled: boolean
  filterExpression: string | null
  transformRules: TransformRule[]
  targetType: string
  targetUrl: string | null
  notificationChannelId: string | null
  templateId: string | null
  orderedDelivery: boolean
  maxRetries: number
  backoffMillis: number
  createdAt: string
  updatedAt: string
}

export interface SourceView {
  id: string
  name: string
  sourceType: string
  parkId: string
  enabled: boolean
  secret: string | null
  createdAt: string
  updatedAt: string
}

export interface ChannelView {
  id: string
  name: string
  type: string
  parkId: string
  config: Record<string, unknown>
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface TemplateView {
  id: string
  name: string
  channelType: string | null
  subject: string | null
  body: string
  parkId: string
  createdAt: string
  updatedAt: string
}

export interface DeliveryView {
  id: number
  parkId: string
  eventId: string
  subscriptionId: string | null
  channelId: string | null
  status: string
  error: string | null
  renderedContent: string | null
  attempts: number
  createdAt: string
  updatedAt: string
}

export interface DispatchDeliveryView {
  id: number
  parkId: string
  eventId: string
  subscriptionId: string
  targetType: string
  target: string | null
  status: string
  attempts: number
  lastError: string | null
  deadLetter: boolean
  createdAt: string
  updatedAt: string
}

export interface StatisticPoint {
  key: string
  total: number
}

export interface DashboardStatistics {
  from: string
  to: string
  dimension: string
  total: number
  points: StatisticPoint[]
}

export interface SituationView {
  activeCount: number
  activeAlerts: EventView[]
}

export interface DispatchResult {
  eventId: string
  matched: number
  delivered: number
  failed: number
  deadLettered: number
  skipped: number
}

export interface IngestResponse {
  eventId: string
  eventType: string
  accepted: boolean
  duplicate: boolean
  status: string
}
