# Jenkins Email Notification Setup Guide

## Problem Analysis
- ✅ Jenkinsfile correctly uses `emailext` 
- ✅ Email is enabled (`EMAIL_ENABLED = 'true'`)
- ✅ Recipient email is set
- ❌ **SMTP server not configured in Jenkins** (most likely issue)

## Step-by-Step Fix

### Step 1: Install Email Extension Plugin

1. Go to: **Manage Jenkins** → **Manage Plugins** → **Available**
2. Search for: `Email Extension Plugin`
3. Install it (restart Jenkins if needed)

### Step 2: Configure SMTP Settings

1. Go to: **Manage Jenkins** → **Configure System**
2. Scroll down to **"Extended E-mail Notification"** section

**Configure these settings:**

```
SMTP server: smtp.gmail.com
SMTP Port: 465
Use SSL: ✅ (checked)
Use TLS: ❌ (unchecked)
Use SMTP Authentication: ✅ (checked)
User Name: anastasia.suhareva@gmail.com
Password: [Your Gmail App Password - see below]
Default user e-mail suffix: @gmail.com
```

### Step 3: Get Gmail App Password

**Important:** You CANNOT use your regular Gmail password!

1. Go to: https://myaccount.google.com/apppasswords
2. Sign in with your Gmail account
3. Select "Mail" and "Other (Custom name)"
4. Enter "Jenkins" as the name
5. Click "Generate"
6. Copy the 16-character password (no spaces)
7. Use this password in Jenkins SMTP settings

### Step 4: Test Email Configuration

1. In Jenkins: **Manage Jenkins** → **Configure System**
2. Scroll to **"Extended E-mail Notification"**
3. Click **"Test configuration by sending test e-mail"**
4. Enter your email: `anastasia.suhareva@gmail.com`
5. Click **"Test configuration"**
6. Check your email (including spam folder)

### Step 5: Configure Default Email Settings (Optional)

In the same **"Extended E-mail Notification"** section:

- **Default Subject**: `Build ${BUILD_STATUS}: ${PROJECT_NAME} - Build #${BUILD_NUMBER}`
- **Default Content**: Leave default or customize
- **Default Recipients**: `anastasia.suhareva@gmail.com` (optional, Jenkinsfile already sets this)

### Step 6: Save and Test

1. Click **"Save"** at the bottom
2. Run a Jenkins build
3. Check your email for notifications

## Troubleshooting

### If emails still don't arrive:

1. **Check spam folder** - Gmail often filters automated emails
2. **Check Jenkins logs**: 
   - Go to: **Manage Jenkins** → **System Log**
   - Look for email-related errors
3. **Verify App Password**:
   - Make sure you're using App Password, not regular password
   - Regenerate if needed
4. **Check firewall**: Port 465 should be open
5. **Try different SMTP settings**:
   - Port: 587
   - Use TLS instead of SSL
   - SMTP server: `smtp.gmail.com`

### Common Errors:

- **"Connection error"**: SMTP server/port wrong, or firewall blocking
- **"Authentication failed"**: Wrong password or not using App Password
- **"No valid crumb"**: CSRF issue - refresh Jenkins page

## Alternative: Use Default Mailer (Simpler)

If Email Extension Plugin is too complex:

1. Go to: **Manage Jenkins** → **Configure System**
2. Find **"E-mail Notification"** section (not Extended)
3. Configure SMTP there
4. Update Jenkinsfile to use `mail()` instead of `emailext()`

But `emailext` is better - more features and better formatting.

