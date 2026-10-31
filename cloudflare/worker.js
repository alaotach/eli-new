/**
 * ELI5 Backend Proxy - Cloudflare Worker
 * Manages API keys, model routing, and prompt engineering in one place.
 */

const DEFAULT_MODEL = "google/gemini-3-flash-preview";
const TARGET_API_URL = "https://ai.hackclub.com/proxy/v1/chat/completions";

const PROMPTS = {
  explain: {
    Detailed: "Give a very detailed, thorough analysis with deep context about this.",
    Straightforward: "Give a direct, clear, no-fluff summary of this.",
    ELI5: "Explain this like I am 5 years old. Use simple words and fun analogies.",
    default: "Explain this casually."
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
    const apiUrl = env.API_BASE_URL || TARGET_API_URL;

    try {
      const body = await request.json();
      let prompt = "";

      // 1. Handle /explain endpoint
      if (url.pathname === "/explain" || url.pathname === "/api/explain") {
        const text = body.text || "";
        const mode = body.mode || "Straightforward";
        const modeInstruction = PROMPTS.explain[mode] || PROMPTS.explain.default;
        prompt = `${modeInstruction}\n\nText to explain:\n${text}\n\nExplanation:`;
      }
      // 2. Handle /proofread endpoint
      else if (url.pathname === "/proofread" || url.pathname === "/api/proofread") {
        const text = body.text || "";
        const instructions = body.instructions || "";
        if (!instructions.trim()) {
          prompt = `${PROMPTS.proofread.base}${text}`;
        } else {
          prompt = PROMPTS.proofread.custom(instructions.trim(), text);
        }
      }
      // 3. Fallback: transparent pass-through for custom requests
      else {
        return fetch(apiUrl, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${apiKey}`,
            "Host": new URL(apiUrl).host
          },
          body: JSON.stringify(body)
        });
      }

      // Build payload for AI provider with streaming enabled
      const aiPayload = {
        model: model,
        stream: true,
        messages: [{ role: "user", content: prompt }]
      };

      const aiResponse = await fetch(apiUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`,
          "Host": new URL(apiUrl).host
        },
        body: JSON.stringify(aiPayload)
      });

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

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization"
  };
}
