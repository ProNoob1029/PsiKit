package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.limelightvision.LLFieldMap
import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.LLResultTypes.CalibrationResult
import com.qualcomm.hardware.limelightvision.LLStatus
import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.hardware.limelightvision.PsiKitLimelightJsonFactory.resultFromJson
import com.qualcomm.hardware.limelightvision.PsiKitLimelightJsonFactory.statusFromJson
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.firstinspires.ftc.robotcore.internal.usb.EthernetOverUsbSerialNumber
import org.json.JSONArray
import org.json.JSONObject
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.wpi.math.Pose2d
import org.psilynx.psikit.core.wpi.math.Pose3d
import org.psilynx.psikit.core.wpi.math.Rotation2d
import org.psilynx.psikit.ftc.loggableField
import org.psilynx.psikit.ftc.loggableEnumField
import org.psilynx.psikit.ftc.loggableStringField
import org.psilynx.psikit.ftc.loggableIntField
import org.psilynx.psikit.ftc.loggableBooleanField
import java.net.InetAddress

class Limelight3AWrapper(
    private val device: Limelight3A?,
    name: String = ""
) : Limelight3A(
    EthernetOverUsbSerialNumber.fromIpAddress("127.0.0.1", "psikit"),
    device?.deviceName ?: "MockLimelight3A",
    InetAddress.getLoopbackAddress()
), HardwareInput<Limelight3A> {
    override var readTime = 0.0
    override var wrieTime = 0.0
    override val cacheResets = mutableListOf<() -> Unit>()
    override val hardwareName = name

    private val _latestResult by loggableField(
        device?.let { it::getLatestResult },
        resultFromJson("", 0L),
        { table, value: LLResult, name ->
            val subTable = table.getSubtable(name)
            val jsonString = value.toString()
            subTable.put("json", jsonString)
            subTable.put("controlHubTimeStamp", LogTable.LogValue(value.controlHubTimeStamp, "ms"))

            putDerivedResultFields(table, jsonString)
        },
        { table, name ->
            val subTable = table.getSubtable(name)
            resultFromJson(
                subTable.get("json", ""),
                subTable.get("controlHubTimeStamp", 0L)
            ) ?: resultFromJson("", 0L)
        }
    )

    private val _status by loggableField(
        device?.let { it::getStatus },
        LLStatus(),
        { table, value: LLStatus, name ->
            val subTable = table.getSubtable(name)
            val jsonString = value.toJsonString()
            subTable.put("json", jsonString)
        },
        { table, name ->
            val subTable = table.getSubtable(name)
            statusFromJson(
                subTable.get("json", ""),
            ) ?: LLStatus()
        }
    )

    private val running by loggableBooleanField(device?.let { it::isRunning })
    private val connected by loggableBooleanField(device?.let { it::isConnected })

    private val _connectionInfo by loggableStringField(device?.let { it::getConnectionInfo })
    private val _manufacturer by loggableEnumField(device?.let { it::getManufacturer }, HardwareDevice.Manufacturer.Other)
    private val _deviceName by loggableStringField(device?.let { it::getDeviceName })
    private val _version by loggableIntField(device?.let { it::getVersion })

    override fun new(wrapped: Limelight3A?, name: String) = Limelight3AWrapper(wrapped, name)

    private fun putPose2dAnd3dFromJsonPose6(table: LogTable, keyPrefix: String, arr: JSONArray?) {
        if (arr == null || arr.length() < 6) return
        val x = arr.optDouble(0, 0.0)
        val y = arr.optDouble(1, 0.0)
        val yawDeg = arr.optDouble(5, 0.0)
        val pose2d = Pose2d(x, y, Rotation2d.fromDegrees(yawDeg))
        table.put("${keyPrefix}Pose2d", pose2d)
        table.put("${keyPrefix}Pose3d", Pose3d(pose2d))
    }

    private fun putDerivedResultFields(table: LogTable, resultJson: String) {
        val t = table.getSubtable("result")
        val field = table.getSubtable("field")

        if (resultJson.isBlank()) {
            t.put("valid", false)
            t.put("pipelineIndex", 0)
            t.put("tx", 0.0)
            t.put("ty", 0.0)
            t.put("fiducialCount", 0)
            t.put("fiducialId0", -1)

            // Field-friendly poses (structs)
            field.put("botPose2d", Pose2d.kZero)
            field.put("botPose3d", Pose3d.kZero)
            field.put("wpiBluePose2d", Pose2d.kZero)
            field.put("wpiBluePose3d", Pose3d.kZero)
            field.put("wpiRedPose2d", Pose2d.kZero)
            field.put("wpiRedPose3d", Pose3d.kZero)
            return
        }

        try {
            val obj = JSONObject(resultJson)
            val v = obj.optInt("v", 0)
            t.put("valid", v != 0)

            t.put("pipelineIndex", obj.optInt("pID", 0))
            t.put("tx", obj.optDouble("tx", 0.0))
            t.put("ty", obj.optDouble("ty", 0.0))

            val fid = obj.optJSONArray("Fiducial")
            val fidCount = fid?.length() ?: 0
            t.put("fiducialCount", fidCount)
            val fid0 = if (fidCount > 0) fid?.optJSONObject(0) else null
            t.put("fiducialId0", fid0?.optInt("fID", -1) ?: -1)

            // AdvantageScope field widgets can consume Pose2d/Pose3d structs directly.
            putPose2dAnd3dFromJsonPose6(field, "bot", obj.optJSONArray("botpose"))
            putPose2dAnd3dFromJsonPose6(field, "wpiBlue", obj.optJSONArray("botpose_wpiblue"))
            putPose2dAnd3dFromJsonPose6(field, "wpiRed", obj.optJSONArray("botpose_wpired"))
        } catch (_: Throwable) {
            // Keep logging resilient; if parsing fails we still have raw resultJson.
        }
    }

    private fun LLStatus.toJsonString(): String {
        return try {
            val q = cameraQuat
            JSONObject()
                .put(
                    "cameraQuat",
                    JSONObject()
                        .put("w", q.w)
                        .put("x", q.x)
                        .put("y", q.y)
                        .put("z", q.z)
                )
                .put("cid", cid)
                .put("cpu", cpu)
                .put("finalYaw", finalYaw)
                .put("fps", fps)
                .put("hwType", hwType)
                .put("name", name)
                .put("pipeImgCount", pipeImgCount)
                .put("pipelineIndex", pipelineIndex)
                .put("pipelineType", pipelineType)
                .put("ram", ram)
                .put("snapshotMode", snapshotMode)
                .put("temp", temp)
                .toString()
        } catch (_: Throwable) {
            ""
        }
    }

    // --- Limelight API overrides ---

    override fun start() {
        if (Logger.isReplay()) return
        try {
            device?.start()
//            cachedRunning = device?.isRunning ?: cachedRunning
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun pause() {
        if (Logger.isReplay()) return
        try {
            device?.pause()
//            cachedRunning = device?.isRunning ?: cachedRunning
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun stop() {
        if (Logger.isReplay()) return
        try {
            device?.stop()
//            cachedRunning = device?.isRunning ?: cachedRunning
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun isRunning() = running

    override fun setPollRateHz(rateHz: Int) {
        if (Logger.isReplay()) return
        try {
            device?.setPollRateHz(rateHz)
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun getTimeSinceLastUpdate(): Long {
        if (Logger.isReplay()) return 0L
        return try {
            device?.timeSinceLastUpdate ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    override fun isConnected() = connected

    override fun getLatestResult(): LLResult = _latestResult

    override fun getStatus(): LLStatus = _status

    override fun reloadPipeline(): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.reloadPipeline() ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun pipelineSwitch(index: Int): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.pipelineSwitch(index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun captureSnapshot(snapname: String): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.captureSnapshot(snapname) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun deleteSnapshots(): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.deleteSnapshots() ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun deleteSnapshot(snapname: String): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.deleteSnapshot(snapname) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun updatePythonInputs(
        input1: Double,
        input2: Double,
        input3: Double,
        input4: Double,
        input5: Double,
        input6: Double,
        input7: Double,
        input8: Double
    ): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.updatePythonInputs(input1, input2, input3, input4, input5, input6, input7, input8) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun updatePythonInputs(inputs: DoubleArray): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.updatePythonInputs(inputs) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun updateRobotOrientation(yaw: Double): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.updateRobotOrientation(yaw) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun uploadPipeline(jsonString: String, index: Int?): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.uploadPipeline(jsonString, index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun uploadFieldmap(fieldmap: LLFieldMap, index: Int?): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.uploadFieldmap(fieldmap, index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun uploadPython(pythonString: String, index: Int?): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.uploadPython(pythonString, index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun getCalDefault(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calDefault ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun getCalFile(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calFile ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun getCalEEPROM(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calEEPROM ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun getCalLatest(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calLatest ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun shutdown() {
        if (Logger.isReplay()) return
        try {
            device?.shutdown()
        } catch (_: Throwable) {
            // ignore
        }
    }

    // --- HardwareDevice overrides ---

    override fun getManufacturer(): HardwareDevice.Manufacturer = _manufacturer

    override fun getDeviceName() = _deviceName

    override fun getConnectionInfo() = _connectionInfo

    override fun getVersion() = _version

    override fun resetDeviceConfigurationForOpMode() {
        if (Logger.isReplay()) return
        try {
            device?.resetDeviceConfigurationForOpMode()
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun close() {
        if (Logger.isReplay()) return
        try {
            device?.close()
        } catch (_: Throwable) {
            // ignore
        }
    }
}
