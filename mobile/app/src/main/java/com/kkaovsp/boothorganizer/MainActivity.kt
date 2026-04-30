package com.kkaovsp.boothorganizer

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

sealed class Screen {
    object Home : Screen()
    object Login : Screen()
    object Register : Screen()
    object Events : Screen()
    data class Booths(val eventId: String, val eventName: String) : Screen()
    object Reservations : Screen()
    object Admin : Screen()
    object CreateEvent : Screen()
    object MerchantApproval : Screen()
    object PaymentApproval : Screen()
    object Profile : Screen()
    object Reports : Screen()
    object Notifications : Screen()
}

class MainActivity : Activity() {
    private val primary = Color.rgb(79, 70, 229)
    private val primaryDark = Color.rgb(55, 48, 163)
    private val primaryLight = Color.rgb(224, 231, 255)
    private val secondary = Color.rgb(6, 182, 212)
    private val success = Color.rgb(16, 185, 129)
    private val warning = Color.rgb(245, 158, 11)
    private val danger = Color.rgb(239, 68, 68)
    private val bg = Color.rgb(248, 250, 252)
    private val surface = Color.WHITE
    private val surfaceAlt = Color.rgb(241, 245, 249)
    private val border = Color.rgb(226, 232, 240)
    private val textPrimary = Color.rgb(15, 23, 42)
    private val textSecondary = Color.rgb(100, 116, 139)
    private val textMuted = Color.rgb(148, 163, 184)

    private lateinit var api: ApiClient
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var user: JSONObject? = null
    private var currentScreen: Screen = Screen.Home
    private var language = "th"
    private var selectedReportEventId = ""
    private var pendingSlipPaymentId: String? = null
    private var pendingSlipReservationId: String? = null
    private val prefs by lazy { getSharedPreferences("booth-organizer-mobile", MODE_PRIVATE) }
    private val currency by lazy { NumberFormat.getCurrencyInstance(Locale("th", "TH")) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = ApiClient(this)
        api.accessToken = prefs.getString("access_token", null)
        language = prefs.getString("language", "th") ?: "th"
        renderShell()
        refreshMe(false)
    }

