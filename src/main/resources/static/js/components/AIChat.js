export function AIChat(state, options = {}) {
    const { availableModels = [], selectedModel = "", messages = [], loading = false, error = "" } = options;

    const messagesHtml = messages.map(msg => `
        <div class="chat-message chat-message-${msg.role}">
            <div class="chat-message-content">
                <p>${escapeHtml(msg.content)}</p>
            </div>
        </div>
    `).join("");

    return `
        <div class="dashboard-main" id="ai-chat-main">
            <div class="dashboard-header">
                <div>
                    <h1>AI Assistant</h1>
                    <p>Ask questions about your portfolio and investments</p>
                </div>
            </div>

            <div class="ai-chat-container">
                <!-- Model Selection & Settings -->
                <div class="ai-chat-settings card">
                    <div class="settings-row">
                        <div class="ai-model-select-group">
                            <label for="ai-model-select" class="ai-label">Select AI Model:</label>
                            <select id="ai-model-select" class="ai-select" ${loading ? "disabled" : ""}>
                                ${availableModels.map(model => `
                                    <option value="${model}" ${selectedModel === model ? "selected" : ""}>
                                        ${model}
                                    </option>
                                `).join("")}
                            </select>
                        </div>
                        <div class="ai-model-info">
                            <p class="ai-info-text">
                                💡 Models powered by <strong>Zhipu AI</strong>
                            </p>
                        </div>
                    </div>
                </div>

                <!-- Chat Messages Area -->
                <div class="ai-chat-box card">
                    <div class="chat-messages-container" id="chat-messages">
                        ${messages.length === 0 ? `
                            <div class="chat-empty-state">
                                <div class="empty-icon">💬</div>
                                <h3>Start a Conversation</h3>
                                <p>Ask me about your portfolio performance, investment strategies, market trends, or any financial questions.</p>
                            </div>
                        ` : messagesHtml}
                        ${loading ? `
                            <div class="chat-message chat-message-user">
                                <div class="chat-message-content">
                                    <p class="typing-indicator">
                                        <span></span><span></span><span></span>
                                    </p>
                                </div>
                            </div>
                        ` : ""}
                    </div>
                </div>

                <!-- Error Display -->
                ${error ? `
                    <div class="ai-error-box">
                        <strong>Error:</strong> ${escapeHtml(error)}
                    </div>
                ` : ""}

                <!-- Input Area -->
                <div class="ai-chat-input-box card">
                    <form id="ai-chat-form" class="ai-input-form">
                        <div class="ai-input-wrapper">
                            <textarea
                                id="ai-message-input"
                                class="ai-input"
                                placeholder="Ask a question about your portfolio... (Shift+Enter for new line, Enter to send)"
                                rows="3"
                                ${loading ? "disabled" : ""}
                            ></textarea>
                            <button
                                type="submit"
                                class="ai-send-btn primary-btn"
                                id="ai-send-btn"
                                ${loading ? "disabled" : ""}
                            >
                                <span class="btn-text">Send</span>
                                <span class="btn-icon">➤</span>
                            </button>
                        </div>
                    </form>
                </div>

                <!-- Quick Suggestions -->
                ${messages.length === 0 ? `
                    <div class="ai-suggestions">
                        <p class="suggestions-label">Quick Questions:</p>
                        <div class="suggestions-grid">
                            <button type="button" class="ai-suggestion-btn" data-suggestion="What is my portfolio allocation?">
                                📊 Portfolio Allocation
                            </button>
                            <button type="button" class="ai-suggestion-btn" data-suggestion="What is my current total return?">
                                📈 Total Return
                            </button>
                            <button type="button" class="ai-suggestion-btn" data-suggestion="Which holdings are losing money?">
                                📉 Losing Positions
                            </button>
                            <button type="button" class="ai-suggestion-btn" data-suggestion="What are investment tips for diversification?">
                                🎯 Diversification Tips
                            </button>
                        </div>
                    </div>
                ` : ""}
            </div>
        </div>
    `;
}

// Utility function to escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

