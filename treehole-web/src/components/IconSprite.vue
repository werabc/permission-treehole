<template>
  <!-- 全局 SVG 图标库：一次挂载，全站用 <AppIcon name="..."/> 引用（替代原 emoji） -->
  <svg width="0" height="0" style="position:absolute" aria-hidden="true" focusable="false">
    <defs>
      <linearGradient id="dhGrad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#5fe0bd" />
        <stop offset="100%" stop-color="#a78bfa" />
      </linearGradient>

      <symbol id="i-tree" viewBox="0 0 24 24" fill="none" stroke="url(#dhGrad)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 2.6c-3.7 0-6.4 2.5-6.4 6 0 2.2 1.1 3.7 2.2 4.7-.6.5-1.2 1.2-1.2 2.3h10.8c0-1.1-.6-1.8-1.2-2.3 1.1-1 2.2-2.5 2.2-4.7 0-3.5-2.7-6-6.4-6Z" />
        <path d="M12 15.6V21M12 18.6l2.6-2M12 17.2l-2.3-1.8" />
      </symbol>

      <symbol id="i-search" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></symbol>
      <symbol id="i-bell" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 1 0-12 0c0 6-2 7-2 7h16s-2-1-2-7" /><path d="M13.7 20a2 2 0 0 1-3.4 0" /></symbol>
      <symbol id="i-cog" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-2.9 1.2 2 2 0 1 1-4 0 1.7 1.7 0 0 0-2.9-1.2l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1A1.7 1.7 0 0 0 4.6 15a2 2 0 1 1 0-4 1.7 1.7 0 0 0 1.2-2.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1A1.7 1.7 0 0 0 11.5 4a2 2 0 1 1 4 0 1.7 1.7 0 0 0 2.9 1.2l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1A1.7 1.7 0 0 0 22 11a2 2 0 1 1 0 4Z" /></symbol>
      <symbol id="i-user" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><circle cx="12" cy="8" r="4" /><path d="M4.5 20a7.5 7.5 0 0 1 15 0" /></symbol>
      <symbol id="i-arrow-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5m6-7-7 7 7 7" /></symbol>
      <symbol id="i-chevron-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m9 6 6 6-6 6" /></symbol>
      <symbol id="i-chevron-down" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6" /></symbol>
      <symbol id="i-eye" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z" /><circle cx="12" cy="12" r="2.6" /></symbol>
      <symbol id="i-eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4l16 16M9.9 5.7A10.6 10.6 0 0 1 12 5.5c6.4 0 10 6.5 10 6.5a17 17 0 0 1-3 3.9M6.3 8.1A17.4 17.4 0 0 0 2 12s3.6 6.5 10 6.5a10.4 10.4 0 0 0 3.5-.6" /></symbol>
      <symbol id="i-like" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20.3 4.6 13a4.6 4.6 0 0 1 6.5-6.5l.9.9.9-.9A4.6 4.6 0 0 1 19.4 13Z" /></symbol>
      <symbol id="i-comment" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M20 12a8 8 0 0 1-11.6 7.1L4 20.5l1.4-4.4A8 8 0 1 1 20 12Z" /></symbol>
      <symbol id="i-star" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"><path d="m12 3.6 2.6 5.4 5.9.8-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9L3.5 9.8l5.9-.8Z" /></symbol>
      <symbol id="i-star-fill" viewBox="0 0 24 24"><path d="m12 3.6 2.6 5.4 5.9.8-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9L3.5 9.8l5.9-.8Z" fill="currentColor" /></symbol>
      <symbol id="i-flag" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M5 21V4m0 0h11l-1.6 3.5L16 11H5" /></symbol>
      <symbol id="i-trash" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M9 7V5h6v2m-8 0 1 13h8l1-13" /></symbol>
      <symbol id="i-lock" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="4.5" y="10" width="15" height="10.5" rx="2.5" /><path d="M8 10V7.5a4 4 0 0 1 8 0V10" /></symbol>
      <symbol id="i-spark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v4m0 10v4M3 12h4m10 0h4M6 6l2.5 2.5M15.5 15.5 18 18M18 6l-2.5 2.5M8.5 15.5 6 18" /></symbol>
      <symbol id="i-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" /></symbol>
      <symbol id="i-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><circle cx="12" cy="12" r="4.2" /><path d="M12 2.5v2.2m0 14.6v2.2M2.5 12h2.2m14.6 0h2.2M5.3 5.3l1.6 1.6m10.2 10.2 1.6 1.6M18.7 5.3l-1.6 1.6M6.9 17.1l-1.6 1.6" /></symbol>
      <symbol id="i-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M12 5v14M5 12h14" /></symbol>
      <symbol id="i-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="m4.5 12.5 5 5 10-11" /></symbol>
      <symbol id="i-pencil" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h4L19.5 8.5a2.1 2.1 0 0 0-3-3L5 17v3Z" /><path d="m15 6.5 3 3" /></symbol>
      <symbol id="i-exit" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M14 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h8" /><path d="M17 15l4-3-4-3M21 12H10" /></symbol>
      <symbol id="i-image" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3.5" y="4.5" width="17" height="15" rx="2.5" /><circle cx="9" cy="10" r="1.6" /><path d="m5 17.5 4.5-4.5 3.5 3.5 2.5-2.5 4 4" /></symbol>
      <symbol id="i-send" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M21 3 10.5 13.5M21 3l-6.8 18-3.7-7.5L3 9.8 21 3Z" /></symbol>
      <symbol id="i-clock" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></symbol>
      <symbol id="i-mail" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="2.5" /><path d="m4 7 8 6 8-6" /></symbol>
      <symbol id="i-home" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1Z" /></symbol>
      <symbol id="i-mega" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 10v4a1 1 0 0 0 1 1h2l7 4V5L7 9H5a1 1 0 0 0-1 1Z" /><path d="M17.5 9a4 4 0 0 1 0 6" /></symbol>
      <symbol id="i-cell" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="7" height="7" rx="2" /><rect x="13" y="4" width="7" height="7" rx="2" /><rect x="4" y="13" width="7" height="7" rx="2" /><rect x="13" y="13" width="7" height="7" rx="2" /></symbol>
      <symbol id="i-shield" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 21s7-3.2 7-9V5.6L12 3 5 5.6V12c0 5.8 7 9 7 9Z" /><path d="m9.2 12 2 2 3.6-4" /></symbol>
      <symbol id="i-more" viewBox="0 0 24 24" fill="currentColor"><circle cx="5" cy="12" r="1.9" /><circle cx="12" cy="12" r="1.9" /><circle cx="19" cy="12" r="1.9" /></symbol>
      <symbol id="i-close" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18" /></symbol>
    </defs>
  </svg>
</template>
