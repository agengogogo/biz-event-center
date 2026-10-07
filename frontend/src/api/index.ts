import http, { PARK_KEY, ROLES_KEY, TOKEN_KEY } from './http'
import type {
  ChannelView,
  DashboardStatistics,
  DeliveryView,
  DispatchDeliveryView,
  DispatchResult,
  IngestResponse,
  PageResponse,
  SituationView,
  SourceView,
  SubscriptionView,
  TemplateView,
  EventView,
  TraceView
} from '@/types'

interface LoginResponse {
  token: string
  parkId: string
  roles: string[]
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await http.post<LoginResponse>('/auth/login', { username, password })
  return data
}

export function storeSession(session: LoginResponse): void {
  localStorage.setItem(TOKEN_KEY, session.token)
  localStorage.setItem(PARK_KEY, session.parkId)
  localStorage.setItem(ROLES_KEY, JSON.stringify(session.roles))
}

export async function listEvents(params: Record<string, unknown>): Promise<PageResponse<EventView>> {
  const { data } = await http.get<PageResponse<EventView>>('/events', { params })
  return data
}

export async function getEvent(eventId: string): Promise<EventView> {
  const { data } = await http.get<EventView>(`/events/${eventId}`)
  return data
}

export async function getTraces(eventId: string): Promise<TraceView[]> {
  const { data } = await http.get<TraceView[]>(`/events/${eventId}/traces`)
  return data
}

export async function createEvent(body: Record<string, unknown>): Promise<IngestResponse> {
  const { data } = await http.post<IngestResponse>('/events', body)
  return data
}

export async function dispatchEvent(eventId: string): Promise<DispatchResult> {
  const { data } = await http.post<DispatchResult>(`/events/${eventId}/dispatch`)
  return data
}

export async function redeliverEvent(eventId: string): Promise<DispatchResult> {
  const { data } = await http.post<DispatchResult>(`/events/${eventId}/redeliver`)
  return data
}

export async function listDeadLetters(): Promise<DispatchDeliveryView[]> {
  const { data } = await http.get<DispatchDeliveryView[]>('/deliveries/dead-letters')
  return data
}

export async function listSources(): Promise<SourceView[]> {
  const { data } = await http.get<SourceView[]>('/sources')
  return data
}

export async function createSource(body: Record<string, unknown>): Promise<SourceView> {
  const { data } = await http.post<SourceView>('/sources', body)
  return data
}

export async function deleteSource(id: string): Promise<void> {
  await http.delete(`/sources/${id}`)
}

export async function listSubscriptions(): Promise<SubscriptionView[]> {
  const { data } = await http.get<SubscriptionView[]>('/subscriptions')
  return data
}

export async function createSubscription(body: Record<string, unknown>): Promise<SubscriptionView> {
  const { data } = await http.post<SubscriptionView>('/subscriptions', body)
  return data
}

export async function updateSubscription(
  id: string,
  body: Record<string, unknown>
): Promise<SubscriptionView> {
  const { data } = await http.put<SubscriptionView>(`/subscriptions/${id}`, body)
  return data
}

export async function setSubscriptionEnabled(
  id: string,
  enabled: boolean
): Promise<SubscriptionView> {
  const action = enabled ? 'enable' : 'disable'
  const { data } = await http.post<SubscriptionView>(`/subscriptions/${id}/${action}`)
  return data
}

export async function deleteSubscription(id: string): Promise<void> {
  await http.delete(`/subscriptions/${id}`)
}

export async function listChannels(): Promise<ChannelView[]> {
  const { data } = await http.get<ChannelView[]>('/notification-channels')
  return data
}

export async function createChannel(body: Record<string, unknown>): Promise<ChannelView> {
  const { data } = await http.post<ChannelView>('/notification-channels', body)
  return data
}

export async function listTemplates(): Promise<TemplateView[]> {
  const { data } = await http.get<TemplateView[]>('/notification-templates')
  return data
}

export async function createTemplate(body: Record<string, unknown>): Promise<TemplateView> {
  const { data } = await http.post<TemplateView>('/notification-templates', body)
  return data
}

export async function listNotificationDeliveries(eventId?: string): Promise<DeliveryView[]> {
  const { data } = await http.get<DeliveryView[]>('/notifications', { params: { eventId } })
  return data
}

export async function resendNotification(id: number): Promise<DeliveryView> {
  const { data } = await http.post<DeliveryView>(`/notifications/${id}/resend`)
  return data
}

export async function getStatistics(
  dimension: string,
  interval: string
): Promise<DashboardStatistics> {
  const { data } = await http.get<DashboardStatistics>('/dashboard/statistics', {
    params: { dimension, interval }
  })
  return data
}

export async function getSituation(): Promise<SituationView> {
  const { data } = await http.get<SituationView>('/dashboard/situation')
  return data
}
