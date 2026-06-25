package com.example.data

import java.util.UUID

object SeedData {

    fun getInitialTasks(): List<TaskEntity> {
        val tasks = mutableListOf<TaskEntity>()
        val adminId = "admin_seed"

        // Category A: Video Platforms
        tasks.add(TaskEntity(
            id = "task_a1",
            advertiserId = adminId,
            campaignName = "YouTube: Full Video Watch + Like + Comment",
            platform = "YouTube",
            category = "Video Platforms",
            taskType = "Watch + Like + Comment",
            targetUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            instructions = "1. Open the YouTube link.\n2. Watch the full video (at least 3-5 minutes).\n3. Like the video and subscribe to the channel.\n4. Leave a positive comment about the video content.\n5. Take a screenshot showing you liked, commented, and subscribed with your channel visible.",
            advPricePkr = 5.00,
            userPayoutPkr = 3.50,
            adminMarginPkr = 1.50,
            totalSlots = 500,
            slotsFilled = 42,
            isFeatured = true
        ))

        tasks.add(TaskEntity(
            id = "task_a3",
            advertiserId = adminId,
            campaignName = "TikTok: Video Watch & Follow Creator",
            platform = "TikTok",
            category = "Video Platforms",
            taskType = "Watch + Like + Follow",
            targetUrl = "https://www.tiktok.com/@halalearnings",
            instructions = "1. Open the TikTok link.\n2. Watch the video completely.\n3. Like the video and follow the creator's profile.\n4. Comment 'Informative video!'.\n5. Submit a screenshot showing your TikTok comment and that you follow the creator.",
            advPricePkr = 3.50,
            userPayoutPkr = 2.45,
            adminMarginPkr = 1.05,
            totalSlots = 1000,
            slotsFilled = 120,
            isFeatured = false
        ))

        tasks.add(TaskEntity(
            id = "task_a5",
            advertiserId = adminId,
            campaignName = "Instagram: Reel Watch, Comment & Follow",
            platform = "Instagram",
            category = "Video Platforms",
            taskType = "Reel Watch + Like + Follow",
            targetUrl = "https://www.instagram.com/reels/",
            instructions = "1. Click the link to open the Instagram Reel.\n2. Watch the Reel completely.\n3. Double tap to like, and follow the account.\n4. Leave a comment related to the Reel.\n5. Take a screenshot showing you have liked and followed the creator.",
            advPricePkr = 4.00,
            userPayoutPkr = 2.80,
            adminMarginPkr = 1.20,
            totalSlots = 300,
            slotsFilled = 15,
            isFeatured = true
        ))

        // Category B: Social Media Follow / Subscribe
        tasks.add(TaskEntity(
            id = "task_b2",
            advertiserId = adminId,
            campaignName = "Instagram: Follow Official Profile",
            platform = "Instagram",
            category = "Social Media",
            taskType = "Profile Follow",
            targetUrl = "https://www.instagram.com/taskmallah_pk",
            instructions = "1. Open the Instagram link.\n2. Click the 'Follow' button on the profile.\n3. Do not unfollow or your earnings will be revoked.\n4. Take a screenshot of the profile showing the 'Following' status.",
            advPricePkr = 2.00,
            userPayoutPkr = 1.40,
            adminMarginPkr = 0.60,
            totalSlots = 2000,
            slotsFilled = 480,
            isFeatured = false
        ))

        tasks.add(TaskEntity(
            id = "task_b7",
            advertiserId = adminId,
            campaignName = "Twitter/X: Follow Creator Profile",
            platform = "Twitter / X",
            category = "Social Media",
            taskType = "Profile Follow",
            targetUrl = "https://twitter.com/taskmallah_pk",
            instructions = "1. Open the Twitter/X link.\n2. Click 'Follow' on the profile.\n3. Keep the account followed.\n4. Submit a screenshot showing you have successfully followed.",
            advPricePkr = 2.00,
            userPayoutPkr = 1.40,
            adminMarginPkr = 0.60,
            totalSlots = 800,
            slotsFilled = 98,
            isFeatured = false
        ))

        tasks.add(TaskEntity(
            id = "task_b9",
            advertiserId = adminId,
            campaignName = "Telegram: Join Halal Earning Channel",
            platform = "Telegram",
            category = "Social Media",
            taskType = "Channel Join",
            targetUrl = "https://t.me/taskmallah_announcements",
            instructions = "1. Click the Telegram link.\n2. Click 'Join Channel'.\n3. Do not leave the channel for at least 30 days.\n4. Submit a screenshot showing you are in the channel.",
            advPricePkr = 2.00,
            userPayoutPkr = 1.40,
            adminMarginPkr = 0.60,
            totalSlots = 1500,
            slotsFilled = 650,
            isFeatured = false
        ))

        // Category C: App Store Tasks
        tasks.add(TaskEntity(
            id = "task_c1",
            advertiserId = adminId,
            campaignName = "Play Store: Download App & 5-Star Review",
            platform = "Google Play Store",
            category = "App Store",
            taskType = "Download + Review",
            targetUrl = "https://play.google.com/store",
            instructions = "1. Open the Google Play Store link.\n2. Install the application.\n3. Open the app for 1 minute.\n4. Rate 5 stars and write a short positive review (min 10 words) about the user interface.\n5. Submit a screenshot of your Google Play Store review with your username clearly visible.",
            advPricePkr = 15.00,
            userPayoutPkr = 10.00,
            adminMarginPkr = 5.00,
            totalSlots = 250,
            slotsFilled = 12,
            isFeatured = true
        ))

        tasks.add(TaskEntity(
            id = "task_c2",
            advertiserId = adminId,
            campaignName = "Play Store: Install App & Open",
            platform = "Google Play Store",
            category = "App Store",
            taskType = "Download + Open",
            targetUrl = "https://play.google.com/store",
            instructions = "1. Install the app from Google Play Store.\n2. Open the app and keep it running for 30 seconds.\n3. Take a screenshot inside the app after launch showing it is successfully running.",
            advPricePkr = 8.00,
            userPayoutPkr = 5.60,
            adminMarginPkr = 2.40,
            totalSlots = 500,
            slotsFilled = 112,
            isFeatured = false
        ))

        // Category D: Website & SEO Tasks
        tasks.add(TaskEntity(
            id = "task_d1",
            advertiserId = adminId,
            campaignName = "Website Visit: Read Blog for 30 Seconds",
            platform = "Website & SEO",
            category = "SEO & Web",
            taskType = "Visit (30s Dwell)",
            targetUrl = "https://www.pakistanjobs.gov.pk",
            instructions = "1. Open the website link.\n2. Scroll through the homepage and click on one article.\n3. Spend at least 30 seconds on the site.\n4. Take a screenshot at the website with the system clock visible at the top to verify 30s dwell time.",
            advPricePkr = 1.00,
            userPayoutPkr = 0.70,
            adminMarginPkr = 0.30,
            totalSlots = 5000,
            slotsFilled = 2300,
            isFeatured = false
        ))

        tasks.add(TaskEntity(
            id = "task_d4",
            advertiserId = adminId,
            campaignName = "Google Maps: Add 5-Star Review to Local Shop",
            platform = "Google Maps",
            category = "SEO & Web",
            taskType = "Google Maps Review",
            targetUrl = "https://maps.google.com",
            instructions = "1. Open the Google Maps link.\n2. Rate the business 5 stars.\n3. Write a positive review mentioning 'excellent service and polite staff'.\n4. Submit a screenshot of your maps review with your profile name visible.",
            advPricePkr = 10.00,
            userPayoutPkr = 7.00,
            adminMarginPkr = 3.00,
            totalSlots = 100,
            slotsFilled = 31,
            isFeatured = true
        ))

        tasks.add(TaskEntity(
            id = "task_d6",
            advertiserId = adminId,
            campaignName = "Trustpilot: 5-Star Business Review",
            platform = "Trustpilot",
            category = "SEO & Web",
            taskType = "Trustpilot Review",
            targetUrl = "https://www.trustpilot.com",
            instructions = "1. Open the Trustpilot link.\n2. Rate the service 5 stars.\n3. Write a genuine positive feedback (minimum 50 words) about fast shipping and customer support.\n4. Submit a screenshot showing the submitted review on Trustpilot.",
            advPricePkr = 12.00,
            userPayoutPkr = 8.40,
            adminMarginPkr = 3.60,
            totalSlots = 150,
            slotsFilled = 45,
            isFeatured = false
        ))

        // Category E: Music & Podcast
        tasks.add(TaskEntity(
            id = "task_e1",
            advertiserId = adminId,
            campaignName = "Spotify: Follow Artist & Stream Track",
            platform = "Spotify",
            category = "Music & Podcast",
            taskType = "Stream + Follow",
            targetUrl = "https://open.spotify.com",
            instructions = "1. Open Spotify and go to the artist profile.\n2. Click 'Follow' on the artist.\n3. Stream the target song fully (at least 2 minutes).\n4. Take a screenshot showing the song is playing with the 'Followed' checkmark visible.",
            advPricePkr = 2.00,
            userPayoutPkr = 1.40,
            adminMarginPkr = 0.60,
            totalSlots = 1200,
            slotsFilled = 190,
            isFeatured = false
        ))

        // Category F: E-Commerce
        tasks.add(TaskEntity(
            id = "task_f1",
            advertiserId = adminId,
            campaignName = "Daraz PK: Product Review with Rating",
            platform = "Daraz Pakistan",
            category = "E-Commerce",
            taskType = "Product Review",
            targetUrl = "https://www.daraz.pk",
            instructions = "1. Open Daraz product link.\n2. Tap 5-stars for the product.\n3. Write a helpful comment: 'Bohat achi quality hai, fast delivery!'\n4. Submit screenshot showing your submitted review on Daraz app.",
            advPricePkr = 8.00,
            userPayoutPkr = 5.60,
            adminMarginPkr = 2.40,
            totalSlots = 400,
            slotsFilled = 188,
            isFeatured = true
        ))

        // Category J: Crypto & Fintech
        tasks.add(TaskEntity(
            id = "task_j1",
            advertiserId = adminId,
            campaignName = "Binance: Register and Complete KYC Lvl 1",
            platform = "Binance",
            category = "Crypto & Fintech",
            taskType = "Register + KYC",
            targetUrl = "https://www.binance.com",
            instructions = "1. Click the Binance referral link.\n2. Create a new account.\n3. Complete KYC Level 1 verification.\n4. Take a screenshot of your Binance profile showing 'Verified' status badge.",
            advPricePkr = 30.00,
            userPayoutPkr = 21.00,
            adminMarginPkr = 9.00,
            totalSlots = 50,
            slotsFilled = 3,
            isFeatured = true
        ))

        // Category K: Surveys
        tasks.add(TaskEntity(
            id = "task_k1",
            advertiserId = adminId,
            campaignName = "Complete Google Form Survey (10 Questions)",
            platform = "Google Forms",
            category = "Survey & Feedback",
            taskType = "Google Form Survey",
            targetUrl = "https://forms.google.com",
            instructions = "1. Open the Google Form link.\n2. Answer all 10 questions truthfully.\n3. Click Submit.\n4. Take a screenshot of the 'Your response has been recorded' confirmation page.",
            advPricePkr = 5.00,
            userPayoutPkr = 3.50,
            adminMarginPkr = 1.50,
            totalSlots = 1000,
            slotsFilled = 450,
            isFeatured = false
        ))

        return tasks
    }
}
