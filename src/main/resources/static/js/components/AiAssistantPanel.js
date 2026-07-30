import { toInputSafeText } from "../utils/formatters.js";

export function AiAssistantPanel({
    loading = false,
    busy = false,
    provider = "",
    models = [],
    selectedModel = "",
    draftMessage = "",
    responseText = "",
    errorMessage = "",
    apiKeyStatus = ""
} = {}) {
    const safeModels = Array.isArray(models) ? models : [];
    const selected = safeModels.includes(selectedModel) ? selectedModel : (safeModels[0] || "");

    const modelOptions = safeModels
        .map((model) => `<option value="${toInputSafeText(model)}" ${model === selected ? "selected" : ""}>${toInputSafeText(model)}</option>`)
        .join("");

    const statusLine = loading
        ? "Loading assistant configuration..."
        : (apiKeyStatus || "Assistant is ready.");

    const responseMarkup = responseText
        ? `<article class="card info-panel"><h2>Assistant Response</h2><p>${toInputSafeText(responseText)}</p></article>`
        : "";

    return `
        <article class="card info-panel">
            <h2>Ask AI Assistant</h2>
            <p>${toInputSafeText(statusLine)}</p>
            <form id="ai-assistant-form" class="manager-grid" novalidate>
                <label for="ai-model-select">Model</label>
                <select id="ai-model-select" ${loading || busy ? "disabled" : ""}>
                    <option value="">Select a model</option>
                    ${modelOptions}
                </select>

                <label for="ai-chat-input">Question</label>
                <textarea id="ai-chat-input" rows="5" maxlength="2000" placeholder="Ask about your portfolio or market context..." ${loading || busy ? "disabled" : ""}>${toInputSafeText(draftMessage)}</textarea>

                <div class="manager-actions">
                    <button type="submit" class="primary-btn" ${loading || busy ? "disabled" : ""}>${busy ? "Sending..." : "Ask Assistant"}</button>
                </div>
            </form>
            <p class="settings-note">Provider: ${toInputSafeText(provider || "unknown")}</p>
            <p class="form-error" id="ai-assistant-error">${toInputSafeText(errorMessage)}</p>
        </article>
        ${responseMarkup}
    `;
}

