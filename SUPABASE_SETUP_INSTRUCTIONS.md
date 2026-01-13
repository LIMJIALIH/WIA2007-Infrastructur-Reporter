# Supabase Dashboard Configuration for Password Reset

## Problem
Email links redirect to `localhost:3000` instead of opening the app.

## Root Cause
Supabase uses the **Site URL** from dashboard settings as the default redirect.

## Solution: Configure Supabase Dashboard

### Step 1: Go to Authentication Settings
1. Open: https://supabase.com/dashboard/project/zowpyfmlpkzvvaqkjljf/auth/url-configuration
2. Or navigate: Dashboard → Your Project → Authentication → URL Configuration

### Step 2: Update Site URL
**Current:** `http://localhost:3000` (WRONG!)
**Change to:** `infrastructurereporter://reset-password`

### Step 3: Add Redirect URLs
In the **Redirect URLs** section, add BOTH:
```
infrastructurereporter://reset-password
infrastructurereporter://**
```

### Step 4: Save Changes
Click the **SAVE** button at the bottom.

---

## Why localhost:3000?
By default, Supabase assumes you're building a web app during development, so it uses localhost:3000 as the Site URL. For mobile apps, you must configure your custom URL scheme.

---

## Verification Steps:
1. Go to Supabase Dashboard → Authentication → URL Configuration
2. Verify **Site URL** = `infrastructurereporter://reset-password`
3. Verify **Redirect URLs** includes your app scheme
4. Save and wait 1-2 minutes for propagation
5. Send a new password reset email
6. Click the link in the email
7. App should open to ResetPasswordActivity ✅

---

## Still Not Working?
If you still see localhost:3000 after configuring:
1. Clear your browser cache
2. Wait 2-3 minutes (settings take time to propagate)
3. Request a NEW password reset link (old emails still use old settings)
4. Make sure your app is installed on the device/emulator
