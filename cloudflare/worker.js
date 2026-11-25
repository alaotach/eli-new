/**
 * ELI5 Backend Proxy - Cloudflare Worker
 * Manages API keys, model routing, and prompt engineering in one place.
 */

const DEFAULT_MODEL = "google/gemini-3-flash-preview";
const FALLBACK_MODEL = "openrouter/free";
const TARGET_API_URL = "https://ai.hackclub.com/proxy/v1/chat/completions";

const PROMPTS = {
  explain: {
    Detailed: "Give a thorough but organized explanation in this format: 1. One-sentence summary. 2.In detail: 3 to 5 short points covering context, why it matters and any catches. 3.Example: one concrete real-world example. Define jargon on first use. Max about 500 words.",
    Straightforward: "Give a direct, clear, no-fluff explaination of what the user is asking it could be a word or a sentence explain that. use as simple words as possible explain all the tough words that are not common enough and explain it in plain english to the user..",
    Direct: "Give a direct, clear, no-fluff summary of this.",
    ELI5: "Explain it in plain, simple English, the way a smart friend would to someone outside the field. Everyday words, short sentences, define any hard term in a few words. Use an analogy only if it genuinely makes the idea clearer, and draw it from ordinary adult life (money, work, cooking, traffic). Never use toys, cartoons or kid references, never talk down, and never mention being five. and use as simple words as possible explain all the tough words that are not common enough and explain it in plain english to the user.",
    default: "Give a direct, clear, no-fluff explaination of what the user is asking it could be a word or a sentence explain that. use as simple words as possible explain all the tough words that are not common enough and explain it in plain english to the user.."
  },
  proofread: {
    base: "Proofread and correct the following text. Do not use any em dashes (—). Keep the tone natural and human-like. Just provide the corrected text without any preamble or explanation:\n\n",
    custom: (instructions, text) =>
      `Modify the following text based strictly on this instruction: "${instructions}". Do not use any em dashes (—). Keep the tone natural and human-like. Just provide the corrected text without any preamble or explanation:\n\n${text}`
  }
};

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders() });
    }

    if (request.method !== "POST") {
      return new Response(JSON.stringify({ error: "Method not allowed" }), {
        status: 405,
        headers: { "Content-Type": "application/json", ...corsHeaders() }
      });
    }

    const url = new URL(request.url);
    const apiKey = env.API_KEY;
    if (!apiKey) {
      return new Response(JSON.stringify({ error: "Worker API_KEY secret not configured" }), {
        status: 500,
        headers: { "Content-Type": "application/json", ...corsHeaders() }
      });
    }

    const model = env.MODEL || DEFAULT_MODEL;
    const fallbackModel = env.FALLBACK_MODEL || FALLBACK_MODEL;
    const apiUrl = env.API_BASE_URL || TARGET_API_URL;

    try {
      const body = await request.json();
      let prompt = "";

      // 1. Handle /explain endpoint
      if (url.pathname === "/explain" || url.pathname === "/api/explain") {
        let text = body.text || body.prompt || body.content || "";
        if (!text && Array.isArray(body.messages) && body.messages.length > 0) {
          text = body.messages[body.messages.length - 1]?.content || "";
        }
        const rawMode = body.mode || "Straightforward";
        // Case-insensitive lookup with fallback
        const modeKey = Object.keys(PROMPTS.explain).find(
          k => k.toLowerCase() === rawMode.toLowerCase()
        ) || "default";

        // Optional language parameter: defaults to English (no additional prompt modifier)
        // Only active if explicitly requested as Hindi to preserve 100% legacy behavior
        const language = (body.language || "English").toLowerCase();
        const langInstruction = language.includes("hindi") ? " Explain in clear, natural, and friendly Hindi." : "";
        const modeInstruction = PROMPTS.explain[modeKey] || PROMPTS.explain.default;
        prompt = `${modeInstruction}${langInstruction}\n\nText to explain:\n${text}\n\nExplanation:`;
      }
      // 2. Handle /proofread endpoint
      else if (url.pathname === "/proofread" || url.pathname === "/api/proofread") {
        const text = body.text || body.baseText || body.content || "";
        const instructions = body.instructions || body.customInstructions || "";
        if (!instructions.trim()) {
          prompt = `${PROMPTS.proofread.base}${text}`;
        } else {
          prompt = PROMPTS.proofread.custom(instructions.trim(), text);
        }
      }
      // 3. Fallback: transparent pass-through for custom requests
      else {
        const aiResponse = await fetchWithFallback(apiUrl, apiKey, body, fallbackModel);
        return new Response(aiResponse.body, {
          status: aiResponse.status,
          headers: {
            ...Object.fromEntries(aiResponse.headers),
            ...corsHeaders()
          }
        });
      }

      // Build payload for AI provider with streaming enabled
      const aiPayload = {
        model: model,
        stream: true,
        messages: [{ role: "user", content: prompt }]
      };

      const aiResponse = await fetchWithFallback(apiUrl, apiKey, aiPayload, fallbackModel);

      return new Response(aiResponse.body, {
        status: aiResponse.status,
        headers: {
          ...Object.fromEntries(aiResponse.headers),
          ...corsHeaders()
        }
      });

    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 500,
        headers: { "Content-Type": "application/json", ...corsHeaders() }
      });
    }
  }
};

/**
 * Sends a request to the AI provider and automatically falls back to fallbackModel
 * if the primary model responds with HTTP 402 (Payment Required).
 */
async function fetchWithFallback(apiUrl, apiKey, payload, fallbackModel) {
  const headers = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${apiKey}`,
    "Host": new URL(apiUrl).host
  };

  let aiResponse = await fetch(apiUrl, {
    method: "POST",
    headers,
    body: JSON.stringify(payload)
  });

  if (aiResponse.status === 402 && payload.model !== fallbackModel) {
    try {
      await aiResponse.body?.cancel();
    } catch (_) {}

    const fallbackPayload = { ...payload, model: fallbackModel };
    aiResponse = await fetch(apiUrl, {
      method: "POST",
      headers,
      body: JSON.stringify(fallbackPayload)
    });
  }

  return aiResponse;
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization"
  };
}
