package com.slacklock

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {

    private lateinit var bigButton: Button
    private lateinit var statusText: TextView
    private lateinit var helpText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bigButton = findViewById(R.id.big_button)
        statusText = findViewById(R.id.status_text)
        helpText = findViewById(R.id.help_text)

        bigButton.setOnClickListener { handleButtonPress() }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val accessibilityEnabled = isAccessibilityEnabled()
        if (BlockState.isBlocked(this)) {
            val until = Date(BlockState.blockUntilMillis(this))
            val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
            statusText.text = if (accessibilityEnabled)
                getString(R.string.status_blocked_enforcing, fmt.format(until))
            else
                getString(R.string.status_blocked_not_enforcing, fmt.format(until))
            statusText.visibility = View.VISIBLE
            bigButton.visibility = if (accessibilityEnabled) View.GONE else View.VISIBLE
            bigButton.text = getString(R.string.enable_enforcement_text)
            helpText.text = if (accessibilityEnabled)
                getString(R.string.help_locked)
            else
                getString(R.string.help_locked_not_enforcing)
        } else {
            statusText.visibility = View.GONE
            bigButton.visibility = View.VISIBLE
            bigButton.text = getString(R.string.big_button_text)
            helpText.text = if (accessibilityEnabled)
                getString(R.string.help_ready)
            else
                getString(R.string.help_needs_permission)
        }
    }

    private fun handleButtonPress() {
        if (!BlockState.hasAcceptedAccessibilityDisclosure(this)) {
            showAccessibilityDisclosureDialog()
            return
        }

        if (!isAccessibilityEnabled()) {
            showEnableAccessibilityDialog()
            return
        }

        if (!BlockState.isBlocked(this)) {
            showStartBlockConfirmation()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, BlockerService::class.java).flattenToString()
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (info in enabled) {
            val id = info.id ?: continue
            if (id.equals(expected, ignoreCase = true)) return true
            // Some OEMs format the id slightly differently — fall back to substring check.
            if (id.contains(packageName) && id.contains("BlockerService")) return true
        }
        // Belt-and-braces: also check the secure setting string.
        val flat = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(flat)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    private fun showEnableAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.enable_title)
            .setMessage(R.string.enable_message)
            .setPositiveButton(R.string.enable_open) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAccessibilityDisclosureDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.disclosure_title)
            .setMessage(R.string.disclosure_message)
            .setPositiveButton(R.string.disclosure_accept) { _, _ ->
                BlockState.acceptAccessibilityDisclosure(this)
                if (isAccessibilityEnabled()) {
                    showStartBlockConfirmation()
                } else {
                    showEnableAccessibilityDialog()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showStartBlockConfirmation() {
        val until = Date(BlockState.nextBlockUntilMillis())
        val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_title)
            .setMessage(getString(R.string.confirm_message, fmt.format(until)))
            .setPositiveButton(R.string.confirm_lock) { _, _ ->
                BlockState.startBlockUntilNext6am(this)
                refreshUi()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
