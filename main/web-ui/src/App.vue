<template>
  <header class="app-header">
    <RouterLink to="/" class="app-title">看板</RouterLink>
  </header>
  <main class="app-main">
    <RouterView />
  </main>
</template>

<style>
:root {
  font-family: system-ui, -apple-system, 'PingFang TC', 'Noto Sans TC', sans-serif;
  color: #1f2933;
  background: #f4f5f7;
}
body { margin: 0; }
a { color: #0b61a4; }
button, input, textarea { font: inherit; }
button { cursor: pointer; border: 1px solid #c3c9d1; background: #fff; border-radius: 4px; padding: 2px 8px; }
button:hover { background: #eef1f4; }
input, textarea { border: 1px solid #c3c9d1; border-radius: 4px; padding: 2px 6px; }
.app-header { background: #0b61a4; padding: 10px 16px; }
.app-title { color: #fff; font-weight: 700; text-decoration: none; font-size: 18px; }
.app-main { padding: 16px; }
.inline-form { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; margin: 6px 0; }
.muted { color: #66707a; font-size: 13px; }
.page-links { margin-bottom: 8px; }
.tag { background: #ffe8a3; border-radius: 4px; padding: 0 6px; font-size: 13px; }
</style>
