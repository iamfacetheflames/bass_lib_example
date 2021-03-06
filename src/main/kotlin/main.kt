
import jouvieje.bass.Bass
import jouvieje.bass.BassInit
import jouvieje.bass.defines.BASS_SAMPLE
import jouvieje.bass.exceptions.BassException
import jouvieje.bass.structures.HSAMPLE
import java.io.File
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
    fun getPosition(): Double
    fun getDuration(): Double
    fun isSuccessfullyPlayed(): Boolean
    fun setListener(listener: Listener)

    interface Listener {
        fun onError(message: String)
    }

}

class BassPlayer: Player {

    private var isInitLibrary: Boolean = false
    private var channel: HCHANNEL? = null
    private var handle: HSAMPLE? = null
    private var currentListener: Player.Listener? = null
    private var isPlaying: Boolean = false

    private fun error(message: String) {
        currentListener?.onError(message)
    }

    private fun initLibrary() {
        try {
            BassInit.loadLibraries()
            if (BassInit.NATIVEBASS_LIBRARY_VERSION() != BassInit.NATIVEBASS_JAR_VERSION()) {
                error(
                    "Error!  NativeBass library version (%08x) is different to jar version (%08x)\n" +
                            "${
                                BassInit.NATIVEBASS_LIBRARY_VERSION()
                            } \n" +
                            "${
                                BassInit.NATIVEBASS_JAR_VERSION()
                            }"
                )
            }
            if (Bass.BASS_GetVersion() and -0x10000 shr 16 != BassInit.BASSVERSION()) {
                error("An incorrect version of BASS lib was loaded")
            } else {
                isInitLibrary = true
            }
        } catch (e: BassException) {
            error("NativeBass error! %s\n ${e.message}")
        }
    }

    private fun channelIsActive(): Boolean {
        return channel?.let { channel ->
            Bass.BASS_ChannelIsActive(channel.asInt()) == 1
        } ?: false
    }

    override fun isSuccessfullyPlayed(): Boolean {
        return if (
            isInitLibrary &&
            isPlaying &&
            !channelIsActive()
        ) {
            isPlaying = false
            true
        } else {
            false
        }
    }

    override fun init() {
        initLibrary()
        if (isInitLibrary) {
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
                isInitLibrary = false
            } else {
                isInitLibrary = true
            }
        } else {
            isInitLibrary = false
        }
    }

    override fun release() {
        stop()
        isInitLibrary = false
        handle = null
        channel = null
        Bass.BASS_Free()
    }

    override fun open(path: String) {
        if (!isInitLibrary) {
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
            } else {
                isPlaying = true
            }
        }
    }

    override fun pause() {
        channel?.let { channel ->
            Bass.BASS_ChannelPause(channel.asInt())
            isPlaying = false
        }
    }

    override fun stop() {
        handle?.let { handle ->
            Bass.BASS_SampleFree(handle)
            isPlaying = false
        }
    }

    override fun seek(seconds: Double) {
        channel?.let { channel ->
            val prebuf = Bass.BASS_ChannelSeconds2Bytes(channel.asInt(), seconds)
            Bass.BASS_ChannelSetPosition(channel.asInt(), prebuf, BASS_POS_BYTE)
        }
    }

    override fun setVolume(value: Float, durationSec: Float) {
        channel?.let { channel ->
            val time = (durationSec * 1000).toInt()
            Bass.BASS_ChannelSlideAttribute(channel.asInt(), BASS_ATTRIB_VOL, value, time)
        }
    }

    override fun getPosition(): Double {
        channel?.let { channel ->
            val positionBytes = Bass.BASS_ChannelGetPosition(channel.asInt(), BASS_POS_BYTE)
            return Bass.BASS_ChannelBytes2Seconds(channel.asInt(), positionBytes)
        }
        return -1.0
    }

    override fun getDuration(): Double {
        channel?.let { channel ->
            val durationBytes = Bass.BASS_ChannelGetLength(channel.asInt(), BASS_POS_BYTE)
            return Bass.BASS_ChannelBytes2Seconds(channel.asInt(), durationBytes)
        }
        return -1.0
    }

    override fun setListener(listener: Player.Listener) {
        currentListener = listener
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
            "position" -> println(
                "current position ${player.getPosition()} seconds"
            )
            "duration" -> println(
                "file duration ${player.getDuration()} seconds"
            )
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