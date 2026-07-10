const express = require('express');
const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
const cors = require('cors');
const path = require('path');

puppeteer.use(StealthPlugin());

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

let browser = null;
let page = null;

// Endpoint untuk mengirim reaksi
app.post('/api/react', async (req, res) => {
    const { url, emojis } = req.body;

    if (!url || !emojis || emojis.length === 0) {
        return res.status(400).json({ error: 'URL dan emoji wajib diisi' });
    }

    try {
        if (!browser) {
            browser = await puppeteer.launch({
                headless: false,
                args: ['--no-sandbox', '--disable-setuid-sandbox']
            });
            page = await browser.newPage();
            await page.setViewport({ width: 1280, height: 720 });
        }

        // Buka WhatsApp Web
        await page.goto('https://web.whatsapp.com', { waitUntil: 'networkidle2' });
        
        // Tunggu user scan QR (manual)
        await page.waitForSelector('div[data-testid="chat-list"]', { timeout: 60000 });

        // Navigasi ke URL channel
        await page.goto(url, { waitUntil: 'networkidle2' });
        await page.waitForTimeout(3000);

        // Cari tombol reaksi
        const reactionButton = await page.$('div[aria-label="Reactions"]');
        if (!reactionButton) {
            return res.status(404).json({ error: 'Tombol reaksi tidak ditemukan' });
        }
        await reactionButton.click();
        await page.waitForTimeout(1000);

        // Kirim setiap emoji (70x per emoji)
        let totalSent = 0;
        for (const emoji of emojis) {
            for (let i = 0; i < 70; i++) {
                // Cari dan klik emoji
                const emojiSelector = `span[data-emoji="${emoji}"]`;
                const emojiElement = await page.$(emojiSelector);
                if (emojiElement) {
                    await emojiElement.click();
                    totalSent++;
                    await page.waitForTimeout(50);
                } else {
                    // Fallback: klik berdasarkan teks
                    const emojiText = await page.$x(`//span[contains(text(), '${emoji}')]`);
                    if (emojiText.length > 0) {
                        await emojiText[0].click();
                        totalSent++;
                        await page.waitForTimeout(50);
                    }
                }
            }
        }

        // Tutup panel reaksi
        await page.keyboard.press('Escape');

        res.json({ 
            success: true, 
            total: totalSent,
            message: `${totalSent} reaksi terkirim ke ${url}`
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({ error: error.message });
    }
});

// Endpoint untuk reset browser
app.post('/api/reset', async (req, res) => {
    if (browser) {
        await browser.close();
        browser = null;
        page = null;
    }
    res.json({ success: true });
});

app.listen(3000, () => {
    console.log('🔥 Server running on http://localhost:3000');
});
