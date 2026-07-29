import { icons } from "./icons.js";

export function TopNavbar() {
    return `
        <header class="top-navbar">
            <div class="brand-wrap">
                <button class="icon-btn mobile-only" id="menu-toggle" aria-label="Open navigation menu">${icons.menu}</button>
                <div class="brand-icon">${icons.home}</div>
                <strong class="brand-title">Portfolio Manager</strong>
            </div>
            <div class="top-actions">
                <button class="icon-btn" id="theme-toggle-btn" aria-label="Toggle theme">${icons.sun}</button>
                <button class="icon-btn" id="notify-btn" aria-label="View notifications">${icons.bell}</button>
                <button class="profile-btn" aria-label="Open user menu">
                    <span class="avatar">PM</span>
                    <span class="profile-name">Portfolio Manager</span>
                    <span class="chevron">${icons.chevronDown}</span>
                </button>
            </div>
        </header>
    `;
}