    @Deprecated("startActivityForResult is enough here and avoids AndroidX dependencies.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SLIP_PICK_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            try {
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
                // Some providers do not support persistable permissions.
            }
            uploadSelectedSlip(uri)
        }
    }

    private fun renderShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        setContentView(root)
        buildHeader()

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            setBackgroundColor(bg)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        showCurrentScreen()
    }

    private fun buildHeader() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(10), dp(24), dp(8))
            background = rect(surface, dp(0), border, dp(1))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(2).toFloat()
            }
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(brandView(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(smallButton(if (language == "th") "TH" else "EN", false) {
            language = if (language == "th") "en" else "th"
            prefs.edit().putString("language", language).apply()
            renderShell()
        })
        header.addView(top)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.RIGHT
            setPadding(0, dp(8), 0, 0)
        }
        if (user == null) {
            actions.addView(smallButton(t("login"), false) { navigate(Screen.Login) })
            actions.addView(space(8, 1))
            actions.addView(smallButton(t("register"), true) { navigate(Screen.Register) })
        } else {
            actions.addView(smallButton("🔔", false) { navigate(Screen.Notifications) })
            actions.addView(space(8, 1))
            actions.addView(smallButton(t("logout"), false, danger) { logout() })
        }
        header.addView(actions)

        val navScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(10), dp(2), 0)
        }
        navButton(nav, t("home"), currentScreen is Screen.Home) { navigate(Screen.Home) }
        navButton(nav, t("events"), currentScreen is Screen.Events || currentScreen is Screen.Booths) { navigate(Screen.Events) }
        user?.let {
            navButton(nav, t("reservations"), currentScreen is Screen.Reservations) { navigate(Screen.Reservations) }
            navButton(nav, t("profile"), currentScreen is Screen.Profile) { navigate(Screen.Profile) }
            if (role() == "BOOTH_MANAGER") {
                navButton(nav, t("create_event"), currentScreen is Screen.CreateEvent) { navigate(Screen.CreateEvent) }
                navButton(nav, t("reports"), currentScreen is Screen.Reports) { navigate(Screen.Reports) }
                navButton(
                    nav,
                    t("admin"),
                    currentScreen is Screen.Admin || currentScreen is Screen.MerchantApproval || currentScreen is Screen.PaymentApproval,
                ) { navigate(Screen.Admin) }
            }
        }
        navScroll.addView(nav)
        header.addView(navScroll)
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun brandView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val icon = label("🏪", 22f, primary, Typeface.BOLD).apply {
                gravity = Gravity.CENTER
                background = rect(primaryLight, dp(10))
                includeFontPadding = false
            }
            addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
            addView(space(10, 1))
            val title = label(t("brand"), 22f, primary, Typeface.BOLD).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                includeFontPadding = false
            }
            addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun showCurrentScreen() {
        content.removeAllViews()
        when (val screen = currentScreen) {
            Screen.Home -> showHome()
            Screen.Login -> showLogin()
            Screen.Register -> showRegister()
            Screen.Events -> showEvents()
            is Screen.Booths -> showBooths(screen.eventId, screen.eventName)
            Screen.Reservations -> requireAuth { showReservations() }
            Screen.Admin -> requireManager { showAdmin() }
            Screen.CreateEvent -> requireManager { showCreateEvent() }
            Screen.MerchantApproval -> requireManager { showMerchantApproval() }
            Screen.PaymentApproval -> requireManager { showPaymentApproval() }
            Screen.Profile -> requireAuth { showProfile() }
            Screen.Reports -> requireManager { showReports() }
            Screen.Notifications -> requireAuth { showNotifications() }
        }
    }

    private fun showHome() {
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(64), dp(32), dp(64))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(primary, secondary)
            ).apply { cornerRadius = dp(24).toFloat() }
        }
        hero.addView(label(t("home_title"), 44f, Color.WHITE, Typeface.BOLD).apply { gravity = Gravity.CENTER })
        hero.addView(label(t("home_subtitle"), 17.6f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            alpha = 0.9f
            setPadding(0, dp(12), 0, dp(28))
        })
        hero.addView(button(t("browse_events"), false) { navigate(Screen.Events) }.apply {
            setTextColor(primary)
            background = rect(Color.WHITE, dp(6))
        })
        content.addView(hero, matchWrap(bottom = dp(40)))

        featureCard("🎪", t("find_events"), t("find_events_desc")) { navigate(Screen.Events) }
        featureCard("🛒", t("reserve_booth"), t("reserve_booth_desc")) { navigate(Screen.Events) }
        featureCard("💳", t("easy_payment"), t("easy_payment_desc")) { navigate(Screen.Events) }
        if (user != null) {
            featureCard("🔔", t("notifications_feature"), t("notifications_desc")) { navigate(Screen.Notifications) }
        }
    }

    private fun showLogin() {
        pageTitle(t("login_title"), t("login_subtitle"))
        val username = input(t("username"))
        val password = input(t("password"), password = true)
        card {
            addField(t("username"), username)
            addField(t("password"), password)
            addView(button(t("login_submit"), true) {
                login(username.text.toString(), password.text.toString())
            }, matchWrap(top = dp(8)))
            addView(textLink(t("go_register")) { navigate(Screen.Register) }, matchWrap(top = dp(14)))
        }
    }

    private fun showRegister() {
        pageTitle(t("register_title"), t("register_subtitle"))
        val username = input(t("username"))
        val password = input(t("password"), password = true)
        val fullName = input(t("full_name"))
        val contact = input(t("contact"))
        val citizen = input(t("citizen_id"), number = true)
        val sellerInfo = input(t("seller_info"), multiline = true)
        val product = input(t("product_desc"), multiline = true)
        val merchantBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val roleGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            val general = RadioButton(this@MainActivity).apply {
                id = View.generateViewId()
                text = t("general_user")
                isChecked = true
            }
            val merchant = RadioButton(this@MainActivity).apply {
                id = View.generateViewId()
                text = t("merchant")
            }
            addView(general)
            addView(merchant)
            setOnCheckedChangeListener { _, checkedId ->
                merchantBox.visibility = if (checkedId == merchant.id) View.VISIBLE else View.GONE
            }
        }

        card {
            addField(t("username"), username)
            addField(t("password"), password)
            addField(t("full_name"), fullName)
            addField(t("contact"), contact)
            addView(label(t("account_type"), 14f, textSecondary, Typeface.BOLD), matchWrap(top = dp(4)))
            addView(roleGroup)
            merchantBox.visibility = View.GONE
            merchantBox.addField(t("citizen_id"), citizen)
            merchantBox.addField(t("seller_info"), sellerInfo)
            merchantBox.addField(t("product_desc"), product)
            addView(merchantBox)
            addView(button(t("register_submit"), true) {
                val isMerchant = roleGroup.indexOfChild(roleGroup.findViewById(roleGroup.checkedRadioButtonId)) == 1
                val body = JSONObject()
                    .put("username", username.text.toString())
                    .put("password", password.text.toString())
                    .put("name", fullName.text.toString())
                    .put("contact_info", contact.text.toString())
                    .put("role", if (isMerchant) "merchant" else "general")
                if (isMerchant) {
                    body.put("citizen_id", citizen.text.toString())
                        .put("seller_information", sellerInfo.text.toString())
                        .put("product_description", product.text.toString())
                }
                task({ api.postJson("/auth/register", body) }, {
                    toast(t("register_success"))
                    navigate(Screen.Login)
                })
            }, matchWrap(top = dp(8)))
            addView(textLink(t("go_login")) { navigate(Screen.Login) }, matchWrap(top = dp(14)))
        }
    }

    private fun showEvents() {
        pageTitle(t("events_title"))
        if (role() == "BOOTH_MANAGER") {
            content.addView(button(t("create_event"), true) { navigate(Screen.CreateEvent) }, matchWrap(bottom = dp(12)))
        }
        setLoading()
        task({ JSONArray(api.get("/events")) }, { events ->
            content.removeAllViews()
            pageTitle(t("events_title"))
            if (role() == "BOOTH_MANAGER") {
                content.addView(button(t("create_event"), true) { navigate(Screen.CreateEvent) }, matchWrap(bottom = dp(12)))
            }
            if (events.length() == 0) emptyState("🎪", t("no_events"))
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                eventCard(event)
            }
        })
    }

    private fun eventCard(event: JSONObject) {
        card {
            addView(row().apply {
                addView(label(event.clean("name"), 18f, textPrimary, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(statusBadge(eventStatus(event), eventStatusColor(event)))
            })
            addView(label("📍 ${event.clean("location")}", 14f, textSecondary), matchWrap(top = dp(6)))
            addView(label("📅 ${event.clean("start_date")} - ${event.clean("end_date")}", 14f, textSecondary))
            val desc = event.clean("description")
            if (desc.isNotBlank()) addView(label(desc, 14f, textSecondary), matchWrap(top = dp(8)))
            addView(row().apply {
                addView(smallButton(t("view_booths"), true) {
                    navigate(Screen.Booths(event.clean("event_id"), event.clean("name")))
                })
                if (role() == "BOOTH_MANAGER") {
                    addView(space(8, 1))
                    addView(smallButton(t("edit"), false) { eventDialog(event) })
                    addView(space(8, 1))
                    addView(smallButton(t("delete"), false, danger) {
                        confirm(t("delete_event_confirm")) {
                            task({ api.delete("/events/${event.clean("event_id")}") }, { showEvents() })
                        }
                    })
                }
            }, matchWrap(top = dp(12)))
        }
    }

    private fun showCreateEvent() {
        pageTitle(t("create_event_title"))
        eventForm(null)
    }

    private fun eventDialog(event: JSONObject) {
        val view = eventFormView(event)
        AlertDialog.Builder(this)
            .setTitle(t("edit_event"))
            .setView(view.first)
            .setNegativeButton(t("cancel"), null)
            .setPositiveButton(t("save"), null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val body = view.second()
                        task({ api.putJson("/events/${event.clean("event_id")}", body) }, {
                            dialog.dismiss()
                            showEvents()
                        })
                    }
                }
                dialog.show()
            }
    }

    private fun eventForm(event: JSONObject?) {
        val form = eventFormView(event)
        card {
            addView(form.first)
            addView(row().apply {
                addView(button(t("save"), true) {
                    val body = form.second()
                    if (event == null) {
                        task({ api.postJson("/events", body) }, {
                            toast(t("create_event_success"))
                            navigate(Screen.Events)
                        })
                    } else {
                        task({ api.putJson("/events/${event.clean("event_id")}", body) }, { navigate(Screen.Events) })
                    }
                })
                addView(space(8, 1))
                addView(button(t("cancel"), false) { navigate(Screen.Events) })
            }, matchWrap(top = dp(10)))
        }
    }

    private fun eventFormView(event: JSONObject?): Pair<LinearLayout, () -> JSONObject> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val name = input(t("event_name"), event?.clean("name").orEmpty())
        val desc = input(t("description"), event?.clean("description").orEmpty(), multiline = true)
        val location = input(t("location"), event?.clean("location").orEmpty())
        val start = input("YYYY-MM-DD", event?.clean("start_date").orEmpty())
        val end = input("YYYY-MM-DD", event?.clean("end_date").orEmpty())
        box.addField(t("event_name"), name)
        box.addField(t("description"), desc)
        box.addField(t("location"), location)
        box.addField(t("start_date"), start)
        box.addField(t("end_date"), end)
        return box to {
            JSONObject()
                .put("name", name.text.toString())
                .put("description", desc.text.toString())
                .put("location", location.text.toString())
                .put("start_date", start.text.toString())
                .put("end_date", end.text.toString())
        }
    }

    private fun showBooths(eventId: String, eventName: String) {
        if (eventId.isBlank()) {
            pageTitle(t("booths_title"))
            emptyState("🏪", t("choose_event_first"))
            return
        }
        pageTitle("${t("booths_title")} - $eventName")
        if (role() == "BOOTH_MANAGER") {
            content.addView(button(t("add_booth"), true) { boothDialog(eventId, null) }, matchWrap(bottom = dp(12)))
        }
        setLoading()
        task({ JSONArray(api.get("/events/$eventId/booths")) }, { booths ->
            content.removeAllViews()
            pageTitle("${t("booths_title")} - $eventName")
            if (role() == "BOOTH_MANAGER") {
                content.addView(button(t("add_booth"), true) { boothDialog(eventId, null) }, matchWrap(bottom = dp(12)))
            }
            if (booths.length() == 0) emptyState("🏪", t("no_booths"))
            for (i in 0 until booths.length()) boothCard(eventId, eventName, booths.getJSONObject(i))
        })
    }

    private fun boothCard(eventId: String, eventName: String, booth: JSONObject) {
        val available = booth.clean("status") == "AVAILABLE"
        val borderColor = when (booth.clean("status")) {
            "AVAILABLE" -> Color.rgb(167, 243, 208)
            "RESERVED" -> Color.rgb(253, 230, 138)
            else -> null
        }
        card(borderColor) {
            addView(row().apply {
                addView(label("#${booth.clean("booth_number")}", 19f, textPrimary, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(statusBadge(booth.clean("status"), when (booth.clean("status")) {
                    "AVAILABLE" -> success
                    "RESERVED" -> warning
                    else -> textMuted
                }))
            })
            addView(label(formatMoney(booth.optDouble("price", 0.0)), 22f, primary, Typeface.BOLD), matchWrap(top = dp(6)))
            addView(label("📏 ${booth.clean("size")}   📍 ${booth.clean("location")}", 14f, textSecondary), matchWrap(top = dp(6)))
            val facilities = mutableListOf(if (booth.clean("type") == "INDOOR") t("indoor") else t("outdoor"), booth.clean("classification"), booth.clean("duration_type"))
            if (booth.optBoolean("electricity")) facilities.add("${t("electricity")} (${booth.optInt("outlets")} ${t("outlets")})")
            if (booth.optBoolean("water_supply")) facilities.add(t("water"))
            addView(label(facilities.joinToString(" · "), 13f, textSecondary), matchWrap(top = dp(4)))
            addView(row().apply {
                if (role() == "MERCHANT" && available) {
                    addView(smallButton(t("reserve"), true, success) {
                        confirm(t("reserve_confirm")) {
                            val body = JSONObject()
                                .put("booth_id", booth.clean("booth_id"))
                                .put("reservation_type", "SHORT_TERM")
                            task({ api.postJson("/reservations", body) }, {
                                toast(t("reserve_success"))
                                navigate(Screen.Reservations)
                            })
                        }
                    })
                }
                if (role() == "BOOTH_MANAGER") {
                    addView(smallButton(t("edit"), false) { boothDialog(eventId, booth) })
                    addView(space(8, 1))
                    addView(smallButton(t("delete"), false, danger) {
                        confirm(t("delete_booth_confirm")) {
                            task({ api.delete("/booths/${booth.clean("booth_id")}") }, {
                                showBooths(eventId, eventName)
                            })
                        }
                    })
                }
            }, matchWrap(top = dp(12)))
        }
    }

    private fun boothDialog(eventId: String, booth: JSONObject?) {
        val form = boothFormView(eventId, booth)
        AlertDialog.Builder(this)
            .setTitle(if (booth == null) t("add_booth") else t("edit_booth"))
            .setView(form.first)
            .setNegativeButton(t("cancel"), null)
            .setPositiveButton(t("save"), null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val work = if (booth == null) {
                            { api.postJson("/booths", form.second()) }
                        } else {
                            { api.putJson("/booths/${booth.clean("booth_id")}", form.second()) }
                        }
                        task(work, {
                            dialog.dismiss()
                            showBooths(eventId, currentBoothEventName())
                        })
                    }
                }
                dialog.show()
            }
    }

    private fun boothFormView(eventId: String, booth: JSONObject?): Pair<LinearLayout, () -> JSONObject> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val number = input("A-01", booth?.clean("booth_number").orEmpty())
        val size = input("3x3 m", booth?.clean("size").orEmpty())
        val price = input("0.00", booth?.clean("price").orEmpty(), number = true)
        val location = input(t("location"), booth?.clean("location").orEmpty())
        val type = spinner(listOf("INDOOR" to t("indoor_plain"), "OUTDOOR" to t("outdoor_plain")), booth?.clean("type").orEmpty().ifBlank { "INDOOR" })
        val duration = spinner(listOf("SHORT_TERM" to t("short_term"), "LONG_TERM" to t("long_term")), booth?.clean("duration_type").orEmpty().ifBlank { "SHORT_TERM" })
        val classification = spinner(listOf("FIXED" to t("fixed"), "TEMPORARY" to t("temporary")), booth?.clean("classification").orEmpty().ifBlank { "FIXED" })
        val electricity = CheckBox(this).apply {
            text = t("electricity")
            isChecked = booth?.optBoolean("electricity") ?: false
        }
        val outlets = input("0", (booth?.optInt("outlets") ?: 0).toString(), number = true)
        val water = CheckBox(this).apply {
            text = t("water")
            isChecked = booth?.optBoolean("water_supply") ?: false
        }
        box.addField(t("booth_number"), number)
        box.addField(t("size"), size)
        box.addField(t("price"), price)
        box.addField(t("location"), location)
        box.addField(t("type"), type)
        box.addField(t("duration"), duration)
        box.addField(t("classification"), classification)
        box.addView(electricity)
        box.addField(t("outlets"), outlets)
        box.addView(water)
        return box to {
            JSONObject()
                .put("event_id", eventId)
                .put("booth_number", number.text.toString())
                .put("size", size.text.toString())
                .put("price", price.text.toString().toDoubleOrNull() ?: 0.0)
                .put("location", location.text.toString())
                .put("type", type.selectedValue())
                .put("duration_type", duration.selectedValue())
                .put("classification", classification.selectedValue())
                .put("electricity", electricity.isChecked)
                .put("outlets", outlets.text.toString().toIntOrNull() ?: 0)
                .put("water_supply", water.isChecked)
        }
    }

    private fun currentBoothEventName(): String {
        val screen = currentScreen
        return if (screen is Screen.Booths) screen.eventName else ""
    }

    private fun showReservations() {
        pageTitle(t("reservations_title"))
        setLoading()
        task({ JSONArray(api.get("/reservations")) }, { reservations ->
            content.removeAllViews()
            pageTitle(t("reservations_title"))
            if (reservations.length() == 0) emptyState("📋", t("no_reservations"))
            for (i in 0 until reservations.length()) reservationCard(reservations.getJSONObject(i))
        })
    }

    private fun reservationCard(reservation: JSONObject) {
        val payment = reservation.optJSONObject("payment") ?: JSONObject()
        val booth = reservation.optJSONObject("booth") ?: JSONObject()
        card {
            addView(row().apply {
                addView(label("${t("booth")} #${booth.clean("booth_number").ifBlank { reservation.clean("booth_id") }}", 18f, textPrimary, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(statusBadge(reservation.clean("status"), reservationColor(reservation.clean("status"))))
            })
            addView(label("${formatMoney(booth.optDouble("price", 0.0))} · ID: ${reservation.clean("reservation_id")}", 13f, textSecondary), matchWrap(top = dp(6)))
            if (payment.clean("payment_status").isNotBlank()) {
                addView(statusBadge("${t("payment")}: ${payment.clean("payment_status")}", paymentColor(payment.clean("payment_status"))), matchWrap(top = dp(6)))
            }
            if (payment.clean("slip_url").isNotBlank()) {
                addView(statusBadge(t("slip_uploaded"), secondary), matchWrap(top = dp(6)))
            }

            if (role() == "MERCHANT" && reservation.clean("status") == "PENDING_PAYMENT") {
                val amount = input(t("amount"), booth.clean("price"), number = true)
                val method = spinner(
                    listOf("CREDIT_CARD" to t("credit_card"), "TRUEMONEY" to t("truemoney"), "BANK_TRANSFER" to t("bank_transfer")),
                    "BANK_TRANSFER"
                )
                addView(divider())
                addField(t("amount"), amount)
                addField(t("payment_method"), method)
                addView(row().apply {
                    addView(smallButton(t("submit_payment"), true) {
                        val body = JSONObject()
                            .put("reservation_id", reservation.clean("reservation_id"))
                            .put("amount", amount.text.toString().toDoubleOrNull() ?: 0.0)
                            .put("method", method.selectedValue())
                        task({ JSONObject(api.postJson("/payments", body)) }, { response ->
                            if (method.selectedValue() == "BANK_TRANSFER") {
                                toast(t("payment_created_choose_slip"))
                                pickSlip(response.clean("payment_id"), reservation.clean("reservation_id"))
                            } else {
                                toast(t("payment_submitted"))
                                showReservations()
                            }
                        })
                    })
                    addView(space(8, 1))
                    addView(smallButton(t("cancel_reservation"), false, danger) {
                        confirm(t("cancel_reservation_confirm")) {
                            task({ api.patchJson("/reservations/${reservation.clean("reservation_id")}/cancel") }, {
                                toast(t("cancel_success"))
                                showReservations()
                            })
                        }
                    })
                }, matchWrap(top = dp(10)))
            }

            val needsSlip = role() == "MERCHANT" &&
                reservation.clean("status") == "WAITING_FOR_APPROVAL" &&
                payment.clean("method") == "BANK_TRANSFER" &&
                payment.clean("slip_url").isBlank()
            if (needsSlip) {
                addView(divider())
                addView(label(t("slip_not_uploaded"), 14f, danger))
                addView(smallButton(t("upload_slip"), true) {
                    pickSlip(payment.clean("payment_id"), reservation.clean("reservation_id"))
                }, matchWrap(top = dp(8)))
            }

            if (role() == "BOOTH_MANAGER" && reservation.clean("status") == "WAITING_FOR_APPROVAL" && payment.clean("payment_id").isNotBlank()) {
                addView(divider())
                val disabled = payment.clean("method") == "BANK_TRANSFER" && payment.clean("slip_url").isBlank()
                val approve = smallButton(t("approve_payment"), true, success) {
                    confirm(t("approve_payment_confirm")) {
                        task({ api.patchJson("/payments/${payment.clean("payment_id")}/approve") }, {
                            toast(t("payment_approved"))
                            showReservations()
                        })
                    }
                }.apply { isEnabled = !disabled }
                addView(approve)
                if (disabled) addView(label(t("slip_required"), 13f, danger), matchWrap(top = dp(6)))
            }
        }
    }

    private fun showAdmin() {
        pageTitle(t("admin_title"))
        featureCard("👥", t("approve_merchants"), t("approve_merchants_desc")) { navigate(Screen.MerchantApproval) }
        featureCard("💳", t("review_payments"), t("review_payments_desc")) { navigate(Screen.PaymentApproval) }
        featureCard("📊", t("view_reports"), t("view_reports_desc")) { navigate(Screen.Reports) }
    }

    private fun showMerchantApproval() {
        pageTitle(t("merchant_approval_title"))
        setLoading()
        task({ JSONArray(api.get("/users")) }, { users ->
            content.removeAllViews()
            pageTitle(t("merchant_approval_title"))
            if (users.length() == 0) emptyState("👥", t("no_users"))
            for (i in 0 until users.length()) {
                val item = users.getJSONObject(i)
                card {
                    addView(label(item.clean("name").ifBlank { item.clean("username") }, 18f, textPrimary, Typeface.BOLD))
                    addView(label("${item.clean("username")} · ${item.clean("role")}", 14f, textSecondary), matchWrap(top = dp(2)))
                    addView(statusBadge(item.clean("approval_status").ifBlank { "PENDING" }, approvalColor(item.clean("approval_status"))), matchWrap(top = dp(8)))
                    addView(label("${t("moi_validation")}: ${if (item.isNull("citizen_valid")) "N/A" else if (item.optBooleanLike("citizen_valid")) t("valid") else t("invalid")}", 14f, textSecondary), matchWrap(top = dp(8)))
                    if (item.clean("seller_information").isNotBlank()) addView(label(item.clean("seller_information"), 14f, textSecondary), matchWrap(top = dp(6)))
                    if (item.clean("product_description").isNotBlank()) addView(label(item.clean("product_description"), 13f, textMuted), matchWrap(top = dp(4)))
                    val status = spinner(listOf("PENDING" to "PENDING", "APPROVED" to "APPROVED", "REJECTED" to "REJECTED"), item.clean("approval_status").ifBlank { "PENDING" })
                    addField(t("status"), status)
                    addView(smallButton(t("save_status"), true) {
                        val body = JSONObject().put("status", status.selectedValue())
                        task({ api.patchJson("/users/${item.clean("id")}/merchant_status", body) }, {
                            toast(t("status_saved"))
                            showMerchantApproval()
                        })
                    }, matchWrap(top = dp(8)))
                }
            }
        })
    }

    private fun showPaymentApproval() {
        pageTitle(t("payment_approval_title"))
        setLoading()
        task({ JSONArray(api.get("/payments")) }, { payments ->
            content.removeAllViews()
            pageTitle(t("payment_approval_title"))
            val pending = (0 until payments.length()).map { payments.getJSONObject(it) }
                .filter { it.clean("payment_status") == "PENDING" }
            if (pending.isEmpty()) emptyState("✅", t("no_pending_payments"))
            pending.forEach { payment ->
                card {
                    addView(label("${t("payment")} ${payment.clean("payment_id")}", 16f, textPrimary, Typeface.BOLD))
                    addView(label("${t("reservation_id")}: ${payment.clean("reservation_id")}", 13f, textSecondary), matchWrap(top = dp(4)))
                    addView(label("${formatMoney(payment.optDouble("amount", 0.0))} · ${payment.clean("method")}", 15f, primary, Typeface.BOLD), matchWrap(top = dp(6)))
                    val requiresSlip = payment.clean("method") == "BANK_TRANSFER"
                    if (requiresSlip) {
                        if (payment.clean("slip_url").isBlank()) {
                            addView(statusBadge(t("missing_slip"), danger), matchWrap(top = dp(8)))
                        } else {
                            addView(statusBadge(t("slip_uploaded"), success), matchWrap(top = dp(8)))
                            addView(smallButton(t("view_slip"), false) {
                                task({ JSONObject(api.get("/payments/${payment.clean("payment_id")}/slip")) }, { slip ->
                                    AlertDialog.Builder(this@MainActivity)
                                        .setTitle(t("view_slip"))
                                        .setMessage("${slip.clean("message")}\n${slip.clean("slip_url")}")
                                        .setPositiveButton("OK", null)
                                        .show()
                                })
                            }, matchWrap(top = dp(8)))
                        }
                    }
                    val validated = CheckBox(this@MainActivity).apply {
                        text = t("validated")
                        visibility = if (requiresSlip && payment.clean("slip_url").isNotBlank()) View.VISIBLE else View.GONE
                    }
                    addView(validated)
                    val approve = smallButton(t("approve"), true, success) {
                        task({ api.patchJson("/payments/${payment.clean("payment_id")}/approve") }, {
                            toast(t("payment_approved"))
                            showPaymentApproval()
                        })
                    }
                    approve.isEnabled = !requiresSlip || payment.clean("slip_url").isNotBlank()
                    if (requiresSlip && payment.clean("slip_url").isNotBlank()) {
                        approve.isEnabled = false
                        validated.setOnCheckedChangeListener { _, checked -> approve.isEnabled = checked }
                    }
                    addView(approve, matchWrap(top = dp(8)))
                }
            }
        })
    }

    private fun showProfile() {
        pageTitle(t("profile_title"))
        setLoading()
        task({ JSONObject(api.get("/users/me")) }, { profile ->
            content.removeAllViews()
            pageTitle(t("profile_title"))
            val name = input(t("full_name"), profile.clean("name"))
            val contact = input(t("contact"), profile.clean("contact_info"))
            card {
                addView(label("${t("username")}: ${profile.clean("username")}", 16f, textPrimary, Typeface.BOLD))
                addView(label("${t("role")}: ${profile.clean("role")}", 14f, textSecondary), matchWrap(top = dp(4)))
                addField(t("full_name"), name)
                addField(t("contact"), contact)
                addView(button(t("save_profile"), true) {
                    val body = JSONObject()
                        .put("name", name.text.toString())
                        .put("contact_info", contact.text.toString())
                    task({ api.patchJson("/users/me", body) }, {
                        toast(t("profile_saved"))
                        refreshMe(false)
                        showProfile()
                    })
                }, matchWrap(top = dp(8)))
            }
            if (profile.clean("role") == "MERCHANT") {
                val seller = input(t("seller_info"), profile.clean("seller_information"), multiline = true)
                val product = input(t("product_desc"), profile.clean("product_description"), multiline = true)
                card {
                    addView(row().apply {
                        addView(label(t("seller_info"), 17f, textPrimary, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        addView(statusBadge(profile.clean("approval_status"), approvalColor(profile.clean("approval_status"))))
                    })
                    addField(t("seller_info"), seller)
                    addField(t("product_desc"), product)
                    addView(button(t("save_seller"), true) {
                        val body = JSONObject()
                            .put("seller_information", seller.text.toString())
                            .put("product_description", product.text.toString())
                        task({ api.patchJson("/users/me/seller", body) }, {
                            toast(t("seller_saved"))
                            showProfile()
                        })
                    }, matchWrap(top = dp(8)))
                }
            }
        })
    }

    private fun showReports() {
        pageTitle(t("reports_title"))
        setLoading()
        task({ JSONArray(api.get("/reports/events")) }, { events ->
            content.removeAllViews()
            pageTitle(t("reports_title"))
            if (events.length() == 0) {
                emptyState("📊", t("no_events"))
                return@task
            }
            val eventOptions = reportEventOptions(events)
            if (selectedReportEventId.isBlank()) selectedReportEventId = eventOptions.first().first
            addReportControls(eventOptions, selectedReportEventId)
            addViewSafe(label(t("reports_hint"), 14f, textSecondary), top = dp(4))
        })
    }

    private fun generateReport(eventId: String) {
        setLoading()
        task({
            JSONObject(api.get("/reports/reservations-payments?event_id=$eventId")) to JSONArray(api.get("/reports/events"))
        }, { result ->
            val report = result.first
            val eventOptions = reportEventOptions(result.second)
            content.removeAllViews()
            pageTitle(t("reports_title"))
            selectedReportEventId = eventId
            addReportControls(eventOptions, eventId)
            val event = report.optJSONObject("event") ?: JSONObject()
            val rows = report.optJSONArray("rows") ?: JSONArray()
            val rowList = (0 until rows.length()).map { rows.getJSONObject(it) }
            val confirmed = rowList.count { it.clean("reservation_status") == "CONFIRMED" }
            val pending = rowList.count { it.clean("reservation_status") in listOf("PENDING", "PENDING_PAYMENT", "WAITING_FOR_APPROVAL") }
            val total = rowList.sumOf { it.optDouble("payment_amount", 0.0) }
            val headerBox = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(primary, secondary)
                ).apply { cornerRadius = dp(10).toFloat() }
            }
            headerBox.addView(label(event.clean("name"), 20f, Color.WHITE, Typeface.BOLD))
            headerBox.addView(label("📅 ${event.clean("start_date")} - ${event.clean("end_date")} · 📍 ${event.clean("location")}", 14f, Color.WHITE).apply { alpha = 0.9f }, matchWrap(top = dp(4)))
            content.addView(headerBox, matchWrap(bottom = dp(20)))
            statRow(t("total_reservations"), rowList.size.toString(), "📊")
            statRow(t("confirmed"), confirmed.toString(), "✅", success)
            statRow(t("pending"), pending.toString(), "⏳", warning)
            val revenueColor = Color.rgb(124, 58, 237)
            statRow(t("total_revenue"), formatMoney(total), "💰", revenueColor)
            if (rowList.isEmpty()) {
                emptyState("📭", t("no_report_rows"))
            } else {
                pageTitle(t("reservation_details"))
                rowList.forEach { row ->
                    card {
                        addView(label("${t("booth")} ${row.clean("booth_number")}", 16f, textPrimary, Typeface.BOLD))
                        addView(label("${t("merchant")}: ${row.clean("merchant_name").ifBlank { "—" }}", 14f, textSecondary), matchWrap(top = dp(4)))
                        addView(label("${t("status")}: ${row.clean("reservation_status")} · ${t("payment")}: ${row.clean("payment_status").ifBlank { "—" }}", 13f, textSecondary), matchWrap(top = dp(4)))
                        addView(label("${row.clean("payment_method").ifBlank { "—" }} · ${formatMoney(row.optDouble("payment_amount", 0.0))}", 14f, primary, Typeface.BOLD), matchWrap(top = dp(4)))
                    }
                }
            }
        })
    }

    private fun reportEventOptions(events: JSONArray): List<Pair<String, String>> {
        return (0 until events.length()).map {
            val event = events.getJSONObject(it)
            event.clean("event_id") to "${event.clean("name")} - ${event.clean("location")}"
        }
    }

    private fun addReportControls(eventOptions: List<Pair<String, String>>, selectedEventId: String) {
        val selected = selectedEventId.ifBlank { eventOptions.first().first }
        val select = spinner(eventOptions, selected)
        card {
            addField(t("select_event"), select)
            addView(row().apply {
                addView(button(t("generate_report"), true) {
                    selectedReportEventId = select.selectedValue()
                    generateReport(selectedReportEventId)
                })
                addView(space(8, 1))
                addView(button(t("download_csv"), false) {
                    selectedReportEventId = select.selectedValue()
                    val selectedName = eventOptions.firstOrNull { it.first == selectedReportEventId }?.second?.substringBefore(" - ").orEmpty()
                    downloadCsv(selectedReportEventId, selectedName)
                })
            }, matchWrap(top = dp(10)))
        }
    }

    private fun downloadCsv(eventId: String, eventName: String) {
        task({
            val csv = api.get("/reports/reservations-payments.csv?event_id=$eventId")
            val fileName = "booth_report_${safeFileName(eventName.ifBlank { eventId })}_${System.currentTimeMillis()}.csv"
            saveCsvToDownloads(fileName, csv)
        }, { savedLocation ->
            toast("${t("csv_downloaded")}: $savedLocation")
        })
    }

    private fun saveCsvToDownloads(fileName: String, csv: String): String {
        val bytes = csv.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Cannot create CSV file in Downloads")
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("Cannot open CSV output stream")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
                return "Downloads/$fileName"
            } catch (error: Exception) {
                contentResolver.delete(uri, null, null)
                throw error
            }
        }

        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    private fun showNotifications() {
        pageTitle(t("notifications_title"))
        setLoading()
        task({ JSONArray(api.get("/notifications")) }, { notes ->
            content.removeAllViews()
            pageTitle(t("notifications_title"))
            if (notes.length() == 0) emptyState("🔔", t("no_notifications"))
            for (i in 0 until notes.length()) {
                val note = notes.getJSONObject(i)
                card {
                    addView(row().apply {
                        addView(label(note.clean("title"), 17f, textPrimary, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        if (!note.optBoolean("is_read")) addView(statusBadge(t("unread"), secondary))
                    })
                    addView(label(note.clean("message"), 14f, textSecondary), matchWrap(top = dp(6)))
                    if (!note.optBoolean("is_read")) {
                        addView(smallButton(t("mark_read"), false) {
                            task({ api.patchJson("/notifications/${note.clean("notification_id")}/read") }, { showNotifications() })
                        }, matchWrap(top = dp(8)))
                    }
                }
            }
        })
    }

    private fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            toast(t("missing_credentials"))
            return
        }
        task({ JSONObject(api.postForm("/auth/login", mapOf("username" to username, "password" to password))) }, { response ->
            val token = response.clean("access_token")
            api.accessToken = token
            prefs.edit().putString("access_token", token).apply()
            refreshMe(true)
        })
    }

    private fun logout() {
        task({ api.postJson("/auth/logout", JSONObject()) }, {
            api.accessToken = null
            prefs.edit().remove("access_token").apply()
            user = null
            currentScreen = Screen.Home
            renderShell()
        }, {
            api.accessToken = null
            prefs.edit().remove("access_token").apply()
            user = null
            currentScreen = Screen.Home
            renderShell()
        })
    }

    private fun refreshMe(goHome: Boolean) {
        val token = api.accessToken
        if (token.isNullOrBlank()) {
            user = null
            if (goHome) navigate(Screen.Home) else renderShell()
            return
        }
        task({ JSONObject(api.get("/auth/me")) }, { response ->
            user = response
            if (goHome) {
                toast(t("login_success"))
                navigate(Screen.Home)
            } else {
                renderShell()
            }
        }, {
            user = null
            api.accessToken = null
            prefs.edit().remove("access_token").apply()
            if (goHome) toast(t("login_failed"))
            renderShell()
        })
    }

    private fun pickSlip(paymentId: String, reservationId: String) {
        pendingSlipPaymentId = paymentId
        pendingSlipReservationId = reservationId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, SLIP_PICK_REQUEST)
    }

    private fun uploadSelectedSlip(uri: Uri) {
        val paymentId = pendingSlipPaymentId ?: return
        task({ api.uploadSlip(paymentId, uri) }, {
            toast(t("slip_uploaded"))
            pendingSlipPaymentId = null
            pendingSlipReservationId = null
            showReservations()
        })
    }

    private fun navigate(screen: Screen) {
        currentScreen = screen
        renderShell()
    }

    private fun requireAuth(block: () -> Unit) {
        if (user == null) {
            navigate(Screen.Login)
        } else {
            block()
        }
    }

    private fun requireManager(block: () -> Unit) {
        if (role() != "BOOTH_MANAGER") {
            toast(t("forbidden"))
            navigate(Screen.Home)
        } else {
            block()
        }
    }

    private fun <T> task(work: () -> T, success: (T) -> Unit, failure: ((String) -> Unit)? = null) {
        Thread {
            try {
                val result = work()
                runOnUiThread { success(result) }
            } catch (error: ApiException) {
                runOnUiThread {
                    val handler = failure
                    if (handler == null) toast(error.message) else handler(error.message)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    val message = error.message ?: t("unexpected_error")
                    val handler = failure
                    if (handler == null) toast(message) else handler(message)
                }
            }
        }.start()
    }

    private fun pageTitle(title: String, subtitle: String = "") {
        content.addView(label(title, 25f, textPrimary, Typeface.BOLD), matchWrap(bottom = if (subtitle.isBlank()) dp(12) else dp(4)))
        if (subtitle.isNotBlank()) {
            content.addView(label(subtitle, 15f, textSecondary), matchWrap(bottom = dp(14)))
        }
    }

    private fun featureCard(icon: String, title: String, body: String, action: () -> Unit) {
        card {
            gravity = Gravity.CENTER
            addView(label(icon, 32f, primary, Typeface.BOLD).apply { gravity = Gravity.CENTER })
            addView(label(title, 16f, textPrimary, Typeface.BOLD).apply { gravity = Gravity.CENTER }, matchWrap(top = dp(10)))
            addView(label(body, 13.6f, textSecondary).apply { gravity = Gravity.CENTER }, matchWrap(top = dp(6)))
            setOnClickListener { action() }
        }
    }

    private fun statRow(labelText: String, value: String, icon: String, color: Int = primary) {
        card {
            gravity = Gravity.CENTER
            addView(label(icon, 24f, textPrimary).apply { gravity = Gravity.CENTER }, matchWrap(bottom = dp(6)))
            addView(label(value, 28f, color, Typeface.BOLD).apply { gravity = Gravity.CENTER })
            addView(label(labelText, 12f, textSecondary, Typeface.BOLD).apply {
                gravity = Gravity.CENTER
                isAllCaps = true
            }, matchWrap(top = dp(4)))
        }
    }

    private fun card(borderColor: Int? = null, block: LinearLayout.() -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = rect(surface, dp(10), borderColor ?: border, dp(if (borderColor != null) 2 else 1))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(1).toFloat()
            }
            block()
        }
        content.addView(box, matchWrap(bottom = dp(20)))
    }

    private fun LinearLayout.addField(title: String, view: View) {
        addView(label(title, 13f, textSecondary, Typeface.BOLD), matchWrap(top = dp(10), bottom = dp(4)))
        addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun setLoading() {
        content.addView(ProgressBar(this), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(16)
        })
    }

    private fun emptyState(icon: String, message: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(36), dp(16), dp(36))
        }
        box.addView(label(icon, 34f, textMuted, Typeface.BOLD).apply { gravity = Gravity.CENTER })
        box.addView(label(message, 15f, textSecondary).apply { gravity = Gravity.CENTER }, matchWrap(top = dp(8)))
        content.addView(box, matchWrap())
    }

    private fun label(text: String, size: Float, color: Int, style: Int = Typeface.NORMAL): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            typeface = Typeface.DEFAULT_BOLD.takeIf { style == Typeface.BOLD } ?: Typeface.DEFAULT
            setLineSpacing(0f, 1.12f)
        }
    }

    private fun input(hint: String, value: String = "", multiline: Boolean = false, number: Boolean = false, password: Boolean = false): EditText {
        return EditText(this).apply {
            setText(value)
            this.hint = hint
            textSize = 14.4f
            setTextColor(textPrimary)
            setHintTextColor(textMuted)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = rect(surface, dp(6), border, dp(1))
            inputType = when {
                password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                number -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                multiline -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                else -> InputType.TYPE_CLASS_TEXT
            }
            if (multiline) {
                minLines = 3
                gravity = Gravity.TOP
            }
        }
    }

    private fun spinner(options: List<Pair<String, String>>, selected: String): Spinner {
        return Spinner(this).apply {
            tag = options
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, options.map { it.second })
            val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
            setSelection(index)
        }
    }

    private fun Spinner.selectedValue(): String {
        val options = tag as? List<Pair<String, String>> ?: return selectedItem?.toString().orEmpty()
        return options.getOrNull(selectedItemPosition)?.first.orEmpty()
    }

    private fun button(text: String, primaryStyle: Boolean, colorOverride: Int? = null, action: () -> Unit): Button {
        val fill = colorOverride ?: if (primaryStyle) primary else surfaceAlt
        val textColor = if (primaryStyle || colorOverride != null) Color.WHITE else textPrimary
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 14.4f
            setTextColor(textColor)
            setPadding(dp(18), dp(8), dp(18), dp(8))
            background = rect(fill, dp(6), if (primaryStyle || colorOverride != null) fill else border, dp(1))
            setOnClickListener { action() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stateListAnimator = null
                elevation = dp(2).toFloat()
            }
        }
    }

    private fun smallButton(text: String, primaryStyle: Boolean, colorOverride: Int? = null, action: () -> Unit): Button {
        return button(text, primaryStyle, colorOverride, action).apply {
            textSize = 12.8f
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }
    }

    private fun navButton(parent: LinearLayout, text: String, active: Boolean, action: () -> Unit) {
        parent.addView(smallButton(text, active, null, action), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            rightMargin = dp(7)
        })
    }

    private fun textLink(text: String, action: () -> Unit): TextView {
        return label(text, 14f, primary, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            setOnClickListener { action() }
        }
    }

    private fun statusBadge(text: String, color: Int): TextView {
        return label(text.ifBlank { "—" }, 12f, color, Typeface.BOLD).apply {
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = rect(lighten(color), dp(999))
        }
    }

    private fun divider(): View {
        return View(this).apply {
            setBackgroundColor(border)
            layoutParams = matchWrap(top = dp(12), bottom = dp(8)).apply { height = 1 }
        }
    }

    private fun row(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun space(width: Int, height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(width), dp(height))
        }
    }

    private fun rect(fill: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (stroke != null && strokeWidth > 0) setStroke(strokeWidth, stroke)
        }
    }

    private fun matchWrap(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = top
            bottomMargin = bottom
        }
    }

    private fun addViewSafe(view: View, top: Int = 0, bottom: Int = 0) {
        content.addView(view, matchWrap(top, bottom))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun confirm(message: String, yes: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setNegativeButton(t("cancel"), null)
            .setPositiveButton(t("confirm")) { _, _ -> yes() }
            .show()
    }

    private fun role(): String = user?.clean("role").orEmpty()

    private fun eventStatus(event: JSONObject): String {
        return try {
            val today = LocalDate.now()
            val start = LocalDate.parse(event.clean("start_date"))
            val end = LocalDate.parse(event.clean("end_date"))
            when {
                today.isBefore(start) -> t("upcoming")
                today.isAfter(end) -> t("ended")
                else -> t("ongoing")
            }
        } catch (_: Exception) {
            t("upcoming")
        }
    }

    private fun eventStatusColor(event: JSONObject): Int {
        return when (eventStatus(event)) {
            t("ongoing") -> success
            t("ended") -> textMuted
            else -> secondary
        }
    }

    private fun reservationColor(status: String): Int {
        return when (status) {
            "PENDING_PAYMENT" -> warning
            "WAITING_FOR_APPROVAL" -> secondary
            "CONFIRMED" -> success
            "CANCELLED" -> danger
            else -> textMuted
        }
    }

    private fun paymentColor(status: String): Int {
        return when (status) {
            "APPROVED" -> success
            "PENDING" -> warning
            "REJECTED" -> danger
            else -> textMuted
        }
    }

    private fun approvalColor(status: String): Int {
        return when (status) {
            "APPROVED" -> success
            "REJECTED" -> danger
            "PENDING" -> warning
            else -> textMuted
        }
    }

    private fun lighten(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.rgb((r + (255 - r) * 0.86).toInt(), (g + (255 - g) * 0.86).toInt(), (b + (255 - b) * 0.86).toInt())
    }

    private fun formatMoney(value: Double): String = currency.format(value)

    private fun safeFileName(value: String): String {
        return value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9ก-๙]+"), "_")
            .trim('_')
            .ifBlank { "event" }
    }

    private fun JSONObject.clean(key: String): String {
        if (!has(key) || isNull(key)) return ""
        return optString(key, "")
    }

    private fun JSONObject.optBooleanLike(key: String): Boolean {
        if (!has(key) || isNull(key)) return false
        val value = opt(key)
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value == "true" || value == "1"
            else -> false
        }
    }

    private fun t(key: String): String {
        val map = if (language == "th") th else en
        return map[key] ?: th[key] ?: key
    }

    companion object {
        private const val SLIP_PICK_REQUEST = 8401

        private val th = mapOf(
            "brand" to "Booth Organizer",
            "home" to "หน้าหลัก",
            "events" to "งานอีเวนต์",
            "reservations" to "การจอง",
            "profile" to "โปรไฟล์",
            "create_event" to "สร้างงาน",
            "reports" to "รายงาน",
            "admin" to "แอดมิน",
            "login" to "เข้าสู่ระบบ",
            "register" to "สมัครสมาชิก",
            "logout" to "ออกจากระบบ",
            "home_title" to "🏪 BoothOrganizer",
            "home_subtitle" to "ค้นหางานอีเวนต์ จองบูธ และจัดการธุรกิจของคุณได้ในที่เดียว",
            "browse_events" to "ดูงานอีเวนต์",
            "find_events" to "ค้นหางาน",
            "find_events_desc" to "สำรวจงานอีเวนต์และตลาดที่กำลังจะมาถึง พร้อมข้อมูลบูธว่าง",
            "reserve_booth" to "จองบูธ",
            "reserve_booth_desc" to "ผู้ค้าสามารถสมัครจองบูธได้ทันทีและติดตามสถานะการจอง",
            "easy_payment" to "ชำระเงินง่าย",
            "easy_payment_desc" to "ชำระเงินผ่านบัตรเครดิต TrueMoney หรือโอนเงินพร้อมอัปโหลดสลิป",
            "notifications_feature" to "การแจ้งเตือน",
            "notifications_desc" to "ติดตามการยืนยันการจอง การอนุมัติการชำระเงิน และอื่นๆ",
            "login_title" to "ยินดีต้อนรับกลับ",
            "login_subtitle" to "เข้าสู่ระบบบัญชี BoothOrganizer ของคุณ",
            "username" to "ชื่อผู้ใช้",
            "password" to "รหัสผ่าน",
            "login_submit" to "เข้าสู่ระบบ",
            "go_register" to "ยังไม่มีบัญชี? สมัครที่นี่",
            "go_login" to "มีบัญชีอยู่แล้ว? เข้าสู่ระบบ",
            "register_title" to "สร้างบัญชี",
            "register_subtitle" to "สมัคร BoothOrganizer เพื่อค้นหาและจองบูธ",
            "full_name" to "ชื่อ-นามสกุล",
            "contact" to "ข้อมูลติดต่อ",
            "account_type" to "ประเภทบัญชี",
            "general_user" to "ผู้ใช้ทั่วไป",
            "merchant" to "ผู้ค้า",
            "citizen_id" to "เลขบัตรประชาชน",
            "seller_info" to "ข้อมูลผู้ขาย",
            "product_desc" to "รายละเอียดสินค้า",
            "register_submit" to "สร้างบัญชี",
            "register_success" to "สมัครสำเร็จ กรุณาเข้าสู่ระบบ",
            "events_title" to "🎪 งานอีเวนต์",
            "no_events" to "ยังไม่มีงานอีเวนต์",
            "upcoming" to "กำลังจะมา",
            "ongoing" to "กำลังดำเนินการ",
            "ended" to "สิ้นสุดแล้ว",
            "view_booths" to "ดูบูธ",
            "edit" to "แก้ไข",
            "delete" to "ลบ",
            "delete_event_confirm" to "ลบงานนี้และบูธ/การจองที่เกี่ยวข้องทั้งหมด?",
            "save" to "บันทึก",
            "cancel" to "ยกเลิก",
            "confirm" to "ยืนยัน",
            "edit_event" to "แก้ไขงาน",
            "create_event_title" to "🎪 สร้างงาน",
            "event_name" to "ชื่องาน",
            "description" to "รายละเอียด",
            "location" to "สถานที่",
            "start_date" to "วันเริ่ม",
            "end_date" to "วันสิ้นสุด",
            "create_event_success" to "สร้างงานแล้ว",
            "booths_title" to "🏪 บูธ",
            "choose_event_first" to "กรุณาเลือกงานจากหน้างานอีเวนต์ก่อน",
            "no_booths" to "ยังไม่มีบูธที่เพิ่มในงานนี้",
            "add_booth" to "เพิ่มบูธ",
            "edit_booth" to "แก้ไขบูธ",
            "booth_number" to "หมายเลขบูธ",
            "size" to "ขนาด",
            "price" to "ราคา (บาท)",
            "type" to "ประเภท",
            "duration" to "ระยะเวลา",
            "classification" to "ประเภทบูธ",
            "indoor" to "🏠 ในร่ม",
            "outdoor" to "🌳 กลางแจ้ง",
            "indoor_plain" to "ในร่ม",
            "outdoor_plain" to "กลางแจ้ง",
            "short_term" to "ระยะสั้น",
            "long_term" to "ระยะยาว",
            "fixed" to "ถาวร",
            "temporary" to "ชั่วคราว",
            "electricity" to "ไฟฟ้า",
            "outlets" to "เต้าเสียบ",
            "water" to "น้ำ",
            "reserve" to "จอง",
            "reserve_confirm" to "จองบูธนี้?",
            "reserve_success" to "สร้างการจองแล้ว (รอชำระเงิน)",
            "delete_booth_confirm" to "ลบบูธนี้?",
            "reservations_title" to "📋 การจองของคุณ",
            "no_reservations" to "ไม่พบการจอง",
            "booth" to "บูธ",
            "payment" to "การชำระเงิน",
            "amount" to "จำนวนเงิน",
            "payment_method" to "วิธีชำระเงิน",
            "credit_card" to "บัตรเครดิต",
            "truemoney" to "TrueMoney Wallet",
            "bank_transfer" to "โอนเงินผ่านธนาคาร",
            "submit_payment" to "ส่งการชำระเงิน",
            "cancel_reservation" to "ยกเลิก",
            "cancel_reservation_confirm" to "ยกเลิกการจองนี้?",
            "payment_created_choose_slip" to "สร้างการชำระเงินแล้ว กรุณาเลือกไฟล์สลิป",
            "payment_submitted" to "ส่งการชำระเงินแล้ว กรุณารอตรวจสอบ",
            "slip_not_uploaded" to "ยังไม่ได้อัปโหลดสลิป",
            "upload_slip" to "อัปโหลดสลิป",
            "slip_uploaded" to "อัปโหลดสลิปแล้ว",
            "approve_payment" to "อนุมัติการชำระเงิน",
            "approve_payment_confirm" to "อนุมัติการชำระเงินนี้และยืนยันบูธ?",
            "payment_approved" to "อนุมัติการชำระเงินแล้ว",
            "slip_required" to "ต้องมีสลิปก่อนอนุมัติ",
            "cancel_success" to "ยกเลิกการจองแล้ว",
            "admin_title" to "🛠️ แดชบอร์ดแอดมิน",
            "approve_merchants" to "อนุมัติผู้ค้า",
            "approve_merchants_desc" to "ตรวจสอบใบสมัครผู้ค้าที่รอดำเนินการและอัปเดตสถานะ",
            "review_payments" to "ตรวจสอบการชำระเงิน",
            "review_payments_desc" to "ตรวจสอบและอนุมัติการชำระเงินที่รอดำเนินการจากผู้ค้า",
            "view_reports" to "ดูรายงาน",
            "view_reports_desc" to "สร้างรายงานการจองและการชำระเงินตามงาน พร้อมส่งออก CSV",
            "merchant_approval_title" to "👥 การอนุมัติผู้ค้า",
            "no_users" to "ไม่พบผู้ใช้",
            "moi_validation" to "การตรวจสอบ MOI",
            "valid" to "ถูกต้อง",
            "invalid" to "ไม่ถูกต้อง",
            "status" to "สถานะ",
            "save_status" to "บันทึกสถานะ",
            "status_saved" to "บันทึกสถานะแล้ว",
            "payment_approval_title" to "✅ การอนุมัติการชำระเงิน",
            "no_pending_payments" to "ไม่มีการชำระเงินที่รอดำเนินการ",
            "reservation_id" to "รหัสการจอง",
            "missing_slip" to "ไม่มีสลิป",
            "view_slip" to "ดูสลิป",
            "validated" to "ตรวจสอบแล้ว",
            "approve" to "อนุมัติ",
            "profile_title" to "👤 โปรไฟล์ของฉัน",
            "role" to "บทบาท",
            "save_profile" to "บันทึกโปรไฟล์",
            "profile_saved" to "บันทึกโปรไฟล์แล้ว",
            "save_seller" to "บันทึกข้อมูลผู้ขาย",
            "seller_saved" to "บันทึกข้อมูลผู้ขายแล้ว",
            "reports_title" to "📊 รายงาน",
            "select_event" to "เลือกงาน",
            "generate_report" to "สร้างรายงาน",
            "download_csv" to "ดาวน์โหลด CSV",
            "csv_downloaded" to "ดาวน์โหลดแล้ว",
            "reports_hint" to "เลือกงานแล้วกดสร้างรายงานเพื่อดูข้อมูลการจองและการชำระเงิน",
            "total_reservations" to "การจองทั้งหมด",
            "confirmed" to "ยืนยันแล้ว",
            "pending" to "รอดำเนินการ",
            "total_revenue" to "รายได้รวม",
            "reservation_details" to "รายละเอียดการจอง",
            "no_report_rows" to "ไม่พบการจองสำหรับงานนี้",
            "notifications_title" to "🔔 การแจ้งเตือน",
            "no_notifications" to "ยังไม่มีการแจ้งเตือน",
            "unread" to "ยังไม่ได้อ่าน",
            "mark_read" to "ทำเครื่องหมายว่าอ่านแล้ว",
            "login_success" to "เข้าสู่ระบบสำเร็จ",
            "login_failed" to "เข้าสู่ระบบล้มเหลว",
            "missing_credentials" to "กรุณากรอกชื่อผู้ใช้และรหัสผ่าน",
            "forbidden" to "ไม่มีสิทธิ์เข้าหน้านี้",
            "unexpected_error" to "เกิดข้อผิดพลาด"
        )

        private val en = mapOf(
            "home" to "Home",
            "events" to "Events",
            "reservations" to "Reservations",
            "profile" to "Profile",
            "create_event" to "Create Event",
            "reports" to "Reports",
            "admin" to "Admin",
            "login" to "Login",
            "register" to "Register",
            "logout" to "Logout",
            "home_title" to "🏪 BoothOrganizer",
            "home_subtitle" to "Discover events, reserve booths, and manage your merchant business in one place.",
            "browse_events" to "Browse Events",
            "find_events" to "Find Events",
            "find_events_desc" to "Explore upcoming events and markets with booth availability.",
            "reserve_booth" to "Reserve a Booth",
            "reserve_booth_desc" to "Merchants can reserve booths and track reservation status.",
            "easy_payment" to "Easy Payment",
            "easy_payment_desc" to "Pay by credit card, TrueMoney, or bank transfer with slip upload.",
            "notifications_feature" to "Notifications",
            "notifications_desc" to "Track reservation confirmations and payment approvals.",
            "login_title" to "Welcome back",
            "login_subtitle" to "Sign in to your BoothOrganizer account",
            "username" to "Username",
            "password" to "Password",
            "login_submit" to "Sign In",
            "go_register" to "No account? Register here",
            "go_login" to "Already have an account? Sign in",
            "register_title" to "Create an account",
            "register_subtitle" to "Join BoothOrganizer to discover and reserve booths",
            "full_name" to "Full Name",
            "contact" to "Contact Info",
            "account_type" to "Account Type",
            "general_user" to "General User",
            "merchant" to "Merchant",
            "citizen_id" to "Citizen ID",
            "seller_info" to "Seller Information",
            "product_desc" to "Product Description",
            "register_submit" to "Create Account",
            "register_success" to "Registered successfully, please log in",
            "events_title" to "🎪 Events",
            "no_events" to "No events yet",
            "upcoming" to "Upcoming",
            "ongoing" to "Ongoing",
            "ended" to "Ended",
            "view_booths" to "View Booths",
            "edit" to "Edit",
            "delete" to "Delete",
            "delete_event_confirm" to "Delete this event and all related booths/reservations?",
            "save" to "Save",
            "cancel" to "Cancel",
            "confirm" to "Confirm",
            "edit_event" to "Edit Event",
            "create_event_title" to "🎪 Create Event",
            "event_name" to "Event Name",
            "description" to "Description",
            "location" to "Location",
            "start_date" to "Start Date",
            "end_date" to "End Date",
            "create_event_success" to "Event created",
            "booths_title" to "🏪 Booths",
            "choose_event_first" to "Please choose an event first",
            "no_booths" to "No booths have been added to this event yet",
            "add_booth" to "Add Booth",
            "edit_booth" to "Edit Booth",
            "booth_number" to "Booth Number",
            "size" to "Size",
            "price" to "Price (THB)",
            "type" to "Type",
            "duration" to "Duration",
            "classification" to "Classification",
            "indoor" to "🏠 Indoor",
            "outdoor" to "🌳 Outdoor",
            "indoor_plain" to "Indoor",
            "outdoor_plain" to "Outdoor",
            "short_term" to "Short-term",
            "long_term" to "Long-term",
            "fixed" to "Fixed",
            "temporary" to "Temporary",
            "electricity" to "Electricity",
            "outlets" to "outlets",
            "water" to "Water",
            "reserve" to "Reserve",
            "reserve_confirm" to "Reserve this booth?",
            "reserve_success" to "Reservation created (pending payment)",
            "delete_booth_confirm" to "Delete this booth?",
            "reservations_title" to "📋 Your Reservations",
            "no_reservations" to "No reservations found",
            "booth" to "Booth",
            "payment" to "Payment",
            "amount" to "Amount",
            "payment_method" to "Payment Method",
            "credit_card" to "Credit Card",
            "truemoney" to "TrueMoney Wallet",
            "bank_transfer" to "Bank Transfer",
            "submit_payment" to "Submit Payment",
            "cancel_reservation" to "Cancel",
            "cancel_reservation_confirm" to "Cancel this reservation?",
            "payment_created_choose_slip" to "Payment created. Choose a slip file.",
            "payment_submitted" to "Payment submitted. Please wait for review.",
            "slip_not_uploaded" to "Slip not uploaded yet",
            "upload_slip" to "Upload Slip",
            "slip_uploaded" to "Slip uploaded",
            "approve_payment" to "Approve Payment",
            "approve_payment_confirm" to "Approve this payment and confirm the booth?",
            "payment_approved" to "Payment approved",
            "slip_required" to "Slip is required before approval",
            "cancel_success" to "Reservation cancelled",
            "admin_title" to "🛠️ Admin Dashboard",
            "approve_merchants" to "Approve Merchants",
            "approve_merchants_desc" to "Review pending merchant applications and update status.",
            "review_payments" to "Review Payments",
            "review_payments_desc" to "Verify and approve pending payment submissions.",
            "view_reports" to "View Reports",
            "view_reports_desc" to "Generate event-based reports and export CSV.",
            "merchant_approval_title" to "👥 Merchant Approvals",
            "no_users" to "No users found",
            "moi_validation" to "MOI Validation",
            "valid" to "Valid",
            "invalid" to "Invalid",
            "status" to "Status",
            "save_status" to "Save Status",
            "status_saved" to "Status saved",
            "payment_approval_title" to "✅ Payment Approvals",
            "no_pending_payments" to "No pending payments",
            "reservation_id" to "Reservation ID",
            "missing_slip" to "Missing slip",
            "view_slip" to "View Slip",
            "validated" to "Validated",
            "approve" to "Approve",
            "profile_title" to "👤 My Profile",
            "role" to "Role",
            "save_profile" to "Save Profile",
            "profile_saved" to "Profile saved",
            "save_seller" to "Save Seller Info",
            "seller_saved" to "Seller info saved",
            "reports_title" to "📊 Reports",
            "select_event" to "Select Event",
            "generate_report" to "Generate Report",
            "download_csv" to "Download CSV",
            "csv_downloaded" to "Downloaded",
            "reports_hint" to "Select an event and generate a report.",
            "total_reservations" to "Total Reservations",
            "confirmed" to "Confirmed",
            "pending" to "Pending",
            "total_revenue" to "Total Revenue",
            "reservation_details" to "Reservation Details",
            "no_report_rows" to "No reservations found for this event",
            "notifications_title" to "🔔 Notifications",
            "no_notifications" to "No notifications yet",
            "unread" to "Unread",
            "mark_read" to "Mark read",
            "login_success" to "Login successful",
            "login_failed" to "Login failed",
            "missing_credentials" to "Username and password are required",
            "forbidden" to "You do not have access to this page",
            "unexpected_error" to "Unexpected error"
        )
    }
}
