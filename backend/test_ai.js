const { GoogleGenAI } = require('@google/genai');
require('dotenv').config({path: './.env'});

async function test() {
  try {
    const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    const response = await ai.models.generateContent({
      model: process.env.GEMINI_MODEL || 'gemini-2.0-flash',
      contents: 'Hello',
    });
    console.log("Response text type:", typeof response.text);
    console.log("Response text value:", response.text);
  } catch(e) {
    console.error("Error:", e);
  }
}
test();
