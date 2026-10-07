import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', redirect: '/events' },
        {
          path: 'events',
          name: 'events',
          component: () => import('@/views/EventListView.vue')
        },
        {
          path: 'events/create',
          name: 'event-create',
          component: () => import('@/views/EventCreateView.vue')
        },
        {
          path: 'events/:id',
          name: 'event-detail',
          component: () => import('@/views/EventDetailView.vue')
        },
        {
          path: 'sources',
          name: 'sources',
          component: () => import('@/views/SourceListView.vue')
        },
        {
          path: 'subscriptions',
          name: 'subscriptions',
          component: () => import('@/views/SubscriptionListView.vue')
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/NotificationView.vue')
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'events' }
  }
  return true
})

export default router
