package net.ankio.bluetooth.ui


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.bluetooth.BuildConfig
import net.ankio.bluetooth.R
import net.ankio.bluetooth.bluetooth.BleDevice
import net.ankio.bluetooth.databinding.AboutDialogBinding
import net.ankio.bluetooth.databinding.ActivityMainBinding
import net.ankio.bluetooth.service.SendWebdavServer
import net.ankio.bluetooth.utils.BleAdvertiserManager
import net.ankio.bluetooth.utils.ClipboardUtils
import net.ankio.bluetooth.utils.HookUtils
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.WebdavUtils
import rikka.html.text.toHtml
import java.lang.Exception
import java.util.ArrayList


class MainActivity : BaseActivity() {

    //视图绑定
    private lateinit var binding: ActivityMainBinding
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // BLE广播相关
    private var bleEventReceiver: BroadcastReceiver? = null
    private var bleCountdownJob: Job? = null
    private var lastToastMessage = "" // 避免重复Toast
    private var lastToastTime = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tag = "MainActivity"
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbarLayout = binding.toolbarLayout
        toolbar = binding.toolbar
        scrollView = binding.scrollView

        
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.theme -> {
                    start<SettingsActivity>()
                    true
                }
                R.id.more -> {
                    val binding = AboutDialogBinding.inflate(LayoutInflater.from(this), null, false)
                    binding.sourceCode.movementMethod = LinkMovementMethod.getInstance()
                    binding.sourceCode.text = getString(
                        R.string.about_view_source_code,
                        "<b><a href=\"https://github.com/AnkioTomas/bluetooth\n\">GitHub</a></b>"
                    ).toHtml()

                    binding.versionName.text = packageManager.getPackageInfo(packageName, 0).versionName
                    MaterialAlertDialogBuilder(this)
                        .setView(binding.root)
                        .show()

                    true
                }
                else -> false
            }

        }
        binding.search.setOnClickListener {
            start<ScanActivity>()
        }
        onViewCreated()
    }

    /**
     * 判断是否为发送端
     */
    private fun isSender(): Boolean {
        return SpUtils.getBoolean("pref_enable_webdav", false) && SpUtils.getBoolean("pref_as_sender", false)
    }

    /**
     * 尝试去启动服务
     */
    private fun serverConnect(){
        if (isSender()) {
            startServer()
        }else{
            stopServer()
            if(SpUtils.getBoolean("pref_enable_webdav", false)){
               try {
                   syncFromServer()
               }catch (e:Exception){
                  Toast.makeText(this,e.message?:"",Toast.LENGTH_SHORT).show()
               }
            }
        }
        refreshStatus()
    }
    /**
     * 设置插件状态
     */
    private fun setActive(@StringRes text: Int, @AttrRes backgroundColor:Int, @AttrRes textColor:Int, @DrawableRes drawable:Int){
        binding.active.setBackgroundColor(getThemeAttrColor(backgroundColor))
        binding.imageView.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                drawable
            )
        )
        binding.msgLabel.text = getString(text)
        binding.imageView.setColorFilter(getThemeAttrColor(textColor))
        binding.msgLabel.setTextColor(getThemeAttrColor(textColor))
    }

    /**
     * 状态刷新
     */
    private fun refreshStatus(){
        //如果是发送端，判断服务状态
        if (isSender()) {
            Log.i(tag,"isServerRunning => ${SendWebdavServer.isRunning}")
            if(!SendWebdavServer.isRunning){//判断服务是否运行
                setActive(R.string.server_error,com.google.android.material.R.attr.colorErrorContainer,com.google.android.material.R.attr.colorOnErrorContainer, R.drawable.ic_error)
            }else{
                setActive(R.string.server_working,com.google.android.material.R.attr.colorPrimary,com.google.android.material.R.attr.colorOnPrimary,R.drawable.ic_success)
            }
        //其他情况就判断插件是否运行
        }else if (HookUtils.getActiveAndSupportFramework()) {
            if(HookUtils.getXposedVersion() < 93){
                setActive(R.string.active_version,com.google.android.material.R.attr.colorSecondary,com.google.android.material.R.attr.colorOnSecondary,R.drawable.ic_error)
                return
            }

            SpUtils.putInt("app_version",BuildConfig.VERSION_CODE)
            //其他情况就是激活
            setActive(R.string.active_success,com.google.android.material.R.attr.colorPrimary,com.google.android.material.R.attr.colorOnPrimary,R.drawable.ic_success)

        } else {
            //其他情况就是未激活
            setActive(R.string.active_error,com.google.android.material.R.attr.colorErrorContainer,com.google.android.material.R.attr.colorOnErrorContainer, R.drawable.ic_error)
        }
    }
    override fun onResume() {
        super.onResume()

        // 延迟状态刷新，减少频率
        coroutineScope.launch {
            delay(500) // 延迟500ms再刷新状态
            setMacBluetoothData()
            serverConnect()
            refreshStatus()

            // 延迟刷新BLE状态，避免过频繁更新
            delay(200)
            runOnUiThread {
                updateBleAdvertiserStatus(BleAdvertiserManager.ServiceState.isEnabled(this@MainActivity))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理BLE事件接收器
        bleEventReceiver?.let { receiver ->
            BleAdvertiserManager.unregisterEventReceiver(this, receiver)
        }
        bleEventReceiver = null

        
        // 停止倒计时
        stopBleCountdown()
    }

    /**
     * 启动BLE倒计时（6分钟）
     */
    private fun startBleCountdown() {
        stopBleCountdown() // 先停止之前的倒计时

        bleCountdownJob = coroutineScope.launch {
            delay(6 * 60 * 1000L) // 6分钟

            // 6分钟后自动停止
            if (BleAdvertiserManager.ServiceState.isEnabled(this@MainActivity)) {
                Log.i(tag, "6分钟倒计时结束，自动停止BLE广播")
                binding.bleAdvertiserSwitch.isChecked = false
                stopBleAdvertising()
                showToast("外围服务运行时间已到，已自动停止")
            }
        }
    }

    /**
     * 停止BLE倒计时
     */
    private fun stopBleCountdown() {
        bleCountdownJob?.cancel()
        bleCountdownJob = null
    }

    
    /**
     * 启动服务
     */
    private fun startServer(){
        //如果服务没有启动先启动
        if(!SendWebdavServer.isRunning){
            SendWebdavServer.isRunning = true
            Log.i(tag,"bluetooth server start!")
            val intent = Intent(this, SendWebdavServer::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }

    /**
     * 停止服务
     */
    private fun stopServer(){
        if(SendWebdavServer.isRunning){
            Log.i(tag,"bluetooth server stop!")
            val intent = Intent(this, SendWebdavServer::class.java)
            this@MainActivity.stopService( intent)
        }
    }

    /**
     * 从服务端同步数据
     */
    private fun syncFromServer(){
        coroutineScope .launch(Dispatchers.IO) {
            try {
                // 在后台线程中执行网络操作
                val bluetoothData  = WebdavUtils(SpUtils.getString("webdav_username", ""),SpUtils.getString("webdav_password", "")).getFromServer()
                if(bluetoothData!=null){
                    //只更新data，mac地址
                    SpUtils.putString("pref_data",bluetoothData.data)
                    SpUtils.putString("pref_mac",bluetoothData.mac)
                }
                withContext(Dispatchers.Main) {
                    //同步完成数据后，重绘页面
                    setMacBluetoothData()
                }
            }catch (e: SardineException){
                e.message?.let { Log.e(tag, it) }
                withContext(Dispatchers.Main) {
                    showMsg(R.string.webdav_error)
                }
            }
        }
    }

    fun saveToLocal(){
        val historyJson = SpUtils.getString("history", "")
        var localHistoryList = Gson().fromJson(historyJson, object : TypeToken<List<BleDevice>>() {}.type)
                as? MutableList<BleDevice>
        if(localHistoryList==null){
            localHistoryList = ArrayList()
        }
        val pref_data = SpUtils.getString("pref_data", "")
        val pref_mac = SpUtils.getString("pref_mac", "")
        val pref_rssi = SpUtils.getString("pref_rssi", "-50")
        var value = pref_rssi
        if (value == "") value = "-50" // 默认值应该是-50，不是0
        var insert = false
        var saveHistory = ArrayList<BleDevice>()
        localHistoryList.forEach {
            if(it.address == pref_mac){
                it.data = pref_data
                it.rssi = value.toInt()
                saveHistory.add(it)
                insert = true
            }else{
                saveHistory.add(it)
            }
        }
        if(!insert){
            saveHistory.add(BleDevice(pref_data,getString(R.string.manual_increase),value.toInt(),pref_mac,""))
        }
        SpUtils.putString("history",Gson().toJson(saveHistory))
    }

    /**
     * 设置页面
     */
    private fun setMacBluetoothData() {
        //蓝牙数据设置
        SpUtils.getString("pref_mac", "").apply {
            binding.macLabel.setText(this)
            binding.macLabel.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    SpUtils.putString("pref_mac", s.toString())
                    saveToLocal()
                }
            })

        }
        SpUtils.getString("pref_data", "").apply {
            binding.broadcastLabel.setText(this)
            binding.broadcastLabel.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    SpUtils.putString("pref_data", s.toString())
                    saveToLocal()
                }
            })

        }
        SpUtils.getString("pref_rssi", "-50").apply {
            var value = this
            if (value == "") value = "-50"

            val rssiValue = value.toIntOrNull() ?: -50
            val sliderValue = when {
                rssiValue >= 0 -> 100f  // 0 dBm = 100
                rssiValue <= -100 -> 0f  // -100 dBm = 0
                else -> (100 + rssiValue).toFloat() // -50 dBm = 50
            }

            binding.signalLabel.value = sliderValue
            binding.tvRssi.text = "$value dBm"

            binding.signalLabel.addOnChangeListener { _, sliderValue, _ ->
                val newRssi = when {
                    sliderValue >= 100 -> 0
                    sliderValue <= 0 -> -100
                    else -> (sliderValue - 100).toInt()
                }
                binding.tvRssi.text = "$newRssi dBm"
                SpUtils.putString("pref_rssi", newRssi.toString())
                saveToLocal()
            }

        }

        //启用webdav
        SpUtils.getBoolean("pref_enable_webdav", false).apply {
            binding.webdavEnable.isChecked = this
            binding.webdavEnable.setOnCheckedChangeListener { _, isChecked ->
                SpUtils.putBoolean("pref_enable_webdav", isChecked)
                //启用webdav重连服务
                serverConnect()
            }
        }
        //是否作为发送端
        SpUtils.getBoolean("pref_as_sender", false).apply {
            binding.asSender.isChecked = this
            binding.asSender.setOnCheckedChangeListener { _, isChecked ->
                SpUtils.putBoolean("pref_as_sender", isChecked)
                if (isChecked) {
                    binding.enable.visibility = View.GONE
                    SpUtils.putBoolean("pref_enable", false)
                } else {
                    binding.enable.visibility = View.VISIBLE
                }
                serverConnect()
            }

        }
        //是否开启模拟
        SpUtils.getBoolean("pref_enable", false).apply {
            binding.switchButton.isChecked = this
            binding.switchButton.setOnCheckedChangeListener { _, isChecked ->
                SpUtils.putBoolean("pref_enable", isChecked)
            }
        }
        //配置信息
        SpUtils.getString("webdav_server", "https://dav.jianguoyun.com/dav/").apply {
            binding.webdavLabel.setText(this)
            binding.webdavLabel.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    SpUtils.putString("webdav_server", s.toString())
                }
            })

        }
        SpUtils.getString("webdav_username", "").apply {
            binding.usernameLabel.setText(this)
            binding.usernameLabel.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    SpUtils.putString("webdav_username", s.toString())
                }
            })

        }
        SpUtils.getString("webdav_password", "").apply {
            binding.passwordLabel.setText(this)
            binding.passwordLabel.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    SpUtils.putString("webdav_password", s.toString())
                }
            })

        }
        SpUtils.getString("pref_company", "").apply {
            binding.companyLabel.setText(this)
            binding.companyLabel.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    SpUtils.putString("pref_company", s.toString())
                }
            })

        }
        SpUtils.getString("pref_mac2", "").apply {
            binding.mac2Label.setText(this)
            binding.mac2Label.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    SpUtils.putString("pref_mac2", s.toString())
                }
            })

        }

        SpUtils.getString("webdav_last", getString(R.string.webdav_no_sync)).apply {
            binding.lastDate.text = this
        }

        initBleAdvertiser()
    }

    /**
     * 初始化BLE广播器
     */
    private fun initBleAdvertiser() {
        try {
            val compatibility = BleAdvertiserManager.checkCompatibility(this)
            if (!compatibility.isCompatible) {
                Log.w(tag, "设备不支持BLE广播")
                binding.bleAdvertiserPanel.visibility = View.GONE
                return
            }

            setupBleAdvertiserUI()
            registerBleEventReceiver()
            Log.d(tag, "BLE广播器初始化完成")
        } catch (e: Exception) {
            Log.e(tag, "BLE广播器初始化失败", e)
            binding.bleAdvertiserPanel.visibility = View.GONE
        }
    }

    /**
     * 设置BLE广播UI
     */
    private fun setupBleAdvertiserUI() {
        val isEnabled = BleAdvertiserManager.ServiceState.isEnabled(this)

        binding.bleAdvertiserSwitch.setOnCheckedChangeListener { _, isChecked ->
            onBleSwitchChanged(isChecked)
        }

        binding.bleAdvertiserSwitch.setOnCheckedChangeListener(null)
        binding.bleAdvertiserSwitch.isChecked = isEnabled
        binding.bleAdvertiserSwitch.setOnCheckedChangeListener { _, isChecked ->
            onBleSwitchChanged(isChecked)
        }

        updateBleAdvertiserStatus(isEnabled)

        binding.bleAdvertiserPanel.setOnClickListener {
            binding.bleAdvertiserSwitch.isChecked = !binding.bleAdvertiserSwitch.isChecked
        }

        binding.bleAdvertiserPanel.setOnLongClickListener {
            showBleDebugInfo()
            true
        }
    }

    /**
     * 注册BLE事件广播接收器
     */
    private fun registerBleEventReceiver() {
        bleEventReceiver = BleAdvertiserManager.registerEventReceiver(this) { event, message ->
            runOnUiThread {
                handleBleEvent(event, message)
            }
        }
    }

    
    /**
     * 处理BLE事件
     */
    private fun handleBleEvent(event: String, message: String?) {
        Log.d(tag, "处理BLE事件: $event")

        when (event) {
            "STARTED" -> {
                startBleCountdown()
                updateBleAdvertiserStatus(true)
                binding.bleAdvertiserSwitch.isChecked = true
                showToast("BLE广播启动成功（6分钟自动关闭）")
            }
            "STOPPED" -> {
                stopBleCountdown()
                updateBleAdvertiserStatus(false)
                binding.bleAdvertiserSwitch.isChecked = false
            }
            "FAILED", "EXCEPTION", "BLUETOOTH_DISABLED", "DEVICE_NOT_SUPPORTED", "PERMISSION_DENIED" -> {
                stopBleCountdown()
                binding.bleAdvertiserSwitch.isChecked = false
                updateBleAdvertiserStatus(false)
                showToast(message ?: "BLE广播操作失败")
            }
        }
    }

    /**
     * BLE开关变化处理
     */
    private fun onBleSwitchChanged(enable: Boolean) {
        val currentState = BleAdvertiserManager.ServiceState.isEnabled(this)

        if (currentState != enable) {
            Log.d(tag, "BLE开关状态变化: $currentState -> $enable")
            if (enable) {
                startBleAdvertising()
            } else {
                stopBleAdvertising()
            }
        } else {
            Log.d(tag, "BLE开关状态未变化，跳过操作: $enable")
        }
    }

    /**
     * 处理BLE广播开关切换
     */
    private fun handleBleAdvertiserToggle(enable: Boolean) {
        if (enable) {
            startBleAdvertising()
        } else {
            stopBleAdvertising()
        }
    }

    /**
     * 启动BLE广播
     */
    private fun startBleAdvertising() {
        try {
            if (!BleAdvertiserManager.hasRequiredPermissions(this)) {
                requestBleAdvertiserPermissions()
                binding.bleAdvertiserSwitch.isChecked = false
                return
            }

            if (!BleAdvertiserManager.isBluetoothEnabled(this)) {
                BleAdvertiserManager.requestEnableBluetooth(this)
                binding.bleAdvertiserSwitch.isChecked = false
                return
            }
            BleAdvertiserManager.startAdvertising(this)
            Log.i(tag, "BLE广播启动请求已发送")
        } catch (e: Exception) {
            Log.e(tag, "启动BLE广播失败", e)
            showToast("启动失败: ${e.message}")
            binding.bleAdvertiserSwitch.isChecked = false
        }
    }

    /**
     * 停止BLE广播
     */
    private fun stopBleAdvertising() {
        try {
            stopBleCountdown()
            BleAdvertiserManager.stopAdvertising(this)
            Log.i(tag, "BLE广播停止请求已发送")
        } catch (e: Exception) {
            Log.e(tag, "停止BLE广播失败", e)
            showToast("停止失败: ${e.message}")
        }
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        val currentTime = System.currentTimeMillis()

        if (message == lastToastMessage && currentTime - lastToastTime < 3000) {
            Log.d(tag, "跳过重复Toast: $message")
            return
        }

        lastToastMessage = message
        lastToastTime = currentTime

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新BLE广播状态显示
     */
    private fun updateBleAdvertiserStatus(isActive: Boolean) {
        binding.bleAdvertiserStatus.text = if (isActive) {
            getString(R.string.ble_advertiser_active)
        } else {
            getString(R.string.ble_advertiser_inactive)
        }
    }

    /**
     * 请求BLE广播权限
     */
    private fun requestBleAdvertiserPermissions() {
        val missingPermissions = BleAdvertiserManager.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions,
                BleAdvertiserManager.getPermissionRequestCode()
            )
        }
    }

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BleAdvertiserManager.getPermissionRequestCode()) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                binding.bleAdvertiserSwitch.isChecked = true
                handleBleAdvertiserToggle(true)
            } else {
                showMsg("BLE广播权限被拒绝，无法使用此功能")
                binding.bleAdvertiserSwitch.isChecked = false
            }
        }
    }

    /**
     * 处理蓝牙开启结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == BleAdvertiserManager.getBluetoothEnableRequestCode()) {
            if (resultCode == RESULT_OK) {
                binding.bleAdvertiserSwitch.isChecked = true
                handleBleAdvertiserToggle(true)
            } else {
                showMsg("蓝牙开启被拒绝，无法使用BLE广播功能")
                binding.bleAdvertiserSwitch.isChecked = false
            }
        }
    }

    /**
     * 显示BLE调试信息
     */
    private fun showBleDebugInfo() {
        try {
            val debugInfo = buildString {
                appendLine("=== BLE广播调试信息 ===")

                val compatibility = BleAdvertiserManager.checkCompatibility(this@MainActivity)
                appendLine("兼容性: ${if (compatibility.isCompatible) "✅ 通过" else "❌ 失败"}")
                appendLine("兼容性信息: ${compatibility.message}")

                val hasPermissions = BleAdvertiserManager.hasRequiredPermissions(this@MainActivity)
                appendLine("权限状态: ${if (hasPermissions) "✅ 完整" else "❌ 缺失"}")
                if (!hasPermissions) {
                    val missing = BleAdvertiserManager.getMissingPermissions(this@MainActivity)
                    appendLine("缺失权限: ${missing.joinToString(", ")}")
                }

                val bluetoothEnabled = BleAdvertiserManager.isBluetoothEnabled(this@MainActivity)
                appendLine("蓝牙状态: ${if (bluetoothEnabled) "✅ 已开启" else "❌ 已关闭"}")

                val bleSupported = BleAdvertiserManager.isBleAdvertisingSupported(this@MainActivity)
                appendLine("BLE广播支持: ${if (bleSupported) "✅ 支持" else "❌ 不支持"}")

                val isEnabled = SpUtils.getBoolean("pref_ble_advertiser_enabled", false)
                appendLine("广播开关: ${if (isEnabled) "✅ 已开启" else "❌ 已关闭"}")

                val macAddress = SpUtils.getString("pref_mac", "未配置")
                val broadcastData = SpUtils.getString("pref_data", "未配置")
                appendLine("配置MAC: $macAddress")
                appendLine("广播数据长度: ${broadcastData.length/2} 字节")

                appendLine("Android版本: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("设备支持BLE: ${packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)}")
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("BLE广播调试信息")
                .setMessage(debugInfo)
                .setPositiveButton("确定", null)
                .setNeutralButton("复制到剪贴板") { _, _ ->
                    ClipboardUtils.put(this@MainActivity, debugInfo)
                    Toast.makeText(this@MainActivity, "调试信息已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
                .show()

        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "获取调试信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}