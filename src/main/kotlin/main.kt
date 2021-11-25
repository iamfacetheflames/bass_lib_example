
import jouvieje.bass.Bass
import jouvieje.bass.BassInit
import jouvieje.bass.defines.BASS_SAMPLE
import jouvieje.bass.exceptions.BassException
import jouvieje.bass.structures.HSAMPLE
import java.io.File
import jouvieje.bass.Bass.BASS_ChannelSeconds2Bytes
import jouvieje.bass.defines.BASS_ATTRIB.BASS_ATTRIB_VOL
import jouvieje.bass.defines.BASS_POS.BASS_POS_BYTE
import jouvieje.bass.structures.HCHANNEL

interface Player {

    fun init()
    fun release()
    fun open(path: String)
    fun play()
    fun pause()
    fun stop()
    fun seek(seconds: Double)
    fun setVolume(value: Float, durationSec: Float)

    interface Listener {
        fun onError(message: String)
    }

}

class BassPlayer: Player {

    private var isInit: Boolean = false
    private var channel: HCHANNEL? = null
    private var handle: HSAMPLE? = null

    override fun init() {
        initLibrary()
        if (isInit) {
            /* Initialize default output device */
            if (
                !Bass.BASS_Init(
                    -1,
                    44100,
                    0,
                    null,
                    null
                )
            ) {
                error("Can't initialize device")
                isInit = false
            } else {
                isInit = true
            }
        } else {
            // todo show error
            isInit = false
        }
    }

    override fun release() {
        stop()
        isInit = false
        handle = null
        channel = null
    }

    override fun open(path: String) {
        if (!isInit) {
            init()
        }
        val file = File(path)
        handle = Bass.BASS_SampleLoad(false, file.absolutePath, 0, 0, 3, BASS_SAMPLE.BASS_SAMPLE_OVER_POS)
        channel = Bass.BASS_SampleGetChannel(handle, false)
    }

    override fun play() {
        channel?.let { channel ->
            if (!Bass.BASS_ChannelPlay(channel.asInt(), false)) {
                error("Can't play sample")
                // todo save error
            }
        }
    }

    override fun pause() {
        channel?.let { channel ->
            Bass.BASS_ChannelPause(channel.asInt())
        }
    }

    override fun stop() {
        handle?.let { handle ->
            Bass.BASS_SampleFree(handle)
        }
    }

    override fun seek(seconds: Double) {
        channel?.let { channel ->
            val prebuf = BASS_ChannelSeconds2Bytes(channel.asInt(), seconds)
            Bass.BASS_ChannelSetPosition(channel.asInt(), prebuf, BASS_POS_BYTE)
        }
    }

    override fun setVolume(value: Float, durationSec: Float) {
        channel?.let { channel ->
            val time = (durationSec * 1000).toInt()
            Bass.BASS_ChannelSlideAttribute(channel.asInt(), BASS_ATTRIB_VOL, value, time)
        }
    }

    private fun initLibrary() {
        try {
            BassInit.loadLibraries()
            if (BassInit.NATIVEBASS_LIBRARY_VERSION() != BassInit.NATIVEBASS_JAR_VERSION()) {
                println(
                    "Error!  NativeBass library version (%08x) is different to jar version (%08x)\n" +
                            "${
                                BassInit.NATIVEBASS_LIBRARY_VERSION()
                            } \n" +
                            "${
                                BassInit.NATIVEBASS_JAR_VERSION()
                            }"
                )
            }
            // check the correct BASS was loaded
            if (Bass.BASS_GetVersion() and -0x10000 shr 16 != BassInit.BASSVERSION()) {
                println("An incorrect version of BASS.DLL was loaded")
            } else {
                isInit = true
            }
        } catch (e: BassException) {
            println("NativeBass error! %s\n ${e.message}")
        }
    }

}


fun main(args: Array<String>) {
    println("hello!")
    val player = BassPlayer()
    var currentCommand = ""
    while (currentCommand != "exit") {
        when(currentCommand) {
            "open" -> {
                println("input file path:")
                val path = getCommand()
                player.open(path)
            }
            "play" -> player.play()
            "stop" -> player.stop()
            "pause" -> player.pause()
            "seek" -> {
                println("input seek position (sec):")
                val position = getCommand().toDouble()
                player.seek(position)
            }
            "volume" -> {
                println("input volume value (0..0 .. 1.0):")
                val volume = getCommand().toFloat()
                println("input volume change duration (sec):")
                val duration = getCommand().toFloat()
                player.setVolume(volume, duration)
            }
        }
        println("input command:")
        currentCommand = getCommand()
    }
    player.release()
    println("buy!")
}

fun getCommand(): String {
    return readLine()?.trim() ?: ""
}