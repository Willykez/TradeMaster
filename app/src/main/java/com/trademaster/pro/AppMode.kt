package com.trademaster.pro

// Mirrors the `mode` toggle from the web app (client/admin). Hoisted at the
// top of the composition and passed down, rather than duplicated per-screen
// ViewModels, since it's genuinely just a display/permission switch.
enum class AppMode { CLIENT, ADMIN }
