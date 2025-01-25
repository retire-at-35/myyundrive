import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 添加分享管理页面组件导入
import Share from '@/views/home/Share.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { 
      requiresGuest: true  // 只允许未登录用户访问
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { 
      requiresGuest: true  // 只允许未登录用户访问
    }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPassword.vue'),
    meta: { 
      requiresGuest: true  // 只允许未登录用户访问
    }
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true },
    redirect: '/home/index',
    children: [
      {
        path: 'index',
        name: 'index',
        component: () => import('@/views/home/Index.vue')
      },
      {
        path: 'share',
        name: 'share',
        component: () => import('@/views/home/Share.vue')
      },
      {
        path: 'recycle',
        name: 'recycle',
        component: () => import('@/views/home/Recycle.vue')
      },
      {
        path: 'setting',
        component: () => import('@/views/home/Setting.vue'),
        meta: { requiresAdmin: true },
        children: [
          {
            path: '',
            name: 'setting',
            redirect: '/home/setting/users'
          },
          {
            path: 'users',
            name: 'user-manage',
            component: () => import('@/views/home/settings/UserManage.vue')
          },
          {
            path: 'files',
            name: 'file-manage',
            component: () => import('@/views/home/settings/FileManage.vue')
          }
        ]
      },
      {
        path: 'share',
        name: 'share-manage',
        component: Share
      }
    ]
  },
  {
    path: '/share/:shareId',
    name: 'share',
    component: () => import('@/views/ShareView.vue')
  },
  {
    path: '/share-files',
    name: 'shareFiles',
    component: () => import('@/views/ShareFileList.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 修改路由守卫实现
router.beforeEach(async (to, from, next) => {
  const store = useUserStore()
  
  // 先处理登录相关页面
  if (to.meta.requiresGuest) {
    // 如果已登录且要去登录/注册/找回密码页面，重定向到首页
    if (store.isLogin) {
      next({ path: '/home', replace: true })
      return
    }
    // 未登录可以访问这些页面
    next()
    return
  }

  // 再处理需要登录的页面
  if (to.meta.requiresAuth && !store.isLogin) {
    next({ path: '/login', replace: true })
    return
  }

  // 处理需要管理员权限的页面
  if (to.meta.requiresAdmin && !store.userInfo.isAdmin) {
    next({ path: '/home', replace: true })
    return
  }

  // 其他情况正常导航
  next()
})

export default router
